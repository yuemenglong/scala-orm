package io.github.yuemenglong.orm.operate.sql.core

import io.github.yuemenglong.orm.api.operate.sql.core.{Constant, Expr, ExprLike, FunctionCall, ResultColumn, SelectStmt, SqlItem, TableColumn, TableLike, TableOrSubQuery, Var}
import io.github.yuemenglong.orm.impl.kit.UnreachableException

import scala.collection.mutable.ArrayBuffer

/**
 * Created by <yuemenglong@126.com> on 2018/3/17.
 */

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

object TableLikeUtil {
  def create(name: String, uid: String): TableLike = new TableLikeImpl {
    override private[orm] val _table = ((name, uid), null)
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
    override private[orm] val _on = Var[Expr](null)
  }

  def create(stmt: SelectStmt, uid: String): TableLike = new TableLikeImpl {
    override private[orm] val _table = (null, (stmt, uid))
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
    override private[orm] val _on = Var[Expr](null)
  }
}

trait TableLikeImpl extends TableLike with TableOrSubQueryImpl {
  private[orm] val _on: Var[Expr]

  def join(t: TableLike, joinType: String): TableLike = {
    _joins.find { case (_, x, _) => x.equals(t) } match {
      case Some((_, t, _)) =>
        t.asInstanceOf[TableLike]
      case None =>
        _joins += ((joinType, t, t.asInstanceOf[TableLikeImpl]._on))
        t
    }
  }

  def join(t: TableLike, joinType: String, leftColunm: String, rightColumn: String): TableLike = {
    _joins.find { case (_, x, _) => x.equals(t) } match {
      case Some((_, t, _)) =>
        t.asInstanceOf[TableLike]
      case None =>
        val c = ExprUtil.create(getColumn(leftColunm).expr, "=", t.getColumn(rightColumn).expr)
        t.on(c)
        _joins += ((joinType, t, t.asInstanceOf[TableLikeImpl]._on))
        t
    }
  }

  def on(e: ExprLike[_]): TableLike = {
    if (_on == null) {
      throw new RuntimeException("Root Table Has No On[Expr]")
    }
    _on.get match {
      case null => _on.set(e.toExpr)
      case _ => _on.set(_on.get.and(e))
    }
    this
  }

  def getColumn(column: String, alias: String = null): ResultColumn = {
    val col = ExprUtil.column(getAlias, column)
    val ali = alias match {
      case null => s"${getAlias}$$${column}"
      case _ => alias
    }
    new ResultColumnImpl {
      override private[orm] val expr = col
      override private[orm] val uid = ali
    }
  }

  def getAlias: String = _table match {
    case ((_, alias), null) => alias
    case (null, (_, alias)) => alias
    case _ => throw new UnreachableException
  }
}
