package io.github.yuemenglong.orm.sql

import io.github.yuemenglong.orm.kit.UnreachableException
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.field.Field

import scala.collection.mutable.ArrayBuffer

object Expr {
  def const[T](v: T): Expr = new Expr() {
    val c = new Constant {
      override val value = v.asInstanceOf[Object]
    }
    override val children = (c, null, null, null, null, null, null, null, null, null)
  }

  def column(t: String, c: String): Expr = new Expr() {
    val tc = new TableColumn {
      override val table = t
      override val column = c
    }
    override val children = (null, tc, null, null, null, null, null, null, null, null)
  }

  def func(f: String, d: Boolean, p: Array[Expr]): Expr = new Expr {
    val fc = new FunctionCall {
      override val fn = f
      override val distinct = d
      override val params = p
    }
    override val children = (null, null, fc, null, null, null, null, null, null, null)
  }

  def stmt(s: SelectStmt): Expr = new Expr {
    override val children = (null, null, null, s, null, null, null, null, null, null)
  }

  def apply(op: String, e: ExprT[_]): Expr = new Expr {
    override val children = (null, null, null, null, (op, e.toExpr), null, null, null, null, null)
  }

  def apply(e: ExprT[_], op: String): Expr = new Expr {
    override val children = (null, null, null, null, null, (e.toExpr, op), null, null, null, null)
  }

  def apply(l: ExprT[_], op: String, r: ExprT[_]): Expr = new Expr {
    override val children = (null, null, null, null, null, null, (l.toExpr, op, r.toExpr), null, null, null)
  }

  def apply(e: ExprT[_], l: ExprT[_], r: ExprT[_]): Expr = new Expr {
    override val children = (null, null, null, null, null, null, null, (e.toExpr, l.toExpr, r.toExpr), null, null)
  }

  def apply(es: ExprT[_]*): Expr = new Expr {
    override val children = (null, null, null, null, null, null, null, null, es.map(_.toExpr).toArray, null)
  }

  def apply(sql: String, params: Array[Object] = Array()): Expr = new Expr {
    override val children = (null, null, null, null, null, null, null, null, null, (sql, params))
  }

  def asTableColumn(e: Expr): TableColumn = e.children match {
    case (null, c, null, null, null, null, null, null, null, null) => c
    case _ => throw new RuntimeException("Not TableColumn Expr")
  }

  def asFunctionCall(e: Expr): FunctionCall = e.children match {
    case (null, null, fn, null, null, null, null, null, null, null) => fn
    case _ => throw new RuntimeException("Not FunctionCall Expr")
  }

  def asSelectStmt(e: Expr): SelectStmt = e.children match {
    case (null, null, null, s, null, null, null, null, null, null) => s
    case _ => throw new RuntimeException("Not SelectStmt Expr")
  }
}

