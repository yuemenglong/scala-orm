package orm.operate.impl

import orm.kit.Kit
import orm.meta.{EntityMeta, FieldMeta}
import orm.operate.traits.core._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/15.
  */
class FieldImpl(val field: String, val meta: FieldMeta, val parent: JoinImpl) extends Field {
  override def getColumn: String = s"${parent.getAlias}.${meta.column}"

  override def getAlias: String = s"${parent.getAlias}$$${meta.field}"

  override def getParent: Node = parent

  override def eql(value: Object): Cond = ???

  override def assign(value: Object): Assign = ???

  override def as[T](clazz: Class[T]): Selectable[T] = ???
}

class JoinImpl(val field: String, val meta: EntityMeta, val parent: JoinImpl) extends Join {
  private val joins = new ArrayBuffer[JoinImpl]()
  private val fields = new ArrayBuffer[FieldImpl]()

  override def join(field: String): Join = {
    if (!meta.fieldMap.contains(field) || !meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Unknown Join Field $field")
    }
    joins.find(_.field == field) match {
      case Some(join) => join
      case None =>
        val refer = meta.fieldMap(field).refer
        val join = new JoinImpl(field, refer, this)
        joins += join
        join
    }
  }

  override def get(field: String): Field = {
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Unknown Join Field $field")
    }
    fields.find(_.field == field) match {
      case Some(f) => f
      case None =>
        val fieldMeta = meta.fieldMap(field)
        val f = new FieldImpl(field, fieldMeta, this)
        fields += f
        f
    }
  }

  override def getParent: Node = parent

  override def as[T](clazz: Class[T]): Selectable[T] = ???

  override def getAlias: String = {
    if (parent == null) {
      Kit.lowerCaseFirst(meta.entity)
    } else {
      s"${parent.getAlias}_${Kit.lowerCaseFirst(field)}"
    }
  }

  override def getTableWithJoinCond: String = {
    if (parent == null) {
      s"${meta.table} AS $getAlias"
    } else {
      val fieldMeta = parent.meta.fieldMap(field)
      val leftColumn = parent.meta.fieldMap(fieldMeta.left).column
      val rightColumn = meta.fieldMap(fieldMeta.right).column
      val leftTable = parent.meta.table
      val rightTable = meta.table
      s"LEFT JOIN $getAlias ON $leftTable.$leftColumn = $rightTable.$rightColumn"
    }
  }
}

