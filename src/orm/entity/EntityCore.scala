package orm.entity

import orm.meta.{EntityMeta, OrmMeta}

/**
  * Created by Administrator on 2017/5/18.
  */
class EntityCore(val meta: EntityMeta, var fieldMap: Map[String, Object]) {

  def get(field: String): Object = {
    if (this.meta.fieldMap(field).isNormalOrPkey()) {
      return this.getValue(field)
    } else {
      return null
    }
  }

  def set(field: String, value: Object): Object = {
    if (this.meta.fieldMap(field).isNormalOrPkey()) {
      return this.setValue(field, value)
    } else {
      return null
    }
  }

  def getValue(field: String): Object = {
    //    require(this.fieldMap.contains(field))
    return this.fieldMap(field)
  }

  def setValue(field: String, value: Object): Object = {
    this.fieldMap += (field -> value)
    return value
  }

  override def toString: String = {
    val content = this.meta.fieldVec.map(field => {
      if (field.isNormalOrPkey()) {
        val value = this.fieldMap(field.name)
        if (value == null) {
          s"${field.name}: null"
        } else if (field.tp == "String") {
          s"""${field.name}: "${this.fieldMap(field.name)}""""
        } else {
          s"""${field.name}: ${this.fieldMap(field.name)}"""
        }
      } else {
        throw new RuntimeException("")
      }
    }).mkString(", ")
    s"{${content}}"
  }
}

object EntityCore {
  def create(meta: EntityMeta): EntityCore = {
    val map = meta.fieldVec.map((field) => {
      (field.name, null)
    }).groupBy(_._1).mapValues(_.head._2)
    new EntityCore(meta, map)
  }

  def empty(meta: EntityMeta): EntityCore = {
    new EntityCore(meta, Map())
  }
}

