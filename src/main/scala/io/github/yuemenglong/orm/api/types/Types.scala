package io.github.yuemenglong.orm.api.types

import java.sql.Timestamp

/**
 * Created by <yuemenglong@126.com> on 2017/8/4.
 */

object Types {
  type Boolean = java.lang.Boolean
  type Integer = java.lang.Integer
  type Long = java.lang.Long
  type Float = java.lang.Float
  type Double = java.lang.Double
  type BigDecimal = java.math.BigDecimal
  type String = java.lang.String
  type Date = java.sql.Date
  type DateTime = java.sql.Timestamp

  //  implicit def intToObject(x: Int): java.lang.Integer = new java.lang.Integer(x)
  //
  //  implicit def longToObject(x: Long): java.lang.Long = new java.lang.Long(x)
  //
  //  implicit def doubleToObject(x: Double): java.lang.Double = new java.lang.Double(x)
  //
  //  implicit def booleanToObject(x: Boolean): java.lang.Boolean = new java.lang.Boolean(x)

  implicit def toDate(date: java.util.Date): java.sql.Date = new java.sql.Date(date.getTime)

  implicit def toDateTime(date: java.util.Date): java.sql.Timestamp = new Timestamp(date.getTime)

  val BooleanClass: Class[Boolean] = classOf[Boolean]
  val IntegerClass: Class[Integer] = classOf[Integer]
  val LongClass: Class[Long] = classOf[Long]
  val FloatClass: Class[Float] = classOf[Float]
  val DoubleClass: Class[Double] = classOf[Double]
  val BigDecimalClass: Class[BigDecimal] = classOf[BigDecimal]
  val StringClass: Class[String] = classOf[String]
  val DateClass: Class[Date] = classOf[Date]
  val DateTimeClass: Class[DateTime] = classOf[DateTime]

  def newInstance(clazz: Class[_]): Object = {
    clazz match {
      case IntegerClass => new Integer(0)
      case LongClass => new Long(0)
      case FloatClass => new Float(0)
      case DoubleClass => new Double(0)
      case BooleanClass => new Boolean(false)
      case StringClass => new String("")
      case DateClass => new Date(new java.util.Date().getTime)
      case DateTimeClass => new DateTime(new java.util.Date().getTime)
      case BigDecimalClass => new BigDecimal(0)
    }
  }
}

