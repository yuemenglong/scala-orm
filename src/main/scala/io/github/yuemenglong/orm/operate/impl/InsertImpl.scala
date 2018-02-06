package io.github.yuemenglong.orm.operate.impl

import java.sql.{Connection, Statement}

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.meta.{EntityMeta, OrmMeta}
import io.github.yuemenglong.orm.operate.traits.ExecutableInsert

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

class InsertImpl[T](clazz: Class[T]) extends ExecutableInsert[T] {
  if (!OrmMeta.entityMap.contains(clazz)) {
    throw new RuntimeException(s"Not Entity: ${clazz.getSimpleName}")
  }
  val meta: EntityMeta = OrmMeta.entityMap(clazz)
  var array: Array[T] = Array().asInstanceOf[Array[T]] //Array.newBuilder[T](ClassTag(clazz)).result()

  override def execute(session: Session): Int = {
    val entities = array.map(obj => {
      if (!obj.isInstanceOf[Entity]) {
        throw new RuntimeException("Not Entity, Maybe Need Convert")
      }
      obj.asInstanceOf[Entity]
    })

    if (entities.length == 0) return 0

    val fields = meta.fields().filter(_.isNormalOrPkey)
    val columns = fields.map(_.column).mkString(", ")
    val holders = fields.map(_ => "?").mkString(", ")
    val sql = s"INSERT INTO `${meta.table}`($columns) VALUES ($holders)"
    val params = entities.map(entity => {
      fields.map(field => {
        entity.$$core().fieldMap.contains(field.name) match {
          case true => entity.$$core().fieldMap(field.name)
          case false => null
        }
      }).toArray
    })
    session.batch(sql, params, stmt => {
      if (meta.pkey.isAuto) {
        val rs = stmt.getGeneratedKeys
        val ids = Stream.continually({
          if (rs.next()) {
            rs.getObject(1)
          } else {
            null
          }
        }).takeWhile(_ != null).toArray
        array.zipWithIndex.foreach { case (o, i) =>
          val core = o.asInstanceOf[Entity].$$core()
          core.setPkey(ids(i))
        }
      }
    })
    //    val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    //    Logger.info(sql)

//    val inTransaction = !conn.getAutoCommit
//    if (!inTransaction) {
//      conn.setAutoCommit(false)
//    }
//    entities.foreach(entity => {
//      val ab = ArrayBuffer[Object]()
//      fields.zipWithIndex.foreach { case (field, i) =>
//        val value = if (entity.$$core().fieldMap.contains(field.name)) {
//          entity.$$core().fieldMap(field.name)
//        } else {
//          null
//        }
//        stmt.setObject(i + 1, value)
//        ab += value
//      }
//      Logger.info(s"Batch Params => [${ab.map(String.valueOf(_)).mkString(", ")}]")
//      stmt.addBatch()
//    })

//    val ret = stmt.executeBatch()
//    if (meta.pkey.isAuto) {
//      val rs = stmt.getGeneratedKeys
//      val ids = Stream.continually({
//        if (rs.next()) {
//          rs.getObject(1)
//        } else {
//          null
//        }
//      }).takeWhile(_ != null).toArray
//      array.zipWithIndex.foreach { case (o, i) =>
//        val core = o.asInstanceOf[Entity].$$core()
//        core.setPkey(ids(i))
//      }
//    }
//    if (!inTransaction) {
//      conn.commit()
//    }
//    ret.sum
  }

  override def values(arr: Array[T]): ExecutableInsert[T] = {
    array = arr
    this
  }
}
