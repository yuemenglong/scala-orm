package io.github.yuemenglong.orm.operate.execute

import io.github.yuemenglong.orm.api.operate.sql.core.TableLike
import io.github.yuemenglong.orm.api.operate.sql.table.Table
import io.github.yuemenglong.orm.operate.execute.traits.ExecutableDeleteImpl
import io.github.yuemenglong.orm.session.Session

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

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
