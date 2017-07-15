import java.sql.{Connection, ResultSet}

import test.model.Obj

/**
  * Created by yml on 2017/7/14.
  */
trait Node {
  def parent: Node

  def root: Node
}

trait ToSql {
  def toSql: String
}

trait Expr extends ToSql {
  def toParam: Array[Object]
}

trait Cond extends Expr {
  def and(cond: Cond): Cond

  def or(cond: Cond): Cond
}

trait AsSelectable {
  def as[T](clazz: Class[T]): Selectable[T]
}

trait CondOp {
  def eql(value: Object): Cond
}

trait AssignOp {
  def assign(value: Object): Assign
}

trait Selectable[T] {
  def pick(rs: ResultSet): T

  def getColumnWithAs: String
}

trait Field extends Node with CondOp with AssignOp {
  def getColumn: String

  def getAlias: String
}

trait Join extends Node with AsSelectable {
  def join(field: String): Join

  def get(field: String): Field
}


trait SelectView[T] extends Selectable[T] with Join {
  def getFromExpr: String
}

trait UpdateView[T] extends Join {
  def getTableExpr: String
}


trait Select {
  def limit(l: Long)

  def offset(l: Long)

  def asc(field: Field)

  def desc(field: Field)
}

trait Executable[T] {
  def execute(conn: Connection): Int
}

trait ExecuteNode extends Node {
  def insert(field: String): ExecuteNode

  def update(field: String): ExecuteNode

  def delete(field: String): ExecuteNode
}


trait ExecuteRoot[T] extends ExecuteNode with Executable[T] {

}

trait BatchBuilder[T] {
  def values(arr: Array[T]): Executable[T]
}

trait Assign extends Expr {

}

trait UpdateBuilder[T] {
  def set(assign: Assign): Update[T]

  def set(assigns: Array[Assign]): Update[T]
}

trait Update[T] extends UpdateBuilder[T] with Executable[T] {
  def where(c: Cond): Executable[T]
}

object ORM {
  def execute[T](ex: Executable[T]): Int = ???

  def insert[T](clazz: Class[T]): ExecuteRoot[T] = ???

  def update[T](clazz: Class[T]): ExecuteRoot[T] = ???

  def delete[T](clazz: Class[T]): ExecuteRoot[T] = ???

  ////

  def batch[T](clazz: Class[T]): BatchBuilder[T] = ???

  def update[T](view: UpdateView[T]): UpdateBuilder[T] = ???
}

