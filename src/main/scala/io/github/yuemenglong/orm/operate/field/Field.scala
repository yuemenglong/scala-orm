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
trait FieldT extends Field with ExprOps[FieldT] {

  def toExpr: Expr = expr

  def fromExpr(e: Expr): FieldT = {
    val that = this
    new FieldT {
      override private[orm] val uid = that.uid
      override private[orm] val expr = e
    }
  }
}

trait Field extends ResultColumn {
  def getAlias: String = uid

//  override def assign(e: ExprT[_]) = e match {
//    case null => Assign(Expr.asTableColumn(expr), null)
//    case _ => Assign(Expr.asTableColumn(expr), e.toExpr)
//  }

  def to[T](clazz: Class[T]): SelectableFieldT[T] = {
    val that = this
    val thatClazz = clazz
    new SelectableFieldT[T] {
      override val clazz = thatClazz
      override private[orm] val expr = that.expr
      override private[orm] val uid = that.uid
    }
  }

  def toInt: SelectableFieldT[Integer] = to(classOf[Integer])

  def toLong: SelectableFieldT[Long] = to(classOf[Long])

  def toDouble: SelectableFieldT[Double] = to(classOf[Double])

  def toStr: SelectableFieldT[String] = to(classOf[String])

  def toBool: SelectableFieldT[Boolean] = to(classOf[Boolean])

  override def as(alias: String): Field = {
    val that = this
    new Field {
      override private[orm] val uid = alias
      override private[orm] val expr = that.expr
    }
  }
}

trait SelectableFieldT[T] extends SelectableField[T] with ExprOps[SelectableFieldT[T]] {
  def toExpr: Expr = expr

  def fromExpr(e: Expr): SelectableFieldT[T] = {
    val that = this
    new SelectableFieldT[T] {
      override val clazz = that.clazz
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
