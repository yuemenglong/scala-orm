package io.github.yuemenglong.orm.api.operate.sql.field

import io.github.yuemenglong.orm.api.operate.query.Selectable
import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprOps, ResultColumn}

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
