package io.github.yuemenglong.orm.kit

import java.lang.reflect.{Field, Method}
import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.logger.Logger

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

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

  def upperCaseFirst(str: String): String = {
    str.substring(0, 1).toUpperCase() + str.substring(1)
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

  def getDeclaredMethods(clazz: Class[_]): Array[Method] = {
    val ret = new ArrayBuffer[Method]()
    clazz.getDeclaredMethods.foreach(ret += _)
    var parent = clazz.getSuperclass
    while (parent != null) {
      parent.getDeclaredMethods.foreach(ret += _)
      parent = parent.getSuperclass
    }
    ret.toArray
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

  def logSql(sql: String, params: Array[Object]): Unit = {
    val paramsSql = params.map {
      case null => "null"
      case v => v.toString
    }.mkString(", ")
    Logger.info(s"\n$sql\n[$paramsSql]")
  }

  def execute(conn: Connection, sql: String, params: Array[Object]): Int = {
    logSql(sql, params)
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    val ret = stmt.executeUpdate()
    stmt.close()
    ret
  }

  def query[T](conn: Connection, sql: String, params: Array[Object],
               fn: (ResultSet) => Array[T]): Array[T] = {
    logSql(sql, params)
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    var rs: ResultSet = null
    try {
      rs = stmt.executeQuery()
      fn(rs)
    } catch {
      case e: Throwable => throw e
    } finally {
      rs.close()
      stmt.close()
    }
  }
}
