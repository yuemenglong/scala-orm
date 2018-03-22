package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException

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

  def groupBy(es: Expr*): S = {
    core._groupBy = es.toArray
    this.asInstanceOf[S]
  }

  def having(e: Expr): S = {
    core._having = e
    this.asInstanceOf[S]
  }

  def orderBy(e: Expr, t: String): S = {
    core._orderBy += ((e, t))
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

trait Table extends TableSource {
  def join(t: Table, joinType: String, leftColunm: String, rightColumn: String): Table = {
    val c = Expr(getColumn(leftColunm).expr, "=", t.getColumn(rightColumn).expr)

    val jp: JoinPart = children(0) match {
      case ((name, uid), null, null) => new JoinPart {
        override val table = Table(name, uid)
        override val joins = Array((joinType, t, c))
      }
      case (null, (stmt, uid), null) => new JoinPart {
        override val table = Table(stmt, uid)
        override val joins = Array((joinType, t, c))
      }
      case (null, null, j) => new JoinPart {
        override private[orm] val table = j.table
        override private[orm] val joins = j.joins ++ Array((joinType, t, c))
      }
      case _ => throw new UnreachableException
    }
    children(0) = (null, null, jp)
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

  def getUid(children: (
    (String, String), // tableName, uid
      (SelectStmt, String), // (Select xx) AS
      JoinPart, // JoinPart
    )): String = children match {
    case ((_, uid), null, null) => uid
    case (null, (_, uid), null) => uid
    case (null, null, j) => getUid(j.table.children(0))
    case _ => throw new UnreachableException
  }

  def getAlias: String = getUid(children(0))
}

object Table {
  def apply(c: (
    (String, String), // tableName, uid
      (SelectStmt, String), // (Select xx) AS
      JoinPart, // JoinPart
    )): Table = new Table {
    override private[orm] val children = Array(c)
  }

  def apply(table: String, uid: String): Table = Table(((table, uid), null, null))

  def apply(stmt: SelectStmt, uid: String): Table = Table((null, (stmt, uid), null))
}

trait UpdateStatement extends UpdateStmt

trait DeleteStatement extends DeleteStmt

