package orm.operate.impl

import java.sql.Connection

import orm.kit.Kit
import orm.lang.interfaces.Entity
import orm.operate.traits.{ExecutableUpdate, UpdateBuilder}
import orm.operate.traits.core.{Assign, Cond, Executable, Root}

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */
class UpdateBuilderImpl(root: Root[_]) extends UpdateBuilder {
  override def set(as: Assign*): ExecutableUpdate = new UpdateImpl(root, as.toArray)

  override def set(a: Assign): ExecutableUpdate = new UpdateImpl(root, Array(a))
}

class UpdateImpl(root: Root[_], as: Array[Assign]) extends ExecutableUpdate {
  var cond: Cond = new CondRoot
  var assigns: Array[Assign] = as

  override def where(c: Cond): Executable = {
    cond = c
    this
  }

  def getSql: String = {
    val setSql = assigns.map(_.getSql).mkString(", ")
    val condSql = cond.getSql match {
      case "" => "1 = 1"
      case s => s
    }
    s"UPDATE ${root.getTableWithJoinCond} SET $setSql\nWHERE $condSql"
  }

  def getParams: Array[Object] = {
    assigns.flatMap(_.getParams) ++ cond.getParams
  }

  override def execute(conn: Connection): Int = Kit.execute(conn, getSql, getParams)

  override def postExecute(fn: (Entity) => Unit): Unit = {}

  override def set(as: Assign*): ExecutableUpdate = {
    assigns ++= as
    this
  }

  override def set(a: Assign): ExecutableUpdate = {
    assigns ++= Array(a)
    this
  }
}


