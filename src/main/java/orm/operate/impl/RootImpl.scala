package orm.operate.impl

import java.sql.ResultSet

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
  val joins = new ArrayBuffer[JoinImpl]()
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

class SelectJoinImpl[T](val clazz: Class[T], val impl: JoinImpl) extends Join with SelectJoin with Selectable[T] {
  if (clazz != impl.meta.clazz) {
    throw new RuntimeException("Class Not Match")
  }
  var selects = new ArrayBuffer[SelectJoinImpl[_]]()
  var fields = impl.meta.managedFieldVec().map(f => new FieldImpl(f.name, f, impl))

  override def getAlias: String = impl.getAlias

  override def getTableWithJoinCond: String = impl.getTableWithJoinCond

  override def join(field: String): Join = impl.join(field)

  override def get(field: String): Field = impl.get(field)

  override def getParent: Node = impl.getParent

  override def as[R](clazz: Class[R]): Selectable[R] = throw new RuntimeException("Already Selectable")

  override def pick(rs: ResultSet): T = ???

  override def getColumnWithAs: String = {
    def go(join: JoinImpl): String = {
      val selfColumn = fields.map(field => s"${field.getColumn} AS ${field.getAlias}")
      // 1. 属于自己的字段 2. 级联的部分
      (selfColumn ++ selects.flatMap(_.getColumnWithAs)).mkString("\n")
    }

    go(impl)
  }

  override def select(field: String): SelectJoin = {
    if (!impl.meta.fieldMap.contains(field) || !impl.meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Unknown Object Field $field")
    }
    selects.find(_.impl.field == field) match {
      case Some(s) => s
      case None =>
        val refer = impl.meta.fieldMap(field).refer
        val j = new JoinImpl(field, refer, impl)
        val s = new SelectJoinImpl(refer.clazz, j)
        selects += s
        s
    }
  }
}