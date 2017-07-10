package orm.Session

import java.sql.Connection

/**
  * Created by Administrator on 2017/6/6.
  */
class Transaction(session: Session) {
  session.getConnection.setAutoCommit(false)

  def commit(): Unit = {
    session.getConnection.commit()
    session.getConnection.setAutoCommit(true)
    session.clearTransaction
  }

  def rollback(): Unit = {
    session.getConnection.rollback()
    session.getConnection.setAutoCommit(true)
    session.clearTransaction
  }
}
