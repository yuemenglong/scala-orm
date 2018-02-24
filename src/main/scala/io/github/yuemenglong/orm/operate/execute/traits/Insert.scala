package io.github.yuemenglong.orm.operate.execute.traits

/**
  * Created by yml on 2017/7/15.
  */
//noinspection ScalaFileName
trait ExecutableInsert[T] extends Executable {
  def values(arr: Array[T]): ExecutableInsert[T]
}
