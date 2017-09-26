package com.comall

import scala.collection.mutable.ListBuffer
import scala.util.control.Breaks._

/**
  * Created by Administrator on 2017/9/19.
  */
object  firstAccess extends serializable{
  def addTag(key:String,values:Iterable[String]): ListBuffer[String] ={
    val returnList = new ListBuffer[String]()//返回数据
    if ("-".equals(key.split("_")(0)) ){
      for (line <- values){
        returnList += label(line, false)
      }
    }else {
      var firstFlag = false
      for (line <- values) {
        breakable {
          //unique为空且只有unique分组只有一条数据，直接returnList
          if (values.size == 1) {
            returnList += label(line, true)
            break
          }
          if (firstFlag) {
            returnList += label(line, false)
          } else {
            returnList += label(line, true)
            firstFlag = true
          }
        }
      }
    }
    returnList
  }

  def  label(log:String,isFirst: Boolean): String ={
    if (log.contains("\"firstAccess\"")) {
      return FillUtil.replaceJsonField("firstAccess",
        if(isFirst)  "1" else "0", log);
    } else {
      return log.substring(0, log.length() - 1) + ",\"firstAccess\":\"" + ( if (isFirst)  "1" else "0") + "\"}";
    }
  }
}
