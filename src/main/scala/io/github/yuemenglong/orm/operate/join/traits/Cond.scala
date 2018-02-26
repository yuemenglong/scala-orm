package io.github.yuemenglong.orm.operate.join.traits

import io.github.yuemenglong.orm.operate.field.traits.{Field, FieldExpr}

/**
  * Created by yml on 2017/7/15.
  */

trait Cond extends Expr {
  def and(cond: Cond): Cond

  def or(cond: Cond): Cond
}

trait CondOp {
  def eql[T](v: T): Cond

  def eql(f: FieldExpr): Cond

  def neq[T](v: T): Cond

  def neq(f: FieldExpr): Cond

  def gt[T](v: T): Cond

  def gt(f: FieldExpr): Cond

  def gte[T](v: T): Cond

  def gte(f: FieldExpr): Cond

  def lt[T](v: T): Cond

  def lt(f: FieldExpr): Cond

  def lte[T](v: T): Cond

  def lte(f: FieldExpr): Cond

  def like(v: String): Cond

  def in[T](a: Array[T])(implicit ev: T => Object): Cond

  def in(a: Array[Object]): Cond

  def nin[T](a: Array[T])(implicit ev: T => Object): Cond

  def nin(a: Array[Object]): Cond

  def isNull: Cond

  def notNull(): Cond
}




