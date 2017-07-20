package yy.orm.operate.traits.core

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
  def eql(v: Object): Cond

  def eql(f: Field): Cond

  def neq(v: Object): Cond

  def neq(f: Field): Cond

  def gt(v: Object): Cond

  def gt(f: Field): Cond

  def gte(v: Object): Cond

  def gte(f: Field): Cond

  def lt(v: Object): Cond

  def lt(f: Field): Cond

  def lte(v: Object): Cond

  def lte(f: Field): Cond

  def in(a: Array[Object]): Cond

  def isNull: Cond

  def notNull(): Cond
}

trait AssignOp {
  def assign(v: Object): Assign

  def assign(f: Field): Assign
}

trait Assign extends Expr

