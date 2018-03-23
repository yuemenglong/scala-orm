package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.operate.join.traits.{Cond, Root}
import io.github.yuemenglong.orm.sql.{DeleteStatement, Expr, ExprT}

/**
  * Created by yml on 2017/7/15.
  */
//noinspection ScalaFileName
trait ExecutableDelete extends Executable with DeleteStatement {
  def from(root: Root[_]): ExecutableDelete = {
    _table = root
    this
  }

  def where(e: ExprT[_]): ExecutableDelete = {
    _where = e.toExpr
    this
  }
}
