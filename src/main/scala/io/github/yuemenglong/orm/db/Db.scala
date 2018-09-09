package io.github.yuemenglong.orm.db

import java.sql.Connection

import com.jolbox.bonecp.{BoneCP, BoneCPConfig}
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.meta.{EntityMeta, OrmMeta}

/**
  * Created by Administrator on 2017/5/16.
  */

trait DbConfig {
  def initPool(): BoneCP = {
    val config = new BoneCPConfig
    config.setJdbcUrl(url)
    config.setUsername(username)
    config.setPassword(password)
    config.setMinConnectionsPerPartition(min)
    config.setMaxConnectionsPerPartition(max)
    config.setPartitionCount(partition)
    new BoneCP(config)
  }

  def username: String

  def password: String

  def db: String

  def url: String

  def min: Int

  def max: Int

  def partition: Int
}

case class MysqlConfig(host: String, port: Int,
                       username: String, password: String, db: String,
                       min: Int = 5, max: Int = 30, partition: Int = 3) extends DbConfig {
  override def url: String = {
    s"jdbc:mysql://$host:$port/$db?useUnicode=true&characterEncoding=UTF-8"
  }
}

case class HsqldbConfig(username: String, password: String, db: String,
                        min: Int = 5, max: Int = 30, partition: Int = 3) extends DbConfig {
  override def url: String = s"jdbc:hsqldb:file:${db}"
}

class Db(config: DbConfig) {

  def this(host: String, port: Int, username: String, password: String, db: String) = {
    this(MysqlConfig(host, port, username, password, db))
  }

  def this(host: String, port: Int, username: String, password: String, db: String,
           min: Int, max: Int, partition: Int) = {
    this(MysqlConfig(host, port, username, password, db, min, max, partition))
  }

  def this(username: String, password: String, db: String) = {
    this(HsqldbConfig(username, password, db))
  }

  def this(username: String, password: String, db: String,
           min: Int, max: Int, partition: Int) = {
    this(HsqldbConfig(username, password, db, min, max, partition))
  }

  val pool: BoneCP = config.initPool()
  val db: String = config.db

  def openConnection(): Connection = {
    try {
      //      val driver = "com.mysql.jdbc.Driver"
      //      Class.forName(driver)
      //      DriverManager.getConnection(url, username, password)
      pool.getConnection
    } catch {
      case e: Throwable => throw new RuntimeException(s"[Open Connection Error] ${e.getMessage}")
    }
  }

  def openConnection[T](fn: Connection => T): T = {
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

  def entities(): Array[EntityMeta] = {
    OrmMeta.dbVec.length match {
      case 0 => OrmMeta.entityVec.toArray
      case _ => OrmMeta.entityVec.filter(_.db == db).toArray
    }
  }

  def check(ignoreUnused: Boolean = false): Unit = {
    openConnection(conn => {
      Checker.checkEntities(conn, db, entities(), ignoreUnused)
    })
  }

  def rebuild(): Unit = {
    this.drop()
    this.create()
  }

  def drop(): Unit = {
    entities().foreach(entity => {
      val sql = Table.getDropSql(entity)
      Logger.info(sql)
      this.execute(sql)
    })
  }

  def create(): Unit = {
    entities().foreach(entity => {
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
    this.openConnection(conn => {
      val stmt = conn.prepareStatement(sql)
      params.zipWithIndex.foreach { case (p, i) => stmt.setObject(i + 1, p) }
      stmt.executeUpdate()
    })
  }

  def beginTransaction[T](fn: Session => T): T = {
    val session = openSession()
    val tx = session.beginTransaction()
    try {
      val ret = fn(session)
      tx.commit()
      ret
    } catch {
      case e: Throwable =>
        try {
          session.errorTrace()
          tx.rollback()
        } catch {
          case e2: Throwable => Logger.error("Roll Back Error", e2)
        }
        throw e
    } finally {
      session.close()
    }
  }
}

