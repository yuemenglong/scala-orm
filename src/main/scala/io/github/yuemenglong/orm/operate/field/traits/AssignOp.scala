package io.github.yuemenglong.orm.operate.field.traits

import io.github.yuemenglong.orm.operate.core.traits.Expr2
import io.github.yuemenglong.orm.sql.{Assign, Expr}

/**
  * Created by <yuemenglong@126.com> on 2018/2/24.
  */
trait AssignOp {

  def assign(e: Expr): Assign

  def assign[T](v: T): Assign = assign(Expr.const(v))

  def :=(e: Expr): Assign = assign(e)

  def :=[T](v: T): Assign = assign(v)
}

