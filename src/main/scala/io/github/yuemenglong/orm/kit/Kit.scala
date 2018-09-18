package io.github.yuemenglong.orm.kit

import java.lang.reflect.{Field, Method}
import java.sql.ResultSet

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types._

import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/24.
  */
object Kit {
  def lodashCase(str: String): String = {
    """[A-Z]""".r.replaceAllIn(lowerCaseFirst(str), m => "_" + m.group(0).toLowerCase())
  }

  def lowerCaseFirst(str: String): String = {
    str.substring(0, 1).toLowerCase() + str.substring(1)
  }

  def upperCaseFirst(str: String): String = {
    str.substring(0, 1).toUpperCase() + str.substring(1)
  }

  def getDeclaredFields(clazz: Class[_]): Array[Field] = {
    val parent = clazz.getSuperclass
    if (parent != null) {
      getDeclaredFields(parent) ++ clazz.getDeclaredFields
    } else {
      clazz.getDeclaredFields
    }
  }

  def getDeclaredMethods(clazz: Class[_]): Array[Method] = {
    val parent = clazz.getSuperclass
    if (parent != null) {
      getDeclaredMethods(parent) ++ clazz.getDeclaredMethods
    } else {
      clazz.getDeclaredMethods
    }
  }

  def newArray(clazz: Class[_], values: Entity*): Array[_] = {
    val ct = ClassTag[Entity](clazz)
    var builder = Array.newBuilder(ct)
    builder ++= values
    builder.result()
  }

  def getArrayType(clazz: Class[_]): Class[_] = {
    if (!clazz.isArray) {
      return clazz
    }
    val name = clazz.getName.replaceAll("(^\\[L)|(;$)", "")
    Class.forName(name)
  }

  def getObjectFromResultSet[T](resultSet: ResultSet, alias: String, clazz: Class[T]): T = {
    val obj = resultSet.getObject(alias)
    val intClass = classOf[Integer]
    val longClass = classOf[Long]
    val boolClass = classOf[Boolean]
    val bdClass = classOf[BigDecimal]
    if (obj == null) {
      null.asInstanceOf[T]
    } else if (obj.getClass == clazz) {
      obj.asInstanceOf[T]
    } else (obj, clazz) match {
      case (n: Integer, `longClass`) => n.toLong.asInstanceOf[T]
      case (n: Integer, `boolClass`) => (n != 0).asInstanceOf[T]
      case (n: Integer, `bdClass`) => new BigDecimal(n).asInstanceOf[T]
      case (n: Long, `intClass`) => n.toInt.asInstanceOf[T]
      case (n: Long, `boolClass`) => (n != 0).asInstanceOf[T]
      case (n: Long, `bdClass`) => new BigDecimal(n).asInstanceOf[T]
      case (n: BigDecimal, `intClass`) => n.intValue().asInstanceOf[T]
      case (n: BigDecimal, `longClass`) => n.longValue().asInstanceOf[T]
      case (n: BigDecimal, `boolClass`) => throw new RuntimeException("Unreachable")
      case _ => obj.asInstanceOf[T]
    }
  }
}
