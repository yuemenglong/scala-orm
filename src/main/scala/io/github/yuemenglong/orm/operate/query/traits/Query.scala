package io.github.yuemenglong.orm.operate.query.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.traits.{Cond, Expr, SubRoot}

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


trait QueryBuilder[T] {
  def from[R](selectRoot: SubRoot[R]): Query[R, T]
}

trait Query[R, T] extends Queryable[T] with Expr {

  def limit(l: Long): Query[R, T]

  def offset(l: Long): Query[R, T]

  def asc(field: Field): Query[R, T]

  def desc(field: Field): Query[R, T]

  def where(cond: Cond): Query[R, T]

  def groupBy(field: Field, fields: Field*): Query[R, T]

  def having(cond: Cond): Query[R, T]
}

trait SelectableTuple[T] extends Selectable[T] {
  def walk(tuple: T, fn: (Entity) => Entity): T
}

