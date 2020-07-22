package io.github.yuemenglong.orm.impl.operate.execute

import io.github.yuemenglong.orm.api.operate.execute.ExecutableUpdate
import io.github.yuemenglong.orm.api.operate.sql.core.ExprLike
import io.github.yuemenglong.orm.api.operate.sql.table.Root
import io.github.yuemenglong.orm.session.Session
import io.github.yuemenglong.orm.impl.operate.sql.core.UpdateStatementImpl

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */
//noinspection ScalaFileName
trait ExecutableUpdateImpl extends ExecutableUpdate with UpdateStatementImpl {
  def set[T <: ExprLike[_]](as: T*): ExecutableUpdate = {
    _sets = as.map(_.toExpr).toArray
    this
  }

  def where(e: ExprLike[_]): ExecutableUpdate = {
    _where = e.toExpr
    this
  }
}

class UpdateImpl(root: Root[_]) extends ExecutableUpdateImpl {
  override val _table: Root[_] = root

  override def execute(session: Session): Int = {
    val sql = {
      val sb = new StringBuffer()
      genSql(sb)
      sb.toString
    }
    val params = {
      val ab = new ArrayBuffer[Object]()
      genParams(ab)
      ab.toArray
    }
    session.execute(sql, params)
  }

}


