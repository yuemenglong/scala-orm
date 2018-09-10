package io.github.yuemenglong.orm.db

import io.github.yuemenglong.orm.meta.{EntityMeta, IndexInfo}

/**
  * Created by Administrator on 2017/5/16.
  */

trait DbContext {
  def getCreateTableSql(meta: EntityMeta): String = {
    val columns = meta.fields().filter(field => field.isNormalOrPkey).map(field => {
      field.getDbSql
    }).mkString(", ")
    val sql = s"CREATE TABLE IF NOT EXISTS `${meta.table}`($columns)$createTablePostfix;"
    sql
  }

  def getDropTableSql(meta: EntityMeta): String = {
    getDropTableSql(meta.table)
  }

  def getDropTableSql(table: String): String = {
    val sql = s"DROP TABLE IF EXISTS `$table`;"
    sql
  }

  def getCreateIndexSql(info: IndexInfo): String = {
    val unique = info.unique match {
      case true => "UNIQUE "
      case false => ""
    }
    val column = info.field.column
    val table = info.field.entity.table
    s"CREATE ${unique}INDEX idx_${table}_${column} ON ${table}(${column})"
  }

  def getDropIndexSql(info: IndexInfo): String = {
    s"DROP INDEX idx_${info.field.column} ON ${info.field.entity.table}"
  }

  def createTablePostfix: String = " ENGINE=InnoDB DEFAULT CHARSET=utf8;"

  def autoIncrement: String = "AUTO_INCREMENT"
}

class MysqlContext extends DbContext {
}

class HsqldbContext extends DbContext {
}

class SqliteContext extends DbContext {
  override def createTablePostfix: String = ""

  override def autoIncrement: String = "AUTOINCREMENT"
}

