package io.github.yuemenglong.orm.operate.impl.core

import io.github.yuemenglong.orm.operate.traits.core.{Cond, Field}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by yml on 2017/7/15.
  */


abstract class JointCond(cs: Cond*) extends Cond {
  var conds: ArrayBuffer[Cond] = cs.to[ArrayBuffer]

  def validConds: ArrayBuffer[Cond] = {
    val ret = conds.filter(!_.isInstanceOf[CondHolder])
    ret.length match {
      case 0 => ArrayBuffer(new CondHolder())
      case _ => ret
    }
  }

  override def getParams: Array[Object] = {
    conds.flatMap(_.getParams).toArray
  }
}

case class And(cs: Cond*) extends JointCond(cs: _*) {
  override def getSql: String = validConds.map(_.getSql).mkString(" AND ")

  override def and(cond: Cond): Cond = {
    conds += cond
    this
  }

  override def or(cond: Cond): Cond = Or(this, cond)
}

case class Or(cs: Cond*) extends JointCond(cs: _*) {
  override def getSql: String = validConds.size match {
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

class CondHolder extends CondItem {
  override def getParams: Array[Object] = Array()

  override def getSql: String = "1=1"
}

class JoinCond(leftTable: String, leftColumn: String, rightTable: String, rightColumn: String) extends CondItem {
  override def getParams = Array()

  override def getSql = s"$leftTable.$leftColumn = $rightTable.$rightColumn"
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

case class LikeFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op() = "LIKE"
}

case class InFA[T](f: Field, a: Array[T])(implicit ev: T => Object) extends CondItem {
  override def getSql: String = {
    s"${f.getColumn} IN (${a.map(_ => "?").mkString(", ")})"
  }

  override def getParams: Array[Object] = a.map(_.asInstanceOf[Object])
}

case class NinFA[T](f: Field, a: Array[T])(implicit ev: T => Object) extends CondItem {
  override def getSql: String = {
    s"${f.getColumn} NOT IN (${a.map(_ => "?").mkString(", ")})"
  }

  override def getParams: Array[Object] = a.map(_.asInstanceOf[Object])
}

case class IsNull(f: Field) extends CondItem {
  override def getSql: String = s"${f.getColumn} IS NULL"

  override def getParams: Array[Object] = Array()
}

case class NotNull(f: Field) extends CondItem {
  override def getSql: String = s"${f.getColumn} IS NOT NULL"

  override def getParams: Array[Object] = Array()
}
