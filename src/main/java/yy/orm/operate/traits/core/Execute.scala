package yy.orm.operate.traits.core

import java.sql.Connection

import yy.orm.lang.interfaces.Entity

/**
  * Created by yml on 2017/7/15.
  */
trait Executable {
  def execute(conn: Connection): Int

  def postExecute(fn: (Entity) => Unit): Unit
}

trait ExecuteJoin {
  def insert(field: String): ExecuteJoin

  def update(field: String): ExecuteJoin

  def delete(field: String): ExecuteJoin

  def ignore(field: String): ExecuteJoin

  def insert(obj: Object): ExecuteJoin

  def update(obj: Object): ExecuteJoin

  def delete(obj: Object): ExecuteJoin

  def ignore(obj: Object): ExecuteJoin
}

trait ExecuteRoot extends ExecuteJoin with Executable




