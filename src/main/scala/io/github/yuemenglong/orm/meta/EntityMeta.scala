package io.github.yuemenglong.orm.meta

import java.lang.reflect.Method

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.anno.Entity

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/16.
  */
class EntityMeta(val clazz: Class[_]) {
  val entity: String = clazz.getSimpleName
  val table: String = EntityMeta.pickTable(clazz)
  var pkey: FieldMeta = _
  var fieldVec: ArrayBuffer[FieldMeta] = ArrayBuffer()
  var fieldMap: Map[String, FieldMeta] = Map()

  var getterMap: Map[Method, FieldMeta] = Map()
  var setterMap: Map[Method, FieldMeta] = Map()

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
}

