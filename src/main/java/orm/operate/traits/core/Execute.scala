package orm.operate.traits.core

import java.sql.Connection

import orm.lang.interfaces.Entity

/**
  * Created by yml on 2017/7/15.
  */
trait Executable {
  def execute(conn: Connection): Int

  def postExecute(fn: (Entity) => Unit): Unit
}

trait ExecutableJoin {
  def insert(field: String): ExecutableJoin

  def update(field: String): ExecutableJoin

  def delete(field: String): ExecutableJoin

  def ignore(field: String): ExecutableJoin

  def insert(obj: Object): ExecutableJoin

  def update(obj: Object): ExecutableJoin

  def delete(obj: Object): ExecutableJoin

  def ignore(obj: Object): ExecutableJoin
}

trait ExecutableRoot extends ExecutableJoin with Executable




