package yy.orm.operate.traits.core

import java.sql.{Connection, ResultSet}

import yy.orm.lang.interfaces.Entity
import yy.orm.operate.traits.core.JoinType.JoinType

import scala.collection.mutable

/**
  * Created by yml on 2017/7/14.
  */
trait Queryable[T] {
  def query(conn: Connection): Array[T]
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


