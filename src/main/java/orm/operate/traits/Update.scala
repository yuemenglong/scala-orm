package orm.operate.traits

import orm.operate.traits.core.{Assign, Cond, Executable}

import scala.annotation.varargs

/**
  * Created by yml on 2017/7/15.
  */
trait UpdateBuilder {
  @varargs def set(as: Assign*): ExecutableUpdate

  def set(a: Assign): ExecutableUpdate
}

trait ExecutableUpdate extends UpdateBuilder with Executable {
  def where(c: Cond): core.Executable
}
