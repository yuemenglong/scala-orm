package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2018/3/17.
  */
trait SqlItem {
  def genSql(sb: StringBuffer)

  def genParams(ab: ArrayBuffer[Object])

  def bufferMkString(sb: StringBuffer, list: Seq[SqlItem], gap: String): Unit = {
    list.zipWithIndex.foreach { case (e, i) =>
      e.genSql(sb)
      if (i != list.length - 1) {
        sb.append(gap)
      }
    }
  }

  def nonEmpty(list: Seq[_]): Boolean = list != null && list.nonEmpty
}

trait SelectStmt extends SqlItem {
  private[orm] val core: SelectCore
  private[orm] var comps = new ArrayBuffer[(String, SelectCore)]()

  override def genSql(sb: StringBuffer): Unit = {
    core.genSql(sb)
    if (nonEmpty(comps)) {
      comps.foreach { case (op, s) =>
        sb.append(s" ${op} ")
        s.genSql(sb)
      }
    }
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    core.genParams(ab)
    if (nonEmpty(comps)) {
      comps.foreach(_._2.genParams(ab))
    }
  }
}

class SelectCore(cs: Array[ResultColumn] = Array()) extends SqlItem {
  private[orm] var _distinct: Boolean = _
  private[orm] var _columns: Array[ResultColumn] = cs
  private[orm] var _from: Array[TableSource] = _
  private[orm] var _where: Expr = _
  private[orm] var _groupBy: Array[Expr] = _
  private[orm] var _having: Expr = _
  private[orm] var _orderBy: ArrayBuffer[(Expr, String)] = new ArrayBuffer[(Expr, String)]()
  private[orm] var _limit: Integer = _
  private[orm] var _offset: Integer = _

  override def genSql(sb: StringBuffer): Unit = {
    _distinct match {
      case true => sb.append("SELECT DISTINCT\n")
      case false => sb.append("SELECT\n")
    }
    bufferMkString(sb, _columns, ",\n")
    if (nonEmpty(_from)) {
      sb.append("\nFROM\n")
      bufferMkString(sb, _from, ",\n")
    }
    if (_where != null) {
      sb.append("\nWHERE\n")
      _where.genSql(sb)
    }
    if (nonEmpty(_groupBy)) {
      sb.append("\nGROUP BY\n")
      bufferMkString(sb, _groupBy, ", ")
      if (_having != null) {
        sb.append("\nHAVING\n")
        _having.genSql(sb)
      }
    }
    if (nonEmpty(_orderBy)) {
      sb.append(" ORDER BY ")
      _orderBy.foreach { case (e, o) =>
        sb.append(" ")
        e.genSql(sb)
        sb.append(s" ${o}")
      }
    }
    if (_limit != null) {
      sb.append(" LIMIT ?")
      if (_offset != null) {
        sb.append(" OFFSET ?")
      }
    }
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    _columns.foreach(_.genParams(ab))
    if (_from != null) {
      _from.foreach(_.genParams(ab))
    }
    if (_where != null) {
      _where.genParams(ab)
    }
    if (nonEmpty(_groupBy)) {
      _groupBy.foreach(_.genParams(ab))
      if (_having != null) {
        _having.genParams(ab)
      }
    }
    if (nonEmpty(_orderBy)) {
      _orderBy.foreach(_._1.genParams(ab))
    }
    (_limit, _offset) match {
      case (null, null) =>
      case (l, null) => ab += l
      case (l, o) => ab += l += o
      case _ => throw new UnreachableException
    }
  }
}

trait Expr extends SqlItem with ExprOp {
  private[orm] val children: (
    Constant,
      TableColumn,
      FunctionCall,
      (String, Expr),
      (Expr, String),
      (Expr, String, Expr), // A AND B, A IN (1,2,3)
      (Expr, Expr, Expr), // BETWEEN AND
      (Expr, String, SelectStmt), // IN (SUBQUERY)
      Array[Expr], // (A, B)
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
      bufferMkString(sb, list, ", ")
      sb.append(")")
    case _ => throw new UnreachableException
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = children match {
    case (c, null, null, null, null, null, null, null, null) =>
      c.genParams(ab)
    case (null, t, null, null, null, null, null, null, null) =>
      t.genParams(ab)
    case (null, null, f, null, null, null, null, null, null) =>
      f.genParams(ab)
    case (null, null, null, (_, e), null, null, null, null, null) =>
      e.genParams(ab)
    case (null, null, null, null, (e, _), null, null, null, null) =>
      e.genParams(ab)
    case (null, null, null, null, null, (l, _, r), null, null, null) =>
      l.genParams(ab)
      r.genParams(ab)
    case (null, null, null, null, null, null, (e, l, r), null, null) =>
      e.genParams(ab)
      l.genParams(ab)
      r.genParams(ab)
    case (null, null, null, null, null, null, null, (e, _, s), null) =>
      e.genParams(ab)
      s.genParams(ab)
    case (null, null, null, null, null, null, null, null, list) =>
      list.foreach(_.genParams(ab))
    case _ => throw new UnreachableException
  }

  override def toExpr = this
}

object Expr {
  def const[T](v: T): Expr = new Expr() {
    val c = new Constant {
      override val value = v.asInstanceOf[Object]
    }
    override val children = (c, null, null, null, null, null, null, null, null)
  }

