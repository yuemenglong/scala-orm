package io.github.yuemenglong.orm.operate.join.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.entity.{EntityCore, EntityManager}
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.core.traits.Join
import io.github.yuemenglong.orm.operate.field.{FieldImpl, SelectableFieldImpl}
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.JoinType
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.query.traits.Selectable

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait Cascade extends Join {

  def getMeta: EntityMeta = inner.meta

  def get(field: String): Field = {
    if (!getMeta.fieldMap.contains(field) || getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    val fieldMeta = getMeta.fieldMap(field)
    new FieldImpl(fieldMeta, this)
  }

  def join(field: String, joinType: JoinType): Cascade = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    if (getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer.db != getMeta.db) {
      throw new RuntimeException(s"Field $field Not the Same DB")
    }
    val referMeta = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
    val table = referMeta.refer.table
    val leftColumn = getMeta.fieldMap(referMeta.left).column
    val rightColumn = referMeta.refer.fieldMap(referMeta.right).column
    val j = join(leftColumn, rightColumn, table, field, joinType)
    j.inner.meta = referMeta.refer
    new Cascade {
      override val inner = j.inner
    }
  }

  def join(field: String): Cascade = join(field, JoinType.INNER)

  def leftJoin(field: String): Cascade = join(field, JoinType.LEFT)

  def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): TypedSelectableCascade[T] = {
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
    val joinName = Kit.lowerCaseFirst(referMeta.entity)
    val table = referMeta.table
    val leftColumn = getMeta.fieldMap(left).column
    val rightColumn = referMeta.fieldMap(right).column
    val j = join(leftColumn, rightColumn, table, joinName, joinType)
    j.inner.meta = referMeta
    new TypedSelectableCascade[T] {
      override private[orm] val inner = j.inner
    }
  }

  def joinAs[T](left: String, right: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(left, right, clazz, JoinType.INNER)

  def leftJoinAs[T](left: String, right: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(left, right, clazz, JoinType.LEFT)
}

trait SelectFieldCascade extends Cascade {

  def select(field: String): SelectFieldCascade = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Object Field $field")
    }
    getSelects.find(_._1 == field) match {
      case Some(p) => p._2
      case None =>
        val j = leftJoin(field)
        val s = new SelectFieldCascade {
          override private[orm] val inner = j.inner
        }
        inner.selects += ((field, s))
        s
    }
  }

  def fields(fields: String*): SelectFieldCascade = {
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
    inner.fields = Array(pkey) ++ ret
    this
  }

  def ignore(fields: String*): SelectFieldCascade = {
    fields.foreach(f => {
      if (!getMeta.fieldMap.contains(f)) {
        throw new RuntimeException(s"Not Exists Field, $f")
      }
      val fieldMeta = getMeta.fieldMap(f)
      if (!fieldMeta.isNormal) {
        throw new RuntimeException(s"Only Normal Field Can Ignore, $f")
      }
    })
    inner.ignores = fields.toSet
    this
  }

  private[orm] def validFields(): Array[FieldImpl] = {
    val fields = inner.fields match {
      case null =>
        getMeta.fields().filter(_.isNormalOrPkey)
          .map(f => new FieldImpl(f, this)).toArray
      case _ => inner.fields
    }
    val ignores = inner.ignores match {
      case null => Set[String]()
      case _ => inner.ignores
    }
    fields.filter(f => !ignores.contains(f.getField))
  }

  private[orm] def getFilterKey(core: EntityCore): String = {
    s"$getAlias@${core.getPkey.toString}"
  }

  private[orm] def getSelects: ArrayBuffer[(String, SelectFieldCascade)] = {
    if (inner.selects == null) {
      inner.selects = new ArrayBuffer[(String, SelectFieldCascade)]()
    }
    inner.selects
  }

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
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

  def getColumnWithAs: String = {
    def go(select: SelectFieldCascade): Array[String] = {
      val selfColumn = select.validFields().map(field => s"${field.getColumn} AS ${field.getAlias}")
      // 1. 属于自己的字段 2. 级联的部分
      select.inner.selects match {
        case null => selfColumn
        case selects => selfColumn ++ selects.map(_._2.asInstanceOf[SelectFieldCascade]).flatMap(go)
      }
    }

    go(this).mkString(",\n")
  }
}

trait TypedCascade[T] extends Cascade {

  private def typedCascade[R](field: String, joinType: JoinType) = {
    val j: Cascade = this.join(field, joinType)
    new TypedCascade[R] {
      override private[orm] val inner = j.inner
    }
  }

  def apply[R](fn: T => R): SelectableField[R] = get(fn)

  def get[R](fn: (T => R)): SelectableField[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = get(marker.toString).asInstanceOf[FieldImpl]
    new SelectableFieldImpl[R](field.meta.clazz.asInstanceOf[Class[R]], field)
  }

  def join[R](fn: (T => R), joinType: JoinType): TypedCascade[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedCascade(field, joinType)
  }

  def join[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.LEFT)

  def leftJoinAs[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.LEFT)

  def joins[R](fn: (T => Array[R]), joinType: JoinType): TypedCascade[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedCascade(field, joinType)
  }

  def joins[R](fn: (T => Array[R])): TypedCascade[R] = joins(fn, JoinType.INNER)

  def leftJoins[R](fn: (T => Array[R])): TypedCascade[R] = joins(fn, JoinType.LEFT)

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedCascade[R] = {
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
    val j = this.joinAs(left, right, clazz)
    new TypedSelectableCascade[R] {
      override private[orm] val inner = j.inner
    }
  }

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedCascade[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedCascade[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)

  def as(): TypedSelectableCascade[T] = {
    val that = this
    new TypedSelectableCascade[T] {
      override private[orm] val inner = that.inner
    }
  }
}

trait TypedSelectableCascade[T] extends TypedCascade[T]
  with SelectFieldCascade with Selectable[T] {

  private def typedSelect[R](field: String) = {
    val j = this.select(field)
    val ret = new TypedSelectableCascade[R] {
      override private[orm] val inner = j.inner
    }
    ret
  }

  def select[R](fn: T => R): TypedSelectableCascade[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedSelect[R](field)
  }

  def selects[R](fn: T => Array[R]): TypedSelectableCascade[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedSelect[R](field)
  }

  def fields(fns: (T => Object)*): TypedSelectableCascade[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    this.fields(fields: _*)
    this
  }

  def ignore(fns: (T => Object)*): TypedSelectableCascade[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    this.ignore(fields: _*)
    this
  }

  private def pickSelfAndRefer(select: SelectFieldCascade, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val a = select.pickSelf(resultSet, filterMap)
    if (a == null) {
      return null
    }
    pickRefer(select, a, resultSet, filterMap)
    a
  }

  private def getOneManyFilterKey(field: String, core: EntityCore): String = {
    s"$getAlias@$field@${core.getPkey.toString}"
  }

  private def pickRefer(selfSelect: SelectFieldCascade, a: Object, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]) {
    val aCore = EntityManager.core(a)
    selfSelect.getSelects.foreach { case (field, select) =>
      val fieldMeta = getMeta.fieldMap(field)
      val b = pickSelfAndRefer(select, resultSet, filterMap)
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

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = {
    pickSelfAndRefer(this, resultSet, filterMap).asInstanceOf[T]
  }

  override def getKey(value: Object): String = {
    value match {
      case null => ""
      case _ => value.asInstanceOf[Entity].$$core().getPkey.toString
    }
  }

  override def getType: Class[T] = getMeta.clazz.asInstanceOf[Class[T]]
}


