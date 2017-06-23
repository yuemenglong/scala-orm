package orm.kit

import java.lang.reflect.{Field, ParameterizedType}
import java.util

import orm.meta.FieldMetaTypeKind

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
    isGenericType(field) match {
      case false => field.getType()
      case true => field.getGenericType().asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[_]]
    }
  }

  def isGenericType(field: Field): Boolean = {
    field.getGenericType().isInstanceOf[ParameterizedType]
  }

  def newInstance(clazz: Class[_]): Object = {
    clazz.isInterface() match {
      case false => clazz.newInstance().asInstanceOf[Object]
      case true => clazz.getName() match {
        case "java.util.Collection" => return new util.ArrayList[Object]
        case "java.util.List" => return new util.ArrayList[Object]
        case "java.util.Set" => return new util.HashSet[Object]
        case _ => throw new RuntimeException(s"Unsupport Interface Type: [${clazz.getName}]")
      }
    }
  }

  def collection[T](list: Seq[T]): util.Collection[T] = {
    val coll = new util.ArrayList[T]()
    list.foreach(item => {
      coll.add(item)
    })
    coll
  }

  def seq[T](coll: util.Collection[T]): Seq[T] = {
    val ret = new ArrayBuffer[T]
    coll.forEach(item => {
      ret += item
    })
    ret.toList
  }
}
