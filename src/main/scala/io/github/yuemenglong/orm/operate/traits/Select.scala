package io.github.yuemenglong.orm.operate.traits

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.operate.traits.core._

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */

trait Query[T] extends Queryable[T] {

  def from(selectRoot: Root[_]): Query[T]

  def limit(l: Long): Query[T]

  def offset(l: Long): Query[T]

  def asc(field: String): Query[T] = this.asc(this.getRoot.get(field))

  def asc(field: Field): Query[T]

  def desc(field: String): Query[T] = this.desc(this.getRoot.get(field))

  def desc(field: Field): Query[T]

  def where(cond: Cond): Query[T]

  def groupBy(fields: Array[String]): Query[T] = this.groupBy(fields.map(f => this.getRoot.get(f)))

  def groupBy(fields: Array[Field]): Query[T]

  def groupBy(field: String): Query[T] = this.groupBy(this.getRoot.get(field))

  def groupBy(field: Field): Query[T]

  def having(cond: Cond): Query[T]

  def getRoot: Root[_]
}

trait SelectableTuple[T] extends Selectable[T] {
  def walk(tuple: T, fn: (Entity) => Entity): T
}

