package io.github.yuemenglong.orm.db

import java.sql.Connection

import io.github.yuemenglong.orm.meta.{EntityMeta, FieldMeta}

/**
  * Created by <yuemenglong@126.com> on 2017/8/2.
  */
object Checker {
  def checkEntities(conn: Connection, db: String, metas: Array[EntityMeta], ignoreUnused: Boolean = false): Unit = {
    //1. 先获取所有表结构
    val sql =
      s"""SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA='$db'"""
    val stmt = conn.createStatement()
    val rs = stmt.executeQuery(sql)
    val dbTableSet: Set[String] = Stream.continually({
      if (rs.next()) (true, rs.getString(1)) else (false, null)
    }).takeWhile(_._1).map(_._2)(collection.breakOut)
    val entityTableSet: Set[String] = metas.map(_.table)(collection.breakOut)
    val entityMap: Map[String, EntityMeta] = metas.map(p => (p.table, p))(collection.breakOut)
    val needDrops: Array[String] = if (ignoreUnused) {
      Array()
    } else {
      dbTableSet.diff(entityTableSet).map(table => {
        Table.getDropSql(table)
      }).toArray
    }
    val needCreates = entityTableSet.diff(dbTableSet).map(table => {
      Table.getCreateSql(entityMap(table))
    }).toArray
    val needUpdates = dbTableSet.intersect(entityTableSet).flatMap(table => {
      val meta = entityMap(table)
      checkEntity(conn, meta, ignoreUnused)
    }).toArray
    val tips = (needDrops ++ needCreates ++ needUpdates).mkString("\n")
    if (tips.nonEmpty) {
      val info = s"Table Schema Not Match Entity Meta, You May Need To\n$tips"
      throw new RuntimeException(info)
    }
    rs.close()
    stmt.close()
  }

  def checkEntity(conn: Connection, meta: EntityMeta, ignoreUnused: Boolean = false): Array[String] = {
    val stmt = conn.createStatement()
    val sql = s"SELECT * FROM `${meta.table}` LIMIT 1"
    val rs = stmt.executeQuery(sql)
    val tableMetaData = rs.getMetaData
    val tableColumnMap: Map[String, String] = 1.to(tableMetaData.getColumnCount).map(idx => {
      val column = tableMetaData.getColumnName(idx).toLowerCase()
      val dbType = tableMetaData.getColumnTypeName(idx)
      (column, dbType)
    })(collection.breakOut)
    val entityColumnMap: Map[String, String] = meta.fields().filter(f => f.isNormalOrPkey)
      .map(f => (f.column.toLowerCase(), f.dbType))(collection.breakOut)
    val columnMap: Map[String, FieldMeta] = meta.fields().filter(_.isNormalOrPkey)
      .map(f => (f.column.toLowerCase(), f))(collection.breakOut)
    // need drop
    val needDrops: Array[String] = if (ignoreUnused) {
      Array()
    } else {
      tableColumnMap.map { case (column, _) =>
        if (!entityColumnMap.contains(column)) {
          Column.getDropSql(meta.table, column)
        } else {
          null
        }
      }.filter(_ != null).toArray
    }
    // need add
    val needAdds = entityColumnMap.map { case (column, _) =>
      if (!tableColumnMap.contains(column)) {
        Column.getAddSql(columnMap(column))
      } else {
        null
      }
    }.filter(_ != null).toArray
    // type mismatch
    tableColumnMap.filter(p => entityColumnMap.contains(p._1)).foreach { case (column, tableDbType) =>
      val entityDbType = entityColumnMap(column)
      val eq = (tableDbType, entityDbType) match {
        case ("VARCHAR", "LONGTEXT") => true
        case ("TINYINT", "BOOLEAN") => true
        case ("TIMESTAMP", "DATETIME") => true
        case ("CHAR", "VARCHAR") => true
        case ("CHAR", "ENUM") => true
        case (a, b) => a == b
      }
      if (!eq) {
        val field = columnMap(column)
        throw new RuntimeException(s"${meta.entity}:${field.name} Type Not Match, $tableDbType:$entityDbType")
      }
    }
    rs.close()
    stmt.close()
    needDrops ++ needAdds
  }

}
