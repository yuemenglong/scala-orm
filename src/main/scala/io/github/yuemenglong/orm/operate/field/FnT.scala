package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.operate.field.traits.SelectableField
import io.github.yuemenglong.orm.sql.{Expr, ResultColumn}

/**
  * Created by <yuemenglong@126.com> on 2018/3/22.
  */

trait FnT[T] extends ResultColumn with SelectableField[T] {
  def distinct: FnT[T] = {
    val fnCall = Expr.asFunctionCall(expr)
    val newExpr = Expr.func(fnCall.fn, d = true, fnCall.params)
    val that = this
    new FnT[T] {
      override val clazz = that.clazz
      override private[orm] val expr = newExpr
      override private[orm] val uid = that.uid
    }
  }
}

trait FnOp {
  def count(): FnT[Long] = new FnT[Long] {
    override private[orm] val uid = "$count$"
    override private[orm] val expr = Expr.func("COUNT(*)", d = false, Array())
    override val clazz = classOf[Long]
  }

  def count(c: ResultColumn): FnT[Long] = {
    new FnT[Long] {
      override val clazz = classOf[Long]
      override private[orm] val uid = s"$$count$$${c.uid}"
      override private[orm] val expr = Expr.func("COUNT", d = false, Array(c.toExpr))
    }
  }
}

object Fn extends FnOp
