package io.github.yuemenglong.orm.operate.execute

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.operate.execute.traits.{ExecutableUpdate, ExecutableUpdateImpl}
import io.github.yuemenglong.orm.operate.join.Root

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

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


