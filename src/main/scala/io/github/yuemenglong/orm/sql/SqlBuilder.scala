package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException

/**
  * Created by <yuemenglong@126.com> on 2018/3/17.
  */
trait SqlItem {
  def genSql(sb: StringBuffer)

  def getParams: List[Object]

  def bufferMkString(sb: StringBuffer, list: List[SqlItem], gap: String): Unit = {
    list.zipWithIndex.foreach { case (e, i) =>
      e.genSql(sb)
      if (i != list.length - 1) {
        sb.append(gap)
      }
    }
  }

  def nonEmpty(list: List[_]): Boolean = list != null && list.nonEmpty
}

trait SelectStmt extends SqlItem {
  var core: SelectCore
  var comps: List[(String, SelectCore)]
  var orderBy: List[(Expr, String)] // ASC/DESC
  var limit: Integer
  var offset: Integer

  override def genSql(sb: StringBuffer): Unit = {
    core.genSql(sb)
    if (nonEmpty(comps)) {
      comps.foreach { case (op, s) =>
        sb.append(s" ${op} ")
        s.genSql(sb)
      }
    }
    if (nonEmpty(orderBy)) {
      sb.append(" ORDER BY ")
      orderBy.foreach { case (e, o) =>
        sb.append(" ")
        e.genSql(sb)
        sb.append(s" ${o}")
      }
    }
    if (limit != null) {
      sb.append(" LIMIT ?")
      if (offset != null) {
        sb.append(" OFFSET ?")
      }
    }
  }

  override def getParams: List[Object] = {
    val lo: List[Object] = (limit, offset) match {
      case (null, null) => List()
      case (l, null) => List(l)
      case (l, o) => List(l, o)
      case _ => throw new UnreachableException
    }
    var list = core.getParams
    if (nonEmpty(comps)) {
      list :::= comps.flatMap(_._2.getParams)
    }
    if (nonEmpty(orderBy)) {
      list :::= orderBy.flatMap(_._1.getParams)
    }
    list :::= lo
    list
  }
}

trait SelectCore extends SqlItem {
  var distinct: Boolean
  var columns: List[ResultColumn]
  var from: TableSource
  var where: Expr
  var groupBy: List[Expr]
  var having: Expr

  override def genSql(sb: StringBuffer): Unit = {
    distinct match {
      case true => sb.append("SELECT DISTINCT ")
      case false => sb.append("SELECT ")
    }
    bufferMkString(sb, columns, ",")
    if (from != null) {
      sb.append(" FROM ")
      from.genSql(sb)
    }
    if (where != null) {
      sb.append(" WHERE ")
      where.genSql(sb)
    }
    if (nonEmpty(groupBy)) {
      sb.append(" GROUP BY ")
      bufferMkString(sb, groupBy, ",")
      if (having != null) {
        sb.append(" HAVING ")
        having.genSql(sb)
      }
    }
  }

  override def getParams: List[Object] = {
    var list = columns.flatMap(_.getParams)
    if (from != null) {
      list :::= from.getParams
    }
    if (where != null) {
      list :::= where.getParams
    }
    if (nonEmpty(groupBy)) {
      list :::= groupBy.flatMap(_.getParams)
      if (having != null) {
        list :::= having.getParams
      }
    }
    list
  }
}

trait Expr extends SqlItem {
  var children: (
    Constant,
      TableColumn,
      FunctionCall,
      (String, Expr),
      (Expr, String),
      (Expr, String, Expr), // A AND B, A IN (1,2,3)
      (Expr, Expr, Expr), // BETWEEN AND
      (Expr, String, SelectStmt), // IN (SUBQUERY)
      List[Expr], // (A, B)
    )

