package io.github.yuemenglong.orm.db

import com.jolbox.bonecp.{BoneCP, BoneCPConfig}

/**
  * Created by Administrator on 2017/5/16.
  */

trait DbConfig {
  val context: DbContext

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
  override def url: String = s"jdbc:mysql://$host:$port/$db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC"

  override val context = new MysqlContext
}

case class HsqldbConfig(username: String, password: String, db: String,
                        min: Int = 5, max: Int = 30, partition: Int = 3) extends DbConfig {
  override def url: String = s"jdbc:hsqldb:file:${db}"

  override val context = new HsqldbContext
}

case class SqliteConfig(db: String, min: Int = 5, max: Int = 30, partition: Int = 3) extends DbConfig {
  override def url: String = s"jdbc:sqlite:${db}"

  override val context = new SqliteContext

  override def username: String = ""

  override def password: String = ""
}

