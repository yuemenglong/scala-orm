package orm.kit

import java.lang.reflect.Field

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/24.
  */
object Kit {
  def lodashCase(str: String): String = {
    //    val lowerCaseFirst = str.substring(0, 1).toLowerCase() + str.substring(1)
    """[A-Z]""".r.replaceAllIn(lowerCaseFirst(str), m => "_" + m.group(0).toLowerCase())
  }

  def lowerCaseFirst(str: String): String = {
    str.substring(0, 1).toLowerCase() + str.substring(1)
  }

  //  def getGenericType(field: Field): Class[_] = {
  //    if (isGenericType(field)) {
  //      field.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[_]]
  //    } else {
  //      field.getType
  //    }
  //  }
  //
  //  def isGenericType(field: Field): Boolean = {
  //    field.getGenericType.isInstanceOf[ParameterizedType]
  //  }

  def getDeclaredFields(clazz: Class[_]): Array[Field] = {
    val ret = new ArrayBuffer[Field]()
    clazz.getDeclaredFields.foreach(ret += _)
    var parent = clazz.getSuperclass
    while (parent != null) {
      parent.getDeclaredFields.foreach(ret += _)
      parent = parent.getSuperclass
    }
    ret.toArray
  }
}
