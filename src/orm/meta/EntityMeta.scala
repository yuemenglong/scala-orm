package orm.meta

/**
  * Created by Administrator on 2017/5/16.
  */
class EntityMeta(val clazz: Class[_]) {
  val entity: String = clazz.getSimpleName()
  val table: String = clazz.getSimpleName()
  var pkey: FieldMeta = null
  var fieldVec: Array[FieldMeta] = Array()
  var fieldMap: Map[String, FieldMeta] = Map()
}

