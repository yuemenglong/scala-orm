package io.github.yuemenglong.orm.operate.traits

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.operate.traits.core._

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */

trait Query[T] extends Queryable[T] {

  def select[T1](t: Selectable[T1]): Query[T1]

  def select[T1, T2](t1: Selectable[T1], t2: Selectable[T2]): Query[(T1, T2)]

  def from(selectRoot: SelectRoot[_]): Query[T]

  def limit(l: Long): Query[T]

  def offset(l: Long): Query[T]

  def asc(field: Field): Query[T]

  def desc(field: Field): Query[T]

  def where(cond: Cond): Query[T]

  def groupBy(fields: Array[Field]): Query[T]

  def groupBy(field: Field): Query[T]

  def having(cond: Cond): Query[T]

  def getRoot: SelectRoot[_]
}

trait SelectableTuple[T] extends Selectable[T] {
  def walk(tuple: T, fn: (Entity) => Entity): T
}

