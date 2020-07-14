package io.github.yuemenglong.orm.operate.sql.core

import io.github.yuemenglong.orm.kit.UnreachableException

import scala.collection.mutable.ArrayBuffer

/**
 * Created by <yuemenglong@126.com> on 2018/3/17.
 */
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

trait Constant extends SqlItem {
  private[orm] val value: Object

  override def genSql(sb: StringBuffer): Unit = sb.append("?")

  override def genParams(ab: ArrayBuffer[Object]): Unit = ab += value
}

trait TableColumn extends SqlItem {
  private[orm] val table: String
  private[orm] val column: String

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(s"${table}.${column}")
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {}
}

trait FunctionCall extends SqlItem {
  private[orm] val fn: String // Include COUNT(*)
  private[orm] val distinct: Boolean
  private[orm] val params: Array[Expr]

  override def genSql(sb: StringBuffer): Unit = fn match {
    case "COUNT(*)" => sb.append("COUNT(*)")
    case _ =>
      sb.append(s"${fn}(")
      if (distinct) {
        sb.append("DISTINCT ")
      }
      appendToStringBuffer(sb, params, ", ")
      sb.append(")")
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    params.foreach(_.genParams(ab))
  }
}

trait ResultColumn extends SqlItem {
  private[orm] val expr: Expr
  private[orm] val uid: String

  override def genSql(sb: StringBuffer): Unit = {
    expr.genSql(sb)
    sb.append(s" AS ${uid}")
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    expr.genParams(ab)
  }
}

trait TableOrSubQuery extends SqlItem {
  // (tableName, alias) or (stmt, alias)
  private[orm] val _table: (
    (String, String),
      (SelectStmt, String)
    )
  private[orm] val _joins: ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]

  def genSql(sb: StringBuffer, on: Expr): Unit = {
    _table match {
      case ((name, alias), null) => sb.append(s"`${name}` AS `${alias}`")
      case (null, (stmt, alias)) => sb.append(s"(${stmt}) AS `${alias}`")
      case _ => throw new UnreachableException
    }
    if (on != null) {
      sb.append(" ON ")
      on.genSql(sb)
    }
    _joins.foreach { case (joinType, t, expr) =>
      sb.append(s"\n${joinType} JOIN ")
      t.genSql(sb, expr.get)
    }
  }

  override def genSql(sb: StringBuffer): Unit = genSql(sb, null)

  override def genParams(ab: ArrayBuffer[Object]): Unit = genParams(ab, null)

  def genParams(ab: ArrayBuffer[Object], on: Expr): Unit = {
    _table match {
      case (_, null) =>
      case (null, (stmt, _)) => stmt.genParams(ab)
      case _ => throw new UnreachableException
    }
    if (on != null) {
      on.genParams(ab)
    }
    _joins.foreach { case (_, t, expr) =>
      t.genParams(ab, expr.get)
    }
  }
}
