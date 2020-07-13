package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.sql._
import io.github.yuemenglong.orm.lang.types.Types._

/**
 * Created by <yuemenglong@126.com> on 2018/3/22.
 */

trait FnExpr[T] extends ResultColumn
  with SelectableField[T]
  with ExprOps[FnExpr[T]] {
  def distinct: FnExpr[T]

  override def toExpr: Expr

  override def fromExpr(e: Expr): FnExpr[T]
}

trait FnExprImpl[T] extends FnExpr[T]
  with SelectableFieldImpl[T]
  with ExprOpsImpl[FnExpr[T]] {
  def distinct: FnExpr[T] = {
    val fnCall = Expr.asFunctionCall(expr)
    val newExpr = Expr.func(fnCall.fn, d = true, fnCall.params)
    val that = this
    new FnExprImpl[T] {
      override val clazz: Class[T] = that.clazz
      override private[orm] val expr = newExpr
      override private[orm] val uid = that.uid
    }
  }

  override def toExpr: Expr = expr

  override def fromExpr(e: Expr): FnExpr[T] = {
    val that = this
    new FnExprImpl[T] {
      override val clazz: Class[T] = that.clazz
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}

trait FnOp {
  def count(): FnExpr[Long]

  def count(c: ResultColumn with ExprLike[_]): FnExpr[Long]

  def sum[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def min[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def max[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def exists(e: ExprLike[_]): ExprLike[_]
}

trait FnOpImpl extends FnOp {
  def count(): FnExpr[Long] = new FnExprImpl[Long] {
    override private[orm] val uid = "$count$"
    override private[orm] val expr = Expr.func("COUNT(*)", d = false, Array())
    override val clazz: Class[Long] = classOf[Long]
  }

  def count(c: ResultColumn with ExprLike[_]): FnExpr[Long] = new FnExprImpl[Long] {
    override val clazz: Class[Long] = classOf[Long]
    override private[orm] val uid = s"$$count$$${c.uid}"
    override private[orm] val expr = Expr.func("COUNT", d = false, Array(c.toExpr))
  }

  def sum[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$sum$$${f.uid}"
    override private[orm] val expr = Expr.func("SUM", d = false, Array(f.toExpr))
  }

  def min[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$min$$${f.uid}"
    override private[orm] val expr = Expr.func("MIN", d = false, Array(f.toExpr))
  }

  def max[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$max$$${f.uid}"
    override private[orm] val expr = Expr.func("MAX", d = false, Array(f.toExpr))
  }

  def exists(e: ExprLike[_]): ExprLike[_] = Expr("EXISTS", e)
}

