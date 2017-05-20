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
      this.setValue(field, value)
    } else if (this.meta.fieldMap(field).isPointer()) {
      this.setPointer(field, value)
    }
    return null
  }

  def getValue(field: String): Object = {
    //    require(this.fieldMap.contains(field))
    return this.fieldMap(field)
  }

  def setValue(field: String, value: Object): Unit = {
    this.fieldMap += (field -> value)
  }

  def setPointer(field: String, value: Object): Unit = {
    val fieldMeta = this.meta.fieldMap(field)
    // a.b = b
    this.fieldMap += (field -> value)
    // a.b_id = b.id
    val core = EntityManager.core(value)
    require(core != null)
    this.fieldMap += (fieldMeta.left -> core.fieldMap(fieldMeta.right))
  }

  override def toString: String = {
    val content = this.meta.fieldVec.map(field => {
      if (field.isNormalOrPkey()) {
        val value = this.fieldMap(field.name)
        if (value == null) {
          s"${field.name}: null"
        } else if (field.typeName == "String") {
          s"""${field.name}: "${this.fieldMap(field.name)}""""
        } else {
          s"""${field.name}: ${this.fieldMap(field.name)}"""
        }
      } else if(!field.isOneMany()){
        s"""${field.name}: ${this.fieldMap(field.name)}"""
      }else{
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

