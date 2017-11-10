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
}

trait ExecuteRoot extends ExecuteJoin with Executable {
  override def ignore(fields: String*): ExecuteRoot

  override def ignore(obj: Object): ExecuteRoot
}




