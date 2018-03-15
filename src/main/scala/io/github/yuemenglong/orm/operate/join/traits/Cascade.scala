package io.github.yuemenglong.orm.operate.join.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.meta.EntityMeta
import io.github.yuemenglong.orm.operate.core.traits.{Alias, Expr, Params}
import io.github.yuemenglong.orm.operate.field.traits._
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.join._
import io.github.yuemenglong.orm.operate.query.traits.Selectable

import scala.collection.mutable

trait Table extends Params with Alias {
  def getTable: String

  def get(field: String): Field

  //  def join(left: String, right: String, table: Table, joinType: JoinType): this.type
  //
  //  def join(left: String, right: String, table: Table): this.type = join(left, right, table, JoinType.INNER)
  //
  //  def leftJoin(left: String, right: String, table: Table): this.type = join(left, right, table, JoinType.LEFT)

  def on(c: Cond): this.type
}

trait Cascade extends Table {
  type SelectableJoin[T] = Selectable[T] with SelectFieldCascade with Cascade

  def getMeta: EntityMeta

  def join(field: String): Cascade = join(field, JoinType.INNER)

  def join(field: String, joinType: JoinType): Cascade

  def leftJoin(field: String): Cascade = join(field, JoinType.LEFT)

  def as[T](clazz: Class[T]): SelectableJoin[T]

  def joinAs[T](field: String, clazz: Class[T], joinType: JoinType): SelectableJoin[T] = this.join(field, joinType).as(clazz)

  def joinAs[T](field: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(field, clazz, JoinType.INNER)

  def leftJoinAs[T](field: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(field, clazz, JoinType.LEFT)

  def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): SelectableJoin[T]

  def joinAs[T](left: String, right: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(left, right, clazz, JoinType.INNER)

  def leftJoinAs[T](left: String, right: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(left, right, clazz, JoinType.LEFT)
}

trait SelectFieldCascade {
  type SelectFieldCascadeRet = SelectFieldCascade with Cascade

  def select(field: String): SelectFieldCascadeRet

  def fields(fields: String*): SelectFieldCascadeRet

  def ignore(fields: String*): SelectFieldCascadeRet

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity

}

trait TypedBase {
  type TypedCascadeRet[R] = TypedCascadeImpl[R] with CascadeImpl
  type TypedSelectCascadeRet[R] = TypedSelectCascadeImpl[R] with TypedCascadeImpl[R]
    with SelectableImpl[R] with SelectFieldCascadeImpl with CascadeImpl
}

trait TypedCascade[T] extends TypedBase {

  def apply[R](fn: T => R): SelectableField[R] = get(fn)

  def get[R](fn: (T => R)): SelectableField[R]

  def join[R](fn: (T => R), joinType: JoinType): TypedCascadeRet[R]

  def join[R](fn: (T => R)): TypedCascadeRet[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedCascadeRet[R] = join(fn, JoinType.LEFT)

  def leftJoinAs[R](fn: (T => R)): TypedCascadeRet[R] = join(fn, JoinType.LEFT).as()

  def joins[R](fn: (T => Array[R]), joinType: JoinType): TypedCascadeRet[R]

  def joins[R](fn: (T => Array[R])): TypedCascadeRet[R] = joins(fn, JoinType.INNER)

  def leftJoins[R](fn: (T => Array[R])): TypedCascadeRet[R] = joins(fn, JoinType.LEFT)

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedSelectCascadeRet[R]

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedSelectCascadeRet[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedSelectCascadeRet[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)

  def as(): TypedSelectCascadeRet[T]
}

trait TypedSelectCascade[T] extends TypedBase {

  def select[R](fn: T => R): TypedSelectCascadeRet[R]

  def selects[R](fn: T => Array[R]): TypedSelectCascadeRet[R]

  def fields(fns: (T => Object)*): TypedSelectCascadeRet[T]

  def ignore(fns: (T => Object)*): TypedSelectCascadeRet[T]
}


