package io.github.yuemenglong.orm.operate.execute

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.operate.execute.traits.{Executable, ExecutableUpdate}
import io.github.yuemenglong.orm.operate.field.traits.Assign
import io.github.yuemenglong.orm.operate.join.CondHolder
import io.github.yuemenglong.orm.operate.join.traits.{Cond, Root}

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
    s"UPDATE ${root.getSql} SET $setSql\nWHERE $condSql"
  }

  def getParams: Array[Object] = {
    assigns.flatMap(_.getParams) ++ cond.getParams
  }

  override def execute(session: Session): Int = session.execute(getSql, getParams)

  @varargs override def set(as: Assign*): ExecutableUpdate = {
    assigns ++= as
    this
  }
}


