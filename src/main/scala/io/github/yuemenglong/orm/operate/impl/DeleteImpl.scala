package io.github.yuemenglong.orm.operate.impl

import java.sql.Connection

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.operate.impl.core.CondHolder
import io.github.yuemenglong.orm.operate.traits.ExecutableDelete
import io.github.yuemenglong.orm.operate.traits.core.{Cond, Join, Root}

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

class DeleteImpl(deletes: Join*) extends ExecutableDelete {
  var cond: Cond = new CondHolder
  var root: Root[_] = _

  override def from(root: Root[_]): ExecutableDelete = {
    this.root = root
    this
  }

  override def where(cond: Cond): ExecutableDelete = {
    this.cond = cond
    this
  }

  override def execute(conn: Connection): Int = {
    if (root == null) {
      throw new RuntimeException("Must Spec A Delete Root")
    }
    val condSql = cond.getSql match {
      case "" => "1 = 1"
      case s => s
    }
    val targets = deletes.map(j => s"`${j.getAlias}`").mkString(", ")
    val sql = s"DELETE $targets FROM\n${root.getTable}\nWHERE $condSql"
    val params = root.getParams ++ cond.getParams
    Kit.execute(conn, sql, params)
  }
}
