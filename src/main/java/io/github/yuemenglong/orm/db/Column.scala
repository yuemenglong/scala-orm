package io.github.yuemenglong.orm.db

import io.github.yuemenglong.orm.meta.FieldMetaDeclared

/**
  * Created by <yuemenglong@126.com> on 2017/8/4.
  */
object Column {
  def getAddSql(field: FieldMetaDeclared): String = {
    s"ALTER TABLE ${field.entity.table} ADD ${field.getDbSql}"
  }

  def getDropSql(field: FieldMetaDeclared): String = {
    s"ALTER TABLE ${field.entity.table} DROP ${field.column}"
  }

  def getDropSql(table: String, column: String): String = {
    s"ALTER TABLE $table DROP $column"
  }
}
