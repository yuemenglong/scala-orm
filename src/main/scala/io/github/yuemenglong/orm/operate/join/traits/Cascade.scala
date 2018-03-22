package io.github.yuemenglong.orm.operate.join.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.entity.{EntityCore, EntityManager}
import io.github.yuemenglong.orm.kit.{Kit, UnreachableException}
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.JoinType
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.query.traits.Selectable
import io.github.yuemenglong.orm.sql._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

trait Cascade extends Table {
  val root: Cascade
  val meta: EntityMeta
  var joins: Map[String, (JoinType, Cascade)] = Map()

  def getMeta: EntityMeta = meta

  def get(field: String): Field = {
    if (!getMeta.fieldMap.contains(field) || getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    val alias = s"${getAlias}$$${field}"
    val column = getColumn(getMeta.fieldMap(field).column, alias)
    new Field {
      override private[orm] val uid = column.uid
      override private[orm] val expr = column.expr
    }
  }

  def join(field: String, joinType: JoinType): Cascade = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    if (getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer.db != getMeta.db) {
      throw new RuntimeException(s"Field $field Not the Same DB")
    }
    joins.get(field) match {
      case Some((t, c)) =>
        if (t != joinType) throw new RuntimeException(s"JoinType Not Match, [${t}:${joinType}]")
        c
      case None =>
        val referMeta = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
        val tableName = referMeta.refer.table
        val alias = s"${getAlias}_${Kit.lowerCaseFirst(field)}"
        val leftColumn = getMeta.fieldMap(referMeta.left).column
        val rightColumn = referMeta.refer.fieldMap(referMeta.right).column
        val table = Table(tableName, alias)
        val that = this
        join(table, joinType.toString, leftColumn, rightColumn)
        val ret = new Cascade {
          override private[orm] val children = table.children
          override val meta = referMeta.refer
          override val root = that.root
        }
        joins += (field -> (joinType, ret))
        ret
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
    val tableName = referMeta.table
    val leftColumn = getMeta.fieldMap(left).column
    val rightColumn = referMeta.fieldMap(right).column
    val alias = s"${getAlias}__${joinName}"
    val table = Table(tableName, alias)
    join(table, joinType.toString, leftColumn, rightColumn)
    val that = this
    new TypedSelectableCascade[T] {
      override val meta = referMeta
      override private[orm] val children = table.children
      override val root = that.root
    }
  }

  def joinAs[T](left: String, right: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(left, right, clazz, JoinType.INNER)

  def leftJoinAs[T](left: String, right: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(left, right, clazz, JoinType.LEFT)

  protected def on[T](e: ExprT[_], ret: T): T = {
    // 从根节点找到自己对应的join数据结构，改掉
    val jp = TableSource.asJoinPart(root)
    jp.joins.zipWithIndex.find(_._1._2.children sameElements this.children) match {
      case None => throw new UnreachableException
      case Some(((joinType, table, expr), idx)) =>
        jp.joins(idx) = (joinType, table, expr.and(e))
    }
    ret
  }

  def on(e: ExprT[_]): Cascade = on(e, this)
}

trait SelectFieldCascade extends Cascade {
  private[orm] var _selects = new ArrayBuffer[(String, SelectFieldCascade)]()
  private[orm] var _fields = Array[String]()
  private[orm] var _ignores = Set[String]()

  override def on(e: ExprT[_]): SelectFieldCascade = on(e, this)

  def select(field: String): SelectFieldCascade = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Object Field $field")
    }
    _selects.find(_._1 == field) match {
      case Some(p) => p._2
      case None =>
        val j = leftJoin(field)
        val ret = new SelectFieldCascade {
          override private[orm] val children = j.children
          override val meta = j.meta
          override val root = j.root
        }
        _selects += ((field, ret))
        ret
    }
  }

  def fields(fields: String*): SelectFieldCascade = {
    fields.foreach(f => {
      if (!this.getMeta.fieldMap.contains(f)) {
        throw new RuntimeException(s"Invalid Field $f In ${this.getMeta.entity}")
      }
      if (!this.getMeta.fieldMap(f).isNormal) {
        throw new RuntimeException(s"Not Normal Field $f In ${this.getMeta.entity}")
      }
    })
    _fields = Array(this.getMeta.pkey.name) ++ fields
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
    _ignores = fields.toSet
    this
  }

  private[orm] def validFields(): Array[String] = {
    val fs: Array[String] = _fields.isEmpty match {
      case true => getMeta.fieldVec.filter(_.isNormalOrPkey).map(_.name).toArray
      case false => _fields
    }
    fs.filter(!_ignores.contains(_))
  }

  private[orm] def getFilterKey(core: EntityCore): String = {
    s"$getAlias@${core.getPkey.toString}"
  }

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val map: Map[String, Object] = validFields().map(f => {
      val field = get(f)
      val alias = field.getAlias
      val value = resultSet.getObject(alias)
      (f, value)
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

trait TypedCascade[T] extends Cascade {
  override def on(e: ExprT[_]): TypedCascade[T] = on(e, this)

  private def typedCascade[R](field: String, joinType: JoinType) = {
    val j: Cascade = this.join(field, joinType)
    new TypedCascade[R] {
      override val meta = j.meta
      override private[orm] val children = j.children
      override val root = j.root
    }
  }

  def apply[R](fn: T => R): SelectableField[R] = get(fn)

  def get[R](fn: (T => R)): SelectableField[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = get(marker.toString)
    new SelectableField[R] {
      override val clazz = getMeta.fieldMap(marker.toString).clazz.asInstanceOf[Class[R]]
      override private[orm] val expr = field.expr
      override private[orm] val uid = field.uid
    }
  }

  def join[R](fn: (T => R), joinType: JoinType): TypedCascade[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedCascade(field, joinType)
  }

  def join[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.LEFT)

  def joinAs[R](fn: (T => R), joinType: JoinType): TypedSelectableCascade[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    val referMeta = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
    val left = referMeta.left
    val right = referMeta.right
    joinAs(left, right, referMeta.refer.clazz.asInstanceOf[Class[R]], joinType)
  }

  def joinAs[R](fn: (T => R)): TypedSelectableCascade[R] = joinAs(fn, JoinType.INNER)

  def leftJoinAs[R](fn: (T => R)): TypedSelectableCascade[R] = joinAs(fn, JoinType.LEFT)

  def joinsAs[R](fn: (T => Array[R]), joinType: JoinType): TypedSelectableCascade[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    val referMeta = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
    val left = referMeta.left
    val right = referMeta.right
    joinAs(left, right, referMeta.refer.clazz.asInstanceOf[Class[R]], joinType)
  }

  def joinsAs[R](fn: (T => Array[R])): TypedSelectableCascade[R] = joinsAs(fn, JoinType.INNER)

  def leftJoinsAs[R](fn: (T => Array[R])): TypedCascade[R] = joinsAs(fn, JoinType.LEFT)

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
      override val meta = j.meta
      override private[orm] val children = j.children
      override val root = j.root
    }
  }

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedCascade[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedCascade[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)

}

trait TypedSelectableCascade[T] extends TypedCascade[T]
  with SelectFieldCascade with Selectable[T] {
  override def on(e: ExprT[_]): TypedSelectableCascade[T] = on(e, this)

  private def typedSelect[R](field: String) = {
    val j = this.select(field)
    val ret = new TypedSelectableCascade[R] {
      override val meta = j.meta
      override private[orm] val children = j.children
      override val root = j.root
    }
    ret
  }

  override def getColumns: Array[ResultColumn] = {
    val ab = new ArrayBuffer[ResultColumn]()

    def go(cascade: SelectFieldCascade): Unit = {
      val self = cascade.validFields().map(cascade.get)
      ab ++= self
      cascade._selects.foreach(s => go(s._2))
    }

    go(this)
    ab.toArray
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
    selfSelect._selects.foreach { case (field, select) =>
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


