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

trait Query[R, T] extends Queryable[T] with TypedQuery[R, T] {

  def limit(l: Long): Query[R, T]

  def offset(l: Long): Query[R, T]

  def asc(field: Field): Query[R, T]

  def asc(field: String): Query[R, T] = this.asc(this.getRoot.get(field))

  def desc(field: Field): Query[R, T]

  def desc(field: String): Query[R, T] = this.desc(this.getRoot.get(field))

  def where(cond: Cond): Query[R, T]

  def groupBy(field: Field, fields: Field*): Query[R, T]

  def groupBy(field: String, fields: String*): Query[R, T]

  def having(cond: Cond): Query[R, T]

  def getRoot: Root[R]
}

trait TypedQuery[R, T] {

  def asc(fn: R => Object): Query[R, T]

  def desc(fn: R => Object): Query[R, T]

  def groupBy(fn: (R => Object), fns: (R => Object)*): Query[R, T]
}

trait SelectableTuple[T] extends Selectable[T] {
  def walk(tuple: T, fn: (Entity) => Entity): T
}

