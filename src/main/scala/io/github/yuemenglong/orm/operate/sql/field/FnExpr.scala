package io.github.yuemenglong.orm.operate.sql.field

import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprLike, ExprOps, ResultColumn}
import io.github.yuemenglong.orm.api.operate.sql.field.{FnExpr, OrmFn, SelectableField, SelectableFieldExpr}
import io.github.yuemenglong.orm.operate.sql.core._

trait FnExprImpl[T] extends FnExpr[T]
  with SelectableFieldImpl[T]
  with ExprOpsImpl[FnExpr[T]] {
  def distinct: FnExpr[T] = {
    val fnCall = ExprUtil.asFunctionCall(expr)
    val newExpr = ExprUtil.func(fnCall.fn, d = true, fnCall.params)
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

class OrmFnImpl extends OrmFn {
  def count(): FnExpr[Long] = new FnExprImpl[Long] {
    override private[orm] val uid = "$count$"
    override private[orm] val expr = ExprUtil.func("COUNT(*)", d = false, Array())
    override val clazz: Class[Long] = classOf[Long]
  }

  def count(c: ResultColumn with ExprLike[_]): FnExpr[Long] = new FnExprImpl[Long] {
    override val clazz: Class[Long] = classOf[Long]
    override private[orm] val uid = s"$$count$$${c.uid}"
    override private[orm] val expr = ExprUtil.func("COUNT", d = false, Array(c.toExpr))
  }

  def sum[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$sum$$${f.uid}"
    override private[orm] val expr = ExprUtil.func("SUM", d = false, Array(f.toExpr))
  }

  def min[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$min$$${f.uid}"
    override private[orm] val expr = ExprUtil.func("MIN", d = false, Array(f.toExpr))
  }

  def max[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$max$$${f.uid}"
    override private[orm] val expr = ExprUtil.func("MAX", d = false, Array(f.toExpr))
  }

  def exists(e: ExprLike[_]): ExprLike[_] = ExprUtil.create("EXISTS", e)
}

