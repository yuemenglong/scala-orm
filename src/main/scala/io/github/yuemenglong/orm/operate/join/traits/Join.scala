package io.github.yuemenglong.orm.operate.join.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.meta.EntityMeta
import io.github.yuemenglong.orm.operate.field.traits._
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.join._
import io.github.yuemenglong.orm.operate.query.traits.Selectable

import scala.collection.mutable

trait Expr {
  def getSql: String

  def getParams: Array[Object]
}

trait Join extends Expr {
  type SelectableJoin[T] = Selectable[T] with SelectFieldJoin with Join

  def getMeta: EntityMeta

  def getAlias: String

  def getTableWithJoinCond: String

  def join(field: String): Join = join(field, JoinType.INNER)

  def join(field: String, joinType: JoinType): Join

  def leftJoin(field: String): Join = join(field, JoinType.LEFT)

  def get(field: String): Field

  def on(c: Cond): Join

  def as[T](clazz: Class[T]): SelectableJoin[T]

  def joinAs[T](field: String, clazz: Class[T], joinType: JoinType): SelectableJoin[T] = this.join(field, joinType).as(clazz)

  def joinAs[T](field: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(field, clazz, JoinType.INNER)

  def leftJoinAs[T](field: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(field, clazz, JoinType.LEFT)

  def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): SelectableJoin[T]

  def joinAs[T](left: String, right: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(left, right, clazz, JoinType.INNER)

  def leftJoinAs[T](left: String, right: String, clazz: Class[T]): SelectableJoin[T] = this.joinAs(left, right, clazz, JoinType.LEFT)

  override def getSql: String = getTableWithJoinCond
}

trait SelectFieldJoin {
  type SelectFieldJoinRet = SelectFieldJoin with Join

  def select(field: String): SelectFieldJoinRet

  def fields(fields: String*): SelectFieldJoinRet

  def ignore(fields: String*): SelectFieldJoinRet

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity

}

trait TypedBase {
  type TypedJoinRet[R] = TypedJoinImpl[R] with JoinImpl
  type TypedSelectJoinRet[R] = TypedSelectJoinImpl[R] with TypedJoinImpl[R]
    with SelectableImpl[R] with SelectFieldJoinImpl with JoinImpl
}

trait TypedJoin[T] extends TypedBase {

  def get[R](fn: (T => R)): SelectableField[R]

  def join[R](fn: (T => R), joinType: JoinType): TypedJoinRet[R]

  def join[R](fn: (T => R)): TypedJoinRet[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedJoinRet[R] = join(fn, JoinType.LEFT)

  def leftJoinAs[R](fn: (T => R)): TypedJoinRet[R] = join(fn, JoinType.LEFT).as()

  def joins[R](fn: (T => Array[R]), joinType: JoinType): TypedJoinRet[R]

  def joins[R](fn: (T => Array[R])): TypedJoinRet[R] = joins(fn, JoinType.INNER)

  def leftJoins[R](fn: (T => Array[R])): TypedJoinRet[R] = joins(fn, JoinType.LEFT)

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedSelectJoinRet[R]

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedSelectJoinRet[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedSelectJoinRet[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)

  def as(): TypedSelectJoinRet[T]
}

trait TypedSelectJoin[T] extends TypedBase {

  def select[R](fn: T => R): TypedSelectJoinRet[R]

  def selects[R](fn: T => Array[R]): TypedSelectJoinRet[R]

  def select[R](): (T => Array[R]) => TypedSelectJoinRet[R] = fn => selects(fn)

  def fields(fns: (T => Object)*): TypedSelectJoinRet[T]

  def ignore(fns: (T => Object)*): TypedSelectJoinRet[T]
}

trait TypedRoot[T] {
  def count(fn: T => Object): SelectableField[java.lang.Long]

  def sum[R](fn: T => R): SelectableField[R]

  def max[R](fn: T => R): SelectableField[R]

  def min[R](fn: T => R): SelectableField[R]
}

trait Root[T] extends TypedRoot[T] with TypedSelectJoin[T] with TypedJoin[T]
  with Selectable[T] with SelectFieldJoin with Join {

  def getTable: String

  def count(): Selectable[java.lang.Long]

  def count(field: Field): SelectableField[java.lang.Long]

  def count(field: String): SelectableField[java.lang.Long] = count(this.get(field))

  def sum[R](field: Field, clazz: Class[R]): SelectableField[R]

  def sum[R](field: SelectableField[R]): SelectableField[R] = sum(field, field.getType)

  def sum[R](field: String, clazz: Class[R]): SelectableField[R] = sum(this.get(field), clazz)

  def max[R](field: Field, clazz: Class[R]): SelectableField[R]

  def max[R](field: SelectableField[R]): SelectableField[R] = max(field, field.getType)

  def max[R](field: String, clazz: Class[R]): SelectableField[R] = max(this.get(field), clazz)

  def min[R](field: Field, clazz: Class[R]): SelectableField[R]

  def min[R](field: SelectableField[R]): SelectableField[R] = min(field, field.getType)

  def min[R](field: String, clazz: Class[R]): SelectableField[R] = min(this.get(field), clazz)
}

