package io.github.yuemenglong.orm.api.operate.sql.core

import scala.collection.mutable.ArrayBuffer

trait SqlItem {
  override def toString: String = {
    val sb = new StringBuffer()
    genSql(sb)
    sb.toString
  }

  def genSql(sb: StringBuffer)

  def genParams(ab: ArrayBuffer[Object])

  def appendToStringBuffer(sb: StringBuffer, list: Seq[SqlItem], gap: String): Unit = {
    list.zipWithIndex.foreach { case (e, i) =>
      e.genSql(sb)
      if (i != list.length - 1) {
        sb.append(gap)
      }
    }
  }

  def nonEmpty(list: Seq[_]): Boolean = list != null && list.nonEmpty
}
