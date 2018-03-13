package io.github.yuemenglong.orm.operate.join.traits

import io.github.yuemenglong.orm.operate.field.traits.{Field, FieldExpr}

/**
  * Created by yml on 2017/7/15.
  */

trait Cond extends Expr {
  def and(cond: Cond): Cond

  def &&(cond: Cond): Cond = and(cond)

  def or(cond: Cond): Cond

  def ||(cond: Cond): Cond = or(cond)
}

trait CondOp {
  def eql[T](v: T): Cond

  def eql(f: FieldExpr): Cond

  def ===[T](v: T): Cond = eql(v)

  def ===(f: FieldExpr): Cond = eql(f)

  def neq[T](v: T): Cond

  def neq(f: FieldExpr): Cond

  def !==[T](v: T): Cond = neq(v)

  def !==(f: FieldExpr): Cond = neq(f)

  def gt[T](v: T): Cond

  def gt(f: FieldExpr): Cond

  def >[T](v: T): Cond = gt(v)

  def >(f: FieldExpr): Cond = gt(f)

  def gte[T](v: T): Cond

  def gte(f: FieldExpr): Cond

  def >=[T](v: T): Cond = gte(v)

  def >=(f: FieldExpr): Cond = gte(f)

  def lt[T](v: T): Cond

  def lt(f: FieldExpr): Cond

  def <[T](v: T): Cond = lt(v)

  def <(f: FieldExpr): Cond = lt(f)

  def lte[T](v: T): Cond

  def lte(f: FieldExpr): Cond

  def <=[T](v: T): Cond = lte(v)

  def <=(f: FieldExpr): Cond = lte(f)

  def like(v: String): Cond

  def in[T](a: Array[T])(implicit ev: T => Object): Cond

  def in(a: Array[Object]): Cond

  def nin[T](a: Array[T])(implicit ev: T => Object): Cond

  def nin(a: Array[Object]): Cond

  def isNull: Cond

  def notNull(): Cond
}




