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

  def insert(obj: Object): ExecuteJoin

  def update(obj: Object): ExecuteJoin

  def delete(obj: Object): ExecuteJoin

  def ignore(obj: Object): ExecuteJoin

  def execute(entity: Entity, conn: Connection): Int
}

trait TypedExecuteJoin[T] extends ExecuteJoin {

  def insert[R](fn: (T) => R): TypedExecuteJoin[R]

  def update[R](fn: (T) => R): TypedExecuteJoin[R]

  def delete[R](fn: (T) => R): TypedExecuteJoin[R]

  def fields(fields: String*): TypedExecuteJoin[T] = {
    super.fields(fields: _*)
    this
  }

  def ignore(fields: String*): TypedExecuteJoin[T] = {
    super.ignore(fields: _*)
    this
  }

  def insert(obj: Object): TypedExecuteJoin[T] = {
    super.insert(obj)
    this
  }

  def update(obj: Object): TypedExecuteJoin[T] = {
    super.update(obj)
    this
  }

  def delete(obj: Object): TypedExecuteJoin[T] = {
    super.delete(obj)
    this
  }

  def ignore(obj: Object): TypedExecuteJoin[T] = {
    super.ignore(obj)
    this
  }
}

trait ExecuteRoot extends ExecuteJoin with Executable {
  override def ignore(fields: String*): ExecuteRoot

  override def ignore(obj: Object): ExecuteRoot
}

trait TypedExecuteRoot[T] extends ExecuteRoot with TypedExecuteJoin[T] {
  override def ignore(fields: String*): TypedExecuteRoot[T] = {
    super.ignore(fields)
    this
  }

  override def ignore(obj: Object): TypedExecuteRoot[T] = {
    super.ignore(obj)
    this
  }
}




