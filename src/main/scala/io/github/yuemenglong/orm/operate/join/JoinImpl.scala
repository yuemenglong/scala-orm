package io.github.yuemenglong.orm.operate.join

import java.lang
import java.sql.ResultSet

import io.github.yuemenglong.orm.entity.{EntityCore, EntityManager}
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.field.{FieldImpl, SelectableFieldImpl}
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.join.traits._
import io.github.yuemenglong.orm.operate.query._
import io.github.yuemenglong.orm.operate.query.traits.Selectable

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object JoinType extends Enumeration {
  type JoinType = Value
  val INNER, LEFT, RIGHT, OUTER = Value
}

trait JoinInner {
  val meta: EntityMeta
  val parent: Join
  val joinName: String
  val left: String
  val right: String
  val joinType: JoinType

  protected[orm] var cond: Cond = new CondHolder
  protected[orm] val joins = new ArrayBuffer[JoinImpl]()
}

trait JoinImpl extends Join {
  val inner: JoinInner

  override def getMeta: EntityMeta = inner.meta

  override def join(field: String, joinType: JoinType): Join = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    if (getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer.db != getMeta.db) {
      throw new RuntimeException(s"Field $field Not the Same DB")
    }
    inner.joins.find(_.inner.joinName == field) match {
      case Some(p) => p.inner.joinType == joinType match {
        case true => p
        case false => throw new RuntimeException(s"JoinType Not Match, ${p.inner.joinType} Exists")
      }
      case None =>
        val referField = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
        //        val join = new JoinImpl(referField.refer, this, field, referField.left, referField.right, joinType)
        val that = this
        val jt = joinType
        val joinInner = new JoinInner {
          override val meta: EntityMeta = referField.refer
          override val parent: Join = that
          override val joinName: String = field
          override val left: String = referField.left
          override val right: String = referField.right
          override val joinType: JoinType = jt
        }
        val join = new JoinImpl {
          override val inner: JoinInner = joinInner
        }
        inner.joins += join
        join
    }
  }

  override def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): SelectableJoin[T] = {
    if (!OrmMeta.entityMap.contains(clazz)) {
      throw new RuntimeException(s"$clazz Is Not Entity")
    }
    val referMeta = OrmMeta.entityMap(clazz)
    if (!getMeta.fieldMap.contains(left)) {
      throw new RuntimeException(s"Unknown Field $left On ${getMeta.entity}")
    }
    if (!referMeta.fieldMap.contains(right)) {
      throw new RuntimeException(s"Unknown Field $right On ${referMeta.entity}")
    }
    if (getMeta.db != referMeta.db) {
      throw new RuntimeException(s"$left And $right Not The Same DB")
    }
    val that = this
    val join: JoinImpl = inner.joins.find(_.inner.joinName == referMeta.entity) match {
      case Some(p) => p
      case None =>
        //        val join = new JoinImpl(referMeta, this, referMeta.entity, left, right, joinType)
        val (l, r, jt) = (left, right, joinType)
        val joinInner = new JoinInner {
          override val meta: EntityMeta = referMeta
          override val parent: Join = that
          override val joinName: String = referMeta.entity
          override val left: String = l
          override val right: String = r
          override val joinType: JoinType = jt
        }
        val join = new JoinImpl {
          override val inner: JoinInner = joinInner
        }
        inner.joins += join
        join
    }
    val joinInner = join.inner
    new SelectableImpl[T] with SelectFieldJoinImpl with JoinImpl {
      override val inner: JoinInner = joinInner
    }
  }

  override def get(field: String): Field = {
    if (!getMeta.fieldMap.contains(field) || getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    val fieldMeta = getMeta.fieldMap(field)
    new FieldImpl(fieldMeta, this)
  }

  override def as[T](clazz: Class[T]): SelectableJoin[T] = {
    require(getMeta.clazz == clazz)
    val joinInner = inner
    new SelectableImpl[T] with SelectFieldJoinImpl with JoinImpl {
      override val inner: JoinInner = joinInner
    }
  }

  override def getAlias: String = {
    inner.parent match {
      case null => Kit.lowerCaseFirst(getMeta.entity)
      case _ => s"${inner.parent.getAlias}_${Kit.lowerCaseFirst(inner.joinName)}"
    }
  }

  override def getSql: String = {
    val self = if (inner.parent == null) {
      s"`${getMeta.table}` AS `$getAlias`"
    } else {
      val leftColumn = inner.parent.getMeta.fieldMap(inner.left).column
      val rightColumn = getMeta.fieldMap(inner.right).column
      val leftTable = inner.parent.getAlias
      val rightTable = getAlias
      val condSql = new JoinCond(leftTable, leftColumn, rightTable, rightColumn).and(inner.cond).getSql
      s"${inner.joinType.toString} JOIN `${getMeta.table}` AS `$getAlias` ON $condSql"
    }
    (Array(self) ++ inner.joins.map(_.getSql)).mkString("\n")
  }

  override def getParams: Array[Object] = inner.cond.getParams ++ inner.joins.flatMap(_.getParams).toArray[Object]

  override def on(c: Cond): Join = {
    inner.cond = c
    this
  }
}

