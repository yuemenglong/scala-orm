package io.github.yuemenglong.orm.api.db

import com.jolbox.bonecp.BoneCP
import io.github.yuemenglong.orm.impl.db.DbImpl
import io.github.yuemenglong.orm.impl.meta.{EntityMeta, FieldMeta, IndexInfo}

/**
 * Created by Administrator on 2017/5/16.
 */
trait DbConfig {
  def initPool(): BoneCP

  def setPoolArgs(min: Int, max: Int, partition: Int): DbConfig

  def setIsolation(isolation: Int): DbConfig

  def username: String

  def password: String

  def db: String

  def url: String

  def context: DbContext
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

