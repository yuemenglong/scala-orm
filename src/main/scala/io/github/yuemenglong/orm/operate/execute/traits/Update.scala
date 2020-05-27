package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.sql.{ExprT, UpdateStatement}

/**
 * Created by yml on 2017/7/15.
 */
//noinspection ScalaFileName
trait ExecutableUpdate extends UpdateStatement with Executable {
  def set[T <: ExprT[_]](as: T*): ExecutableUpdate = {
    _sets = as.map(_.toExpr).toArray
    this
  }

  def where(e: ExprT[_]): ExecutableUpdate = {
    _where = e.toExpr
    this
  }
}
