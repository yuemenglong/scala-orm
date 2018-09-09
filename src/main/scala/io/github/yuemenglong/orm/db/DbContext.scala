package io.github.yuemenglong.orm.db

/**
  * Created by Administrator on 2017/5/16.
  */

trait DbContext {
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

