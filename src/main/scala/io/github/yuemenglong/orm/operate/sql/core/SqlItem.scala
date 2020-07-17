package io.github.yuemenglong.orm.operate.sql.core

import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, SqlItem}
import io.github.yuemenglong.orm.impl.kit.UnreachableException

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

trait ConstantImpl extends Constant {
  override def genSql(sb: StringBuffer): Unit = sb.append("?")

  override def genParams(ab: ArrayBuffer[Object]): Unit = ab += value
}

trait TableColumnImpl extends TableColumn {
  override def genSql(sb: StringBuffer): Unit = {
    sb.append(s"${table}.${column}")
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {}
}

trait FunctionCallImpl extends FunctionCall {
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

trait ResultColumnImpl extends ResultColumn {
  override def genSql(sb: StringBuffer): Unit = {
    expr.genSql(sb)
    sb.append(s" AS ${uid}")
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    expr.genParams(ab)
  }
}

trait TableOrSubQueryImpl extends TableOrSubQuery {
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
