import java.text.SimpleDateFormat
import java.util.Locale

import org.apache.spark.{SparkConf, SparkContext}
import com.comall.{firstAccess, regionLog, repairLog}
import org.apache.commons.lang3.StringUtils

import scala.util.Random
import util.control.Breaks._

/**
  * Created by luozhenglin on 2017/9/12.
  */

object analysisAccessLog  {

  def splitIp(ip:String) :String={
    if (ip.contains(","))
      ip.split(",")(0)
    else
      ip
  }
  import com.comall.FillUtil

  def main(args: Array[String]): Unit = {

  // assert(args.length > 1)
    val _from =    args(0).toString // "D:\\access\\logs" //"   "hdfs://10.201.201.205:8020/songshu/songshu/" */
    val _to =     args(1).toString  // "D:\\access\\testsort4.0"    "hdfs://10.201.201.205:8020/songshu/songshu_clean" */

    val conf = new SparkConf()
       .setAppName("analysisAccessLog")//.setMaster("local[2]")
       //.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
       // .set("spark.kryo.registrationRequired", "true")
      //.set("spark.default.parallelism", "225")
     .set("spark.locality.wait", "10")
      .set("spark.shuffle.file.buffer", "512")
      .set("spark.reducer.maxSizeInFlight", "96")
      .set("spark.shuffle.io.maxRetries", "60")
      .set("spark.shuffle.io.retryWait", "60")
      .set("spark.storage.memoryFraction", "0.5")
      .set("spark.shuffle.consolidateFiles", "true")
      .set("spark.shuffle.manager", "hash")

    val sc = SparkContext.getOrCreate(conf)

    val fileRdd= sc.textFile(_from)

    val fillRdd= fileRdd.filter(x=>(FillUtil.fillExtractJson("logTime", x.toString)))

      //过滤uri以特殊格式结尾的
      val  filterUriRdd = fillRdd.filter(line=> {
          val uri = FillUtil.extractFromJson("uri", line).toString.toLowerCase
          val listFormat = List(".jpg", ".png", ".gif", ".js", ".css", ".json", ".ttf")
          var uriFlag = true
          breakable{
            for( x <- listFormat ){
              if(x.endsWith(uri)){
                uriFlag= false
                break
              }else{
                uriFlag= true
              }
            }
          }
          uriFlag
     })

    //过滤掉不正常ip（一天出现上万次）
    val filterIp = filterUriRdd.filter(x=>{
        val ip = FillUtil.extractFromJson("remoteIP", x).toString
        val listIp = List("222.209.70.7","60.1.96.192","101.24.81.98","101.24.82.26",
          "106.114.125.212","106.114.122.242","36.62.33.218","106.114.221.146",
          "106.114.133.68","58.55.181.212","210.6.198.17","111.181.70.227",
          "110.184.242.201","101.24.87.5")
        var ipFlag = true
        breakable{
        for(i <- listIp){
            if(i.equals(ip)){
              ipFlag = false
               break
            }else{
              ipFlag = true
            }
          }
        }
        ipFlag
    })   //.saveAsTextFile(_to)

    val simpleDateFormat = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss",Locale.ENGLISH)

    //拼接ip_time为key，之后排序。将时间字段转换为long类型
    val mapIpTimeRdd= filterIp.map(line=>{
          val time = FillUtil.extractFromJson("logTime",line)
          var keyTime : String = null
          if(!StringUtils.isNumeric(time)){
              keyTime = simpleDateFormat.parse(time).getTime.toString
          }else{
              keyTime = time
          }
          val newLine = FillUtil.replaceJsonField("logTime", keyTime, line)
          val keyIp = splitIp(FillUtil.extractFromJson("remoteIP", line))
          (keyIp+"_"+keyTime,newLine)
      })
    //排序
     val sortByIpTime = mapIpTimeRdd.sortByKey()
    // val mapIpRdd=sortByIpTime .map(line=>( line._1.split("_")(0),line._2.toString ))//.saveAsTextFile("D:\\access\\test_to_002")

    //将数据映射为map。key为ip，如果ip为-，则拼接随机数打散
     val maptoIpRdd = sortByIpTime.map(
          line=>{
            val value = line._2.toString
            val keyIp = line._1.split("_")(0)
             var key = ""
             if("-".equals(keyIp)){
               key = keyIp + "_" + new Random().nextInt(10000).toString
             }else{
               key = keyIp
             }
            (key,value)
        })
    //按ip分组
     val groupIpRdd = maptoIpRdd.groupByKey()
     //修复分组后的数据，
     val listRdd =groupIpRdd.map(x => { repairLog.repairJson(x._1,x._2)} ).flatMap(iter=>{iter.toList})

     //映射为unique为key的map，若key为-则拼接随机数打散
      val maptoUniRdd = listRdd.map(
          line=>{
          val value = line
          val keyUnique = FillUtil.extractFromJson("unique", line)
          var key = ""
          if("-".equals(keyUnique)){
            key = keyUnique + "_" + new Random().nextInt(10000).toString
          }else{
            key = keyUnique
          }
           (key,value)
        })

     // sc.makeRDD(maptoUniRdd.countByKey().map( x=>(x._2,x._1) ).toList).sortByKey(false).saveAsTextFile(_to)

    //按key分组
    val groupUniRdd =  maptoUniRdd.groupByKey()//

    //组内按time排序
    val sortlogRdd = groupUniRdd.map( listLine=> {
      val key = listLine._1
      val lines = listLine._2.toList.sortWith((a,b ) =>( FillUtil.extractFromJson("logTime",a.toString)< FillUtil.extractFromJson("logTime",b.toString) ) )
      (key,lines)
    })

    //排序好的数据进行新老客打标签
    val fisrtAccessLog = sortlogRdd.map(x=>(firstAccess.addTag(x._1,x._2)))  // .flatMap( iter => {iter.toList}) .saveAsTextFile(_to)

    //解析ip地址
    val regionRdd = fisrtAccessLog.map( x => { regionLog.analysistRegion(x)  })
   //保存到hdfs
    val savetext  = regionRdd.flatMap( iter => {iter.toList}).saveAsTextFile(_to)


    sc.stop()
  }


}
