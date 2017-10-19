package io.github.yuemenglong.orm.operate.traits

import io.github.yuemenglong.orm.operate.traits.core.{Assign, Cond, Executable}

import scala.annotation.varargs

/**
  * Created by yml on 2017/7/15.
  */

trait ExecutableUpdate extends Executable {
  @varargs def set(as: Assign*): ExecutableUpdate

  def where(c: Cond): core.Executable
}
