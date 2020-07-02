package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException

import scala.collection.mutable.ArrayBuffer

/**
 * Created by <yuemenglong@126.com> on 2018/3/19.
 */

object TableLike {
  def apply(name: String, uid: String): TableLike = new TableLike {
    override private[orm] val _table = ((name, uid), null)
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
    override private[orm] val _on = Var[Expr](null)
  }

  def apply(stmt: SelectStmt, uid: String): TableLike = new TableLike {
    override private[orm] val _table = (null, (stmt, uid))
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
    override private[orm] val _on = Var[Expr](null)
  }
}

trait TableLike extends TableOrSubQuery {
  private[orm] val _on: Var[Expr]

  def join(t: TableLike, joinType: String): TableLike = {
    _joins += ((joinType, t, t._on))
    t
  }

  def join(t: TableLike, joinType: String, leftColunm: String, rightColumn: String): TableLike = {
    val c = Expr(getColumn(leftColunm).expr, "=", t.getColumn(rightColumn).expr)
    t.on(c)
    _joins += ((joinType, t, t._on))
    t
  }

  def on(e: ExprT[_]): TableLike = {
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

//noinspection ScalaRedundantCast
trait SelectStatement[S] extends SelectStmt with ExprT[S] {

  override def fromExpr(e: Expr): S = Expr.asSelectStmt(e).asInstanceOf[S]

  override def toExpr: Expr = Expr.stmt(this)

  def distinct(): S = {
    core._distinct = true
    this.asInstanceOf[S]
  }

  def select(cs: Array[ResultColumn]): S = {
    core._columns = cs
    this.asInstanceOf[S]
  }

  def from(ts: TableLike*): S = {
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

  //  def asc(e: ExprT[_]): S = {
  //    core._orderBy += ((e.toExpr, "ASC"))
  //    this.asInstanceOf[S]
  //  }
  //
  //  def desc(e: ExprT[_]): S = {
  //    core._orderBy += ((e.toExpr, "DESC"))
  //    this.asInstanceOf[S]
  //  }


  def orderBy(e: ExprT[_]*): S = {
    core._orderBy = e.map(_.toExpr).toArray
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

  def unionAll(stmt: SelectStatement[_]): S = {
    comps += (("UNION ALL", stmt.core))
    this.asInstanceOf[S]
  }
}

trait UpdateStatement extends UpdateStmt

trait DeleteStatement extends DeleteStmt
