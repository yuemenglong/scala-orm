package io.github.yuemenglong.orm.operate.traits

import io.github.yuemenglong.orm.operate.traits.core.Executable

/**
  * Created by yml on 2017/7/15.
  */
//noinspection ScalaFileName
trait ExecutableInsert[T] extends Executable {
  def values(arr: Array[T]): ExecutableInsert[T]
}
