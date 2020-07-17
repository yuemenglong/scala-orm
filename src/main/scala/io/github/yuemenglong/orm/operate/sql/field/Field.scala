package io.github.yuemenglong.orm.operate.sql.field

import java.sql.ResultSet

import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprOps}
import io.github.yuemenglong.orm.impl.kit.Kit
import io.github.yuemenglong.orm.impl.entity.Entity
import io.github.yuemenglong.orm.operate.query.Selectable
import io.github.yuemenglong.orm.operate.sql.core._

import scala.collection.mutable

/**
 * Created by <yuemenglong@126.com> on 2018/3/26.
 */

trait Field extends ResultColumn {
  def getAlias: String

  def to[T](clazz: Class[T]): SelectableFieldExpr[T]

  def toInt: SelectableFieldExpr[Integer]

  def toLong: SelectableFieldExpr[Long]

  def toDouble: SelectableFieldExpr[Double]

  def toStr: SelectableFieldExpr[String]

  def toBool: SelectableFieldExpr[Boolean]
}

trait FieldExpr extends Field with ExprOps[FieldExpr] {

  def toExpr: Expr

  def fromExpr(e: Expr): FieldExpr
}

trait SelectableField[T] extends Field with Selectable[T] {
  def as(alias: String): SelectableField[T]
}

trait SelectableFieldExpr[T] extends SelectableField[T] with ExprOps[SelectableFieldExpr[T]] {
  def toExpr: Expr

  def fromExpr(e: Expr): SelectableFieldExpr[T]
}

trait FieldImpl extends Field with ResultColumnImpl {
  def getAlias: String = uid

  def to[T](clazz: Class[T]): SelectableFieldExpr[T] = {
    val that = this
    val thatClazz = clazz
    new SelectableFieldExprImpl[T] {
      override val clazz = thatClazz
      override private[orm] val expr = that.expr
      override private[orm] val uid = that.uid
    }
  }

  def toInt: SelectableFieldExpr[Integer] = to(classOf[Integer])

  def toLong: SelectableFieldExpr[Long] = to(classOf[Long])

  def toDouble: SelectableFieldExpr[Double] = to(classOf[Double])

  def toStr: SelectableFieldExpr[String] = to(classOf[String])

  def toBool: SelectableFieldExpr[Boolean] = to(classOf[Boolean])
}

trait FieldExprImpl extends FieldExpr with FieldImpl with ExprOpsImpl[FieldExpr] {

  def toExpr: Expr = expr

  def fromExpr(e: Expr): FieldExpr = {
    val that = this
    new FieldExprImpl {
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}

trait SelectableFieldImpl[T] extends SelectableField[T] with FieldImpl {

  val clazz: Class[T]

  override def getType: Class[T] = clazz

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = {
    // 适配sqlite的情况
    Kit.getObjectFromResultSet(resultSet, getAlias, getType)
  }

  override def getKey(value: Object): String = value match {
    case null => ""
    case _ => value.toString
  }

  override def getColumns: Array[ResultColumn] = Array(this)

  override def as(alias: String): SelectableField[T] = {
    val that = this
    new SelectableFieldImpl[T] {
      override val clazz = that.clazz
      override private[orm] val uid = alias
      override private[orm] val expr = that.expr
    }
  }
}

trait SelectableFieldExprImpl[T] extends SelectableFieldExpr[T]
  with SelectableFieldImpl[T]
  with ExprOpsImpl[SelectableFieldExpr[T]] {
  def toExpr: Expr = expr

  def fromExpr(e: Expr): SelectableFieldExpr[T] = {
    val that = this
    new SelectableFieldExprImpl[T] {
      override val clazz = that.clazz
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}
