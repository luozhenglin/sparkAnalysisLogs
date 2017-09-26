package com.comall

import java.util

import org.apache.hadoop.io.NullWritable
import qiniu.ip17mon.IpExt

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by luozhenglin on 2017/9/18.
  */
object regionLog  extends serializable{


  def  analysistRegion(values : Iterable[String]) : ListBuffer[String] ={
    val returnList = new ListBuffer[String]()//返回数据

    val  memberIds = new  mutable.HashSet[String] ();
    val  textList = new ListBuffer[String]()
    for ( text <- values  ) {
      import scala.util.control.Breaks._
      breakable{
        val stringBuilder = new StringBuilder(text.toString());
        var remoteIp = FillUtil.extractFromJson("remoteIP", stringBuilder.toString());
        val key =    FillUtil.extractFromJson("unique", stringBuilder.toString());
        if (remoteIp != null && !"-".equals(remoteIp)) {
          remoteIp = remoteIp.split(",")(0)
          val locationInfo = IpExt.getInstance().analysisIp(remoteIp);
          if (locationInfo != null) {
            val regionBuilder = new StringBuilder();
            regionBuilder.append(",\"country\":\"").append(locationInfo.getCountry()).append("\"")
              .append(",\"province\":\"").append(locationInfo.getState()).append("\"")
              .append(",\"city\":\"")
              .append(if (locationInfo.getCity().endsWith("市"))
                locationInfo.getCity().substring(0, locationInfo.getCity().length() - 1)
              else locationInfo.getCity())
              .append("\"");
            stringBuilder.insert(stringBuilder.length() - 1, regionBuilder);
          }
        }
        //unique为"-"的不处理
        if ("-".equals(key.toString())) {
          returnList += stringBuilder.toString()
          break
        }
        val userid = FillUtil.extractFromJson("userid", stringBuilder.toString());
        if (userid != null && !"-".equals(userid)) {
          memberIds += userid
        }
        textList += (stringBuilder.toString());
      }
    }
    val userId = if (memberIds.isEmpty )  "-" else memberIds.iterator.next()
    for ( log <- textList){
      val uid = FillUtil.extractFromJson("userid", log);
      if (!"-".equals(userId) && (uid == null || "-".equals(uid))) {
        returnList += FillUtil.replaceJsonField("userid", userId, log)
      } else {
        returnList += log
      }
    }
    returnList
  }


}
