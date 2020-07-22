package io.github.yuemenglong.orm.api.db

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.api.session.Session

trait Db {

  def openConnection(): Connection

  def openConnection[T](fn: Connection => T): T

  def shutdown(): Unit

  def check(ignoreUnused: Boolean = false): Unit

  def rebuild(): Unit

  def drop(): Unit

  def create(): Unit

  def openSession(): Session

  def execute(sql: String, params: Array[Object] = Array()): Int

  def query[T](sql: String,
               params: Array[Object],
               fn: ResultSet => T): T

  def beginTransaction[T](fn: Session => T): T
}
