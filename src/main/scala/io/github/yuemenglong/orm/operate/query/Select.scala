package io.github.yuemenglong.orm.operate.query

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.traits.{Cond, Root}
import io.github.yuemenglong.orm.operate.query.traits.{Queryable, Selectable}

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */
trait QueryBuilder[T] {
  def from[R](selectRoot: Root[R]): Query[R, T]
}

trait Query[R, T] extends Queryable[T] {

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

