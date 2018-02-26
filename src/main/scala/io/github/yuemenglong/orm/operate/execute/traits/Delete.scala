package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.operate.join.traits.{Cond, Root}

/**
  * Created by yml on 2017/7/15.
  */
//noinspection ScalaFileName
trait ExecutableDelete extends Executable {
  def from(root: Root[_]): ExecutableDelete

  def where(c: Cond): ExecutableDelete
}
