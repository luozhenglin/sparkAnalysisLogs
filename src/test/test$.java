package com.comall;

/**
  * Created by Administrator on 2017/9/13.
  */
object test extends  serializable{
  def main(args: Array[String]): Unit = {
    val conf =new SparkConf().setMaster("local[2]").setAppName("test")

    val sc = SparkContext.getOrCreate(conf)

  /*  val words = Array("one", "two", "two", "three", "three", "three")

    val wordPairsRDD = sc.parallelize(words).map(word => (word, 1))

    val wordCountsWithReduce = wordPairsRDD.reduceByKey(_ + _).collect().foreach(println)

*/

/*
    val loc = new Locale("en")
    val fm = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss",loc)
    val tm = "30/Jul/2015:05:00:50 +0800"
    val dt2 = fm.parse(tm).getTime;
    println(dt2)*/


/*

    var repairList = new ListBuffer[String]()
    repairList+= "123"
    repairList+= "234"
    repairList+= "456"
    repairList+= "789"

    for (x <- 0 until repairList.size){
      println(x)
      println(repairList.toList(x))
    }


*/
   var repairList =  ListBuffer(1,2,3,4,5,6)


   val rdd = sc.parallelize(repairList).map(x=> x*2).foreach(println(_))
     //saveAsTextFile("D:\\access\\test0")





  }

}
