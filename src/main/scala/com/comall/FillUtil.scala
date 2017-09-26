package com.comall


import java.util
import java.util.regex.{ Pattern}

import net.minidev.json.JSONObject
import net.minidev.json.parser.JSONParser

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable

/**
  * Created by luozhenglin on 2017/9/12.
  */
object  FillUtil extends serializable{


  /**
    * 将json转化为Map
    *
    * @param json 输入json字符串
    * @return
    * */
  def json2Map(json : String) : mutable.HashMap[String,Object] = {

    val map : mutable.HashMap[String,Object]= mutable.HashMap()

    val jsonParser =new JSONParser()

    //将string转化为jsonObject
    val jsonObj: JSONObject = jsonParser.parse(json).asInstanceOf[JSONObject]

    //获取所有键
    val jsonKey = jsonObj.keySet()

    val iter = jsonKey.iterator()

    while (iter.hasNext){
      val field = iter.next()
      val value = jsonObj.get(field).toString

      if(value.startsWith("{")&&value.endsWith("}")){
        val value = mapAsScalaMap(jsonObj.get(field).asInstanceOf[util.HashMap[String, String]])
        map.put(field,value)
      }else{
        map.put(field,value)
      }
    }
    map
  }

  /**
    *从json字符串中提取成员属性
   */
  def extractFromJson(field:String, json:String):String={
    val pattern = Pattern.compile("\"" + field + "\":\"(.+?)\"");
     val matcher = pattern.matcher(json);
     if (matcher.find()){
      return matcher.group(1);
    }
    return null;
  }

  /**
    *从json字符串中提取成员属性,提取到为true,反之为false
    */
  def fillExtractJson(field:String, json:String):Boolean={
    val p = Pattern.compile("\"" + field + "\":\"(.+?)\"");
    val matcher = p.matcher(json);
    var flag : Boolean = true
    if (matcher.find()){
       val matchJson= matcher.group(1)
       if("-".equals(matchJson) || "".equals(matchJson)) {
         flag = false
       }else {
         flag = true
       }
     }
      flag
  }


  /**
    * 替换字段值
    *
    * @param field
    * @param value
    * @param json
    * @return
    */
  def  replaceJsonField(field:String , value :String ,  json :String) :String ={
    val regex = "\"" + field + "\":\"(.+?)\"";
    val replacement = "\"" + field + "\":\"" + value + "\"";
    return  json.replaceAll(regex, replacement);
  }

}


