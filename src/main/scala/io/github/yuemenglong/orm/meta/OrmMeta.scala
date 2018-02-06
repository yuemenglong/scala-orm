package io.github.yuemenglong.orm.meta

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/16.
  */
object OrmMeta {

  var entityVec: ArrayBuffer[EntityMeta] = ArrayBuffer()
  var entityMap: Map[Class[_], EntityMeta] = Map()
  var dbVec: Array[String] = Array()

  def check(): Unit = {
    require(entityVec.nonEmpty)
  }

  def reset(): Unit = {
    entityVec.clear()
    entityMap = Map()
  }
}

