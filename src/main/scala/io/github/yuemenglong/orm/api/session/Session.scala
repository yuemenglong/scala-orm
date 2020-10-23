package io.github.yuemenglong.orm.api.session

import java.sql.{Connection, ResultSet, Statement}

import io.github.yuemenglong.orm.api.operate.execute.Executable
import io.github.yuemenglong.orm.api.operate.query.Query

class Stmt {}

case class SimpleStmt(sql: String, params: Array[Object]) extends Stmt {
  override def toString: String = {
    val paramsSql = params.map {
      case null => "null"
      case v => v.toString
    }.mkString(", ") match {
      case "" => ""
      case s => s"\n[${s}]"
    }
    s"$sql$paramsSql"
  }
}

case class BatchStmt(sql: String, params: Array[Array[Object]]) extends Stmt {
  override def toString: String = {
    val paramsSql = params.map(row => {
      val content = row.map {
        case null => "null"
        case v => v.toString
      }.mkString(", ")
      s"[$content]"
    }).mkString("\n")
    s"$sql\n$paramsSql"
  }
}

trait Session {

  def inTransaction(): Boolean

  def beginTransaction(): Transaction

  def clearTransaction(): Unit

  def isClosed: Boolean

  def close(): Unit

  def getConnection: Connection

  def execute(executor: Executable): Int

  def query[T](query: Query[T]): Array[T]

  def first[T](q: Query[T]): T

  def statements(): Array[Stmt]

  def execute(sql: String,
              params: Array[Object] = Array(),
              postStmt: Statement => Unit = null): Int

  def batch(sql: String, params: Array[Array[Object]],
            postStmt: Statement => Unit = null): Int

  def query(sql: String,
            params: Array[Object] = Array(),
            fn: ResultSet => Array[Array[Any]]): Array[Array[Any]]

  def commit(): Unit = getConnection.commit()

  def rollback(): Unit = getConnection.rollback()
}
