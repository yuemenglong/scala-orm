package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.api.operate.sql.core.{ExprLike, UpdateStatement}
import io.github.yuemenglong.orm.operate.sql.core.UpdateStatementImpl

/**
 * Created by yml on 2017/7/15.
 */
//noinspection ScalaFileName
trait ExecutableUpdate extends UpdateStatement with Executable {
  def set[T <: ExprLike[_]](as: T*): ExecutableUpdate

  def where(e: ExprLike[_]): ExecutableUpdate
}

//noinspection ScalaFileName
trait ExecutableUpdateImpl extends ExecutableUpdate with UpdateStatementImpl {
  def set[T <: ExprLike[_]](as: T*): ExecutableUpdate = {
    _sets = as.map(_.toExpr).toArray
    this
  }

  def where(e: ExprLike[_]): ExecutableUpdate = {
    _where = e.toExpr
    this
  }
}
