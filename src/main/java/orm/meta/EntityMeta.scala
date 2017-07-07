package orm.meta

import orm.kit.Kit
import orm.lang.anno.Entity

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/16.
  */
class EntityMeta(val clazz: Class[_], val ignore: Boolean = false) {
  val entity: String = clazz.getSimpleName
  val table: String = EntityMeta.pickTable(clazz)
  var pkey: FieldMeta = _
  var fieldVec: ArrayBuffer[FieldMeta] = ArrayBuffer()
  var fieldMap: Map[String, FieldMeta] = Map()

  def managedFieldVec(): ArrayBuffer[FieldMeta] = {
    fieldVec.filter(!_.ignore)
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

