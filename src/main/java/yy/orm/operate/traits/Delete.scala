package yy.orm.operate.traits

import yy.orm.operate.traits.core.{Cond, Executable}

/**
  * Created by yml on 2017/7/15.
  */
trait DeleteBuilder {
  def where(c: Cond): ExecutableDelete
}

trait ExecutableDelete extends Executable
