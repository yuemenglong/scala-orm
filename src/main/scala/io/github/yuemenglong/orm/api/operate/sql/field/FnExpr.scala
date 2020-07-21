package io.github.yuemenglong.orm.api.operate.sql.field

import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprLike, ExprOps, ResultColumn}

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

trait OrmFn {
  def count(): FnExpr[Long]

  def count(c: ResultColumn with ExprLike[_]): FnExpr[Long]

  def sum[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def min[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def max[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def exists(e: ExprLike[_]): ExprLike[_]
}
