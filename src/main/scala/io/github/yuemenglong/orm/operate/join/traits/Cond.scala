package io.github.yuemenglong.orm.operate.join.traits

import io.github.yuemenglong.orm.operate.core.traits.Expr
//import io.github.yuemenglong.orm.operate.query.traits.SubQuery

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

  def eql(f: Expr): Cond

  def ===[T](v: T): Cond = eql(v)

  def ===(f: Expr): Cond = eql(f)

  def neq[T](v: T): Cond

  def neq(f: Expr): Cond

  def !==[T](v: T): Cond = neq(v)

  def !==(f: Expr): Cond = neq(f)

  def gt[T](v: T): Cond

  def gt(f: Expr): Cond

  def >[T](v: T): Cond = gt(v)

  def >(f: Expr): Cond = gt(f)

  def gte[T](v: T): Cond

  def gte(f: Expr): Cond

  def >=[T](v: T): Cond = gte(v)

  def >=(f: Expr): Cond = gte(f)

  def lt[T](v: T): Cond

  def lt(f: Expr): Cond

  def <[T](v: T): Cond = lt(v)

  def <(f: Expr): Cond = lt(f)

  def lte[T](v: T): Cond

  def lte(f: Expr): Cond

  def <=[T](v: T): Cond = lte(v)

  def <=(f: Expr): Cond = lte(f)

  def like(v: String): Cond

  def in[T](a: Array[T])(implicit ev: T => Object): Cond

  def in(a: Array[Object]): Cond

//  def in(query: SubQuery[_, _]): Cond

  def nin[T](a: Array[T])(implicit ev: T => Object): Cond

  def nin(a: Array[Object]): Cond

  def isNull: Cond

  def notNull(): Cond
}
