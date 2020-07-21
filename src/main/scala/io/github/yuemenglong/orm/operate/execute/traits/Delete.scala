package io.github.yuemenglong.orm.operate.execute.traits

import io.github.yuemenglong.orm.api.operate.sql.core.{DeleteStatement, ExprLike}
import io.github.yuemenglong.orm.api.operate.sql.table.Root
import io.github.yuemenglong.orm.operate.sql.core.DeleteStatementImpl

/**
 * Created by yml on 2017/7/15.
 */
//noinspection ScalaFileName
trait ExecutableDelete extends Executable with DeleteStatement {
  def from(root: Root[_]): ExecutableDelete

  def where(e: ExprLike[_]): ExecutableDelete
}

//noinspection ScalaFileName
trait ExecutableDeleteImpl extends ExecutableDelete with DeleteStatementImpl {
  def from(root: Root[_]): ExecutableDelete = {
    _table = root
    this
  }

  def where(e: ExprLike[_]): ExecutableDelete = {
    _where = e.toExpr
    this
  }
}
