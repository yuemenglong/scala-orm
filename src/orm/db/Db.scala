package orm.db

import java.sql.{Connection, DriverManager}

import orm.Session.Session
import orm.meta.OrmMeta

/**
  * Created by Administrator on 2017/5/16.
  */
class Db(val host: String, val port: Int, val username: String, val password: String, val db: String) {
  val driver = "com.mysql.jdbc.Driver"
  val url = s"jdbc:mysql://${host}:${port}/${db}"
  Class.forName(driver)

  def getConn(): Connection = {
    try {
      return DriverManager.getConnection(url, username, password)
    } catch {
      case _: Throwable => null
    }
  }

  def rebuild(): Unit = {
    this.drop()
    this.create()
  }

  def drop(): Unit = {
    for (entity <- OrmMeta.entityVec) {
      val sql = s"DROP TABLE IF EXISTS `${entity.table}`"
      println(sql)
      this.execute(sql)
    }
  }

  def create(): Unit = {
    for (entity <- OrmMeta.entityVec) {
      val columns = entity.fieldVec.filter(field => field.isNormalOrPkey()).map((field) => {
        field.getDbSql()
      }).mkString(", ")
      val sql = s"CREATE TABLE IF NOT EXISTS `${entity.table}`(${columns})"
      println(sql)
      this.execute(sql)
    }
  }

  def openSession(): Session = {
    new Session(getConn())
  }

  def execute(sql: String): Int = execute(sql, Array())

  def execute(sql: String, params: Array[Object]): Int = {
    val conn = this.getConn()
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (p, i) => stmt.setObject(i + 1, p) }
    val ret = stmt.executeUpdate()
    conn.close()
    return ret
  }

}
