package orm.kit

import java.util

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/24.
  */
object Kit {
  def array[T](list: util.Collection[T]): ArrayBuffer[T] = {
    var ret = new ArrayBuffer[T]()
    list.stream().forEach(item => {
      ret += item
    })
    return ret
  }

  def list[T](array: Seq[T]): util.ArrayList[T] = {
    val ret = new util.ArrayList[T]()
    array.foreach(item => {
      ret.add(item)
    })
    return ret
  }

  def lodashCase(str: String): String = {
    val lowerCaseFirst = str.substring(0, 1).toLowerCase() + str.substring(1)
    """[A-Z]""".r.replaceAllIn(lowerCaseFirst, m => "_" + m.group(0).toLowerCase())
  }
}
