package com.luozhenglin

/**
  * Created by Administrator on 2017/9/13.
  */
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Apache日志分析
  * Created by ibf on 01/15.
  */
object LogAnalyzer {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName("log-analyzer")
      .setMaster("local[*]")
      .set("spark.eventLog.enabled", "true")
      .set("spark.eventLog.dir", "hdfs://hadoop-senior01:8020/spark-history")
    val sc = SparkContext.getOrCreate(conf)

    // ================日志分析具体代码==================
    // HDFS上日志存储路径
    val path = "/beifeng/spark/access/access.log"

    // 创建rdd
    val rdd = sc.textFile(path)

    // rdd转换，返回进行后续操作
    val apacheAccessLog: RDD[ApacheAccessLog] = rdd
      // 过滤数据
      .filter(line => ApacheAccessLog.isValidateLogLine(line))
      .map(line => {
        // 对line数据进行转换操作
        ApacheAccessLog.parseLogLine(line)
      })

    // 对多次时候用的rdd进行cache
    apacheAccessLog.cache()

    // 需求一：求contentsize的平均值、最小值、最大值
    /*
    * The average, min, and max content size of responses returned from the server.
    * */
    val contentSizeRDD: RDD[Long] = apacheAccessLog
      // 提取计算需要的字段数据
      .map(log => (log.contentSize))

    // 对重复使用的RDD进行cache
    contentSizeRDD.cache()

    // 开始计算平均值、最小值、最大值
    val totalContentSize = contentSizeRDD.sum()
    val totalCount = contentSizeRDD.count()
    val avgSize = 1.0 * totalContentSize / totalCount
    val minSize = contentSizeRDD.min()
    val maxSize = contentSizeRDD.max()

    // 当RDD不使用的时候，进行unpersist
    contentSizeRDD.unpersist()

    // 结果输出
    println(s"ContentSize Avg：${avgSize}, Min: ${minSize}, Max: ${maxSize}")

    // 需求二：请各个不同返回值的出现的数据 ===> wordCount程序
    /*
    * A count of response code's returned.
    * */
    val responseCodeResultRDD = apacheAccessLog
      // 提取需要的字段数据, 转换为key/value键值对，方便进行reduceByKey操作
      // 当连续出现map或者flatMap的时候，将多个map/flatMap进行合并
      .map(log => (log.responseCode, 1))
      // 使用reduceByKey函数，按照key进行分组后，计算每个key出现的次数
      .reduceByKey(_ + _)

    // 结果输出
    println(s"""ResponseCode :${responseCodeResultRDD.collect().mkString(",")}""")

    // 需求三：获取访问次数超过N次的IP地址
    // 需求三额外：对IP地址进行限制，部分黑名单IP地址不统计
    /*
    * All IPAddresses that have accessed this server more than N times.
    * 1. 计算IP地址出现的次数 ===> WordCount程序
    * 2. 数据过滤
    * */
    val blackIP = Array("200-55-104-193.dsl.prima.net.ar", "10.0.0.153", "208-38-57-205.ip.cal.radiant.net")
    // 由于集合比较大，将集合的内容广播出去
    val broadCastIP = sc.broadcast(blackIP)
    val N = 10
    val ipAddressRDD = apacheAccessLog
      // 过滤IP地址在黑名单中的数据
      .filter(log => !broadCastIP.value.contains(log.ipAddress))
      // 获取计算需要的IP地址数据，并将返回值转换为Key/Value键值对类型
      .map(log => (log.ipAddress, 1L))
      // 使用reduceByKey函数进行聚合操作
      .reduceByKey(_ + _)
      // 过滤数据，要求IP地址必须出现N次以上
      .filter(tuple => tuple._2 > N)
    // 获取满足条件IP地址, 为了展示方便，将下面这行代码注释
    //      .map(tuple => tuple._1)

    // 结果输出
    println(s"""IP Address :${ipAddressRDD.collect().mkString(",")}""")

    // 需求四：获取访问次数最多的前K个endpoint的值 ==> TopN
    /*
    * The top endpoints requested by count.
    * 1. 先计算出每个endpoint的出现次数
    * 2. 再进行topK的一个获取操作，获取出现次数最多的前K个值
    * */
    val K = 10
    val topKValues = apacheAccessLog
      // 获取计算需要的字段信息，并返回key/value键值对
      .map(log => (log.endpoint, 1))
      // 获取每个endpoint对应的出现次数
      .reduceByKey(_ + _)
      // 获取前10个元素, 而且使用我们自定义的排序类
      .top(K)(LogSortingUtil.TupleOrdering)
    // 如果只需要endpoint的值，不需要出现的次数，那么可以通过map函数进行转换
    //      .map(_._1)

    // 结果输出
    println(s"""TopK values:${topKValues.mkString(",")}""")


    // 对不在使用的rdd，去除cache
    apacheAccessLog.unpersist()

    // ================日志分析具体代码==================

    sc.stop()
  }
}