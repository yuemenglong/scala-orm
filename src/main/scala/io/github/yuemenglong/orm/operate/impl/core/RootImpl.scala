package io.github.yuemenglong.orm.operate.impl.core

import java.lang
import java.sql.ResultSet

import io.github.yuemenglong.orm.entity.{EntityCore, EntityManager}
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.impl._
import io.github.yuemenglong.orm.operate.traits.core.JoinType.JoinType
import io.github.yuemenglong.orm.operate.traits.core._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/15.
  */
class FieldImpl(val meta: FieldMeta, val parent: JoinImpl) extends Field {
  override def getField: String = meta.name

  override def getColumn: String = s"${parent.getAlias}.${meta.column}"

  override def getAlias: String = s"${parent.getAlias}$$${Kit.lodashCase(meta.name)}"

  override def getRoot: Node = parent.getRoot

  override def as[T](clazz: Class[T]): SelectableField[T] = new SelectableFieldImpl[T](clazz, this)
}

class SelectableFieldImpl[T](clazz: Class[T], val impl: Field) extends SelectableField[T] {
  private var distinctVar: String = ""

  override def getColumn: String = s"$distinctVar${impl.getColumn}"

  override def getField: String = impl.getField

  override def getAlias: String = impl.getAlias

  override def getType: Class[T] = clazz

  override def getRoot: Node = impl.getRoot

  override def as[R](clazz: Class[R]): SelectableField[R] = throw new RuntimeException("Already Selectable")

  override def distinct(): SelectableField[T] = {
    distinctVar = "DISTINCT "
    this
  }

}

class JoinImpl(val meta: EntityMeta, val parent: Join,
               val joinName: String, val left: String, val right: String,
               val joinType: JoinType) extends Join {
  private[orm] var cond: Cond = new CondHolder
  private[orm] val joins = new ArrayBuffer[(String, JoinImpl)]()
  private val fields = new ArrayBuffer[FieldImpl]()

  def this(meta: EntityMeta) {
    // for create root
    this(meta, null, null, null, null, null)
  }

  override def getMeta: EntityMeta = meta

  override def join(field: String, joinType: JoinType): Join = {
    if (!meta.fieldMap.contains(field) || !meta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Join Field $field")
    }
    joins.find(_._1 == field) match {
      case Some(p) => p._2
      case None =>
        val referField = meta.fieldMap(field).asInstanceOf[FieldMetaRefer]
        val join = new JoinImpl(referField.refer, this, field, referField.left, referField.right, joinType)
        joins += ((field, join))
        join
    }
  }

  override def get(field: String): Field = {
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Join Field $field")
    }
    fields.find(_.getField == field) match {
      case Some(f) => f
      case None =>
        val fieldMeta = meta.fieldMap(field)
        val f = new FieldImpl(fieldMeta, this)
        fields += f
        f
    }
  }

  override def getRoot: Node = {
    if (parent != null) {
      parent.getRoot
    } else {
      this
    }
  }

  override def as[T](clazz: Class[T]): SelectableJoin[T] = new SelectableJoinImpl[T](clazz, this)

  override def getAlias: String = {
    if (parent == null) {
      Kit.lowerCaseFirst(meta.entity)
    } else {
      s"${parent.getAlias}_${Kit.lowerCaseFirst(joinName)}"
    }
  }

  override def getTableWithJoinCond: String = {
    if (parent == null) {
      s"`${meta.table}` AS `$getAlias`"
    } else {
      val leftColumn = parent.getMeta.fieldMap(left).column
      val rightColumn = meta.fieldMap(right).column
      val leftTable = parent.getAlias
      val rightTable = getAlias
      val joinCondSql = cond.getSql match {
        case "" => ""
        case s => s" AND $s"
      }
      s"${joinType.toString} JOIN `${meta.table}` AS `$getAlias` ON $leftTable.$leftColumn = $rightTable.$rightColumn$joinCondSql"
    }
  }

  override def getParams: Array[Object] = cond.getParams ++ joins.map(_._2).flatMap(_.getParams).toArray[Object]

  override def on(c: Cond): Join = {
    cond = c
    this
  }

}

class SelectJoinImpl(val impl: JoinImpl) extends SelectJoin {
  protected[impl] var selects = new ArrayBuffer[(String, SelectJoinImpl)]()
  protected[impl] var fields: Array[FieldImpl] = impl.meta.fields().filter(_.isNormalOrPkey).map(f => new FieldImpl(f, impl)).toArray

  override def getAlias: String = impl.getAlias

  override def getTableWithJoinCond: String = impl.getTableWithJoinCond

  override def join(field: String, joinType: JoinType): Join = impl.join(field, joinType)

  override def get(field: String): Field = impl.get(field)

  override def getRoot: Node = impl.getRoot

  override def as[R](clazz: Class[R]): SelectableJoin[R] = impl.as(clazz)

  /*---------------------------------------------------------------------*/

