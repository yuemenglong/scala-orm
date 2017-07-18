package yy.orm.operate.traits

import yy.orm.operate.traits.core.Executable

/**
  * Created by yml on 2017/7/15.
  */
trait BatchBuilder {
  def values(arr: Array[Object]): Executable
}
