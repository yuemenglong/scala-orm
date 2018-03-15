package io.github.yuemenglong.orm.operate.core.traits

import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.JoinType
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType

/**
  * Created by <yuemenglong@126.com> on 2018/3/15.
  */
trait Params {
  def getParams: Array[Object]
}

trait Expr extends Params {
  def getSql: String
}

trait Alias {
  def getAlias: String
}


