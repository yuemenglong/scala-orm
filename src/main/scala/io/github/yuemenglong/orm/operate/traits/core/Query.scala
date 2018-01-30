package io.github.yuemenglong.orm.operate.traits.core

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.String

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
  type Self = SelectFieldJoin with Join

  def select(field: String): Self

  def fields(fields: String*): Self

  def ignore(fields: String*): Self

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity
}

trait Root[T] extends Selectable[T] with SelectFieldJoin with Join {

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

//
//trait TypedJoin[T] extends Join {
//  def join[R](fn: (T => R)): TypedJoin[R] = join(fn, JoinType.INNER)
//
//  def join[R](fn: (T => R), joinType: JoinType): TypedJoin[R]
//
//  def leftJoin[R](fn: (T => R)): TypedJoin[R] = join(fn, JoinType.LEFT)
//
//  def get(fn: (T => Object)): Field
//
//  def joinAs[R](fn: (T => R), clazz: Class[R], joinType: JoinType): SelectableJoin[R] = this.join(fn, joinType).as(clazz)
//
//  def joinAs[R](fn: (T => R), clazz: Class[R]): SelectableJoin[R] = this.joinAs(fn, clazz, JoinType.INNER)
//
//  def leftJoinAs[R](fn: (T => R), clazz: Class[R]): SelectableJoin[R] = this.joinAs(fn, clazz, JoinType.LEFT)
//
//  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object)(rightFn: R => Object): SelectableJoin[R]
//
//  def joinAs[R](clazz: Class[R])(leftFn: T => Object)(rightFn: R => Object): SelectableJoin[R] = this.joinAs(clazz, JoinType.INNER)(leftFn)(rightFn)
//
//  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object)(rightFn: R => Object): SelectableJoin[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn)(rightFn)
//
//}
//
//trait TypedSelectableJoin[T] extends SelectableJoin[T] with TypedJoin[T] {
//  def fields(fns: (T => Object)*): TypedSelectableJoin[T]
//
//  def ignore(fns: (T => Object)*): TypedSelectableJoin[T]
//}
//
//trait TypedRoot[T] extends Root[T] with TypedJoin[T] {
//  def fields(fns: (T => Object)*): TypedRoot[T]
//
//  def ignore(fns: (T => Object)*): TypedRoot[T]
//}