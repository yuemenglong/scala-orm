package orm.Session

import java.sql.Connection

/**
  * Created by Administrator on 2017/6/6.
  */
class Transaction(conn: Connection) {
  conn.setAutoCommit(false)

  def commit(): Unit = {
    conn.commit()
    conn.setAutoCommit(true)
  }

  def rollback(): Unit = {
    conn.rollback()
    conn.setAutoCommit(true)
  }
}
