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
  private[orm] val core: SelectCore
  private[orm] val comps: List[(String, SelectCore)]

  override def genSql(sb: StringBuffer): Unit = {
    core.genSql(sb)
    if (nonEmpty(comps)) {
      comps.foreach { case (op, s) =>
        sb.append(s" ${op} ")
        s.genSql(sb)
      }
    }
  }

  override def getParams: List[Object] = {
    var list = core.getParams
    if (nonEmpty(comps)) {
      list :::= comps.flatMap(_._2.getParams)
    }
    list
  }
}

trait SelectCore extends SqlItem {
  private[orm] val distinct: Boolean
  private[orm] val columns: List[ResultColumn]
  private[orm] val from: List[TableSource]
  private[orm] val where: Expression
  private[orm] val groupBy: List[Expression]
  private[orm] val having: Expression
  private[orm] val orderBy: List[(Expression, String)] // ASC/DESC
  private[orm] val limit: Integer
  private[orm] val offset: Integer

  override def genSql(sb: StringBuffer): Unit = {
    distinct match {
      case true => sb.append("SELECT DISTINCT ")
      case false => sb.append("SELECT ")
    }
    bufferMkString(sb, columns, ",")
    if (nonEmpty(from)) {
      sb.append(" FROM ")
      bufferMkString(sb, from, ",")
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
    var list = columns.flatMap(_.getParams)
    if (from != null) {
      list :::= from.flatMap(_.getParams)
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
    if (nonEmpty(orderBy)) {
      list :::= orderBy.flatMap(_._1.getParams)
    }
    (limit, offset) match {
      case (null, null) =>
      case (l, null) => list :::= List(l)
      case (l, o) => list :::= List(l, o)
      case _ => throw new UnreachableException
    }
    list
  }
}

trait Expression extends SqlItem {
  private[orm] val children: (
    Constant,
      TableColumn,
      FunctionCall,
      (String, Expression),
      (Expression, String),
      (Expression, String, Expression), // A AND B, A IN (1,2,3)
      (Expression, Expression, Expression), // BETWEEN AND
      (Expression, String, SelectStmt), // IN (SUBQUERY)
      List[Expression], // (A, B)
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
    case _ => throw new UnreachableException
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
    case _ => throw new UnreachableException
  }
}

trait Constant extends SqlItem {
  private[orm] val value: Object

  override def genSql(sb: StringBuffer): Unit = sb.append("?")

  override def getParams = List(value)
}

trait TableColumn extends SqlItem {
  private[orm] val table: String
  private[orm] val column: String

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(s"${table}.${column}")
  }

  override def getParams = List()
}

trait FunctionCall extends SqlItem {
  private[orm] val fn: String // Include COUNT(*)
  private[orm] val distinct: Boolean
  private[orm] val params: List[Expression]

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
  private[orm] val expr: Expression
  private[orm] val uid: String

  override def genSql(sb: StringBuffer): Unit = {
    expr.genSql(sb)
    sb.append(s" AS ${uid}")
  }

  override def getParams = {
    expr.getParams
  }
}

trait TableSource extends SqlItem {
  private[orm] val children: Array[(
    (String, String), // tableName, uid
      (SelectStmt, String), // (Select xx) AS
      JoinPart // JoinPart
    )]

  override def genSql(sb: StringBuffer): Unit = children(0) match {
    case ((table, uid), null, null) => sb.append(s"${table} AS ${uid}")
    case (null, (s, uid), null) =>
      sb.append("(")
      s.genSql(sb)
      sb.append(s") AS ${uid}")
    case (null, null, j) => j.genSql(sb)
    case _ => throw new UnreachableException
  }

  override def getParams = children(0) match {
    case (_, null, null) => List()
    case (null, (s, _), null) => s.getParams
    case (null, null, j) => j.getParams
    case _ => throw new UnreachableException
  }
}

trait JoinPart extends SqlItem {
  private[orm] val table: TableSource
  private[orm] val joins: List[(String, TableSource, Expression)] // JoinType

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
    table.getParams ::: joins.flatMap { case (_, t, e) => t.getParams ::: e.getParams }
  }
}
