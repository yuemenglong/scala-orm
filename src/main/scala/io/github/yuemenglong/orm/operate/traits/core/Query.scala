package io.github.yuemenglong.orm.operate.traits.core

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.lang.interfaces.Entity
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

trait Selectable[T] extends Node {
  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getColumnWithAs: String

  def getType: Class[T]

  def getKey(value: Object): String
}

trait SelectJoin extends Join {
  def select(field: String): SelectJoin

  def fields(fields: String*): SelectJoin = this.fields(fields.toArray)

  def fields(fields: Array[String]): SelectJoin

  def ignore(fields: String*): SelectJoin = this.ignore(fields.toArray)

  def ignore(fields: Array[String]): SelectJoin
}

trait SelectableJoin[T] extends Selectable[T] with SelectJoin {
  override def fields(fields: String*): SelectableJoin[T] = this.fields(fields.toArray)

  def fields(fields: Array[String]): SelectableJoin[T]

  override def ignore(fields: String*): SelectableJoin[T] = this.ignore(fields.toArray)

  def ignore(fields: Array[String]): SelectableJoin[T]
}


trait SelectableField[T] extends Field with Selectable[T] {
  override def getColumnWithAs: String = s"$getColumn AS $getAlias"

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = resultSet.getObject(getAlias, getType)

  override def getKey(value: Object): String = {
    if (value == null) {
      ""
    } else {
      value.toString
    }
  }

  def distinct(): SelectableField[T]
}

trait Root[T] extends SelectableJoin[T] {
  def count(): Selectable[java.lang.Long]

  def count(field: Field): SelectableField[java.lang.Long]

  def count(field: String): SelectableField[java.lang.Long] = this.count(this.get(field))

  def sum(field: Field): SelectableField[java.lang.Double]

  def sum(field: String): SelectableField[java.lang.Double] = sum(this.get(field))

  def max[R](field: Field, clazz: Class[R]): SelectableField[R]

  def max[R](field: String, clazz: Class[R]): SelectableField[R] = max(this.get(field), clazz)

  def min[R](field: Field, clazz: Class[R]): SelectableField[R]

  def min[R](field: String, clazz: Class[R]): SelectableField[R] = min(this.get(field), clazz)

  override def fields(fields: String*): Root[T] = this.fields(fields.toArray)

  def fields(fields: Array[String]): Root[T]

  override def ignore(fields: String*): Root[T] = this.ignore(fields.toArray)

  def ignore(fields: Array[String]): Root[T]
}

trait TypedJoin[T] extends Join {
  def join[R](fn: (T => R)): TypedJoin[R] = join(fn, JoinType.INNER)

  def join[R](fn: (T => R), joinType: JoinType): TypedJoin[R]

  def leftJoin[R](fn: (T => R)): TypedJoin[R] = join(fn, JoinType.LEFT)
}

trait TypedSelectableJoin[T] extends SelectableJoin[T] with TypedJoin[T] {
  override def fields(fields: String*): TypedSelectableJoin[T] = this.fields(fields.toArray)

  def fields(fields: Array[String]): TypedSelectableJoin[T]

  override def ignore(fields: String*): TypedSelectableJoin[T] = this.ignore(fields.toArray)

  def ignore(fields: Array[String]): TypedSelectableJoin[T]
}

trait TypedRoot[T] extends Root[T] with TypedJoin[T] {
  override def fields(fields: String*): TypedRoot[T] = this.fields(fields.toArray)

  def fields(fields: Array[String]): TypedRoot[T]

  override def ignore(fields: String*): TypedRoot[T] = this.ignore(fields.toArray)

  def ignore(fields: Array[String]): TypedRoot[T]
}