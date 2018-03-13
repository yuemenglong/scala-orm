package io.github.yuemenglong.orm.operate.field.traits

import io.github.yuemenglong.orm.operate.join.traits.Expr

/**
  * Created by <yuemenglong@126.com> on 2018/2/24.
  */
trait AssignOp {
  def assign[T](v: T): Assign

  def assign(f: Expr): Assign

  def :=[T](v: T): Assign = assign(v)

  def :=(f: Expr): Assign = assign(f)
}

trait Assign extends Expr
