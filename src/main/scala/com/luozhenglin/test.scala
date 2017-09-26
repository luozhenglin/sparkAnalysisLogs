package com.luozhenglin

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import com.comall.FillUtil
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Administrator on 2017/9/22.
  */
object test {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setAppName("analysisAccessLog").setMaster("local[2]")
    val sc = SparkContext.getOrCreate(conf)
    val list = List("1498873127","1498924800","1499011200")
    val rdd= sc.parallelize(list)


     val  hourDateFormat = new SimpleDateFormat("yyyy-MM-dd HH");
    val forRdd = rdd.foreach(
      x=>{

        val date =new Date(x.toLong)
        println(hourDateFormat.format(date))

      }

    )
    /*val logTime = simpleDateFormat.parse(time)
    val newLine = FillUtil.replaceJsonField("logTime", logTime.getTime.toString, value)*/

  }

}
