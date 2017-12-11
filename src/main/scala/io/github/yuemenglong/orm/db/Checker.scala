package io.github.yuemenglong.orm.db

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.lang.Def
import io.github.yuemenglong.orm.meta._

/**
  * Created by <yuemenglong@126.com> on 2017/8/2.
  */
case class ColumnInfo(column: String, ty: String, length: Int, nullable: Boolean,
                      defaultValue: String, pkey: Boolean, auto: Boolean,
                      set: Set[String], precision: Int, scale: Int) {

  def matchs(field: FieldMeta): Boolean = {
    val tyEq = (field, ty) match {
      case (_: FieldMetaBoolean, "TINYINT") => length == 1
      case (_: FieldMetaString, "CHAR") => true
      case (f: FieldMetaString, "VARCHAR") => f.length == length
      case (_: FieldMetaLongText, "VARCHAR") => true
      case (f: FieldMetaEnum, "ENUM") => f.values.toSet == set
      case (_, _) => field.dbType == ty
    }

    val defaultValueEq = field.defaultValue match {
      case null => defaultValue == null
      case Def.NONE_DEFAULT_VALUE => defaultValue == null
      case s => s == defaultValue
    }
    val columnEq = field.column == column
    val nullableEq = field.nullable == nullable
    val pkeyEq = field.isPkey == pkey
    val autoEql = field.isAuto == auto

    if (!tyEq ||
      !defaultValueEq ||
      !columnEq ||
      !nullableEq ||
      !pkeyEq ||
      !autoEql
    ) {
      false
    } else {
      true
    }
  }
}

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

  def checkEntity3(conn: Connection, meta: EntityMeta, ignoreUnused: Boolean = false): Array[String] = {
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
    val needReAdd: Array[String] = tableColumnMap.filter(p => entityColumnMap.contains(p._1)).flatMap { case (column, tableDbType) =>
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
        //        throw new RuntimeException(s"${meta.entity}:${field.name} Type Not Match, $tableDbType:$entityDbType")
        Array(Column.getDropSql(field), Column.getAddSql(field))
      } else {
        Array[String]()
      }
    }.toArray
    // enum test
    val needChange: Array[String] = {
      val enumFields: Map[String, FieldMetaEnum] = meta.fieldVec.filter(_.isInstanceOf[FieldMetaEnum]).map(_.asInstanceOf[FieldMetaEnum])
        .map(f => (f.name, f))(collection.breakOut)
      if (enumFields.isEmpty) {
        Array[String]()
      } else {
        val sql = s"SHOW COLUMNS FROM ${meta.table}"
        val resultSet = conn.prepareStatement(sql).executeQuery()
        val dbEnums = Stream.continually({
          if (resultSet.next()) {
            (resultSet.getString("Field"), resultSet.getString("Type"))
          } else {
            null
          }
        }).takeWhile(_ != null).filter(p => enumFields.contains(p._1))
        dbEnums.map { case (name, ty) =>
          val dbSet = "'(.*?)'".r.findAllMatchIn(ty).map(_.group(1)).toSet
          val entitySet = enumFields(name).values.toSet
          if (dbSet != entitySet) {
            Column.getChangeSql(enumFields(name))
          } else {
            null
          }
        }.filter(_ != null).toArray
      }
    }
    rs.close()
    stmt.close()
    needDrops ++ needAdds ++ needReAdd ++ needChange
  }

  def parseColumnInfo(rs: ResultSet): ColumnInfo = {
    val re = """^(.+?)(\((.+)\))?$""".r
    val decimalRe = """^(\d+),(\d+)$""".r
    val enumRe = """'(.+?)'""".r
    val field = rs.getString("Field")
    val (ty, detail) = rs.getString("Type") match {
      case re(t, _, content) => (t.toUpperCase(), content)
      case re(t) => (t.toUpperCase(), null)
    }
    val length = ty match {
      case "BIGINT" | "INT" | "TINYINT" | "VARCHAR" => detail.toInt
      case _ => 0
    }
    val (precision, scale) = ty match {
      case "DECIMAL" => detail match {
        case decimalRe(p, s) => (p.toInt, s.toInt)
      }
      case _ => (0, 0)
    }
    val set = ty match {
      case "ENUM" => enumRe.findAllMatchIn(detail).map(m => m.group(1)).toSet
      case _ => Set[String]()
    }
    val nullable = rs.getString("Null").toUpperCase() match {
      case "YES" => true
      case "NO" => false
    }
    val key = rs.getString("Key")
    val pkey = key == "PRI"
    val defaultValue = rs.getString("Default")
    val extra = rs.getString("Extra")
    val auto = extra.contains("auto_increment")

    ColumnInfo(field, ty, length, nullable, defaultValue, pkey, auto, set, precision, scale)
  }

  def checkEntity(conn: Connection, meta: EntityMeta, ignoreUnused: Boolean = false): Array[String] = {
    val sql = s"SHOW COLUMNS FROM ${meta.table}"
    val resultSet = conn.prepareStatement(sql).executeQuery()
    val columns = Stream.continually({
      resultSet.next() match {
        case true => parseColumnInfo(resultSet)
        case false => null
      }
    }).takeWhile(_ != null).toArray
    val columnMap = columns.map(c => (c.column, c)).toMap
    val fieldMap = meta.fieldVec.filter(_.isNormalOrPkey).map(f => (f.column, f)).toMap
    //1. 表里有实体没有
    val needDrop = ignoreUnused match {
      case true => Array()
      case false => columnMap.keySet.diff(fieldMap.keySet).map(c => {
        Column.getDropSql(meta.table, c)
      }).toArray
    }
    //2. 实体里面有，表没有
    val needAdd = fieldMap.keySet.diff(columnMap.keySet).map(c => {
      Column.getAddSql(fieldMap(c))
    })
    //3. 都有的字段，但是类型不一致，需要alter
    val needAlter = columns.map(c => {
      val fieldMeta = fieldMap(c.column)
      if (c.matchs(fieldMeta)) {
        null
      } else {
        Column.getModifySql(fieldMeta)
      }
    }).filter(_ != null)
    needDrop ++ needAdd ++ needAlter
  }
}
