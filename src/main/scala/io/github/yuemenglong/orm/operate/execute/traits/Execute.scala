package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.session.Session
import io.github.yuemenglong.orm.impl.entity.Entity

/**
  * Created by yml on 2017/7/15.
  */
trait Executable {
  def execute(session: Session): Int
}

trait ExecuteJoin {
  def fields(fields: String*): ExecuteJoin

  def insert(field: String): ExecuteJoin

  def update(field: String): ExecuteJoin

  def delete(field: String): ExecuteJoin

  def ignore(fields: String*): ExecuteJoin

  def insertFor(obj: Object): ExecuteJoin

  def updateFor(obj: Object): ExecuteJoin

  def deleteFor(obj: Object): ExecuteJoin

  def ignoreFor(obj: Object): ExecuteJoin

  def execute(entity: Entity, session: Session): Int
}

trait TypedExecuteJoin[T] extends ExecuteJoin {

  def insert[R](fn: T => R): TypedExecuteJoin[R]

  def inserts[R](fn: T => Array[R]): TypedExecuteJoin[R]

  def insertArray[R](fn: T => Array[R]): TypedExecuteJoin[R] = inserts(fn)

  def update[R](fn: T => R): TypedExecuteJoin[R]

  def updates[R](fn: T => Array[R]): TypedExecuteJoin[R]

  def updateArray[R](fn: T => Array[R]): TypedExecuteJoin[R] = updates(fn)

  def delete[R](fn: T => R): TypedExecuteJoin[R]

  def deletes[R](fn: T => Array[R]): TypedExecuteJoin[R]

  def deleteArray[R](fn: T => Array[R]): TypedExecuteJoin[R] = deletes(fn)

  def fields(fns: (T => Object)*): TypedExecuteJoin[T]

  def ignore(fns: (T => Object)*): TypedExecuteJoin[T]
}

trait ExecuteRoot extends ExecuteJoin with Executable {
  override def ignore(fields: String*): ExecuteRoot

  override def ignoreFor(obj: Object): ExecuteRoot
}

trait TypedExecuteRoot[T] extends ExecuteRoot with TypedExecuteJoin[T] {
  def root(): T
}




