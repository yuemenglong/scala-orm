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
    } else if (this.meta.fieldMap(field).isOneOne()) {
      this.setOneOne(field, value)
    } else if (this.meta.fieldMap(field).isOneMany()) {
      this.setOneMany(field, value)
    }
    return null
  }

  def getValue(field: String): Object = {
    this.fieldMap.contains(field) match {
      case true => this.fieldMap(field)
      case false => null
    }
  }

  def setValue(field: String, value: Object): Unit = {
    this.fieldMap += (field -> value)
  }

  def setPointer(field: String, value: Object): Unit = {
    val a = this
    val fieldMeta = a.meta.fieldMap(field)
    // a.b = b
    this.fieldMap += (field -> value)
    // a.b_id = b.id
    val b = EntityManager.core(value)
    require(b != null)
    a.syncField(fieldMeta.left, b, fieldMeta.right)
  }

  def setOneOne(field: String, value: Object): Unit = {
    val a = this;
    val fieldMeta = this.meta.fieldMap(field)
    // a.b = b
    a.fieldMap += (field -> value)
    // b.a_id = a.id
    val b = EntityManager.core(value)
    require(b != null)
    b.syncField(fieldMeta.right, a, fieldMeta.left)
  }

  def setOneMany(field: String, value: Object): Unit = {
    val a = this;
    val fieldMeta = a.meta.fieldMap(field)

    val bs: Array[Object] = value.asInstanceOf[Iterable[Object]].map(item => {
      // b.a_id = a.id
      val b = EntityManager.core(item)
      require(b != null)
      b.syncField(fieldMeta.right, a, fieldMeta.left)
      item
    })(collection.breakOut)
    // a.bs = bs
    a.fieldMap += (field -> bs)
  }

  def syncField(left: String, b: EntityCore, right: String): Unit = {
    // a.left = b.right
    if (b.fieldMap.contains(right)) {
      this.fieldMap += (left -> b.fieldMap(right))
    } else if (this.fieldMap.contains(left)) {
      this.fieldMap -= left
    }
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
      } else if (!field.isOneMany()) {
        s"""${field.name}: ${this.fieldMap(field.name)}"""
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

