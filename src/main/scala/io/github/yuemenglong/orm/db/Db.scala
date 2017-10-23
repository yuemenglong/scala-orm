package io.github.yuemenglong.orm.db

import java.sql.{Connection, DriverManager}

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.meta.OrmMeta
import com.jolbox.bonecp.BoneCP
import com.jolbox.bonecp.BoneCPConfig
import io.github.yuemenglong.orm.logger.Logger

/**
  * Created by Administrator on 2017/5/16.
  */
class Db(val host: String, val port: Int,
         val username: String, val password: String, val db: String,
         val min: Int = 5, val max: Int = 30, val partition: Int = 3) {
  val driver = "com.mysql.jdbc.Driver"
  val url = s"jdbc:mysql://$host:$port/$db?useUnicode=true&characterEncoding=UTF-8"
  Class.forName(driver)

  val config = new BoneCPConfig
  config.setJdbcUrl(url)
  config.setUsername(username)
  config.setPassword(password)
  config.setMinConnectionsPerPartition(min)
  config.setMaxConnectionsPerPartition(max)
  config.setPartitionCount(3)
  val pool = new BoneCP(config)

  def openConnection(): Connection = {
    try {
      //      DriverManager.getConnection(url, username, password)
      pool.getConnection
    } catch {
      case e: Throwable => throw new RuntimeException(s"[Open Connection Error] ${e.getMessage}")
    }
  }

  def openConnection[T](fn: (Connection) => T): T = {
    val conn = openConnection()
    try {
      fn(conn)
    } catch {
      case e: Throwable => throw e
    } finally {
      conn.close()
    }
  }

  def shutdown(): Unit = {
    pool.shutdown()
  }

  def check(ignoreUnused: Boolean = false): Unit = {
    openConnection((conn) => {
      Checker.checkEntities(conn, db, OrmMeta.entityVec.filter(!_.ignore).toArray, ignoreUnused)
    })
  }

  def rebuild(): Unit = {
    this.drop()
    this.create()
  }

  def drop(): Unit = {
    OrmMeta.entityVec.filter(!_.ignore).foreach(entity => {
      val sql = Table.getDropSql(entity)
      Logger.info(sql)
      this.execute(sql)
    })
  }

  def create(): Unit = {
    OrmMeta.entityVec.filter(!_.ignore).foreach(entity => {
      val sql = Table.getCreateSql(entity)
      Logger.info(sql)
      this.execute(sql)
    })
  }

  def openSession(): Session = {
    new Session(openConnection())
  }

  def execute(sql: String): Int = execute(sql, Array())

  def execute(sql: String, params: Array[Object]): Int = {
    this.openConnection((conn) => {
      val stmt = conn.prepareStatement(sql)
      params.zipWithIndex.foreach { case (p, i) => stmt.setObject(i + 1, p) }
      stmt.executeUpdate()
    })
  }

  def beginTransaction[T](fn: (Session) => T): T = {
    val session = openSession()
    val tx = session.beginTransaction()
    try {
      val ret = fn(session)
      tx.commit()
      ret
    } catch {
      case e: Throwable =>
        tx.rollback()
        throw e
    } finally {
      session.close()
    }
  }
}

