package io.github.yuemenglong.orm.db

import io.github.yuemenglong.orm.meta.FieldMeta

/**
  * Created by <yuemenglong@126.com> on 2017/8/4.
  */
object Column {
  def add(field: FieldMeta): String = {
    s"ALTER TABLE ${field.entity.table} ADD ${field.getDbSql}"
  }

  def drop(field: FieldMeta): String = {
    s"ALTER TABLE ${field.entity.table} DROP ${field.column}"
  }
}
