package orm.db

import java.sql.{Connection, DriverManager}

import orm.Session.Session
import orm.meta.OrmMeta
import orm.operate.Table

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
      case e: Throwable => throw new RuntimeException(s"[Open Connection Error] ${e.getMessage}")
    }
  }

  def rebuild(): Unit = {
    this.drop()
    this.create()
  }

  def drop(): Unit = {
    OrmMeta.entityVec.filter(!_.ignore).foreach(entity => {
      val sql = Table.getDropSql(entity)
      println(sql)
      this.execute(sql)
    })
  }

  def create(): Unit = {
    OrmMeta.entityVec.filter(!_.ignore).foreach(entity => {
      val sql = Table.getCreateSql(entity)
      println(sql)
      this.execute(sql)
    })
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

object Orm {
  def main(args: Array[String]): Unit = {
    //    val ret = "asf".split("[A-Z]")
    val ret =
      """[A-Z]""".r.replaceAllIn("personCount", m => "_" + m.group(0).toLowerCase())
    println(ret)
  }
}
