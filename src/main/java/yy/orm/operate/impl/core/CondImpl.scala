package yy.orm.operate.impl.core

import yy.orm.operate.traits.core.{Cond, Field}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by yml on 2017/7/15.
  */
class CondRoot extends Cond {
  override def and(cond: Cond): Cond = And(cond)

  override def or(cond: Cond): Cond = Or(cond)

  override def getParams: Array[Object] = Array()

  override def getSql: String = ""
}

abstract class JointCond(cs: Cond*) extends Cond {
  var conds: ArrayBuffer[Cond] = cs.to[ArrayBuffer]

  override def getParams: Array[Object] = {
    conds.flatMap(_.getParams).toArray
  }
}

case class And(cs: Cond*) extends JointCond(cs: _*) {
  override def getSql: String = conds.map(_.getSql).mkString(" AND ")

  override def and(cond: Cond): Cond = {
    conds += cond
    this
  }

  override def or(cond: Cond): Cond = Or(this, cond)
}

case class Or(cs: Cond*) extends JointCond(cs: _*) {
  override def getSql: String = conds.size match {
    case 1 => conds(0).getSql
    case n if n > 1 => s"""(${conds.map(_.getSql).mkString(" OR ")})"""
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

  override def getSql: String = {
    s"${f.getColumn} ${op()} ?"
  }

  override def getParams: Array[Object] = {
    Array(v)
  }
}

abstract class CondFF(f1: Field, f2: Field) extends CondItem {
  def op(): String

  override def getSql: String = {
    s"${f1.getColumn} ${op()} ${f2.getColumn}"
  }

  override def getParams: Array[Object] = {
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
  override def getSql: String = {
    s"${f.getColumn} IN (${a.map(_ => "?").mkString(", ")})"
  }

  override def getParams: Array[Object] = a
}

case class IsNull(f: Field) extends CondItem {
  override def getSql: String = s"${f.getColumn} IS NULL"

  override def getParams: Array[Object] = Array()
}

case class NotNull(f: Field) extends CondItem {
  override def getSql: String = s"${f.getColumn} IS NOT NULL"

  override def getParams: Array[Object] = Array()
}
