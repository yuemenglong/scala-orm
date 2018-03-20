package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.operate.core.traits.Expr2
import io.github.yuemenglong.orm.operate.field.traits.{Assign, Field}

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */
//
//case class AssignFV(f: Field, v: Object) extends Assign {
//  override def getSql: String = s"${f.getColumn} = ?"
//
//  override def getParams: Array[Object] = Array(v)
//}
//
//case class AssignFE(f1: Field, f2: Expr) extends Assign {
//  private def value = f2 match {
//    case null => "NULL"
//    case _ => f2.getSql
//  }
//
//  override def getSql: String = s"${f1.getColumn} = ${value}"
//
//  override def getParams: Array[Object] = f2 match {
//    case null => Array()
//    case _ => f2.getParams
//  }
//}
