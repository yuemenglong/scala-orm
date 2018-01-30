package io.github.yuemenglong.orm.operate.impl

import java.sql.Connection

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.operate.impl.core.CondHolder
import io.github.yuemenglong.orm.operate.traits.ExecutableUpdate
import io.github.yuemenglong.orm.operate.traits.core.{Assign, Cond, Executable, Root}

import scala.annotation.varargs

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

class UpdateImpl(root: Root[_]) extends ExecutableUpdate {
  var cond: Cond = new CondHolder
  var assigns: Array[Assign] = Array()

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
    s"UPDATE ${root.getTable} SET $setSql\nWHERE $condSql"
  }

  def getParams: Array[Object] = {
    assigns.flatMap(_.getParams) ++ cond.getParams
  }

  override def execute(conn: Connection): Int = Kit.execute(conn, getSql, getParams)

  @varargs override def set(as: Assign*): ExecutableUpdate = {
    assigns ++= as
    this
  }

  override def walk(fn: (Entity) => Entity): Unit = {}
}


