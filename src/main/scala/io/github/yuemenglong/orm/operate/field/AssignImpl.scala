package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.operate.field.traits.{Assign, Field}
import io.github.yuemenglong.orm.operate.join.traits.Expr

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

case class AssignFV(f: Field, v: Object) extends Assign {
  override def getSql: String = s"${f.getColumn} = ?"

  override def getParams: Array[Object] = Array(v)
}

case class AssignFF(f1: Field, f2: Field) extends Assign {
  override def getSql: String = s"${f1.getColumn} = ${f2.getColumn}"

  override def getParams: Array[Object] = Array()
}

case class AssignExpr(f1: Field, f2: Expr) extends Assign {
  override def getSql: String = s"${f1.getColumn} = ${f2.getSql}"

  override def getParams = f2.getParams
}

case class AssignNull(f1: Field) extends Assign {
  override def getSql: String = s"${f1.getColumn} = NULL"

  override def getParams: Array[Object] = Array()
}

case class AssignAdd(f1: Field, f2: Field, v: Object) extends Assign {
  override def getParams: Array[Object] = Array(v)

  override def getSql = s"${f1.getColumn} = ${f2.getColumn} + ?"
}

case class AssignSub(f1: Field, f2: Field, v: Object) extends Assign {
  override def getParams: Array[Object] = Array(v)

  override def getSql = s"${f1.getColumn} = ${f2.getColumn} - ?"
}