package io.github.yuemenglong.orm.operate.field

import java.sql.ResultSet

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.{Boolean, Double, Integer, Long, String}
import io.github.yuemenglong.orm.operate.query.Selectable
import io.github.yuemenglong.orm.sql.{Assign, Expr, ExprT, ResultColumn}

import scala.collection.mutable

/**
  * Created by <yuemenglong@126.com> on 2018/3/26.
  */
trait Field extends ResultColumn with AssignOp {
  def getAlias: String = uid

  override def assign(e: ExprT[_]) = e match {
    case null => Assign(Expr.asTableColumn(expr), null)
    case _ => Assign(Expr.asTableColumn(expr), e.toExpr)
  }

  def as[T](clazz: Class[T]): SelectableField[T] = {
    val that = this
    val thatClazz = clazz
    new SelectableField[T] {
      override val clazz = thatClazz
      override private[orm] val expr = that.expr
      override private[orm] val uid = that.uid
    }
  }

  def asInt(): SelectableField[Integer] = as(classOf[Integer])

  def asLong(): SelectableField[Long] = as(classOf[Long])

  def asDouble(): SelectableField[Double] = as(classOf[Double])

  def asStr(): SelectableField[String] = as(classOf[String])

  def asBool(): SelectableField[Boolean] = as(classOf[Boolean])
}

trait SelectableField[T] extends Field with Selectable[T] {
  val clazz: Class[T]

  override def getType = clazz

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = {
    resultSet.getObject(getAlias, getType)
  }

  override def getKey(value: Object): String = value match {
    case null => ""
    case _ => value.toString
  }

  override def getColumns: Array[ResultColumn] = Array(this)

  def as(alias: String) = {
    val that = this
    new SelectableField[T] {
      override val clazz = that.clazz
      override private[orm] val expr = that.expr
      override private[orm] val uid = alias
    }
  }
}
