package orm.kit

import java.util

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/24.
  */
object Kit {
  def array[T](list: util.ArrayList[T]): ArrayBuffer[T] = {
    var ret = new ArrayBuffer[T]()
    for (i <- 0 to list.size() - 1) {
      ret += list.get(i)
    }
    return ret
  }

  def list[T](array: Seq[T]): util.ArrayList[T] = {
    val ret = new util.ArrayList[T]()
    array.foreach(item => {
      ret.add(item)
    })
    return ret
  }
}
