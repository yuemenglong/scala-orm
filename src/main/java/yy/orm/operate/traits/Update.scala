package yy.orm.operate.traits

import yy.orm.operate.traits.core.{Assign, Cond, Executable}

import scala.annotation.varargs

/**
  * Created by yml on 2017/7/15.
  */

trait ExecutableUpdate extends Executable {
  @varargs def set(as: Assign*): ExecutableUpdate

  def where(c: Cond): core.Executable
}
