package orm.operate.traits.core

import java.sql.{Connection, ResultSet}

import orm.lang.interfaces.Entity
import orm.operate.traits.core.JoinType.JoinType

import scala.collection.mutable

/**
  * Created by yml on 2017/7/14.
  */
trait Queryable {
  def query(conn: Connection): Array[Array[Object]]
}

trait AsSelectable {
  def as[T](clazz: Class[T]): Selectable[T]
}

trait Selectable[T] extends Node {
  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getColumnWithAs: String

  def getType: Class[T]

  def getKey(value: Object): String
}

trait SelectJoin extends Join {
  def select(field: String): SelectJoin

  override def join(field: String): Join = join(field, JoinType.LEFT)
}

trait SelectRoot[T] extends Root[T] with Selectable[T] with SelectJoin {
  def count(): Selectable[java.lang.Long]
}

trait SelectBuilder {
  def from(selectRoot: SelectRoot[_]): Query
}

trait Query extends Queryable {
  def limit(l: Long): Query

  def offset(l: Long): Query

  def asc(field: Field): Query

  def desc(field: Field): Query

  def where(cond: Cond): Query
}

trait SelectBuilder1[T] {
  def from(selectRoot: SelectRoot[_]): Query1[T]
}

trait Query1[T] extends Queryable {
  def transform(res: Array[Array[Object]]): Array[T]

  def limit(l: Long): Query1[T]

  def offset(l: Long): Query1[T]

  def asc(field: Field): Query1[T]

  def desc(field: Field): Query1[T]

  def where(cond: Cond): Query1[T]
}
