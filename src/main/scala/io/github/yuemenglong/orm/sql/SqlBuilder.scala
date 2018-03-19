package io.github.yuemenglong.orm.sql

/**
  * Created by <yuemenglong@126.com> on 2018/3/17.
  */
trait SqlExpr {
  def genSql(sb: StringBuffer)

  def getParams: List[Object]

  def bufferMkString(sb: StringBuffer, list: List[SqlExpr], gap: String): Unit = {
    list.zipWithIndex.foreach { case (e, i) =>
      e.genSql(sb)
      if (i != list.length - 1) {
        sb.append(gap)
      }
    }
  }
}

trait SqlStatement extends SqlExpr

trait DmlStatement extends SqlStatement

trait SelectStatement extends DmlStatement {
  var children: (QuerySpecification, QueryExpression)

  override def genSql(sb: StringBuffer): Unit = children match {
    case (q, null) => q.genSql(sb)
    case (null, q) => q.genSql(sb)
  }

  override def getParams: List[Object] = children match {
    case (q, null) => q.getParams
    case (null, q) => q.getParams
  }
}

trait QueryExpression extends SelectStatement {
  var children: (QuerySpecification, QueryExpression) // 带括号

  override def genSql(sb: StringBuffer): Unit = children match {
    case (q, null) =>
      sb.append("(")
      q.genSql(sb)
      sb.append(")")
    case (null, q) =>
      sb.append("(")
      q.genSql(sb)
      sb.append(")")
  }

  override def getParams: List[Object] = children match {
    case (q, null) => q.getParams
    case (null, q) => q.getParams
  }
}

trait QuerySpecification extends SqlExpr {
  //  var selectSpecs: List[SelectSpec]
  var distinct: Boolean = false
  var selectElement: List[SelectElement]
  var fromClause: FromClause
  var orderByClause: OrderByClause
  var limitClause: LimitClause

  override def genSql(sb: StringBuffer): Unit = {
    sb.append("SELECT ")
    if (distinct) {
      sb.append("DISTINCT ")
    }
    bufferMkString(sb, selectElement, ",")
    if (fromClause != null) {
      sb.append(" ")
      fromClause.genSql(sb)
    }
    if (orderByClause != null) {
      sb.append(" ")
      orderByClause.genSql(sb)
    }
    if (limitClause != null) {
      sb.append(" ")
      limitClause.genSql(sb)
    }
  }

  override def getParams: List[Object] = {
    var list = List()
    selectElement.foreach(list :::= _.getParams)
    if (fromClause != null) {
      list :::= fromClause.getParams
    }
    if (orderByClause != null) {
      list :::= orderByClause.getParams
    }
    if (limitClause != null) {
      list :::= limitClause.getParams
    }
    list
  }
}

trait SelectElement extends SqlExpr {
  var uid: String
  var children: (FullColumnName, FunctionCall, Expression)

  override def genSql(sb: StringBuffer): Unit = {
    children match {
      case (f, null, null) => f.genSql(sb)
      case (null, f, null) => f.genSql(sb)
      case (null, null, e) => e.genSql(sb)
    }
    sb.append(s" AS ${uid}")
  }

  override def getParams: List[Object] = children match {
    case (f, null, null) => f.getParams
    case (null, f, null) => f.getParams
    case (null, null, e) => e.getParams
  }

}

trait FullColumnName extends SqlExpr with FunctionArg {
  var table: String
  var column: String

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(s"${table}.${column}")
  }

  override def getParams: List[Object] = List()
}

trait FunctionCall extends SqlExpr with FunctionArg {
  var children: (SpecificFunction, AggregateWindowedFunction)

  override def genSql(sb: StringBuffer): Unit = children match {
    case (null, a) => a.genSql(sb)
  }

  override def getParams: List[Object] = children match {
    case (null, a) => a.getParams
  }

}

trait SpecificFunction extends SqlExpr {
  throw new RuntimeException("Unreachable")
}

trait AggregateWindowedFunction extends SqlExpr {
  var distinct: Boolean = false
  var fn: String // AVG MAX MIN SUM COUNT COUNT(*)
  var functionArgs: List[FunctionArg]

  override def genSql(sb: StringBuffer): Unit = {
    val d = distinct match {
      case true => "DISTINCT "
      case false => ""
    }
    fn match {
      case "COUNT(*)" => sb.append("COUNT(*)")
      case _ =>
        sb.append(s"${fn}(${d}")
        bufferMkString(sb, functionArgs, ",")
        sb.append(")")
    }
  }

  override def getParams: List[Object] = functionArgs.flatMap(_.getParams)
}

trait FunctionArg extends SqlExpr

trait Constant extends SqlExpr with FunctionArg {
  val value: Object

