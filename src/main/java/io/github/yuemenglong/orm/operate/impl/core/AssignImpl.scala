package io.github.yuemenglong.orm.operate.impl.core

import io.github.yuemenglong.orm.operate.traits.core.{Assign, Field}

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

case class AssignNull(f1: Field) extends Assign {
  override def getSql: String = s"${f1.getColumn} = NULL"

  override def getParams: Array[Object] = Array()
}
