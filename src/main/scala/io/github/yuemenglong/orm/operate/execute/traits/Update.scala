package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.operate.field.traits.Assign
import io.github.yuemenglong.orm.operate.join.traits.Cond

import scala.annotation.varargs

/**
  * Created by yml on 2017/7/15.
  */
//noinspection ScalaFileName

trait ExecutableUpdate extends Executable {
  @varargs def set(as: Assign*): ExecutableUpdate

  def where(c: Cond): Executable
}
