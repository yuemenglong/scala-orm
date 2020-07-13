package io.github.yuemenglong.orm.operate.field

import java.sql.ResultSet

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.{Boolean, Double, Integer, Long, String}
import io.github.yuemenglong.orm.operate.query.Selectable
import io.github.yuemenglong.orm.sql._

import scala.collection.mutable

/**
 * Created by <yuemenglong@126.com> on 2018/3/26.
 */

trait Field extends ResultColumn {
  def getAlias: String = uid

  def to[T](clazz: Class[T]): SelectableFieldExpr[T] = {
    val that = this
    val thatClazz = clazz
    new SelectableFieldExpr[T] {
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

  override def as(alias: String): Field = {
    val that = this
    new Field {
      override private[orm] val uid = alias
      override private[orm] val expr = that.expr
    }
  }
}

trait FieldExpr extends Field with ExprOpsImpl[FieldExpr] {

  def toExpr: Expr = expr

  def fromExpr(e: Expr): FieldExpr = {
    val that = this
    new FieldExpr {
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}

trait SelectableField[T] extends Field with Selectable[T] {
  val clazz: Class[T]

  override def getType = clazz

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
    new SelectableField[T] {
      override val clazz = that.clazz
      override private[orm] val uid = alias
      override private[orm] val expr = that.expr
    }
  }
}

trait SelectableFieldExpr[T] extends SelectableField[T] with ExprOpsImpl[SelectableFieldExpr[T]] {
  def toExpr: Expr = expr

  def fromExpr(e: Expr): SelectableFieldExpr[T] = {
    val that = this
    new SelectableFieldExpr[T] {
      override val clazz = that.clazz
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}
