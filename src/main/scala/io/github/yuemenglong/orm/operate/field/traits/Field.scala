package io.github.yuemenglong.orm.operate.field.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.{Boolean, Double, Integer, Long, String}
import io.github.yuemenglong.orm.operate.field._
import io.github.yuemenglong.orm.operate.join._
import io.github.yuemenglong.orm.operate.join.traits.{Cond, CondOp, Expr}
import io.github.yuemenglong.orm.operate.query.traits.Selectable

import scala.collection.mutable

/**
  * Created by <yuemenglong@126.com> on 2018/2/24.
  */
trait Field extends Expr with CondOp with AssignOp {

  def getField: String

  def getColumn: String

  def getAlias: String

  def getSql: String = getColumn

  def getParams: Array[Object] = Array()

  def as[T](clazz: Class[T]): SelectableField[T]

  def asLong(): SelectableField[Long] = as(classOf[Long])

  def asInt(): SelectableField[Integer] = as(classOf[Integer])

  def asDouble(): SelectableField[Double] = as(classOf[Double])

  def asStr(): SelectableField[String] = as(classOf[String])

  def asBool(): SelectableField[Boolean] = as(classOf[Boolean])

  override def eql[T](v: T): Cond = EqFV(this, v.asInstanceOf[Object])

  override def eql(f: Expr): Cond = EqFE(this, f)

  override def neq[T](v: T): Cond = NeFV(this, v.asInstanceOf[Object])

  override def neq(f: Field): Cond = NeFE(this, f)

  override def gt[T](v: T): Cond = GtFV(this, v.asInstanceOf[Object])

  override def gt(f: Field): Cond = GtFE(this, f)

  override def gte[T](v: T): Cond = GteFV(this, v.asInstanceOf[Object])

  override def gte(f: Field): Cond = GteFE(this, f)

  override def lt[T](v: T): Cond = LtFV(this, v.asInstanceOf[Object])

  override def lt(f: Field): Cond = LteFE(this, f)

  override def lte[T](v: T): Cond = LteFV(this, v.asInstanceOf[Object])

  override def lte(f: Field): Cond = LteFE(this, f)

  override def like(v: String): Cond = LikeFV(this, v)

  override def in[T](a: Array[T])(implicit ev: T => Object): Cond = InFA(this, a)

  override def in(a: Array[Object]): Cond = InFA(this, a)

  override def nin[T](a: Array[T])(implicit ev: T => Object): Cond = NinFA(this, a)

  override def nin(a: Array[Object]): Cond = NinFA(this, a)

  override def isNull: Cond = IsNull(this)

  override def notNull(): Cond = NotNull(this)

  override def assign(f: Expr): Assign = AssignFE(this, f)

  override def assign[T](v: T): Assign = AssignFV(this, v.asInstanceOf[Object])

  override def assignAdd[T](f: Field, v: T): Assign = AssignAdd(this, f, v.asInstanceOf[Object])

  override def assignAdd[T](v: T): Assign = AssignAdd(this, this, v.asInstanceOf[Object])

  override def assignSub[T](f: Field, v: T): Assign = AssignSub(this, f, v.asInstanceOf[Object])

  override def assignSub[T](v: T): Assign = AssignSub(this, this, v.asInstanceOf[Object])

  override def assignNull(): Assign = AssignNull(this)
}

trait SelectableField[T] extends Field with Selectable[T] {
  override def getColumnWithAs: String = s"$getColumn AS $getAlias"

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = resultSet.getObject(getAlias, getType)

  override def getKey(value: Object): String = value match {
    case null => ""
    case _ => value.toString
  }

  def distinct(): SelectableField[T]
}
