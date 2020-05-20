package io.github.yuemenglong.orm.operate.execute

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.operate.execute.traits.ExecutableDelete
import io.github.yuemenglong.orm.operate.join.Cascade
import io.github.yuemenglong.orm.sql.TableLike

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

class DeleteImpl(deletes: Cascade*) extends ExecutableDelete {
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
