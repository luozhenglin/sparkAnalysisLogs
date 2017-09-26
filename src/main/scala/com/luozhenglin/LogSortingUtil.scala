package com.luozhenglin

/**
  * Created by Administrator on 2017/9/13.
  */
object LogSortingUtil {

  /**
    * 自定义的一个二元组的比较器
    */
  object TupleOrdering extends scala.math.Ordering[(String, Int)] {
    override def compare(x: (String, Int), y: (String, Int)): Int = {
      // 按照出现的次数进行比较，也就是按照二元组的第二个元素进行比较
      x._2.compare(y._2)
    }
  }

}
