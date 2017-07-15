package orm.operate.traits.core

/**
  * Created by yml on 2017/7/15.
  */
trait GetSql {
  def getSql: String
}

trait Expr extends GetSql {
  def getParam: Array[Object]
}

trait Cond extends Expr {
  def and(cond: Cond): Cond

  def or(cond: Cond): Cond
}

trait CondOp {
  def eql(value: Object): Cond
}

trait AssignOp {
  def assign(value: Object): Assign
}

trait Assign extends Expr {

}