  override def genSql(sb: StringBuffer): Unit = children match {
    case (c, null, null, null, null, null, null, null, null) =>
      c.genSql(sb)
    case (null, t, null, null, null, null, null, null, null) =>
      t.genSql(sb)
    case (null, null, f, null, null, null, null, null, null) =>
      f.genSql(sb)
    case (null, null, null, (op, e), null, null, null, null, null) =>
      sb.append(s"${op} ")
      e.genSql(sb)
    case (null, null, null, null, (e, op), null, null, null, null) =>
      e.genSql(sb)
      sb.append(s" ${op}")
    case (null, null, null, null, null, (l, op, r), null, null, null) =>
      l.genSql(sb)
      sb.append(s" ${op} ")
      r.genSql(sb)
    case (null, null, null, null, null, null, (e, l, r), null, null) =>
      e.genSql(sb)
      sb.append(" BETWEEN ")
      l.genSql(sb)
      sb.append(" AND ")
      r.genSql(sb)
    case (null, null, null, null, null, null, null, (e, op, s), null) =>
      e.genSql(sb)
      sb.append(s" ${op}")
      s.genSql(sb)
    case (null, null, null, null, null, null, null, null, list) =>
      sb.append("(")
      bufferMkString(sb, list, ",")
      sb.append(")")
  }

  override def getParams: List[Object] = children match {
    case (c, null, null, null, null, null, null, null, null) =>
      c.getParams
    case (null, t, null, null, null, null, null, null, null) =>
      t.getParams
    case (null, null, f, null, null, null, null, null, null) =>
      f.getParams
    case (null, null, null, (_, e), null, null, null, null, null) =>
      e.getParams
    case (null, null, null, null, (e, _), null, null, null, null) =>
      e.getParams
    case (null, null, null, null, null, (l, _, r), null, null, null) =>
      l.getParams ::: r.getParams
    case (null, null, null, null, null, null, (e, l, r), null, null) =>
      e.getParams ::: l.getParams ::: r.getParams
    case (null, null, null, null, null, null, null, (e, _, s), null) =>
      e.getParams ::: s.getParams
    case (null, null, null, null, null, null, null, null, list) =>
      list.flatMap(_.getParams)
  }
}

trait Constant extends SqlItem {
  var value: Object

  override def genSql(sb: StringBuffer): Unit = sb.append("?")

  override def getParams = List(value)
}

trait TableColumn extends SqlItem {
  var table: String
  var uid: String

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(s"${table} AS ${uid}")
  }

  override def getParams = List()
}

trait FunctionCall extends SqlItem {
  var fn: String // Include COUNT(*)
  var distinct: Boolean
  var params: List[Expr]

  override def genSql(sb: StringBuffer): Unit = fn match {
    case "COUNT(*)" => sb.append("COUNT(*)")
    case _ =>
      sb.append(s"${fn}(")
      if (distinct) {
        sb.append("DISTINCT ")
      }
      bufferMkString(sb, params, ",")
      sb.append(")")
  }

  override def getParams = {
    params.flatMap(_.getParams)
  }
}

trait ResultColumn extends SqlItem {
  var expr: Expr
  var uid: String

  override def genSql(sb: StringBuffer): Unit = {
    expr.genSql(sb)
    sb.append(s" AS ${uid}")
  }

  override def getParams = {
    expr.getParams
  }
}

trait TableSource extends SqlItem {
  var children: (
    (String, String), // tableName, uid
      JoinPart, // JoinPart
      List[TableSource], // A, B
      (SelectStmt, String) // (Select xx) AS
    )

  override def genSql(sb: StringBuffer): Unit = children match {
    case ((table, uid), null, null, null) => sb.append(s"${table} AS ${uid}")
    case (null, j, null, null) => j.genSql(sb)
    case (null, null, list, null) => bufferMkString(sb, list, ",")
    case (null, null, null, (s, uid)) =>
      sb.append("(")
      s.genSql(sb)
      sb.append(s") AS ${uid}")
  }

  override def getParams = children match {
    case (_, null, null, null) => List()
    case (null, j, null, null) => j.getParams
    case (null, null, list, null) => list.flatMap(_.getParams)
    case (null, null, null, (s, _)) => s.getParams
  }
}

trait JoinPart extends SqlItem {
  var table: TableSource
  var joins: List[(String, TableSource, Expr)] // JoinType

  override def genSql(sb: StringBuffer): Unit = {
    table.genSql(sb)
    joins.foreach { case (joinType, t, e) =>
      sb.append(s" ${joinType} JOIN ")
      t.genSql(sb)
      sb.append(" ON ")
      e.genSql(sb)
    }
  }

  override def getParams: List[Object] = {
    joins.flatMap { case (_, t, e) => t.getParams ::: e.getParams }
  }
}
