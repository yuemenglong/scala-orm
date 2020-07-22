package io.github.yuemenglong.orm.impl.db

import java.sql.Connection

import com.jolbox.bonecp.{BoneCP, BoneCPConfig}
import io.github.yuemenglong.orm.api.db.DbConfig
import io.github.yuemenglong.orm.impl.meta.{EntityMeta, FieldMeta, IndexInfo}

/**
 * Created by Administrator on 2017/5/16.
 */

trait DbConfigImpl extends DbConfig {

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

class MysqlConfig(host: String, port: Int, val username: String, val password: String, val db: String) extends DbConfigImpl {
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

class HsqldbConfig(val username: String, val password: String, val db: String) extends DbConfigImpl {

  override def url: String = s"jdbc:hsqldb:file:${db}"

  override val context = new HsqldbContext
}

class SqliteConfig(val db: String) extends DbConfigImpl {

  override val context = new SqliteContext

  override def url: String = s"jdbc:sqlite:${db}"

  override def username: String = ""

  override def password: String = ""
}

trait DbContext {
  def getCreateTableSql(meta: EntityMeta): String

  def getDropTableSql(table: String): String

  def getDropTableSql(meta: EntityMeta): String

  def getCreateIndexSql(info: IndexInfo): String

  def getDropIndexSql(info: IndexInfo): String

  def getDropIndexSql(name: String, table: String): String

  def getAddColumnSql(field: FieldMeta): String

  def getModifyColumnSql(field: FieldMeta): String

  def getDropColumnSql(table: String, column: String): String

  def getDropColumnSql(field: FieldMeta): String

  def createTablePostfix: String

  def autoIncrement: String

  def check(db: DbImpl, ignoreUnused: Boolean = false): Unit
}

trait DbContextImpl extends DbContext {
  def getCreateTableSql(meta: EntityMeta): String = {
    val columns = meta.fields().filter(field => field.isNormalOrPkey).map(field => {
      field.getDbSql(this)
    }).mkString(", ")
    val sql = s"CREATE TABLE IF NOT EXISTS `${meta.table}`($columns)$createTablePostfix;"
    sql
  }

  def getDropTableSql(table: String): String = {
    val sql = s"DROP TABLE IF EXISTS `$table`;"
    sql
  }

  def getDropTableSql(meta: EntityMeta): String = {
    getDropTableSql(meta.table)
  }

  def getCreateIndexSql(info: IndexInfo): String = {
    val uni = info.unique match {
      case true => "UNIQUE "
      case false => ""
    }
    s"CREATE ${uni}INDEX ${info.name} ON `${info.meta.table}`(${info.columns});"
  }

  def getDropIndexSql(info: IndexInfo): String = {
    getDropIndexSql(info.name, info.meta.table)
    //    s"DROP INDEX ${info.name} ON `${info.meta.table}`;"
  }

  def getDropIndexSql(name: String, table: String): String = {
    s"DROP INDEX ${name} ON `${table}`;"
  }

  def getAddColumnSql(field: FieldMeta): String = {
    s"ALTER TABLE `${field.entity.table}` ADD ${field.getDbSql(this)};"
  }

  def getModifyColumnSql(field: FieldMeta): String = {
    s"ALTER TABLE `${field.entity.table}` MODIFY ${field.getDbSql(this)};"
  }

  def getDropColumnSql(table: String, column: String): String = {
    s"ALTER TABLE `$table` DROP `$column`;"
  }

  def getDropColumnSql(field: FieldMeta): String = {
    getDropColumnSql(field.entity.table, field.column)
  }

  def createTablePostfix: String = " ENGINE=InnoDB DEFAULT CHARSET=utf8"

  def autoIncrement: String = "AUTO_INCREMENT"

  def check(db: DbImpl, ignoreUnused: Boolean = false): Unit = {
    new MysqlChecker(db, ignoreUnused).check()
  }
}

class MysqlContext extends DbContextImpl {
}

class HsqldbContext extends DbContextImpl {
}

class SqliteContext extends DbContextImpl {
  override def createTablePostfix: String = ""

  override def autoIncrement: String = "AUTOINCREMENT"

  override def check(db: DbImpl, ignoreUnused: Boolean = false): Unit = {
    new SqliteChecker(db, ignoreUnused).check()
  }
}

