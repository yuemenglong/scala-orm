package io.github.yuemenglong.orm.operate.join.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.kit.{Kit, UnreachableException}
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.meta.{EntityMeta, FieldMetaRefer}
import io.github.yuemenglong.orm.operate.core.traits.Join
import io.github.yuemenglong.orm.operate.field.FieldImpl
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.JoinType
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.query.traits.Selectable
//import io.github.yuemenglong.orm.operate.field.FieldImpl
//import io.github.yuemenglong.orm.operate.field.traits._
//import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
//import io.github.yuemenglong.orm.operate.join._
//import io.github.yuemenglong.orm.operate.query.traits.Selectable

import scala.collection.mutable

trait Cascade extends Join {
  val inner: Join

  def getMeta: EntityMeta

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
    new Cascade {
      override def getMeta = referMeta.refer

      override val inner = j
    }
  }

  def join(field: String): Cascade = join(field, JoinType.INNER)

  def leftJoin(field: String): Cascade = join(field, JoinType.LEFT)

  //  def joinAs[T](field: String, clazz: Class[T], joinType: JoinType): TypedSelectableCascade[T]
  //
  //  def joinAs[T](field: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(field, clazz, JoinType.INNER)
  //
  //  def leftJoinAs[T](field: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(field, clazz, JoinType.LEFT)
  //
  //  def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): TypedSelectableCascade[T]
  //
  //  def joinAs[T](left: String, right: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(left, right, clazz, JoinType.INNER)
  //
  //  def leftJoinAs[T](left: String, right: String, clazz: Class[T]): TypedSelectableCascade[T] = this.joinAs(left, right, clazz, JoinType.LEFT)

  //--------------------------------------------------------------

  override def getTableName = inner.getTableName

  override def getParent = inner.getParent

  override def getLeftColumn = inner.getLeftColumn

  override def getRightColumn = inner.getRightColumn

  override def getJoinType = inner.getJoinType

  override def getJoinName = inner.getJoinName
}


trait SelectFieldCascade extends Cascade {
  type SelectFieldCascadeRet = SelectFieldCascade with Cascade

  def select(field: String): SelectFieldCascadeRet

  def fields(fields: String*): SelectFieldCascadeRet

  def ignore(fields: String*): SelectFieldCascadeRet

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity

}

trait TypedCascade[T] extends Cascade {

  def apply[R](fn: T => R): SelectableField[R] = get(fn)

  def get[R](fn: (T => R)): SelectableField[R]

  def join[R](fn: (T => R), joinType: JoinType): TypedCascade[R]

  def join[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.LEFT)

  def leftJoinAs[R](fn: (T => R)): TypedCascade[R] = join(fn, JoinType.LEFT)

  def joins[R](fn: (T => Array[R]), joinType: JoinType): TypedCascade[R]

  def joins[R](fn: (T => Array[R])): TypedCascade[R] = joins(fn, JoinType.INNER)

  def leftJoins[R](fn: (T => Array[R])): TypedCascade[R] = joins(fn, JoinType.LEFT)

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedCascade[R]

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedCascade[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedCascade[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)
}

trait TypedSelectableCascade[T] extends TypedCascade[T] with SelectFieldCascade with Selectable[T] {

  def select[R](fn: T => R): TypedSelectableCascade[R]

  def selects[R](fn: T => Array[R]): TypedSelectableCascade[R]

  def fields(fns: (T => Object)*): TypedSelectableCascade[T]

  def ignore(fns: (T => Object)*): TypedSelectableCascade[T]
}


