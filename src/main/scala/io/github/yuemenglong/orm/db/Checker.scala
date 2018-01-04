package io.github.yuemenglong.orm.db

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.lang.Def
import io.github.yuemenglong.orm.meta._

/**
  * Created by <yuemenglong@126.com> on 2017/8/2.
  */
case class ColumnInfo(column: String, ty: String, length: Int, nullable: Boolean,
                      defaultValue: String, key: String, auto: Boolean,
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
    val pkeyEq = field.isPkey == (key == "PRI")
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
    val defaultValue = rs.getString("Default")
    val extra = rs.getString("Extra")
    val auto = extra.contains("auto_increment")

    ColumnInfo(field, ty, length, nullable, defaultValue, key, auto, set, precision, scale)
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
    val needAlter = columnMap.keySet.intersect(fieldMap.keySet).map(c => {
      val fieldMeta = fieldMap(c)
      val columnInfo = columnMap(c)
      if (columnInfo.matchs(fieldMeta)) {
        null
      } else {
        Column.getModifySql(fieldMeta)
      }
    }).filter(_ != null)
    //4. 没有加的索引
    val needCreateIndex = {
      val alreadyUniIndex = columnMap.filter(p => p._2.key == "UNI")
      val needUniIndex = meta.indexVec.filter(_._2).map(p => (p._1.column, p._1)).toMap
      val uni = needUniIndex.keySet.diff(alreadyUniIndex.keySet).map(c => {
        Column.getCreateUnique(meta.table, c)
      })
      val alreadyMulIndex = columnMap.filter(p => p._2.key == "MUL")
      val needMulIndex = meta.indexVec.filter(!_._2).map(p => (p._1.column, p._1)).toMap
      val idx = needMulIndex.keySet.diff(alreadyMulIndex.keySet).map(c => {
        Column.getCreateIndex(meta.table, c)
      })
      uni ++ idx
    }
    needDrop ++ needAdd ++ needAlter ++ needCreateIndex
  }
}
