package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException

/**
  * Created by <yuemenglong@126.com> on 2018/3/19.
  */

trait Select extends SelectStmt {
  def distinct(): Select = {
    core._distinct = true
    this
  }

  def from(ts: Table*): Select = {
    core._from = ts.toList
    this
  }

  def where(expr: Expr): Select = {
    core._where = expr
    this
  }

  def groupBy(es: Expr*): Select = {
    core._groupBy = es.toList
    this
  }

  def having(e: Expr): Select = {
    core._having = e
    this
  }

  def orderBy(e: Expr, t: String): Select = {
    core._orderBy ::= (e, t)
    this
  }

  def limit(l: Integer): Select = {
    core._limit = l
    this
  }

  def offset(o: Integer): Select = {
    core._offset = o
    this
  }

  def union(stmt: Select): Select = {
    comps ::= ("UNION", stmt.core)
    this
  }
}

object Select {
  def apply(columns: ResultColumn*): Select = new Select {
    override private[orm] val core = new SelectCore(columns: _*)
  }
}

trait Table extends TableSource {
  def join(t: Table, joinType: String, leftColunm: String, rightColumn: String): Table = {
    val c = Expr(get(leftColunm).expr, "=", t.get(rightColumn).expr)

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

  def get(column: String): ResultColumn = {
    val col = Expr.column(getAlias, column)
    new ResultColumn {
      override private[orm] val expr = col
      override private[orm] val uid = s"${getAlias}$$${column}"
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

    val select = Select(obj.get("id"), obj.get("name")).from(obj, ptr)
      .where(Expr(obj.get("id").expr, "=", Expr.const(1)))

    val sb = new StringBuffer()
    select.genSql(sb)
    println(sb.toString)
  }
}

