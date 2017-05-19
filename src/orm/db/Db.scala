package orm.db

import java.sql.{Connection, DriverManager}

import orm.meta.OrmMeta

/**
  * Created by Administrator on 2017/5/16.
  */
object Db {
  val driver = "com.mysql.jdbc.Driver"
  val url = "jdbc:mysql://localhost/test"
  val username = "root"
  val password = "root"

  def getConn(): Connection = {
    Class.forName(driver)
    try {
      return DriverManager.getConnection(url, username, password)
    } catch {
      case e: Throwable => null
    }
  }

  def rebuild(): Unit = {
    this.dropTables()
    this.createTables()
  }

  def dropTables(): Unit = {
    for (entity <- OrmMeta.entityVec) {
      var sql = s"DROP TABLE IF EXISTS `${entity.table}`"
      println(sql)
      this.getConn().createStatement().execute(sql)
    }
  }

  def createTables(): Unit = {
    for (entity <- OrmMeta.entityVec) {
      var columns = entity.fieldVec.filter(field => field.isNormalOrPkey()).map((field) => {
        field.getDbSql()
      }).mkString(", ")
      var sql = s"CREATE TABLE IF NOT EXISTS `${entity.table}`(${columns})"
      println(sql)
      this.getConn().createStatement().execute(sql)
    }
  }

  def test(args: Array[String]) {
    // connect to the database named "mysql" on the localhost


    // there's probably a better way to do this
    var connection: Connection = null

    try {
      // make the connection
      Class.forName(driver)
      connection = DriverManager.getConnection(url, username, password)

      // create the statement, and run the select query
      val statement = connection.createStatement()
      val resultSet = statement.executeQuery("SELECT * FROM test")
      while (resultSet.next()) {
        for (idx <- 1 to resultSet.getMetaData().getColumnCount()) {
          println(resultSet.getString(idx));
        }
      }
    } catch {
      case e: Throwable => e.printStackTrace();
    }
    connection.close()
  }
}
