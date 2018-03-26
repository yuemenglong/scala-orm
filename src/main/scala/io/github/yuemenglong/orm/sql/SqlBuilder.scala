package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException
import io.github.yuemenglong.orm.operate.field.traits.Field

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

trait SqlItem {
  override def toString: String = {
    val sb = new StringBuffer()
    genSql(sb)
    sb.toString
  }

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
  private[orm] var _from: Array[TableOrSubQuery] = _
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

trait Expr extends SqlItem
  with ExprOp[Expr]
  with ExprOpMath[Expr]
  with ExprOpCol[Expr] {
  private[orm] val children: (
    Constant,
      TableColumn,
      FunctionCall,
      SelectStmt, // (SUBQUERY)
      (String, Expr),
      (Expr, String),
      (Expr, String, Expr), // A AND B, A IN (1,2,3)
      (Expr, Expr, Expr), // BETWEEN AND
      Array[Expr], // (A, B)
    )
  //      (Expr, String, SelectStmt), // IN (SUBQUERY)

  override def genSql(sb: StringBuffer): Unit = children match {
    case (c, null, null, null, null, null, null, null, null) =>
      c.genSql(sb)
    case (null, t, null, null, null, null, null, null, null) =>
      t.genSql(sb)
    case (null, null, f, null, null, null, null, null, null) =>
      f.genSql(sb)
    case (null, null, null, s, null, null, null, null, null) =>
      sb.append("(")
      s.genSql(sb)
      sb.append(")")
    case (null, null, null, null, (op, e), null, null, null, null) =>
      sb.append(s"${op} ")
      e.genSql(sb)
    case (null, null, null, null, null, (e, op), null, null, null) =>
      e.genSql(sb)
      sb.append(s" ${op}")
    case (null, null, null, null, null, null, (l, op, r), null, null) =>
      l.genSql(sb)
      sb.append(s" ${op} ")
      r.genSql(sb)
    case (null, null, null, null, null, null, null, (e, l, r), null) =>
      e.genSql(sb)
      sb.append(" BETWEEN ")
      l.genSql(sb)
      sb.append(" AND ")
      r.genSql(sb)
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
    case (null, null, null, s, null, null, null, null, null) =>
      s.genParams(ab)
    case (null, null, null, null, (_, e), null, null, null, null) =>
      e.genParams(ab)
    case (null, null, null, null, null, (e, _), null, null, null) =>
      e.genParams(ab)
    case (null, null, null, null, null, null, (l, _, r), null, null) =>
      l.genParams(ab)
      r.genParams(ab)
    case (null, null, null, null, null, null, null, (e, l, r), null) =>
      e.genParams(ab)
      l.genParams(ab)
      r.genParams(ab)
    case (null, null, null, null, null, null, null, null, list) =>
      list.foreach(_.genParams(ab))
    case _ => throw new UnreachableException
  }

  override def toExpr = this

  override def fromExpr(e: Expr) = e
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

  def stmt(s: SelectStmt): Expr = new Expr {
    override val children = (null, null, null, s, null, null, null, null, null)
  }

  def apply(op: String, e: ExprT[_]): Expr = new Expr {
    override val children = (null, null, null, null, (op, e.toExpr), null, null, null, null)
  }

  def apply(e: ExprT[_], op: String): Expr = new Expr {
    override val children = (null, null, null, null, null, (e.toExpr, op), null, null, null)
  }

  def apply(l: ExprT[_], op: String, r: ExprT[_]): Expr = new Expr {
    override val children = (null, null, null, null, null, null, (l.toExpr, op, r.toExpr), null, null)
  }

  def apply(e: ExprT[_], l: ExprT[_], r: ExprT[_]): Expr = new Expr {
    override val children = (null, null, null, null, null, null, null, (e.toExpr, l.toExpr, r.toExpr), null)
  }

  def apply(es: ExprT[_]*): Expr = new Expr {
    override val children = (null, null, null, null, null, null, null, null, es.map(_.toExpr).toArray)
  }

  def asTableColumn(e: Expr): TableColumn = e.children match {
    case (null, c, null, null, null, null, null, null, null) => c
    case _ => throw new RuntimeException("Not TableColumn Expr")
  }

  def asFunctionCall(e: Expr): FunctionCall = e.children match {
    case (null, null, fn, null, null, null, null, null, null) => fn
    case _ => throw new RuntimeException("Not FunctionCall Expr")
  }

  def asSelectStmt(e: Expr): SelectStmt = e.children match {
    case (null, null, null, s, null, null, null, null, null) => s
    case _ => throw new RuntimeException("Not SelectStmt Expr")
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

trait ResultColumn extends SqlItem
  with ExprOp[ResultColumn]
  with ExprOpMath[ResultColumn] {
  private[orm] val expr: Expr
  private[orm] val uid: String

  override def genSql(sb: StringBuffer): Unit = {
    expr.genSql(sb)
    sb.append(s" AS ${uid}")
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    expr.genParams(ab)
  }

  override def toExpr = expr

  override def fromExpr(e: Expr) = {
    val that = this
    new ResultColumn {
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}

trait UpdateStmt extends SqlItem {
  val _table: Table[_]
  var _sets: Array[Assign] = Array() // Table,Column,Expr
  var _where: Expr = _

  override def genSql(sb: StringBuffer): Unit = {
    sb.append("UPDATE\n")
    _table.genSql(sb)
    sb.append("\nSET")
    _sets.zipWithIndex.foreach { case (a, i) =>
      if (i > 0) {
        sb.append(",")
      }
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
    expr match {
      case null => sb.append("NULL")
      case _ => expr.genSql(sb)
    }
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = {
    column.genParams(ab)
    if (expr != null) {
      expr.genParams(ab)
    }
  }
}

object Assign {
  def apply(c: TableColumn, e: Expr): Assign = new Assign {
    override val column = c
    override val expr = e
  }
}

trait DeleteStmt extends SqlItem {
  val _targets: Array[Table[_]]
  var _table: Table[_] = _
  var _where: Expr = _

  override def genSql(sb: StringBuffer): Unit = {
    sb.append("DELETE\n")
    sb.append(_targets.map(t => s"`${t.getAlias}`").mkString(", "))
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

trait ExprT[S] {
  def toExpr: Expr

  def fromExpr(e: Expr): S
}

trait ExprOp[S] extends ExprT[S] {
  def eql(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "=", e.toExpr))

  def eql[T](t: T): S = eql(Expr.const(t))

  def neq(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "<>", e.toExpr))

  def neq[T](t: T): S = neq(Expr.const(t))

  def gt(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, ">", e.toExpr))

  def gt[T](t: T): S = gt(Expr.const(t))

  def gte(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, ">=", e.toExpr))

  def gte[T](t: T): S = gte(Expr.const(t))

  def lt(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "<", e.toExpr))

  def lt[T](t: T): S = lt(Expr.const(t))

  def lte(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "<=", e.toExpr))

  def lte[T](t: T): S = lte(Expr.const(t))

  def ===(e: ExprT[_]): S = eql(e)

  def ===[T](t: T): S = eql(t)

  def !==(e: ExprT[_]): S = neq(e)

  def !==[T](t: T): S = neq(t)

  def >(e: ExprT[_]): S = gt(e)

  def >[T](t: T): S = gt(t)

  def >=(e: ExprT[_]): S = gte(e)

  def >=[T](t: T): S = gte(t)

  def <(e: ExprT[_]): S = lt(e)

  def <[T](t: T): S = lt(t)

  def <=(e: ExprT[_]): S = lte(e)

  def <=[T](t: T): S = lte(t)

  def and(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "AND", e.toExpr))

  def or(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "OR", e.toExpr))

  def isNull: S = fromExpr(Expr(this.toExpr, "IS NULL"))

  def notNull: S = fromExpr(Expr(this.toExpr, "IS NOT NULL"))

  def in(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "IN", e.toExpr))

