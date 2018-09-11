package io.github.yuemenglong.orm.db

import java.sql.{Connection, ResultSet}

import com.jolbox.bonecp.{BoneCP, BoneCPConfig}
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.meta.{EntityMeta, OrmMeta}

object Db {
  private var context: DbContext = new MysqlContext

  private def setContext(ctx: DbContext): Unit = {
    this.context = ctx
  }

  def getContext: DbContext = context
}

class Db(config: DbConfig) {
  Db.setContext(config.context)

  def this(host: String, port: Int, username: String, password: String, db: String) = {
    this(MysqlConfig(host, port, username, password, db))
  }

  def this(host: String, port: Int, username: String, password: String, db: String,
           min: Int, max: Int, partition: Int) = {
    this(MysqlConfig(host, port, username, password, db, min, max, partition))
  }

  def this(db: String) = {
    this(SqliteConfig(db))
  }

  def this(db: String, min: Int, max: Int, partition: Int) = {
    this(SqliteConfig(db, min, max, partition))
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

  def context: DbContext = config.context

  def entities(): Array[EntityMeta] = {
    OrmMeta.dbVec.length match {
      case 0 => OrmMeta.entityVec.toArray
      case _ => OrmMeta.entityVec.filter(_.db == db).toArray
    }
  }

  def check(ignoreUnused: Boolean = false): Unit = {
    context.check(this, ignoreUnused)
  }

  def rebuild(): Unit = {
    this.drop()
    this.create()
  }

  def drop(): Unit = {
    entities().foreach(entity => {
      beginTransaction(session => {
        session.execute(context.getDropTableSql(entity))
      })
      //      val sql = Table.getDropSql(entity)
      //      Logger.info(sql)
      //      this.execute(sql)
    })
  }

  def create(): Unit = {
    entities().foreach(entity => {
      beginTransaction(session => {
        session.execute(context.getCreateTableSql(entity))
        entity.indexVec.foreach(idx => {
          session.execute(context.getCreateIndexSql(idx))
        })
      })
      //      val sql = Table.getCreateSql(entity)
      //      Logger.info(sql)
      //      this.execute(sql)
    })
  }

  def openSession(): Session = {
    new Session(openConnection())
  }

  def execute(sql: String, params: Array[Object] = Array()): Int = {
    this.openConnection(conn => {
      val stmt = conn.prepareStatement(sql)
      params.zipWithIndex.foreach { case (p, i) => stmt.setObject(i + 1, p) }
      val ret = stmt.executeUpdate()
      stmt.close()
      ret
    })
  }

  def query[T](sql: String,
               params: Array[Object],
               fn: ResultSet => T): T = {
    this.openConnection(conn => {
      val stmt = conn.prepareStatement(sql)
      params.zipWithIndex.foreach { case (p, i) => stmt.setObject(i + 1, p) }
      val rs = stmt.executeQuery()
      val ret = fn(rs)
      stmt.close()
      rs.close()
      ret
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

