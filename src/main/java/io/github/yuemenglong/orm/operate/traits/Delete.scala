package io.github.yuemenglong.orm.operate.traits

import io.github.yuemenglong.orm.operate.traits.core.{Cond, Executable}

/**
  * Created by yml on 2017/7/15.
  */
trait ExecutableDelete extends Executable {
  def where(c: Cond): ExecutableDelete
}
