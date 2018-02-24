package io.github.yuemenglong.orm.operate.field.traits

import io.github.yuemenglong.orm.operate.join.traits.Expr

/**
  * Created by <yuemenglong@126.com> on 2018/2/24.
  */
trait AssignOp {
  def assign[T](v: T): Assign

  def assign(f: Expr): Assign

  def assignAdd[T](f: Field, value: T): Assign

  def assignAdd[T](value: T): Assign

  def assignSub[T](f: Field, value: T): Assign

  def assignSub[T](value: T): Assign

  def assignNull(): Assign
}

trait Assign extends Expr
