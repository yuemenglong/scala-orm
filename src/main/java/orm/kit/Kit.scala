package orm.kit

import java.lang.reflect.Field
import java.sql.Connection

import orm.Orm
import orm.entity.EntityManager
import orm.meta.OrmMeta

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

  def getEmptyConstructorMap: Map[Class[_], () => Object] = {
    OrmMeta.entityMap.toArray.map { case (name, meta) =>
      val fn = () => {
        Orm.empty(meta.clazz).asInstanceOf[Object]
      }
      (meta.clazz, fn)
    }.toMap
  }

  def execute(conn:Connection, sql:String, params:Array[Object]):Int={
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    println(sql)
    println(s"[Params] => [${params.map(_.toString).mkString(", ")}]")
    val ret = stmt.executeUpdate()
    stmt.close()
    ret
  }
}
