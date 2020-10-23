package io.github.yuemenglong.orm.api.operate.sql.field

import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprOps, ResultColumn}

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

