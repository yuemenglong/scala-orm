package io.github.yuemenglong.orm.operate.core.traits

import io.github.yuemenglong.orm.lang.types.Types.String

/**
  * Created by <yuemenglong@126.com> on 2018/3/15.
  */

trait Expr {
  def getSql: String

  def getParams: Array[Object]
}

trait Alias {
  def getAlias: String
}
