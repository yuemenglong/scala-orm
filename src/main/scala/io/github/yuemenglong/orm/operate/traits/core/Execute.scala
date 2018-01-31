package io.github.yuemenglong.orm.operate.traits.core

import java.sql.Connection

import io.github.yuemenglong.orm.lang.interfaces.Entity

/**
  * Created by yml on 2017/7/15.
  */
trait Executable {
  def execute(conn: Connection): Int

  def walk(fn: (Entity) => Entity): Unit
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

  def execute(entity: Entity, conn: Connection): Int
}

trait TypedExecuteJoin[T] extends ExecuteJoin {

  def insert[R](fn: T => R): TypedExecuteJoin[R]

  def insert[R](): (T => Array[R]) => TypedExecuteJoin[R] = (fn) => inserts(fn)

  def inserts[R](fn: T => Array[R]): TypedExecuteJoin[R]

  def update[R](fn: T => R): TypedExecuteJoin[R]

  def update[R](): (T => Array[R]) => TypedExecuteJoin[R] = (fn) => updates(fn)

  def updates[R](fn: T => Array[R]): TypedExecuteJoin[R]

  def delete[R](fn: T => R): TypedExecuteJoin[R]

  def delete[R](): (T => Array[R]) => TypedExecuteJoin[R] = (fn) => deletes(fn)

  def deletes[R](fn: T => Array[R]): TypedExecuteJoin[R]

  def fields(fns: (T => Object)*): TypedExecuteJoin[T]

  def ignore(fns: (T => Object)*): TypedExecuteJoin[T]
}

trait ExecuteRoot extends ExecuteJoin with Executable {
  override def ignore(fields: String*): ExecuteRoot

  override def ignoreFor(obj: Object): ExecuteRoot
}

trait TypedExecuteRoot[T] extends ExecuteRoot with TypedExecuteJoin[T] {
}




