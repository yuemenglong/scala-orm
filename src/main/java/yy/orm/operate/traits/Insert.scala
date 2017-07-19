package yy.orm.operate.traits

import yy.orm.operate.traits.core.Executable

import scala.annotation.varargs

/**
  * Created by yml on 2017/7/15.
  */
trait ExecutableInsert[T] extends Executable {
  def values(arr: Array[T]): ExecutableInsert[T]
}
