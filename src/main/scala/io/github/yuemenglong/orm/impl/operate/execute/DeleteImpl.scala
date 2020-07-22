package io.github.yuemenglong.orm.impl.operate.execute

import io.github.yuemenglong.orm.api.operate.execute.ExecutableDelete
import io.github.yuemenglong.orm.api.operate.sql.core.{ExprLike, TableLike}
import io.github.yuemenglong.orm.api.operate.sql.table.{Root, Table}
import io.github.yuemenglong.orm.impl.operate.sql.core.DeleteStatementImpl
import io.github.yuemenglong.orm.session.Session

import scala.collection.mutable.ArrayBuffer

/**
 * Created by <yuemenglong@126.com> on 2017/7/16.
 */
//noinspection ScalaFileName
trait ExecutableDeleteImpl extends ExecutableDelete with DeleteStatementImpl {
  def from(root: Root[_]): ExecutableDelete = {
    _table = root
    this
  }

  def where(e: ExprLike[_]): ExecutableDelete = {
    _where = e.toExpr
    this
  }
}

class DeleteImpl(deletes: Table*) extends ExecutableDeleteImpl {
  override val _targets: Array[TableLike] = deletes.toArray

  override def execute(session: Session): Int = {
    if (_table == null) {
      throw new RuntimeException("Must Spec A Delete Root")
    }
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
