package io.github.yuemenglong.orm.db

import java.sql.Connection

import io.github.yuemenglong.orm.meta.{EntityMeta, FieldMeta}

/**
  * Created by <yuemenglong@126.com> on 2017/8/2.
  */
object Checker {
  def checkEntities(conn: Connection, db: String, metas: Array[EntityMeta]): Unit = {
    //1. 先获取所有表结构
    val sql =
      s"""SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA='$db'"""
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    val tableSet: Set[String] = Stream.continually({
      if (rs.next()) (true, rs.getString(1)) else (false, null)
    }).takeWhile(_._1).map(_._2)(collection.breakOut)
    metas.foreach(meta => {
      val table = meta.table
      if (!tableSet.contains(table)) {
        val createTable = Table.getCreateSql(meta)
        tableSet.foreach(println)
        throw new RuntimeException(s"${meta.entity} Entity's Table Not Found, Maybe You Need:\n $createTable")
      }
      checkEntity(conn, meta)
    })
    if (tableSet.size != metas.length) {
      metas.foreach(m => println(m.entity))
      tableSet.foreach(println)
      throw new RuntimeException(s"Entity' Count Not Match With Tables's Count, ${metas.length}:${tableSet.size}")
    }
  }

  def checkEntity(conn: Connection, meta: EntityMeta): Unit = {
    val st = conn.createStatement()
    val sql = s"SELECT * FROM ${meta.table}"
    val rs = st.executeQuery(sql)
    val metaData = rs.getMetaData
    val columnedField = meta.managedFieldVec().filter(f => f.isNormalOrPkey)
    val columnMap: Map[String, String] = 1.to(metaData.getColumnCount).map(idx => {
      val column = metaData.getColumnName(idx)
      val dbType = metaData.getColumnTypeName(idx)
      (column, dbType)
    })(collection.breakOut)
    columnedField.foreach(field => {
      val column = field.column
      if (!columnMap.contains(column)) {
        val alterSql = field.getAlterSql
        throw new RuntimeException(s"${meta.entity}'s Column $column Is Missed, You May Nedd:\n$alterSql")
      }
      val dbType = columnMap(column)
      val eq = (dbType, field.getDbType) match {
        case ("VARCHAR", "LONGTEXT") => true
        case (a, b) => a == b
      }
      if (!eq) {
        throw new RuntimeException(s"${meta.entity}:${field.name} Type Not Match, $dbType:${field.getDbType}")
      }
    })
    if (metaData.getColumnCount != columnedField.length) {
      println(metaData.getColumnCount, columnedField.length)
      throw new RuntimeException(s"${meta.entity} Entity Field & Table Column Not Match")
    }
  }
}
