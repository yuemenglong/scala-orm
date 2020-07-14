package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.operate.sql.core.{ExprLike, UpdateStatement}

/**
 * Created by yml on 2017/7/15.
 */
//noinspection ScalaFileName
trait ExecutableUpdate extends UpdateStatement with Executable {
  def set[T <: ExprLike[_]](as: T*): ExecutableUpdate

  def where(e: ExprLike[_]): ExecutableUpdate
}

//noinspection ScalaFileName
trait ExecutableUpdateImpl extends ExecutableUpdate {
  def set[T <: ExprLike[_]](as: T*): ExecutableUpdate = {
    _sets = as.map(_.toExpr).toArray
    this
  }

  def where(e: ExprLike[_]): ExecutableUpdate = {
    _where = e.toExpr
    this
  }
}
