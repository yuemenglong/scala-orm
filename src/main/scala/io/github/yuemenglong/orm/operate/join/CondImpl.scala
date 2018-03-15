package io.github.yuemenglong.orm.operate.join

import io.github.yuemenglong.orm.operate.core.traits.Expr
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.traits.Cond
import io.github.yuemenglong.orm.operate.query.traits.SubQuery

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

abstract class CondFE(f1: Field, f2: Expr) extends CondItem {
  def op(): String

  override def getSql: String = {
    s"${f1.getColumn} ${op()} ${f2.getSql}"
  }

  override def getParams: Array[Object] = f2.getParams
}

case class EqFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "="
}

case class EqFE(f1: Field, f2: Expr) extends CondFE(f1, f2) {
  override def op(): String = "="
}

case class NeFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "<>"
}

case class NeFE(f1: Field, f2: Expr) extends CondFE(f1, f2) {
  override def op(): String = "<>"
}

case class GtFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = ">"
}

case class GtFE(f1: Field, f2: Expr) extends CondFE(f1, f2) {
  override def op(): String = ">"
}

case class LtFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "<"
}

case class LtFE(f1: Field, f2: Expr) extends CondFE(f1, f2) {
  override def op(): String = "<"
}

case class GteFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = ">="
}

case class GteFE(f1: Field, f2: Expr) extends CondFE(f1, f2) {
  override def op(): String = ">="
}

case class LteFV(f: Field, v: Object) extends CondFV(f, v) {
  override def op(): String = "<="
}

case class LteFE(f1: Field, f2: Expr) extends CondFE(f1, f2) {
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

case class InFQ(f: Field, q: SubQuery[_, _]) extends CondItem {
  override def getSql: String = {
    s"${f.getColumn} IN ${q.getSql}"
  }

  override def getParams: Array[Object] = q.getParams
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

case class ExistsQ(q: SubQuery[_, _]) extends CondItem {
  override def getSql: String = {
    s" EXISTS ${q.getSql}"
  }

  override def getParams: Array[Object] = q.getParams
}

case class NotExsQ(q: SubQuery[_, _]) extends CondItem {
  override def getSql: String = {
    s" NOT EXISTS ${q.getSql}"
  }

  override def getParams: Array[Object] = q.getParams
}
