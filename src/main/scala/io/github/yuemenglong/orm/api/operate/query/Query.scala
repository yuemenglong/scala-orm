package io.github.yuemenglong.orm.api.operate.query

import java.sql.ResultSet

import io.github.yuemenglong.orm.api.operate.sql.core.{ExprLike, ResultColumn, SelectStatement}
import io.github.yuemenglong.orm.api.operate.sql.table.SubQuery
import io.github.yuemenglong.orm.api.session.Session
import io.github.yuemenglong.orm.impl.entity.Entity

import scala.collection.mutable

/**
 * Created by yml on 2017/7/14.
 */
trait Query[T] {
  def query(session: Session): Array[T]
}

trait Selectable[T] {
  def getColumns: Array[ResultColumn]

  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getType: Class[T]

  def getKey(value: Object): String
}

trait AsSubQuery[T] {
  def all: ExprLike[_]

  def any: ExprLike[_]

  def asTable(alias: String): SubQuery
}

trait QueryBase[T] extends SelectStatement[T] with AsSubQuery[T]

trait Query1[T] extends QueryBase[Query1[T]] with Query[T]

trait Query2[T0, T1] extends QueryBase[Query2[T0, T1]] with Query[(T0, T1)]

trait Query3[T0, T1, T2] extends QueryBase[Query3[T0, T1, T2]] with Query[(T0, T1, T2)]
