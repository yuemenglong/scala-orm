package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2018/3/19.
  */

//noinspection ScalaRedundantCast
trait SelectStatement[S] extends SelectStmt {

  def distinct(): S = {
    core._distinct = true
    this.asInstanceOf[S]
  }

  def select(cs: Array[ResultColumn]): S = {
    core._columns = cs
    this.asInstanceOf[S]
  }

  def from(ts: Table*): S = {
    core._from = ts.toArray
    this.asInstanceOf[S]
  }

  def where(expr: ExprT[_]): S = {
    core._where = expr.toExpr
    this.asInstanceOf[S]
  }

  def groupBy(es: ExprT[_]*): S = {
    core._groupBy = es.map(_.toExpr).toArray
    this.asInstanceOf[S]
  }

  def having(e: ExprT[_]): S = {
    core._having = e.toExpr
    this.asInstanceOf[S]
  }

  def asc(e: ExprT[_]): S = {
    core._orderBy += ((e.toExpr, "ASC"))
    this.asInstanceOf[S]
  }

  def desc(e: ExprT[_]): S = {
    core._orderBy += ((e.toExpr, "DESC"))
    this.asInstanceOf[S]
  }

  def limit(l: Integer): S = {
    core._limit = l
    this.asInstanceOf[S]
  }

  def offset(o: Integer): S = {
    core._offset = o
    this.asInstanceOf[S]
  }

  def union(stmt: SelectStatement[_]): S = {
    comps += (("UNION", stmt.core))
    this.asInstanceOf[S]
  }
}

trait Table extends TableOrSubQuery {
  def join(t: Table, joinType: String, leftColunm: String, rightColumn: String): Var[Expr] = {
    val c = Var(Expr(getColumn(leftColunm).expr, "=", t.getColumn(rightColumn).expr))
    _joins += ((joinType, t, c))
    c
  }

  def getColumn(column: String, alias: String = null): ResultColumn = {
    val col = Expr.column(getAlias, column)
    val ali = alias match {
      case null => s"${getAlias}$$${column}"
      case _ => alias
    }
    new ResultColumn {
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

object Table {
  def apply(name: String, uid: String): Table = new Table {
    override private[orm] val _table = ((name, uid), null)
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
  }

  def apply(stmt: SelectStmt, uid: String): Table = new Table {
    override private[orm] val _table = (null, (stmt, uid))
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
  }
}

trait UpdateStatement extends UpdateStmt

trait DeleteStatement extends DeleteStmt

