package orm.operate.traits

import orm.operate.traits.core.{Cond, Field, Queryable, SelectRoot}

/**
  * Created by <yuemenglong@126.com> on 2017/7/17.
  */

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
