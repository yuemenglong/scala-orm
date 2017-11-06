package io.github.yuemenglong.orm.operate.traits.core

import io.github.yuemenglong.orm.meta.EntityMeta
import io.github.yuemenglong.orm.operate.impl.core._
import io.github.yuemenglong.orm.operate.traits.core.JoinType.JoinType

/**
  * Created by yml on 2017/7/15.
  */
object JoinType extends Enumeration {
  type JoinType = Value
  val INNER, LEFT, RIGHT, OUTER = Value
}

trait Node {
  //  def getParent: Node

  def getRoot: Node
}

trait Field extends Node with CondOp with AssignOp {

  def getField: String

  def getColumn: String

  def getAlias: String

  def as[T](clazz: Class[T]): SelectableField[T]

  override def eql[T](v: T): Cond = EqFV(this, v.asInstanceOf[Object])

  override def eql(f: Field): Cond = EqFF(this, f)

  override def neq[T](v: T): Cond = NeFV(this, v.asInstanceOf[Object])

  override def neq(f: Field): Cond = NeFF(this, f)

  override def gt[T](v: T): Cond = GtFV(this, v.asInstanceOf[Object])

  override def gt(f: Field): Cond = GtFF(this, f)

  override def gte[T](v: T): Cond = GteFV(this, v.asInstanceOf[Object])

  override def gte(f: Field): Cond = GteFF(this, f)

  override def lt[T](v: T): Cond = LtFV(this, v.asInstanceOf[Object])

  override def lt(f: Field): Cond = LteFF(this, f)

  override def lte[T](v: T): Cond = LteFV(this, v.asInstanceOf[Object])

  override def lte(f: Field): Cond = LteFF(this, f)

  override def like(v: String): Cond = LikeFV(this, v)

  override def in[T](a: Array[T])(implicit ev: T => Object): Cond = InFA(this, a)

  override def in(a: Array[Object]): Cond = InFA(this, a)

  override def isNull: Cond = IsNull(this)

  override def notNull(): Cond = NotNull(this)

  override def assign(f: Field): Assign = AssignFF(this, f)

  override def assign[T](v: T): Assign = AssignFV(this, v.asInstanceOf[Object])

  override def assignNull(): Assign = AssignNull(this)
}

trait Join extends Node with Expr {

  def getMeta: EntityMeta

  def getAlias: String

  def getTableWithJoinCond: String

  def join(field: String): Join = join(field, JoinType.INNER)

  def join(field: String, joinType: JoinType): Join

  def leftJoin(field: String): Join = join(field, JoinType.LEFT)

  def get(field: String): Field

  def on(c: Cond): Join

  def as[T](clazz: Class[T]): SelectableJoin[T]

  override def getSql: String = getTableWithJoinCond
}

//
//trait Root[T] extends Join {
//  def getFromExpr: String
//
//  @Deprecated
//  def asSelect(): SelectRoot[T]

//}