trait Expr extends SqlItem
  with ExprOpBool[Expr]
  with ExprOpMath[Expr]
  with ExprOpAssign[Expr]
  with ExprOpOrder[Expr] {
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
      (String, Array[Object]) // (sql, params) For Extend
    )
  //      (Expr, String, SelectStmt), // IN (SUBQUERY)

  def exprGenSql(e: Expr, sb: StringBuffer): Unit = {
    e match {
      case null => sb.append("NULL")
      case _ => e.genSql(sb)
    }
  }

  def exprGenParam(e: Expr, ab: ArrayBuffer[Object]): Unit = {
    e match {
      case null =>
      case _ => e.genParams(ab)
    }
  }

  override def genSql(sb: StringBuffer): Unit = children match {
    case (c, null, null, null, null, null, null, null, null, null) =>
      c.genSql(sb)
    case (null, t, null, null, null, null, null, null, null, null) =>
      t.genSql(sb)
    case (null, null, f, null, null, null, null, null, null, null) =>
      f.genSql(sb)
    case (null, null, null, s, null, null, null, null, null, null) =>
      sb.append("(")
      s.genSql(sb)
      sb.append(")")
    case (null, null, null, null, (op, e), null, null, null, null, null) =>
      sb.append(s"${op} ")
      exprGenSql(e, sb) // e.genSql(sb)
    case (null, null, null, null, null, (e, op), null, null, null, null) =>
      exprGenSql(e, sb) // e.genSql(sb)
      sb.append(s" ${op}")
    case (null, null, null, null, null, null, (l, op, r), null, null, null) =>
      exprGenSql(l, sb) // l.genSql(sb)
      sb.append(s" ${op} ")
      exprGenSql(r, sb) // r.genSql(sb)
    case (null, null, null, null, null, null, null, (e, l, r), null, null) =>
      exprGenSql(e, sb) // e.genSql(sb)
      sb.append(" BETWEEN ")
      exprGenSql(l, sb) // l.genSql(sb)
      sb.append(" AND ")
      exprGenSql(r, sb) // r.genSql(sb)
    case (null, null, null, null, null, null, null, null, list, null) =>
      sb.append("(")
      bufferMkString(sb, list, ", ")
      sb.append(")")
    case (null, null, null, null, null, null, null, null, null, (s, p)) =>
      sb.append(s"${s}")
    case _ => throw new UnreachableException
  }

  override def genParams(ab: ArrayBuffer[Object]): Unit = children match {
    case (c, null, null, null, null, null, null, null, null, null) =>
      c.genParams(ab)
    case (null, t, null, null, null, null, null, null, null, null) =>
      t.genParams(ab)
    case (null, null, f, null, null, null, null, null, null, null) =>
      f.genParams(ab)
    case (null, null, null, s, null, null, null, null, null, null) =>
      s.genParams(ab)
    case (null, null, null, null, (_, e), null, null, null, null, null) =>
      exprGenParam(e, ab) // e.genParams(ab)
    case (null, null, null, null, null, (e, _), null, null, null, null) =>
      exprGenParam(e, ab) // e.genParams(ab)
    case (null, null, null, null, null, null, (l, _, r), null, null, null) =>
      exprGenParam(l, ab) // l.genParams(ab)
      exprGenParam(r, ab) // r.genParams(ab)
    case (null, null, null, null, null, null, null, (e, l, r), null, null) =>
      exprGenParam(e, ab) // e.genParams(ab)
      exprGenParam(l, ab) // l.genParams(ab)
      exprGenParam(r, ab) // r.genParams(ab)
    case (null, null, null, null, null, null, null, null, list, null) =>
      list.foreach(_.genParams(ab))
    case (null, null, null, null, null, null, null, null, null, (_, p)) =>
      ab.append(p: _*)
    case _ => throw new UnreachableException
  }

  override def toExpr = this

  override def fromExpr(e: Expr) = e

  def as(alias: String): Field = {
    val that = this
    new Field {
      override private[orm] val uid = alias
      override private[orm] val expr = that
    }
  }
}

trait ExprT[S] {
  def toExpr: Expr

  def fromExpr(e: Expr): S
}

trait ExprOps[S] extends ExprOpBool[S]
  with ExprOpMath[S]
  with ExprOpAssign[S]
  with ExprOpOrder[S]

trait ExprOpBool[S] extends ExprT[S] {
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

  def between(l: ExprT[_], r: ExprT[_]): S = fromExpr(Expr(this.toExpr, l.toExpr, r.toExpr))

  def between[T](l: T, r: T): S = between(Expr.const(l), Expr.const(r))

  def ===(e: ExprT[_]): S = fromExpr(e match {
    case null => Expr(this.toExpr, "= NULL")
    case _ => Expr(this.toExpr, "=", e.toExpr)
  })

  def ===[T](t: T): S = ===(Expr.const(t))

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

trait ExprOpAssign[S] extends ExprT[S] {
  def assign(e: ExprT[_]): S = fromExpr(e match {
    case null => Expr(this.toExpr, "= NULL")
    case _ => Expr(this.toExpr, "=", e.toExpr)
  })

  def assign[T](v: T): S = assign(Expr.const(v))
}

trait ExprOpOrder[S] extends ExprT[S] {
  def asc(): S = fromExpr(Expr(this.toExpr, "ASC"))

  def desc(): S = fromExpr(Expr(this.toExpr, "DESC"))
}

