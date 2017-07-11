package orm.operate

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/7/11.
  */

trait FieldOp {
  def eql(v: Object): Cond

  def eql(f: FieldImpl): Cond

  def in(a: Array[Object]): Cond
}

trait Cond {
  def toSql: String

  def toParam: Array[Object]

  def and(cond: Cond): Cond
}

abstract class JointCond() extends Cond {
  var conds: ArrayBuffer[Cond] = ArrayBuffer[Cond]()

  override def toParam: Array[Object] = {
    conds.flatMap(_.toParam).toArray
  }
}

case class And(cs: Cond*) extends JointCond {
  cs.foreach(conds += _)

  override def toSql: String = conds.map(_.toSql).mkString(" AND ")

  override def and(cond: Cond): Cond = {
    conds += cond
    this
  }
}

abstract class CondImpl extends Cond {
  def and(cond: Cond): Cond = And(this, cond)
}

abstract class CondFV(f: FieldImpl, v: Object) extends CondImpl {
  def op(): String

  override def toSql: String = {
    s"${f.column} ${op()} ?"
  }

  override def toParam: Array[Object] = {
    Array(v)
  }
}

abstract class CondFF(f1: FieldImpl, f2: FieldImpl) extends CondImpl {
  def op(): String

  override def toSql: String = {
    s"${f1.column} ${op()} ${f2.column}"
  }

  override def toParam: Array[Object] = {
    Array()
  }
}

case class EqFV(f: FieldImpl, v: Object) extends CondFV(f, v) {
  override def op(): String = "="
}

case class EqFF(f1: FieldImpl, f2: FieldImpl) extends CondFF(f1, f2) {
  override def op(): String = "="
}

case class InFA(f: FieldImpl, a: Array[Object]) extends CondImpl {
  override def toSql: String = {
    s"${f.column} IN (${a.map(_ => "?").mkString(", ")})"
  }

  override def toParam: Array[Object] = a
}