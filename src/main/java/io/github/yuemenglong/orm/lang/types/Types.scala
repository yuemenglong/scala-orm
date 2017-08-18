package io.github.yuemenglong.orm.lang.types

/**
  * Created by <yuemenglong@126.com> on 2017/8/4.
  */

object Types {
  type Integer = java.lang.Integer
  type Long = java.lang.Long
  type Float = java.lang.Float
  type Double = java.lang.Double
  type Boolean = java.lang.Boolean
  type String = java.lang.String
  type Date = java.util.Date
  type BigDecimal = java.math.BigDecimal

  val IntegerClass: Class[Integer] = classOf[Integer]
  val LongClass: Class[Long] = classOf[Long]
  val FloatClass: Class[Float] = classOf[Float]
  val DoubleClass: Class[Double] = classOf[Double]
  val BooleanClass: Class[Boolean] = classOf[Boolean]
  val StringClass: Class[String] = classOf[String]
  val DateClass: Class[Date] = classOf[Date]
  val BigDecimalClass: Class[BigDecimal] = classOf[BigDecimal]
}

