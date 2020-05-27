package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.sql.{Assign, Expr, ExprT}

/**
  * Created by <yuemenglong@126.com> on 2018/2/24.
  */
trait AssignOp {

  def assign(e: ExprT[_]): Assign

  def assign[T](v: T): Assign = assign(Expr.const(v))

  def :=(e: ExprT[_]): Assign = assign(e)

  def :=[T](v: T): Assign = assign(v)

  def ===(e: ExprT[_]): Assign = assign(e)

  def ===[T](v: T): Assign = assign(v)
}
