package orm.operate.traits.core

import java.sql.Connection

import orm.meta.{EntityMeta, FieldMeta}
import orm.operate.traits.Selectable

/**
  * Created by yml on 2017/7/15.
  */
trait Node {
  def getParent: Node

  def getRoot: Node
}

trait AsSelectable {
  def as[T](clazz: Class[T]): Selectable[T]
}

trait Field extends Node with CondOp with AssignOp {
  def getColumn: String

  def getAlias: String

  def getMeta: FieldMeta
}

trait Join extends Node with AsSelectable {
  def join(field: String): Join

  def get(field: String): Field

  def getMeta: EntityMeta
}