trait SelectFieldJoinImpl extends SelectFieldJoin {
  self: JoinImpl =>
  protected[orm] var selects: ArrayBuffer[(String, SelectFieldJoinRet)] = new ArrayBuffer[(String, SelectFieldJoinRet)]()
  //  protected[impl] var fields: Array[FieldImpl] = getMeta.fields().filter(_.isNormalOrPkey).map(f => new FieldImpl(f, this)).toArray
  protected[orm] var fields: Array[FieldImpl] = _
  protected[orm] var ignores: Set[String] = Set()

  override def select(field: String): SelectFieldJoinRet = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Object Field $field")
    }
    selects.find(_._1 == field) match {
      case Some(p) => p._2
      case None =>
        val j = leftJoin(field).asInstanceOf[JoinImpl]
        val s = new SelectFieldJoinImpl with JoinImpl {
          override val inner: JoinInner = j.inner
        }
        selects += ((field, s))
        s
    }
  }

  def validFields(): Array[FieldImpl] = {
    (fields match {
      case null =>
        getMeta.fields().filter(_.isNormalOrPkey)
          .map(f => new FieldImpl(f, this)).toArray
      case _ => fields
    }).filter(f => !ignores.contains(f.getField))
  }

  override def fields(fields: String*): SelectFieldJoinRet = {
    val ret = fields.map(f => {
      if (!this.getMeta.fieldMap.contains(f)) {
        throw new RuntimeException(s"Invalid Field $f In ${this.getMeta.entity}")
      }
      if (!this.getMeta.fieldMap(f).isNormal) {
        throw new RuntimeException(s"Not Normal Field $f In ${this.getMeta.entity}")
      }
      new FieldImpl(this.getMeta.fieldMap(f), this)
    })
    val pkey = new FieldImpl(this.getMeta.pkey, this)
    //    this.fields = Array(pkey) ++ fields.map(this.getMeta.fieldMap(_)).map(f => new FieldImpl(f, this))
    this.fields = Array(pkey) ++ ret
    this
  }

  override def ignore(fields: String*): SelectFieldJoinRet = {
    fields.foreach(f => {
      if (!getMeta.fieldMap.contains(f)) {
        throw new RuntimeException(s"Not Exists Field, $f")
      }
      val fieldMeta = getMeta.fieldMap(f)
      if (!fieldMeta.isNormal) {
        throw new RuntimeException(s"Only Normal Field Can Ignore, $f")
      }
    })
    this.ignores = fields.toSet
    this
  }

  protected def getFilterKey(core: EntityCore): String = {
    s"$getAlias@${core.getPkey.toString}"
  }

  override def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val map: Map[String, Object] = validFields().map(field => {
      val alias = field.getAlias
      val value = resultSet.getObject(alias)
      (field.meta.name, value)
    })(collection.breakOut)
    val core = new EntityCore(getMeta, map)
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
}

trait SelectableImpl[T] extends Selectable[T] {
  self: SelectFieldJoinImpl with JoinImpl =>

  private def getOneManyFilterKey(field: String, core: EntityCore): String = {
    s"$getAlias@$field@${core.getPkey.toString}"
  }

  private def pickSelfAndRefer(select: SelectFieldJoinImpl, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val a = select.pickSelf(resultSet, filterMap)
    if (a == null) {
      return null
    }
    pickRefer(select, a, resultSet, filterMap)
    a
  }

