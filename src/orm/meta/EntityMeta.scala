package orm.meta

import orm.lang.anno.Entity

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/16.
  */
class EntityMeta(val clazz: Class[_]) {
  val entity: String = clazz.getSimpleName()
  val table: String = EntityMeta.pickTable(clazz)
  var pkey: FieldMeta = null
  var fieldVec: ArrayBuffer[FieldMeta] = ArrayBuffer()
  var fieldMap: Map[String, FieldMeta] = Map()
}

object EntityMeta {
  def pickTable(clazz: Class[_]): String = {
    val anno = clazz.getDeclaredAnnotation(classOf[Entity])
    if (anno == null) {
      return clazz.getSimpleName().toLowerCase()
    }
    anno.table() match {
      case "" => clazz.getSimpleName().toLowerCase()
      case _ => anno.table()
    }
  }
}