  override def genSql(sb: StringBuffer): Unit = sb.append("?")

  override def getParams: List[Object] = List(value)
}

trait Expression extends SqlExpr with FunctionArg {
  var children: (
    Expression, //NOT
      (Expression, String, Expression),
      Predicate,
    )

  override def genSql(sb: StringBuffer): Unit = children match {
    case (e, null, null) =>
      sb.append("NOT ")
      e.genSql(sb)
    case (null, (l, op, r), null) =>
      l.genSql(sb)
      sb.append(s" ${op} ")
      r.genSql(sb)
    case (null, null, p) =>
      p.genSql(sb)
  }

  override def getParams: List[Object] = children match {
    case (e, null, null) => e.getParams
    case (null, (l, _, r), null) => l.getParams ::: r.getParams
    case (null, null, p) => p.getParams
  }
}

trait Predicate extends SqlExpr {
  var children: (
    (Predicate, Boolean, (SelectStatement, List[Expression])), // NOT? IN
      (Predicate, Boolean), //IS NULL
      (Predicate, String, Predicate),
      (Predicate, String, String, SelectStatement), // >= ALL(SELECT * FROM xxx)
      (Predicate, Predicate, Predicate), // BETWEEN A AND B
      (Predicate, Predicate), // LIKE xxx
      ExpressionAtom,
    )

  override def genSql(sb: StringBuffer): Unit = children match {
    case ((p, b, t), null, null, null, null, null, null) =>
      p.genSql(sb)
      if (b) {
        sb.append(" NOT")
      }
      sb.append(" IN(")
      t match {
        case (s, null) => s.genSql(sb)
        case (null, l) => bufferMkString(sb, l, ",")
      }
      sb.append(")")
    case (null, (p, b), null, null, null, null, null) =>
      p.genSql(sb)
      b match {
        case true => sb.append(" IS NOT NULL")
        case false => sb.append(" IS NULL")
      }
    case (null, null, (l, op, r), null, null, null, null) =>
      l.genSql(sb)
      sb.append(s" ${op} ")
      r.genSql(sb)
    case (null, null, null, (l, op, qf, s), null, null, null) =>
      l.genSql(sb)
      sb.append(s" ${op} ${qf}(")
      s.genSql(sb)
      sb.append(")")
    case (null, null, null, null, (p, l, r), null, null) =>
      p.genSql(sb)
      sb.append(" BETWEEN ")
      l.genSql(sb)
      sb.append(" AND ")
      r.genSql(sb)
    case (null, null, null, null, null, (l, r), null) =>
      l.genSql(sb)
      sb.append(" LIKE ")
      r.genSql(sb)
    case (null, null, null, null, null, null, e) =>
      e.genSql(sb)
  }

  override def getParams: List[Object] = children match {
    case ((p, _, t), null, null, null, null, null, null) => t match {
      case (s, null) => p.getParams ::: s.getParams
      case (null, e) => p.getParams ::: e.flatMap(_.getParams)
    }
    case (null, (p, _), null, null, null, null, null) =>
      p.getParams
    case (null, null, (l, _, r), null, null, null, null) =>
      l.getParams ::: r.getParams
    case (null, null, null, (l, _, _, s), null, null, null) =>
      l.getParams ::: s.getParams
    case (null, null, null, null, (p, l, r), null, null) =>
      p.getParams ::: l.getParams ::: r.getParams
    case (null, null, null, null, null, (l, r), null) =>
      l.getParams ::: r.getParams
    case (null, null, null, null, null, null, e) =>
      e.getParams
  }
}

trait ExpressionAtom extends SqlExpr {
  var children: (
    Constant,
      FullColumnName,
      FunctionCall,
      (String, ExpressionAtom), // op
      List[ExpressionAtom], // (e,e)
      (String, SelectStatement), // EXISTS ()
      (ExpressionAtom, String, ExpressionAtom),
    )

  override def genSql(sb: StringBuffer): Unit = children match {
    case (c, null, null, null, null, null, null) =>
      sb.append(" ")
      c.genSql(sb)
    case (null, col, null, null, null, null, null) =>
      sb.append(" ")
      col.genSql(sb)
    case (null, null, fn, null, null, null, null) =>
      sb.append(" ")
      fn.genSql(sb)
    case (null, null, null, (op, e), null, null, null) =>
      sb.append(s" ${op} ")
      e.genSql(sb)
    case (null, null, null, null, list, null, null) =>
      sb.append("(")
      bufferMkString(sb, list, ",")
      sb.append(")")
    case (null, null, null, null, null, (op, s), null) =>
      sb.append(s" ${op}(")
      s.genSql(sb)
      sb.append(")")
    case (null, null, null, null, null, null, (l, op, r)) =>
      sb.append(" ")
      l.genSql(sb)
      sb.append(s" ${op} ")
      r.genSql(sb)
  }

