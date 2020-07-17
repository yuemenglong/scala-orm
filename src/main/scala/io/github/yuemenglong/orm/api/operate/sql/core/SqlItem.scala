package io.github.yuemenglong.orm.api.operate.sql.core

import io.github.yuemenglong.orm.operate.sql.core.SelectStmt

import scala.collection.mutable.ArrayBuffer

trait SqlItem {
  override def toString: String = {
    val sb = new StringBuffer()
    genSql(sb)
    sb.toString
  }

  def genSql(sb: StringBuffer)

  def genParams(ab: ArrayBuffer[Object])

  def appendToStringBuffer(sb: StringBuffer, list: Seq[SqlItem], gap: String): Unit = {
    list.zipWithIndex.foreach { case (e, i) =>
      e.genSql(sb)
      if (i != list.length - 1) {
        sb.append(gap)
      }
    }
  }

  def nonEmpty(list: Seq[_]): Boolean = list != null && list.nonEmpty
}

class Var[T](private var v: T) {
  def get: T = v

  def set(v: T): Unit = this.v = v

  override def toString: String = {
    v match {
      case null => "NULL"
      case _ => v.toString
    }
  }
}

object Var {
  def apply[T](v: T) = new Var(v)
}

trait Constant extends SqlItem {
  private[orm] val value: Object
}

trait TableColumn extends SqlItem {
  private[orm] val table: String
  private[orm] val column: String
}

trait FunctionCall extends SqlItem {
  private[orm] val fn: String // Include COUNT(*)
  private[orm] val distinct: Boolean
  private[orm] val params: Array[Expr]
}

trait ResultColumn extends SqlItem {
  private[orm] val expr: Expr
  private[orm] val uid: String
}

trait TableOrSubQuery extends SqlItem {
  // (tableName, alias) or (stmt, alias)
  private[orm] val _table: (
    (String, String),
      (SelectStmt, String)
    )
  private[orm] val _joins: ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]

  def genSql(sb: StringBuffer, on: Expr): Unit

  def genParams(ab: ArrayBuffer[Object], on: Expr): Unit

  override def genSql(sb: StringBuffer): Unit = genSql(sb, null)

  override def genParams(ab: ArrayBuffer[Object]): Unit = genParams(ab, null)
}
