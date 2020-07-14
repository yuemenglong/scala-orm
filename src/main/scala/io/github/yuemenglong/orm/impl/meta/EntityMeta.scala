package io.github.yuemenglong.orm.impl.meta

import java.lang.reflect.Method

import io.github.yuemenglong.orm.api.anno.Entity
import io.github.yuemenglong.orm.impl.kit.Kit

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Administrator on 2017/5/16.
 */
case class IndexInfo(meta: EntityMeta, fields: Array[FieldMeta], unique: Boolean) {
  def name: String = {
    val column = fields.map(_.column).mkString("_")
    val table = meta.table
    s"idx$$${table}$$${column}"
  }

  def columns: String = {
    fields.map(f => s"`${f.column}`").mkString(",")
  }
}

class EntityMeta(val clazz: Class[_]) {
  val db: String = EntityMeta.pickDb(clazz)
  val entity: String = clazz.getSimpleName
  val table: String = EntityMeta.pickTable(clazz)
  var pkey: FieldMeta = _
  var fieldVec: ArrayBuffer[FieldMeta] = ArrayBuffer()
  var fieldMap: Map[String, FieldMeta] = Map()

  var getterMap: Map[Method, FieldMeta] = Map()
  var setterMap: Map[Method, FieldMeta] = Map()

  // <组合索引,是否唯一>
  var indexVec: ArrayBuffer[IndexInfo] = ArrayBuffer()

  var ignoreFieldVec: ArrayBuffer[FieldMetaIgnore] = ArrayBuffer()
  var ignoreGetterMap: Map[Method, FieldMetaIgnore] = Map()
  var ignoreSetterMap: Map[Method, FieldMetaIgnore] = Map()

  def fields(): ArrayBuffer[FieldMeta] = {
    fieldVec
  }
}

object EntityMeta {
  def pickTable(clazz: Class[_]): String = {
    val anno = clazz.getDeclaredAnnotation(classOf[Entity])
    if (anno == null) {
      return Kit.lodashCase(clazz.getSimpleName)
    }
    anno.table() match {
      case "" => Kit.lodashCase(clazz.getSimpleName)
      case _ => anno.table()
    }
  }

  def pickDb(clazz: Class[_]): String = {
    val anno = clazz.getDeclaredAnnotation(classOf[Entity])
    if (anno == null) {
      return null
    }
    anno.db() match {
      case "" => null
      case _ => anno.db()
    }
  }
}

