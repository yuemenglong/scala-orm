package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException
import io.github.yuemenglong.orm.lang.types.Types.String

import scala.collection.mutable.ArrayBuffer

/**
 * Created by <yuemenglong@126.com> on 2018/3/19.
 */

object TableLike {
  def apply(name: String, uid: String): TableLike = new TableLikeImpl {
    override private[orm] val _table = ((name, uid), null)
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
    override private[orm] val _on = Var[Expr](null)
  }

  def apply(stmt: SelectStmt, uid: String): TableLike = new TableLikeImpl {
    override private[orm] val _table = (null, (stmt, uid))
    override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
    override private[orm] val _on = Var[Expr](null)
  }
}

trait TableLike extends TableOrSubQuery {
  private[orm] val _on: Var[Expr]

  def join(t: TableLike, joinType: String): TableLike

  def join(t: TableLike, joinType: String, leftColunm: String, rightColumn: String): TableLike

  def on(e: ExprLike[_]): TableLike

  def getColumn(column: String, alias: String = null): ResultColumn

  def getAlias: String
}

trait TableLikeImpl extends TableLike {
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

trait SelectStmt extends SqlItem {
  private[orm] val core: SelectCore
  private[orm] var comps = new ArrayBuffer[(String, SelectCore)]()
}

trait SelectStmtImpl extends SelectStmt {
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

trait SelectCore extends SqlItem {
  private[orm] val cs: Array[ResultColumn]
  private[orm] var _distinct: Boolean = _
  private[orm] var _columns: Array[ResultColumn] = cs
  private[orm] var _from: Array[TableOrSubQuery] = _
  private[orm] var _where: Expr = _
  private[orm] var _groupBy: Array[Expr] = _
  private[orm] var _having: Expr = _
  private[orm] var _orderBy: Array[Expr] = Array[Expr]()
  private[orm] var _limit: Integer = _
  private[orm] var _offset: Integer = _
}

class SelectCoreImpl(val cs: Array[ResultColumn] = Array()) extends SelectCore {

  override def genSql(sb: StringBuffer): Unit = {
    _distinct match {
      case true => sb.append("SELECT DISTINCT\n")
      case false => sb.append("SELECT\n")
    }
    appendToStringBuffer(sb, _columns, ",\n")
    if (nonEmpty(_from)) {
      sb.append("\nFROM\n")
      appendToStringBuffer(sb, _from, ",\n")
    }
    if (_where != null) {
      sb.append("\nWHERE\n")
      _where.genSql(sb)
    }
    if (nonEmpty(_groupBy)) {
      sb.append("\nGROUP BY\n")
      appendToStringBuffer(sb, _groupBy, ", ")
      if (_having != null) {
        sb.append("\nHAVING\n")
        _having.genSql(sb)
      }
    }
    if (nonEmpty(_orderBy)) {
      sb.append(" ORDER BY ")
      var first = true
      _orderBy.foreach(e => {
        if (!first) {
          sb.append(", ")
        }
        first = false
        e.genSql(sb)
      })
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
      _orderBy.foreach(_.genParams(ab))
    }
    (_limit, _offset) match {
      case (null, null) =>
      case (l, null) => ab += l
      case (l, o) => ab += l += o
      case _ => throw new UnreachableException
    }
  }
}

//noinspection ScalaRedundantCast
trait SelectStatement[S] extends SelectStmt with ExprLike[S] {

  def distinct(): S

  def select(cs: Array[ResultColumn]): S

  def from(ts: TableLike*): S

  def where(expr: ExprLike[_]): S

  def groupBy(es: ExprLike[_]*): S

  def having(e: ExprLike[_]): S

  def orderBy(e: ExprLike[_]*): S

  def limit(l: Integer): S

  def offset(o: Integer): S

  def union(stmt: SelectStatement[_]): S

  def unionAll(stmt: SelectStatement[_]): S
}

trait SelectStatementImpl[S] extends SelectStatement[S] with SelectStmtImpl {

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

  def where(expr: ExprLike[_]): S = {
    core._where = expr.toExpr
    this.asInstanceOf[S]
  }

  def groupBy(es: ExprLike[_]*): S = {
    core._groupBy = es.map(_.toExpr).toArray
    this.asInstanceOf[S]
  }

  def having(e: ExprLike[_]): S = {
    core._having = e.toExpr
    this.asInstanceOf[S]
  }

  def orderBy(e: ExprLike[_]*): S = {
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
