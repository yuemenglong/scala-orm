package io.github.yuemenglong.orm.operate.traits.core

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.impl.core._
import io.github.yuemenglong.orm.operate.traits.core.JoinType.JoinType

import scala.collection.mutable

/**
  * Created by yml on 2017/7/14.
  */
trait Queryable[T] {
  def query(session: Session): Array[T]

  def walk(t: T, fn: (Entity) => Entity): T

  def getType: Class[T]
}

trait Selectable[T] {
  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getColumnWithAs: String

  def getType: Class[T]

  def getKey(value: Object): String
}

trait SelectableField[T] extends Field with Selectable[T] {
  override def getColumnWithAs: String = s"$getColumn AS $getAlias"

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = resultSet.getObject(getAlias, getType)

  override def getKey(value: Object): String = value match {
    case null => ""
    case _ => value.toString
  }

  def distinct(): SelectableField[T]
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