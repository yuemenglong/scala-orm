package io.github.yuemenglong.orm.impl.operate.sql.core

import io.github.yuemenglong.orm.api.operate.sql.core._
import io.github.yuemenglong.orm.impl.kit.UnreachableException

import scala.collection.mutable.ArrayBuffer

/**
 * Created by <yuemenglong@126.com> on 2018/3/19.
 */

class SelectCore(getColumns: () => Array[ResultColumn] = () => Array()) extends SqlItem {
  private[orm] lazy val _columns: Array[ResultColumn] = getColumns()
  private[orm] var _distinct: Boolean = _
  private[orm] var _from: Array[TableOrSubQuery] = _
  private[orm] var _where: Expr = _
  private[orm] var _groupBy: Array[Expr] = _
  private[orm] var _having: Expr = _
  private[orm] var _orderBy: Array[Expr] = Array[Expr]()
  private[orm] var _limit: Integer = _
  private[orm] var _offset: Integer = _

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

trait SelectStatementImpl[S] extends SelectStatement[S] {
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

  override def fromExpr(e: Expr): S = ExprUtil.asSelectStmt(e).asInstanceOf[S]

  override def toExpr: Expr = ExprUtil.stmt(this)

  def distinct(): S = {
    core._distinct = true
    this.asInstanceOf[S]
  }

  //  def select(cs: Array[ResultColumn]): S = {
  //    core._columns = cs
  //    this.asInstanceOf[S]
  //  }

  def from(ts: TableLike*): S = {
    core._from = ts.toArray
    this.asInstanceOf[S]
  }

  def where(expr: ExprLike[_]): S = {
    core._where = core._where match {
      case null => expr.toExpr
      case w => w.and(expr)
    }
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
    comps += (("UNION", stmt.asInstanceOf[SelectStatementImpl[_]].core))
    this.asInstanceOf[S]
  }

  def unionAll(stmt: SelectStatement[_]): S = {
    comps += (("UNION ALL", stmt.asInstanceOf[SelectStatementImpl[_]].core))
    this.asInstanceOf[S]
  }
}

trait UpdateStatementImpl extends UpdateStatement {
  var _sets: Array[Expr] = Array() // Table,Column,Expr
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

trait DeleteStatementImpl extends DeleteStatement {
  var _table: TableLike = _
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
