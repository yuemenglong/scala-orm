package io.github.yuemenglong.orm.operate.traits.core

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.impl.core._
import io.github.yuemenglong.orm.operate.traits.core.JoinType.JoinType

import scala.collection.mutable

/**
  * Created by yml on 2017/7/14.
  */
trait Queryable[T] {
  def query(conn: Connection): Array[T]

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

  def get(fn: (T => Object)): Field

  def join[R](fn: (T => R), joinType: JoinType): TypedJoinRet[R]

//  def join[R](fn: (T => Array[R]), joinType: JoinType): TypedJoinRet[R]

  def join[R](fn: (T => R)): TypedJoinRet[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedJoinRet[R] = join(fn, JoinType.LEFT)

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedSelectJoinRet[R]

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedSelectJoinRet[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedSelectJoinRet[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)
}

trait TypedSelectJoin[T] extends TypedBase {

  def select[R](fn: T => R): TypedSelectJoinRet[R]

  def fields(fns: (T => Object)*): TypedSelectJoinRet[T]

  def ignore(fns: (T => Object)*): TypedSelectJoinRet[T]
}

trait Root[T] extends TypedSelectJoin[T] with TypedJoin[T]
  with Selectable[T] with SelectFieldJoin with Join {

  def getTable: String

  def count(): Selectable[java.lang.Long]

  def count(field: Field): SelectableField[java.lang.Long]

  def count(field: String): SelectableField[java.lang.Long] = this.count(this.get(field))

  def sum(field: Field): SelectableField[java.lang.Double]

  def sum(field: String): SelectableField[java.lang.Double] = sum(this.get(field))

  def max[R](field: Field, clazz: Class[R]): SelectableField[R]

  def max[R](field: String, clazz: Class[R]): SelectableField[R] = max(this.get(field), clazz)

  def min[R](field: Field, clazz: Class[R]): SelectableField[R]

  def min[R](field: String, clazz: Class[R]): SelectableField[R] = min(this.get(field), clazz)

}