  override def getParams: List[Object] = children match {
    case (c, null, null, null, null, null, null) =>
      c.getParams
    case (null, col, null, null, null, null, null) =>
      col.getParams
    case (null, null, fn, null, null, null, null) =>
      fn.getParams
    case (null, null, null, (_, e), null, null, null) =>
      e.getParams
    case (null, null, null, null, list, null, null) =>
      list.flatMap(_.getParams)
    case (null, null, null, null, null, (_, s), null) =>
      s.getParams
    case (null, null, null, null, null, null, (l, _, r)) =>
      l.getParams ::: r.getParams
  }
}

trait FromClause extends SqlExpr {
  var tableSources: List[TableSource]
  var where: Expression
  var groupBy: List[GroupByItem]
  var having: Expression

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(" FROM ")
    bufferMkString(sb, tableSources, ",")
    if (where != null) {
      sb.append(" WHERE ")
      where.genSql(sb)
    }
    if (groupBy != null && groupBy.nonEmpty) {
      sb.append(" GROUP BY ")
      bufferMkString(sb, groupBy, ",")
    }
    if (having != null) {
      sb.append(" HAVING ")
      having.genSql(sb)
    }
  }

  override def getParams: List[Object] = {
    var list = tableSources.flatMap(_.getParams)
    if (where != null) {
      list :::= where.getParams
    }
    if (groupBy != null && groupBy.nonEmpty) {
      list :::= groupBy.flatMap(_.getParams)
    }
    if (having != null) {
      list :::= having.getParams
    }
    list
  }
}

trait TableSource extends SqlExpr {
  var table: TableSourceItem
  var joins: List[JoinPart]
  var bracket: Boolean = false // (tableSourceItem joinPart*)

  override def genSql(sb: StringBuffer): Unit = {
    if (bracket) {
      sb.append("(")
    }
    table.genSql(sb)
    if (joins != null && joins.nonEmpty) {
      sb.append(" ")
      bufferMkString(sb, joins, " ")
    }
    if (bracket) {
      sb.append(")")
    }
  }

  override def getParams: List[Object] = {
    var list = table.getParams
    if (joins != null && joins.nonEmpty) {
      list :::= joins.flatMap(_.getParams)
    }
    list
  }
}

trait JoinPart extends SqlExpr {
  var joinType: String
  var table: TableSourceItem
  var on: Expression

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(s" ${joinType} JOIN ")
    table.genSql(sb)
    sb.append(" ON ")
    on.genSql(sb)
  }

  override def getParams: List[Object] = {
    table.getParams ::: on.getParams
  }
}

trait TableSourceItem extends SqlExpr {
  var children: (
    (String, String), // tableName uid
      (SelectStatement, String), // 是否有括号
      List[TableSource]
    )

  override def genSql(sb: StringBuffer): Unit = children match {
    case ((table, uid), null, null) =>
      sb.append(s" ${table} AS ${uid}")
    case (null, (s, uid), null) =>
      sb.append("(")
      s.genSql(sb)
      sb.append(")")
      sb.append(s" AS ${uid}")
    case (null, null, list) => {
      sb.append("(")
      bufferMkString(sb, list, ",")
      sb.append(")")
    }
  }

  override def getParams: List[Object] = children match {
    case (_, null, null) =>
      List()
    case (null, (s, _), null) =>
      s.getParams
    case (null, null, list) =>
      list.flatMap(_.getParams)
  }
}

trait GroupByItem extends SqlExpr {
  var expression: Expression

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(" ")
    expression.genSql(sb)
  }

  override def getParams: List[Object] = expression.getParams
}

trait OrderByClause extends SqlExpr {
  var list: List[OrderByExpression]

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(" ORDER BY ")
    bufferMkString(sb, list, ",")
  }

  override def getParams: List[Object] = {
    list.flatMap(_.getParams)
  }
}

trait OrderByExpression extends SqlExpr {
  var expression: Expression
  var order: String

  override def genSql(sb: StringBuffer): Unit = {
    expression.genSql(sb)
    sb.append(s" ${order}")
  }

  override def getParams: List[Object] = {
    expression.getParams
  }
}

trait LimitClause extends SqlExpr {
  var limit: Integer
  var offset: Integer

  override def genSql(sb: StringBuffer): Unit = {
    sb.append(" LIMIT ?")
    if (offset != null) {
      sb.append(" OFFSET ?")
    }
  }

  override def getParams: List[Object] = {
    offset match {
      case null => List(limit)
      case _ => List(limit, offset)
    }
  }
}
