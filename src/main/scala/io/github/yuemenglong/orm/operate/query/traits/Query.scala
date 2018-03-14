package io.github.yuemenglong.orm.operate.query.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.traits.{Cond, Expr, Root, SubRoot}

import scala.collection.mutable

/**
  * Created by yml on 2017/7/14.
  */
trait Queryable[T] {
  def query(session: Session): Array[T]

  def getType: Class[T]
}

trait Selectable[T] {
  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getColumnWithAs: String

  def getType: Class[T]

  def getKey(value: Object): String
}

trait QueryBuilder[T] {
  def from[R](selectRoot: Root[R]): Query[R, T]

  def distinct: this.type
}

trait SubQueryBuilder[T] {
  def from[R](subRoot: SubRoot[R]): SubQuery[R, T]

  def distinct: this.type
}

trait QueryBase[R, T] extends Queryable[T] with Expr {

  def limit(l: Long): this.type

  def offset(l: Long): this.type

  def asc(field: Field): this.type

  def desc(field: Field): this.type

  def where(cond: Cond): this.type

  def groupBy(field: Field, fields: Field*): this.type

  def having(cond: Cond): this.type

  def distinct(): this.type
}

trait SubQuery[R, T] extends QueryBase[R, T] {
  def all: this.type

  def any: this.type
}

trait Query[R, T] extends QueryBase[R, T] {

}
