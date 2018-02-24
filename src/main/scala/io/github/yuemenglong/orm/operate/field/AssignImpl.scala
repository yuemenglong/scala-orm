package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.operate.field.traits.{Assign, Field, FieldExpr}

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

case class AssignFV(f: Field, v: Object) extends Assign {
  override def getSql: String = s"${f.getColumn} = ?"

  override def getParams: Array[Object] = Array(v)
}

case class AssignFE(f1: Field, f2: FieldExpr) extends Assign {
  override def getSql: String = s"${f1.getColumn} = ${f2.getSql}"

  override def getParams: Array[Object] = f2.getParams
}

case class AssignNull(f1: Field) extends Assign {
  override def getSql: String = s"${f1.getColumn} = NULL"

  override def getParams: Array[Object] = Array()
}
