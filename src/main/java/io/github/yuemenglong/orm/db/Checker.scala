package io.github.yuemenglong.orm.db

import java.sql.Connection

import io.github.yuemenglong.orm.meta.{EntityMeta, FieldMeta}

/**
  * Created by <yuemenglong@126.com> on 2017/8/2.
  */
object Checker {
  def checkEntity(conn: Connection, meta: EntityMeta): Unit = {
    val st = conn.createStatement()
    val sql = s"SELECT * FROM ${meta.table}"
    val rs = st.executeQuery(sql)
    val metaData = rs.getMetaData
    val columnedField = meta.managedFieldVec().filter(f => f.isNormalOrPkey)
    if (metaData.getColumnCount != columnedField.length) {
      println(metaData.getColumnCount, columnedField.length)
      throw new RuntimeException(s"${meta.entity} Entity Field & Table Column Not Match")
    }
    val columnMap: Map[String, FieldMeta] = columnedField.map(field => (field.column, field))(collection.breakOut)
    1.to(metaData.getColumnCount).foreach(idx => {
      val column = metaData.getColumnName(idx)
      if (!columnMap.contains(column)) {
        throw new RuntimeException(s"${meta.entity} Not Has Column: $column")
      }
      val field = columnMap(column)
      val dbType = metaData.getColumnTypeName(idx)
      val eq = (dbType, field.getDbType) match {
        case ("VARCHAR", "LONGTEXT") => true
        case (a, b) => a == b
      }
      if (!eq) {
        throw new RuntimeException(s"${meta.entity}:${field.name} Type Not Match, $dbType:${field.getDbType}")
      }
    })
  }
}
