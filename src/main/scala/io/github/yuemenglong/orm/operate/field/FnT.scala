package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.sql._
import io.github.yuemenglong.orm.lang.types.Types._

/**
  * Created by <yuemenglong@126.com> on 2018/3/22.
  */

trait FnT[T] extends ResultColumn
  with SelectableField[T]
  with ExprOps[FnT[T]] {
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

  override def toExpr = expr

  override def fromExpr(e: Expr) = {
    val that = this
    new FnT[T] {
      override val clazz = that.clazz
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}

trait FnOp {
  def count(): FnT[Long] = new FnT[Long] {
    override private[orm] val uid = "$count$"
    override private[orm] val expr = Expr.func("COUNT(*)", d = false, Array())
    override val clazz = classOf[Long]
  }

  def count(c: ResultColumn with ExprT[_]): FnT[Long] = new FnT[Long] {
    override val clazz = classOf[Long]
    override private[orm] val uid = s"$$count$$${c.uid}"
    override private[orm] val expr = Expr.func("COUNT", d = false, Array(c.toExpr))
  }

  def sum[T](f: SelectableFieldT[T]): FnT[T] = new FnT[T] {
    override val clazz = f.getType
    override private[orm] val uid = s"$$sum$$${f.uid}"
    override private[orm] val expr = Expr.func("SUM", d = false, Array(f.toExpr))
  }

  def min[T](f: SelectableFieldT[T]): FnT[T] = new FnT[T] {
    override val clazz = f.getType
    override private[orm] val uid = s"$$min$$${f.uid}"
    override private[orm] val expr = Expr.func("MIN", d = false, Array(f.toExpr))
  }

  def max[T](f: SelectableFieldT[T]): FnT[T] = new FnT[T] {
    override val clazz = f.getType
    override private[orm] val uid = s"$$max$$${f.uid}"
    override private[orm] val expr = Expr.func("MAX", d = false, Array(f.toExpr))
  }

  def exists(e: ExprT[_]): ExprT[_] = Expr("EXISTS", e)
}

object Fn extends FnOp
