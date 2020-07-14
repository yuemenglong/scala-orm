package io.github.yuemenglong.orm.session

import java.sql.Connection

import io.github.yuemenglong.orm.impl.logger.Logger

/**
  * Created by Administrator on 2017/6/6.
  */
class Transaction(session: Session) {
  session.getConnection.setAutoCommit(false)
  Logger.info("BEGIN")

  def commit(): Unit = {
    session.getConnection.commit()
    session.getConnection.setAutoCommit(true)
    session.clearTransaction()
    Logger.info("COMMIT")
  }

  def rollback(): Unit = {
    session.getConnection.rollback()
    session.getConnection.setAutoCommit(true)
    session.clearTransaction()
    Logger.info("ROLLBACK")
  }
}