  def in[T](arr: Array[T]): S = in(Expr(arr.map(Expr.const(_).asInstanceOf[ExprT[_]]): _*))

  def nin(e: Expr): S = fromExpr(Expr(this.toExpr, "NOT IN", e.toExpr))

  def nin[T](arr: Array[T]): S = nin(Expr(arr.map(Expr.const(_).asInstanceOf[ExprT[_]]): _*))

  def like(s: String): S = fromExpr(Expr(this.toExpr, "LIKE", Expr.const(s)))
}

trait ExprOpCol[S] extends ExprT[S] {
  def as(alias: String): Field = {
    val that = this
    new Field {
      override private[orm] val uid = alias
      override private[orm] val expr = that.toExpr
    }
  }
}

trait ExprOpMath[S] extends ExprT[S] {
  def add(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "+", e.toExpr))

  def add[T](v: T): S = add(Expr.const(v))

  def sub(e: ExprT[_]): S = fromExpr(Expr(this.toExpr, "-", e.toExpr))

  def sub[T](v: T): S = sub(Expr.const(v))

  def +(e: ExprT[_]): S = add(e)

  def +[T](v: T): S = add(v)

  def -(e: ExprT[_]): S = sub(e)

  def -[T](v: T): S = sub(v)
}

trait TableOrSubQuery extends SqlItem {
  private[orm] val _table: (
    (String, String),
      (SelectStmt, String),
    )
  private[orm] val _joins: ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]

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

  override def genSql(sb: StringBuffer): Unit = genSql(sb, null)

  override def genParams(ab: ArrayBuffer[Object]): Unit = genParams(ab, null)

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


