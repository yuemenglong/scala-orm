package orm.operate.traits.core

/**
  * Created by yml on 2017/7/15.
  */
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

trait Join extends Node with AsSelectable {

  def getAlias: String

  def getTableWithJoinCond: String

  def join(field: String): Join

  def get(field: String): Field
}

trait Root extends Join {
  def getFromExpr: String
}
