package yy.orm.operate.traits.core

import java.sql.{Connection, ResultSet}

import yy.orm.lang.interfaces.Entity

import scala.collection.mutable

/**
  * Created by yml on 2017/7/14.
  */
trait Queryable[T] {
  def query(conn: Connection): Array[T]

  def walk(t: T, fn: (Entity) => Entity): T

  def getType: Class[T]
}

//trait AsSelectable {
//  def as[T](clazz: Class[T]): Selectable[T]
//}

trait Selectable[T] extends Node {
  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getColumnWithAs: String

  def getType: Class[T]

  def getKey(value: Object): String
}

trait SelectJoin extends Join {
  def select(field: String): SelectJoin
}

trait SelectableJoin[T] extends Selectable[T] with SelectJoin

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

trait SelectRoot[T] extends Root[T] with Selectable[T] with SelectJoin {
  def count(): Selectable[java.lang.Long]

  def count(field: Field): SelectableField[java.lang.Long]

  def sum(field: Field): SelectableField[java.lang.Long]
}


