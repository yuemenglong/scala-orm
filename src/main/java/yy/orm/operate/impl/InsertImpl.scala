package yy.orm.operate.impl

import java.sql.Connection

import yy.orm.lang.interfaces.Entity
import yy.orm.meta.{EntityMeta, OrmMeta}
import yy.orm.operate.traits.ExecutableInsert

import scala.annotation.varargs
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

class InsertImpl[T](clazz: Class[T]) extends ExecutableInsert[T] {
  if (!OrmMeta.entityMap.contains(clazz.getSimpleName)) {
    throw new RuntimeException(s"Not Entity: ${clazz.getSimpleName}")
  }
  val meta: EntityMeta = OrmMeta.entityMap(clazz.getSimpleName)
  var array: Array[T] = Array.newBuilder[T](ClassTag(clazz)).result()

  override def walk(fn: (Entity) => Entity): Unit = {}

  override def execute(conn: Connection): Int = {
    val entities = array.map(obj => {
      if (!obj.isInstanceOf[Entity]) {
        throw new RuntimeException("Not Entity, Maybe Need Convert")
      }
      obj.asInstanceOf[Entity]
    })
    val fields = meta.managedFieldVec().filter(_.isNormalOrPkey)
    val columns = fields.map(_.column).mkString(", ")
    val holders = fields.map(_ => "?").mkString(", ")
    val sql = s"INSERT INTO ${meta.table}($columns) VALUES ($holders)"
    val stmt = conn.prepareStatement(sql)
    println(sql)

    conn.setAutoCommit(false)
    entities.foreach(entity => {
      val ab = ArrayBuffer[Object]()
      fields.zipWithIndex.foreach { case (field, i) =>
        val value = if (entity.$$core().fieldMap.contains(field.name)) {
          entity.$$core().fieldMap(field.name)
        } else {
          null
        }
        stmt.setObject(i + 1, value)
        ab += value
      }
      println(s"Batch Params => [${ab.map(String.valueOf(_)).mkString(", ")}]")
      stmt.addBatch()
    })

    val ret = stmt.executeBatch()
    conn.commit()
    ret.sum
  }

  override def values(arr: Array[T]): ExecutableInsert[T] = {
    array = arr
    this
  }
}
