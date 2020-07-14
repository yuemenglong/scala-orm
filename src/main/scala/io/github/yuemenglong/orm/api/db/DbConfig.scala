package io.github.yuemenglong.orm.api.db

import java.sql.Connection

import com.jolbox.bonecp.{BoneCP, BoneCPConfig}
import io.github.yuemenglong.orm.impl.db.{DbContext, HsqldbContext, MysqlContext, SqliteContext}

/**
 * Created by Administrator on 2017/5/16.
 */

trait DbConfig {
  val context: DbContext
  private var min: Int = 5
  private var max: Int = 30
  private var partition: Int = 3
  private var isolation: Int = Connection.TRANSACTION_REPEATABLE_READ

  def initPool(): BoneCP = {
    val config = new BoneCPConfig
    config.setJdbcUrl(url)
    config.setUsername(username)
    config.setPassword(password)
    config.setMinConnectionsPerPartition(min)
    config.setMaxConnectionsPerPartition(max)
    config.setPartitionCount(partition)
    config.setDefaultTransactionIsolation(isolation match {
      case Connection.TRANSACTION_NONE => "NONE"
      case Connection.TRANSACTION_READ_UNCOMMITTED => "READ_UNCOMMITTED"
      case Connection.TRANSACTION_READ_COMMITTED => "READ_COMMITTED"
      case Connection.TRANSACTION_REPEATABLE_READ => "REPEATABLE_READ"
      case Connection.TRANSACTION_SERIALIZABLE => "SERIALIZABLE"
    })
    new BoneCP(config)
  }

  def setPoolArgs(min: Int, max: Int, partition: Int): DbConfig = {
    this.min = min
    this.max = max
    this.partition = partition
    this
  }

  def setIsolation(isolation: Int): DbConfig = {
    this.isolation = isolation
    this
  }

  def username: String

  def password: String

  def db: String

  def url: String
}

class MysqlConfig(host: String, port: Int, val username: String, val password: String, val db: String) extends DbConfig {
  def this(url: String) {
    this(null, 0, null, null, null)
    _url = url
  }

  private var useUnicode = true
  private var characterEncoding = "UTF-8"
  private var serverTimezone = "UTC"
  private var _url: String = _

  def useUnicode(b: Boolean): MysqlConfig = {
    useUnicode = b
    this
  }

  def characterEncoding(s: String): MysqlConfig = {
    characterEncoding = s
    this
  }

  def serverTimezone(s: String): MysqlConfig = {
    serverTimezone = s
    this
  }

  override def url: String = _url match {
    case null => s"jdbc:mysql://$host:$port/$db?useUnicode=${useUnicode}&characterEncoding=${characterEncoding}&serverTimezone=${serverTimezone}"
    case _ => _url
  }

  override val context = new MysqlContext
}

class HsqldbConfig(val username: String, val password: String, val db: String) extends DbConfig {

  override def url: String = s"jdbc:hsqldb:file:${db}"

  override val context = new HsqldbContext
}

class SqliteConfig(val db: String) extends DbConfig {

  override val context = new SqliteContext

  override def url: String = s"jdbc:sqlite:${db}"

  override def username: String = ""

  override def password: String = ""
}