  override def select(field: String): SelectJoin = {
    if (!impl.meta.fieldMap.contains(field) || !impl.meta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Object Field $field")
    }
    selects.find(_._1 == field) match {
      case Some(p) => p._2
      case None =>
        val j = impl.leftJoin(field).asInstanceOf[JoinImpl]
        val s = new SelectJoinImpl(j)
        selects += ((field, s))
        s
    }
  }

  override def fields(fields: String*): SelectJoin = {
    this.fields(fields.toArray)
  }

  override def fields(fields: Array[String]): SelectJoin = {
    fields.foreach(f => {
      if (!this.getMeta.fieldMap.contains(f)) {
        throw new RuntimeException(s"Invalid Field $f In ${this.getMeta.entity}")
      }
      if (!this.getMeta.fieldMap(f).isNormal) {
        throw new RuntimeException(s"Not Normal Field $f In ${this.getMeta.entity}")
      }
    })
    val pkey = new FieldImpl(this.getMeta.pkey, impl)
    this.fields = Array(pkey) ++ fields.map(this.getMeta.fieldMap(_)).map(f => new FieldImpl(f, impl))
    this
  }

  protected def getFilterKey(core: EntityCore): String = {
    s"$getAlias@${core.getPkey.toString}"
  }

  protected def getOneManyFilterKey(field: String, core: EntityCore): String = {
    s"$getAlias@$field@${core.getPkey.toString}"
  }

  protected def pickResult(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val a = pickSelf(resultSet, filterMap)
    if (a == null) {
      return null
    }
    pickRefer(a, resultSet, filterMap)
    a
  }

  protected def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val map: Map[String, Object] = fields.map(field => {
      val alias = field.getAlias
      val value = resultSet.getObject(alias)
      (field.meta.name, value)
    })(collection.breakOut)
    val core = new EntityCore(impl.meta, map)
    if (core.getPkey == null) {
      return null
    }
    val key = getFilterKey(core)
    if (filterMap.contains(key)) {
      return filterMap(key)
    }
    val a = EntityManager.wrap(core)
    filterMap += (key -> a)
    a
  }

  protected def pickRefer(a: Object, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]) {
    val aCore = EntityManager.core(a)
    selects.foreach { case (field, select) =>
      val fieldMeta = impl.meta.fieldMap(field)
      val b = select.pickResult(resultSet, filterMap)
      fieldMeta match {
        case _: FieldMetaPointer => aCore.fieldMap += (field -> b)
        case _: FieldMetaOneOne => aCore.fieldMap += (field -> b)
        case f: FieldMetaOneMany =>
          val hasArr = aCore.fieldMap.contains(field)
          if (!hasArr) {
            aCore.fieldMap += (field -> Kit.newArray(f.refer.clazz))
          }
          if (b != null) {
            val key = getOneManyFilterKey(field, b.$$core())
            if (!filterMap.contains(key)) {
              // 该对象还未被加入过一对多数组
              val arr = aCore.fieldMap(field).asInstanceOf[Array[_]]
              val brr = Kit.newArray(f.refer.clazz, b)
              aCore.fieldMap += (field -> (arr ++ brr))
            }
            filterMap += (key -> b)
          }
      }
    }
  }

  override def on(c: Cond): Join = impl.on(c)

  override def getParams: Array[Object] = impl.getParams

  override def getMeta: EntityMeta = impl.getMeta
}

class SelectableJoinImpl[T](val clazz: Class[T], impl: JoinImpl)
  extends SelectJoinImpl(impl) with SelectableJoin[T] {
  if (clazz != impl.meta.clazz) {
    throw new RuntimeException("Class Not Match")
  }

  override def getColumnWithAs: String = {
    def go(select: SelectJoinImpl): Array[String] = {
      val selfColumn = select.fields.map(field => s"${field.getColumn} AS ${field.getAlias}")
      // 1. 属于自己的字段 2. 级联的部分
      selfColumn ++ select.selects.map(_._2).flatMap(go)
    }

    go(this).mkString(",\n")
  }

  override def getType: Class[T] = clazz

  override def getKey(value: Object): String = {
    if (value == null) {
      ""
    } else {
      value.asInstanceOf[Entity].$$core().getPkey.toString
    }
  }

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = pickResult(resultSet, filterMap).asInstanceOf[T]
}

class RootImpl[T](clazz: Class[T], impl: JoinImpl)
  extends SelectableJoinImpl[T](clazz, impl)
    with Root[T] {

  override def getTableWithJoinCond: String = {
    def go(join: JoinImpl): Array[String] = {
      Array(join.getTableWithJoinCond) ++ join.joins.map(_._2).flatMap(go)
    }

    go(impl).mkString("\n")
  }

  override def count(): Selectable[java.lang.Long] = new Count_(this)

  override def count(field: Field): SelectableField[lang.Long] = new Count(field)

  override def sum(field: Field): SelectableField[lang.Long] = new Sum(field)

  override def max[R](field: Field, clazz: Class[R]): SelectableField[R] = new Max(field, clazz)

  override def min[R](field: Field, clazz: Class[R]): SelectableField[R] = new Min(field, clazz)
}
