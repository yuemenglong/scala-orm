package orm.operatebak

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/7/11.
  */

trait FieldOp {
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
}

trait Cond {
  def toSql: String

  def toParam: Array[Object]

  def and(cond: Cond): Cond

  def or(cond: Cond): Cond
}

abstract class JointCond(cs: Cond*) extends Cond {
  var conds: ArrayBuffer[Cond] = cs.to[ArrayBuffer]

  override def toParam: Array[Object] = {
    conds.flatMap(_.toParam).toArray
  }
}

case class And(cs: Cond*) extends JointCond(cs: _*) {

  override def toSql: String = conds.map(_.toSql).mkString(" AND ")

  override def and(cond: Cond): Cond = {
    conds += cond
    this
  }

  override def or(cond: Cond): Cond = Or(this, cond)
}

case class Or(cs: Cond*) extends JointCond(cs: _*) {

  override def toSql: String = conds.size match {
    case 1 => conds(0).toSql
    case n if n > 1 => s"""(${conds.map(_.toSql).mkString(" OR ")})"""
  }

  override def and(cond: Cond): Cond = And(this, cond)

  override def or(cond: Cond): Cond = {
    conds += cond
    this
  }
}

abstract class CondItem extends Cond {
  def and(cond: Cond): Cond = And(this, cond)

  def or(cond: Cond): Cond = Or(this, cond)
}

abstract class CondFV(f: Field, v: Object) extends CondItem {
  def op(): String

  override def toSql: String = {
    s"${f.column} ${op()} ?"
  }

  override def toParam: Array[Object] = {
    Array(v)
  }
}

abstract class CondFF(f1: Field, f2: Field) extends CondItem {
  def op(): String

  override def toSql: String = {
    s"${f1.column} ${op()} ${f2.column}"
  }

  override def toParam: Array[Object] = {
    Array()
  }
}

case class EqFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "="
}

case class EqFF(f1: Field, f2: Field) extends CondFF(f1, f2) {
  override def op(): String = "="
}

case class NeFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "<>"
}

case class NeFF(f1: Field, f2: Field) extends CondFF(f1, f2) {
  override def op(): String = "<>"
}

case class GtFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = ">"
}

case class GtFF(f1: Field, f2: Field) extends CondFF(f1, f2) {
  override def op(): String = ">"
}

case class LtFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "<"
}

case class LtFF(f1: Field, f2: Field) extends CondFF(f1, f2) {
  override def op(): String = "<"
}

case class GteFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = ">="
}

case class GteFF(f1: Field, f2: Field) extends CondFF(f1, f2) {
  override def op(): String = ">="
}

case class LteFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "<="
}

case class LteFF(f1: Field, f2: Field) extends CondFF(f1, f2) {
  override def op(): String = "<="
}

case class InFA(f: Field, a: Array[Object]) extends CondItem {
  override def toSql: String = {
    s"${f.column} IN (${a.map(_ => "?").mkString(", ")})"
  }

  override def toParam: Array[Object] = a
}