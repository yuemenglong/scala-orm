package orm.kit

import java.util

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/24.
  */
object Kit {
  def lodashCase(str: String): String = {
    val lowerCaseFirst = str.substring(0, 1).toLowerCase() + str.substring(1)
    """[A-Z]""".r.replaceAllIn(lowerCaseFirst, m => "_" + m.group(0).toLowerCase())
  }
}
