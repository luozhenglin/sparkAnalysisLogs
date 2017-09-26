package com.comall

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import scala.collection.mutable.ListBuffer

/**
  * Created by luozhenglin on 2017/9/18.
  */
object repairLog extends serializable {


  val  hourDateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
  /**
    *修复数据
    */
  def repairJson( key :String, values  :  Iterable[String]) :ListBuffer[String] ={
    var repairList = new ListBuffer[String]() //待修复的数据
    val returnList = new ListBuffer[String]()//返回数据
    var lastIp : String = null
    var lastLogTime : Date = null

    for(value <-  values){
      val unique = FillUtil.extractFromJson("unique", value)
      val time = FillUtil.extractFromJson("logTime", value)

       val logTime =new Date(time.toLong)

      //待修复数据为空 且unique不为“-” 或者 ip="-" ,null,不需要修复
      if   (( repairList.size==0  && !"-".equals(unique)) || (key == null || "-".equals(key.split("_")(0)))){
        //没ip没法处理
        //有unique不需要处理 直接写
        returnList += value
      }else{
        if (lastIp == null){
          lastIp  = key
          lastLogTime = logTime
          repairList +=  value
        }else if ( !"-".equals(unique)) {
          //进到这里就表示该写数据了
          if ( hourDateFormat.format(logTime ).equals( hourDateFormat.format( lastLogTime) ) ) {
            returnList += value
            //同一天，执行修复
            for  ( x <- 0 until repairList.size){
              var preRepairData = repairList(x)
              //v为当前数据，信息较全，以它为基础使用待修复的数据中部分字段对其进行替换
              var data = FillUtil.replaceJsonField("logTime", FillUtil.extractFromJson("logTime", preRepairData), value)
              data = FillUtil.replaceJsonField("uri", FillUtil.extractFromJson("uri", preRepairData), value)
              data = FillUtil.replaceJsonField("status", FillUtil.extractFromJson("status", preRepairData), value)
              data = FillUtil.replaceJsonField("responseBytes", FillUtil.extractFromJson("responseBytes", preRepairData), value)
              data = FillUtil.replaceJsonField("responseTime", FillUtil.extractFromJson("responseTime", preRepairData), value)
              data = FillUtil.replaceJsonField("referer", FillUtil.extractFromJson("referer", preRepairData), value)
              data = FillUtil.replaceJsonField("userid", FillUtil.extractFromJson("userid", preRepairData), value)
              data = FillUtil.replaceJsonField("userSession", FillUtil.extractFromJson("userSession", preRepairData), value)
              repairList.update(x, data);
              if(x == repairList.size-1) {
                returnList ++= repairList
                repairList.clear()
                lastIp = null;
                lastLogTime = null
              }
            }
          }else{
            //此条数据应该返回
            returnList +=value
          }
        } else {
          //连续的无unique数据
          repairList += value
        }
      }
    }
    if(repairList.size!=0)
      returnList ++=  repairList

    return returnList
  }



}
