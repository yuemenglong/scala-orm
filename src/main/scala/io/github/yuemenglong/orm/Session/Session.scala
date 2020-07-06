package io.github.yuemenglong.orm.Session

import java.sql.{Connection, ResultSet, Statement}

import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.operate.execute.traits.Executable
import io.github.yuemenglong.orm.operate.query.Queryable

import scala.collection.mutable.ArrayBuffer

/**
 * Created by Administrator on 2017/5/24.
 */

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

  def query[T](query: Queryable[T]): Array[T]

  def first[T](q: Queryable[T]): T

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

class SessionImpl(private val conn: Connection) extends Session {
  private var closed = false
  private var tx: Transaction = _
  private var stmts = new ArrayBuffer[Stmt]()

  def inTransaction(): Boolean = {
    tx != null
  }

  def beginTransaction(): Transaction = {
    if (tx == null) {
      tx = new Transaction(this)
    }
    tx
  }

  def clearTransaction(): Unit = {
    tx = null
  }

  def isClosed: Boolean = {
    closed
  }

  def close(): Unit = {
    require(!closed)
    conn.close()
    this.closed = true
  }

  def getConnection: Connection = {
    conn
  }

  def execute(executor: Executable): Int = {
    executor.execute(this)
  }

  def query[T](query: Queryable[T]): Array[T] = {
    query.query(this)
  }

  def first[T](q: Queryable[T]): T = {
    query(q) match {
      case Array() => null.asInstanceOf[T]
      case arr => arr(0)
    }
  }

  private def record(sql: String, params: Array[Object]): Unit = {
    stmts += SimpleStmt(sql, params)
    Logger.info(stmts.last.toString)
    //    val paramsSql = params.map {
    //      case null => "null"
    //      case v => v.toString
    //    }.mkString(", ") match {
    //      case "" => ""
    //      case s => s"\n[${s}]"
    //    }
    //    val record = s"\n$sql$paramsSql"
    //    Logger.info(record)
    //    stmts += record
  }

  private def record(sql: String, params: Array[Array[Object]]): Unit = {
    stmts += BatchStmt(sql, params)
    Logger.info(stmts.last.toString)
    //    val paramsSql = params.map(row => {
    //      val content = row.map {
    //        case null => "null"
    //        case v => v.toString
    //      }.mkString(", ")
    //      s"[$content]"
    //    }).mkString("\n")
    //    val record = s"\n$sql\n$paramsSql"
    //    Logger.info(record)
    //    stmts += record
  }

  def statements(): Array[Stmt] = stmts.toArray

  def execute(sql: String,
              params: Array[Object] = Array(),
              postStmt: Statement => Unit = null): Int = {
    record(sql, params)
    val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    try {
      params.zipWithIndex.foreach { case (param, i) =>
        stmt.setObject(i + 1, param)
      }
      val ret = stmt.executeUpdate()
      if (postStmt != null) postStmt(stmt)
      ret
    } finally {
      stmt.close()
    }
  }

  def batch(sql: String, params: Array[Array[Object]],
            postStmt: Statement => Unit = null): Int = {
    record(sql, params)
    val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    try {
      val inTransaction = !conn.getAutoCommit
      if (!inTransaction) {
        conn.setAutoCommit(false)
      }
      params.foreach(row => {
        row.zipWithIndex.foreach { case (value, idx) =>
          stmt.setObject(idx + 1, value)
        }
        stmt.addBatch()
      })
      val ret = stmt.executeBatch()
      if (postStmt != null) postStmt(stmt)
      if (!inTransaction) {
        conn.commit()
      }
      ret.sum
    } finally {
      stmt.close()
    }
  }

  def query(sql: String,
            params: Array[Object] = Array(),
            fn: ResultSet => Array[Array[Any]]): Array[Array[Any]] = {
    record(sql, params)
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    var rs: ResultSet = null
    try {
      rs = stmt.executeQuery()
      fn(rs)
    } catch {
      case e: Throwable => throw e
    } finally {
      if (rs != null) {
        rs.close()
      }
      stmt.close()
    }
  }
}