  protected def pickRefer(selfSelect: SelectFieldJoinImpl, a: Object, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]) {
    val aCore = EntityManager.core(a)
    selfSelect.selects.foreach { case (field, select) =>
      val fieldMeta = getMeta.fieldMap(field)
      val b = pickSelfAndRefer(select.asInstanceOf[SelectFieldJoinImpl], resultSet, filterMap)
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
              val arr = aCore.fieldMap(field).asInstanceOf[Array[_]].map(_.asInstanceOf[Entity])
              val brr = Kit.newArray(f.refer.clazz, arr ++ Array(b): _*)
              aCore.fieldMap += (field -> brr)
            }
            filterMap += (key -> b)
          }
      }
    }
  }

  override def getColumnWithAs: String = {
    def go(select: SelectFieldJoinImpl): Array[String] = {
      val selfColumn = select.validFields().map(field => s"${field.getColumn} AS ${field.getAlias}")
      // 1. 属于自己的字段 2. 级联的部分
      selfColumn ++ select.selects.map(_._2.asInstanceOf[SelectFieldJoinImpl]).flatMap(go)
    }

    go(this).mkString(",\n")
  }

  override def getType: Class[T] = getMeta.clazz.asInstanceOf[Class[T]]

  override def getKey(value: Object): String = {
    value match {
      case null => ""
      case _ => value.asInstanceOf[Entity].$$core().getPkey.toString
    }
  }

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = {
    pickSelfAndRefer(this, resultSet, filterMap).asInstanceOf[T]
  }
}

trait TypedJoinImpl[T] extends TypedJoin[T] {
  self: JoinImpl =>

  def typedJoin[R](field: String, joinType: JoinType) = {
    val j: JoinImpl = this.join(field, joinType).asInstanceOf[JoinImpl]
    new TypedJoinImpl[R] with SelectableImpl[R] with SelectFieldJoinImpl with JoinImpl {
      override val inner: JoinInner = j.inner
    }
  }

  override def join[R](fn: (T) => R, joinType: JoinType): TypedJoinRet[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedJoin(field, joinType)
  }

  override def joins[R](fn: (T) => Array[R], joinType: JoinType) = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedJoin(field, joinType)
  }

  override def get[R](fn: T => R): SelectableField[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = get(marker.toString).asInstanceOf[FieldImpl]
    new SelectableFieldImpl[R](field.meta.clazz.asInstanceOf[Class[R]], field)
  }

  override def as() = {
    val joinInner = inner
    new TypedSelectJoinImpl[T] with TypedJoinImpl[T] with SelectableImpl[T] with SelectFieldJoinImpl with JoinImpl {
      override val inner: JoinInner = joinInner
    }
  }

  override def joinAs[R](clazz: Class[R], joinType: JoinType)
                        (leftFn: (T) => Object, rightFn: (R) => Object): TypedSelectJoinRet[R] = {
    if (!OrmMeta.entityMap.contains(clazz)) {
      throw new RuntimeException(s"$clazz Is Not Entity")
    }
    val referMeta = OrmMeta.entityMap(clazz)
    val lm = EntityManager.createMarker[T](getMeta)
    val rm = EntityManager.createMarker[R](referMeta)
    leftFn(lm)
    rightFn(rm)
    val left = lm.toString
    val right = rm.toString
    val j = this.joinAs(left, right, clazz).asInstanceOf[JoinImpl]
    new TypedSelectJoinImpl[R] with TypedJoinImpl[R] with SelectableImpl[R] with SelectFieldJoinImpl with JoinImpl {
      override val inner: JoinInner = j.inner
    }
  }
}

trait TypedSelectJoinImpl[T] extends TypedSelectJoin[T] {
  self: TypedJoinImpl[T] with SelectableImpl[T] with SelectFieldJoinImpl with JoinImpl =>

  def typedSelect[R](field: String) = {
    val j = this.select(field).asInstanceOf[SelectFieldJoinImpl with JoinImpl]
    val ret = new TypedSelectJoinImpl[R] with TypedJoinImpl[R] with SelectableImpl[R] with SelectFieldJoinImpl with JoinImpl {
      override val inner: JoinInner = j.inner
    }
    ret.fields = j.fields
    ret.ignores = j.ignores
    ret.selects = j.selects
    ret
  }

  override def select[R](fn: (T) => R): TypedSelectJoinRet[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedSelect[R](field)
  }

  override def selects[R](fn: (T) => Array[R]) = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedSelect[R](field)
  }

  override def fields(fns: (T => Object)*): TypedSelectJoinRet[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    this.fields(fields: _*)
    this
  }

  override def ignore(fns: (T => Object)*): TypedSelectJoinRet[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    this.ignore(fields: _*)
    this
  }
}

trait RootImpl[T] extends Root[T] {
  self: SelectableImpl[T] with SelectFieldJoinImpl with JoinImpl =>

  override def count(): Selectable[java.lang.Long] = new Count_(this)

  override def count(field: Field): SelectableField[lang.Long] = new Count(field)

  override def sum[R](field: Field, clazz: Class[R]): SelectableField[R] = new Sum[R](field, clazz)

  override def max[R](field: Field, clazz: Class[R]): SelectableField[R] = new Max(field, clazz)

  override def min[R](field: Field, clazz: Class[R]): SelectableField[R] = new Min(field, clazz)
}