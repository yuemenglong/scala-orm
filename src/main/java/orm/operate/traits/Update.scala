package orm.operate.traits

import orm.operate.traits.core.{Assign, Cond, Executable}

/**
  * Created by yml on 2017/7/15.
  */
trait UpdateBuilder {
  def set(assign: Assign): Update

  def set(assigns: Array[Assign]): Update
}

trait Update extends UpdateBuilder with Executable {
  def where(c: Cond): core.Executable
}
