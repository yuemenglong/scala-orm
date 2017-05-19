package orm.meta

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/16.
  */
object OrmMeta {
  var entityVec: ArrayBuffer[EntityMeta] = ArrayBuffer()
  var entityMap: Map[String, EntityMeta] = Map()
}

