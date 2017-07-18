package yy.orm.operate.traits

import java.sql.ResultSet

import yy.orm.lang.interfaces.Entity
import yy.orm.operate.traits.core._

import scala.annotation.varargs

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
}

trait SelectableTuple[T] extends Selectable[T] {
  def walk(tuple: T, fn: (Entity) => Entity): T
}

