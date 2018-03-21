package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException

/**
  * Created by <yuemenglong@126.com> on 2018/3/19.
  */

//noinspection ScalaRedundantCast
trait SelectStatement extends SelectStmt {
  type Self = this.type

  def distinct(): Self = {
    core._distinct = true
    this.asInstanceOf[Self]
  }

  def from(ts: Table*): Self = {
    core._from = ts.toList
    this.asInstanceOf[Self]
  }

  def where(expr: Expr): Self = {
    core._where = expr
    this.asInstanceOf[Self]
  }

  def groupBy(es: Expr*): Self = {
    core._groupBy = es.toList
    this.asInstanceOf[Self]
  }

  def having(e: Expr): Self = {
    core._having = e
    this.asInstanceOf[Self]
  }

  def orderBy(e: Expr, t: String): Self = {
    core._orderBy ::= (e, t)
    this.asInstanceOf[Self]
  }

  def limit(l: Integer): Self = {
    core._limit = l
    this.asInstanceOf[Self]
  }

  def offset(o: Integer): Self = {
    core._offset = o
    this.asInstanceOf[Self]
  }

  def union(stmt: SelectStatement): Self = {
    comps ::= ("UNION", stmt.core)
    this.asInstanceOf[Self]
  }
}

object SelectStatement {
  def apply(columns: ResultColumn*): SelectStatement = new SelectStatement {
    override private[orm] val core = new SelectCore(columns: _*)
  }
}

trait Table extends TableSource {
  def join(t: Table, joinType: String, leftColunm: String, rightColumn: String): Table = {
    val c = Expr(getColumn(leftColunm).expr, "=", t.getColumn(rightColumn).expr)

    val jp: JoinPart = children(0) match {
      case ((name, uid), null, null) => new JoinPart {
        override val table = Table(name, uid)
        override val joins = List((joinType, t, c))
      }
      case (null, (stmt, uid), null) => new JoinPart {
        override val table = Table(stmt, uid)
        override val joins = List((joinType, t, c))
      }
      case (null, null, j) => new JoinPart {
        override private[orm] val table = j.table
        override private[orm] val joins = j.joins ::: List((joinType, t, c))
      }
      case _ => throw new UnreachableException
    }
    children(0) = (null, null, jp)
    this
  }

  def getColumn(c: String): ResultColumn = {
    val col = Expr.column(getAlias, c)
    new ResultColumn {
      override private[orm] val expr = col
      override private[orm] val uid = s"${getAlias}$$${c}"
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

object Core {
  def main(args: Array[String]): Unit = {
    val obj = Table("obj", "obj")
    val ptr = Table("ptr", "obj_ptr")

    obj.join(ptr, "LEFT", "ptr_id", "id")

    val select = SelectStatement(obj.getColumn("id"), obj.getColumn("name")).from(obj, ptr)
      .where(Expr(obj.getColumn("id").expr, "=", Expr.const(1)))

    val sb = new StringBuffer()
    select.genSql(sb)
    println(sb.toString)
  }
}

trait ToExpr {
  def toExpr: Expr
}

trait ExprOp extends ToExpr {
  def eql(e: ToExpr): Expr = Expr(this.toExpr, "=", e.toExpr)

  def eql[T](t: T): Expr = Expr(this.toExpr, "=", Expr.const(t))

  def neq(e: ToExpr): Expr = Expr(this.toExpr, "<>", e.toExpr)

  def neq[T](t: T): Expr = Expr(this.toExpr, "<>", Expr.const(t))

  def gt(e: ToExpr): Expr = Expr(this.toExpr, ">", e.toExpr)

  def gt[T](t: T): Expr = Expr(this.toExpr, ">", Expr.const(t))

  def gte(e: ToExpr): Expr = Expr(this.toExpr, ">=", e.toExpr)

  def gte[T](t: T): Expr = Expr(this.toExpr, ">=", Expr.const(t))

  def lt(e: ToExpr): Expr = Expr(this.toExpr, "<", e.toExpr)

  def lt[T](t: T): Expr = Expr(this.toExpr, "<", Expr.const(t))

  def lte(e: ToExpr): Expr = Expr(this.toExpr, "<=", e.toExpr)

  def lte[T](t: T): Expr = Expr(this.toExpr, "<=", Expr.const(t))

  def and(e: ToExpr): Expr = Expr(this.toExpr, "AND", e.toExpr)

  def or(e: ToExpr): Expr = Expr(this.toExpr, "OR", e.toExpr)

}
