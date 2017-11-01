package io.github.yuemenglong.orm.operate.traits.core

/**
  * Created by yml on 2017/7/15.
  */
trait GetSql {
  def getSql: String
}

trait GetParams {
  def getParams: Array[Object]
}

trait Expr extends GetSql with GetParams

trait Cond extends Expr {
  def and(cond: Cond): Cond

  def or(cond: Cond): Cond
}

trait CondOp {
  def eql[T](v: T): Cond

  def eql(f: Field): Cond

  def neq[T](v: T): Cond

  def neq(f: Field): Cond

  def gt[T](v: T): Cond

  def gt(f: Field): Cond

  def gte[T](v: T): Cond

  def gte(f: Field): Cond

  def lt[T](v: T): Cond

  def lt(f: Field): Cond

  def lte[T](v: T): Cond

  def lte(f: Field): Cond

  def like(v: String): Cond

  def in[T](a: Array[T])(implicit ev: T => Object): Cond

  def in(a: Array[Object]): Cond

  def isNull: Cond

  def notNull(): Cond
}

trait AssignOp {
  def assign[T](v: T): Assign

  def assign(f: Field): Assign

  def assignNull(): Assign
}

trait Assign extends Expr

