package io.github.yuemenglong.orm.impl.operate.sql.field

import io.github.yuemenglong.orm.api.OrmFn
import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprLike, ResultColumn}
import io.github.yuemenglong.orm.api.operate.sql.field.{FnExpr, SelectableFieldExpr}
import io.github.yuemenglong.orm.impl.operate.sql.core._

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
