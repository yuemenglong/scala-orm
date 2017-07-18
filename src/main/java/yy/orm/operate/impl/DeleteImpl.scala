package yy.orm.operate.impl

import java.sql.Connection

import yy.orm.kit.Kit
import yy.orm.lang.interfaces.Entity
import yy.orm.operate.impl.core.CondRoot
import yy.orm.operate.traits.{DeleteBuilder, ExecutableDelete}
import yy.orm.operate.traits.core.{Cond, Root}

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */
class DeleteBuilderImpl(root: Root[_]) extends DeleteBuilder {

  var cond: Cond = new CondRoot

  override def where(c: Cond): ExecutableDelete = new DeleteImpl(root, c)
}

class DeleteImpl(root: Root[_], cond: Cond) extends ExecutableDelete {

  override def execute(conn: Connection): Int = {
    val condSql = cond.getSql match {
      case "" => "1 = 1"
      case s => s
    }
    val sql = s"DELETE ${root.getAlias} FROM\n${root.getTableWithJoinCond}\nWHERE $condSql"
    val params = root.getParams ++ cond.getParams
    Kit.execute(conn, sql, params)
  }


  override def postExecute(fn: (Entity) => Unit): Unit = {}
}
