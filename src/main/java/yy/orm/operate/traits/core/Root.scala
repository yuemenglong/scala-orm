package yy.orm.operate.traits.core

import yy.orm.meta.EntityMeta
import yy.orm.operate.traits.core.JoinType.JoinType

/**
  * Created by yml on 2017/7/15.
  */
object JoinType extends Enumeration {
  type JoinType = Value
  val INNER, LEFT, RIGHT, OUTER = Value
}

trait Node {
  def getParent: Node

  def getRoot: Node = {
    if (getParent == null) {
      this
    } else {
      getParent.getRoot
    }
  }
}

trait Field extends Node with CondOp with AssignOp with AsSelectable {
  def getColumn: String

  def getAlias: String
}

trait Join extends Node with AsSelectable with Expr {

  def getMeta: EntityMeta

  def getAlias: String

  def getTableWithJoinCond: String

  def join(field: String): Join = join(field, JoinType.INNER)

  def join(field: String, joinType: JoinType): Join

  def get(field: String): Field

  def on(c: Cond): Join

  override def getSql: String = getTableWithJoinCond
}

trait Root[T] extends Join {
  def getFromExpr: String

  def asSelect(): SelectRoot[T]

}
