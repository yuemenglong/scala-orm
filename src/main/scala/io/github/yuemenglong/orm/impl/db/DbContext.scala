package io.github.yuemenglong.orm.impl.db

import io.github.yuemenglong.orm.api.db.Db
import io.github.yuemenglong.orm.meta.{EntityMeta, FieldMeta, IndexInfo}

/**
  * Created by Administrator on 2017/5/16.
  */

trait DbContext {
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

  def check(db: Db, ignoreUnused: Boolean = false): Unit = {
    new MysqlChecker(db, ignoreUnused).check()
  }
}

class MysqlContext extends DbContext {
}

class HsqldbContext extends DbContext {
}

class SqliteContext extends DbContext {
  override def createTablePostfix: String = ""

  override def autoIncrement: String = "AUTOINCREMENT"

  override def check(db: Db, ignoreUnused: Boolean = false): Unit = {
    new SqliteChecker(db, ignoreUnused).check()
  }
}

