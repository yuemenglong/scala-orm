package io.github.yuemenglong.orm.db

import io.github.yuemenglong.orm.meta.FieldMeta

/**
  * Created by <yuemenglong@126.com> on 2017/8/4.
  */
object Column {
  def getAddSql(field: FieldMeta): String = {
    s"ALTER TABLE `${field.entity.table}` ADD ${field.getDbSql};"
  }

  def getDropSql(field: FieldMeta): String = {
    s"ALTER TABLE `${field.entity.table}` DROP `${field.column}`;"
  }

  def getChangeSql(field: FieldMeta): String = {
    s"ALTER TABLE `${field.entity.table}` CHANGE `${field.column}` ${field.getDbSql};"
  }

  def getModifySql(field: FieldMeta): String = {
    s"ALTER TABLE `${field.entity.table}` MODIFY ${field.getDbSql};"
  }

  def getDropSql(table: String, column: String): String = {
    s"ALTER TABLE `$table` DROP `$column`;"
  }

  def getCreateIndex(table: String, column: String): String = {
    s"ALTER TABLE `$table` ADD INDEX(`$column`);"
  }

  def getCreateUnique(table: String, column: String): String = {
    s"ALTER TABLE `$table` ADD UNIQUE(`$column`);"
  }
}