  def column(t: String, c: String): Expr = new Expr() {
    val tc = new TableColumn {
      override val table = t
      override val column = c
    }
    override val children = (null, tc, null, null, null, null, null, null, null)
  }

  def func(f: String, d: Boolean, p: Array[Expr]): Expr = new Expr {
    val fc = new FunctionCall {
      override val fn = f
      override val distinct = d
      override val params = p
    }
    override val children = (null, null, fc, null, null, null, null, null, null)
  }

  def apply(op: String, e: Expr): Expr = new Expr {
    override val children = (null, null, null, (op, e), null, null, null, null, null)
  }

  def apply(e: Expr, op: String): Expr = new Expr {
    override val children = (null, null, null, null, (e, op), null, null, null, null)
  }

  def apply(l: Expr, op: String, r: Expr): Expr = new Expr {
    override val children = (null, null, null, null, null, (l, op, r), null, null, null)
  }

  def apply(e: Expr, l: Expr, r: Expr): Expr = new Expr {
    override val children = (null, null, null, null, null, null, (e, l, r), null, null)
  }

  def apply(e: Expr, op: String, stmt: SelectStmt): Expr = new Expr {
    override val children = (null, null, null, null, null, null, null, (e, op, stmt), null)
  }

  def apply(es: Expr*): Expr = new Expr {
    override val children = (null, null, null, null, null, null, null, null, es.toArray)
  }

  def asTableColumn(e: Expr): TableColumn = e.children match {
    case (null, c, null, null, null, null, null, null, null) => c
    case _ => throw new RuntimeException("Not TableColumn Expr")
  }
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
      bufferMkString(sb, params, ", ")
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

trait TableSource extends SqlItem {
  private[orm] val children: Array[(
    (String, String), // tableName, uid
      (SelectStmt, String), // (Select xx) AS
      JoinPart // JoinPart
    )]

  override def genSql(sb: StringBuffer): Unit = children(0) match {
    case ((table, uid), null, null) => sb.append(s"`${table}` AS `${uid}`")
    case (null, (s, uid), null) =>
      sb.append("(")
      s.genSql(sb)
      sb.append(s") AS `${uid}`")
    case (null, null, j) => j.genSql(sb)
    case _ => throw new UnreachableException
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = children(0) match {
    case (_, null, null) =>
    case (null, (s, _), null) => s.genParams(ab)
    case (null, null, j) => j.genParams(ab)
    case _ => throw new UnreachableException
  }
}

trait JoinPart extends SqlItem {
  private[orm] val table: TableSource
  private[orm] val joins: Array[(String, TableSource, Expr)] // JoinType

  override def genSql(sb: StringBuffer): Unit = {
    table.genSql(sb)
    joins.foreach { case (joinType, t, e) =>
      sb.append(s"\n${joinType} JOIN ")
      t.genSql(sb)
      sb.append(" ON ")
      e.genSql(sb)
    }
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    table.genParams(ab)
    joins.foreach { case (_, t, e) =>
      t.genParams(ab)
      e.genParams(ab)
    }
  }
}

trait UpdateStmt extends SqlItem {
  val _table: Table
  var _sets: Array[Assign] = Array() // Table,Column,Expr
  var _where: Expr = _

  override def genSql(sb: StringBuffer): Unit = {
    sb.append("UPDATE\n")
    _table.genSql(sb)
    sb.append("\nSET")
    _sets.foreach { a =>
      sb.append(s"\n")
      a.genSql(sb)
    }
    if (_where != null) {
      sb.append("\nWHERE\n")
      _where.genSql(sb)
    }
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    _table.genParams(ab)
    _sets.foreach(_.genParams(ab))
    if (_where != null) {
      _where.genParams(ab)
    }
  }
}

trait Assign extends SqlItem {
  val column: TableColumn
  val expr: Expr

  override def genSql(sb: StringBuffer): Unit = {
    column.genSql(sb)
    sb.append(s" = ")
    expr.genSql(sb)
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    column.genParams(ab)
    expr.genParams(ab)
  }
}

object Assign {
  def apply(c: TableColumn, e: Expr): Assign = new Assign {
    override val column = c
    override val expr = e
  }
}

trait DeleteStmt extends SqlItem {
  val _targets: Array[Table]
  var _table: Table = _
  var _where: Expr = _

  override def genSql(sb: StringBuffer): Unit = {
    sb.append("DELETE\n")
    _targets.foreach(_.genSql(sb))
    sb.append("\nFROM\n")
    _table.genSql(sb)
    if (_where != null) {
      sb.append("\nWHERE\n")
      _where.genSql(sb)
    }
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    _table.genParams(ab)
    if (_where != null) {
      _where.genParams(ab)
    }
  }
}