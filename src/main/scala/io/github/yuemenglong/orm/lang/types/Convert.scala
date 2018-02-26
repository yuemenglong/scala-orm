package io.github.yuemenglong.orm.lang.types

/**
  * Created by <yuemenglong@126.com> on 2017/12/12.
  */
//noinspection LanguageFeature
object Convert {
  implicit def intToObject(x: Int): java.lang.Integer = new java.lang.Integer(x)

  implicit def longToObject(x: Long): java.lang.Long = new java.lang.Long(x)

  implicit def doubleToObject(x: Double): java.lang.Double = new java.lang.Double(x)

  implicit def booleanToObject(x: Boolean): java.lang.Boolean = new java.lang.Boolean(x)
}
