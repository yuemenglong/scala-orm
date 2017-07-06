package orm.kit

import java.lang.reflect.{Field, ParameterizedType}
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

  def getGenericType(field: Field): Class[_] = {
    if (isGenericType(field)) {
      field.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[_]]
    } else {
      field.getType
    }
  }

  def isGenericType(field: Field): Boolean = {
    field.getGenericType.isInstanceOf[ParameterizedType]
  }

  def newInstance(clazz: Class[_]): Object = {
    if (clazz.isInterface) {
      clazz.getName match {
        case "java.util.Collection" => new util.ArrayList[Object]
        case "java.util.List" => new util.ArrayList[Object]
        case "java.util.Set" => new util.HashSet[Object]
        case _ => throw new RuntimeException(s"Unsupport Interface Type: [${clazz.getName}]")
      }
    } else {
      clazz.newInstance().asInstanceOf[Object]
    }
  }

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
