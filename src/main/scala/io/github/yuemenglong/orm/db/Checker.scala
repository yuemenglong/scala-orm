package io.github.yuemenglong.orm.db

import java.sql.{Connection, ResultSet}

import io.github.yuemenglong.orm.lang.Def
import io.github.yuemenglong.orm.meta._

import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/8/2.
  */

class MysqlChecker(db: Db, ignoreUnused: Boolean = false) {

  case class ColumnInfo(column: String, ty: String, length: Int, nullable: Boolean,
                        defaultValue: String, key: String, auto: Boolean,
                        set: Set[String], precision: Int, scale: Int) {

    def matchs(field: FieldMeta): Boolean = {
      val tyEq = (field, ty) match {
        case (_: FieldMetaInteger, "INT") => true
        case (_: FieldMetaBoolean, "TINYINT") => length == 1
        case (_: FieldMetaString, "CHAR") => true
        case (f: FieldMetaString, "VARCHAR") => f.length == length
        case (_: FieldMetaText, "VARCHAR") => true
        case (_: FieldMetaLongText, "VARCHAR") => true
        case (f: FieldMetaEnum, "ENUM") => f.values.toSet == set
        case (_, _) => field.dbType == ty
      }

      val defaultValueEq = field.defaultValue match {
        case null => defaultValue == null
        case Def.ANNOTATION_STRING_NULL => defaultValue == null
        case "now()" => defaultValue != null && defaultValue.startsWith("CURRENT_")
        case s => s == defaultValue
      }
      val columnEq = field.column == column
      val nullableEq = field.nullable == nullable
      val pkeyEq = field.isPkey == (key == "PRI")
      val autoEql = field.isAuto == auto
      val decimalEq = field match {
        case f: FieldMetaDecimal if f.precision != 0 && f.scale != 0 =>
          f.precision == precision && f.scale == scale
        case _ => true
      }

      if (!tyEq ||
        !defaultValueEq ||
        !columnEq ||
        !nullableEq ||
        !pkeyEq ||
        !autoEql ||
        !decimalEq
      ) {
        false
      } else {
        true
      }
    }
  }

  def check(): Unit = {
    val dbName = db.db
    val metas = db.entities()
    db.openConnection(conn => {
      //1. 先获取所有表结构
      val sql =
        s"""SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE' AND TABLE_SCHEMA='$dbName'"""
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
        dbTableSet.diff(entityTableSet).toArray.sorted.map(table => {
          db.context.getDropTableSql(table)
        })
      }
      val needCreates = entityTableSet.diff(dbTableSet).toArray.sorted.map(table => {
        db.context.getCreateTableSql(entityMap(table))
      })
      val needUpdates = dbTableSet.intersect(entityTableSet).toArray.sorted.flatMap(table => {
        val meta = entityMap(table)
        checkEntity(conn, meta, ignoreUnused)
      })
      val tips = (needDrops ++ needCreates ++ needUpdates).mkString("\n")
      if (tips.nonEmpty) {
        val useDb = s"USE $dbName;\n"
        val info = s"Table Schema Not Match Entity Meta, You May Need To\n$useDb$tips"
        throw new RuntimeException(info)
      }
      rs.close()
      stmt.close()
    })
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

  def checkIndex(conn: Connection, meta: EntityMeta): Array[String] = {
    val sql = s"SHOW INDEX FROM `${meta.table}`"
    val resultSet = conn.prepareStatement(sql).executeQuery()
    val onlineMap = Stream.continually({
      resultSet.next() match {
        case true =>
          val unique = resultSet.getInt("Non_unique") == 0
          val name = resultSet.getString("Key_name")
          (name, unique)
        case false => null
      }
    }).takeWhile(_ != null).filter { case (name, _) => name != "PRIMARY" }.toMap
    val defMap = meta.indexVec.map(idx => (idx.name, idx)).toMap
    // unique 性质对不上的
    val needUpdate = onlineMap.keySet.intersect(defMap.keySet).filter(name => {
      onlineMap(name) != defMap(name).unique
    })

    val needInsert = defMap.keySet.diff(onlineMap.keySet)
    // 暂时不考虑删除，只考虑同名情况下的更新

    needUpdate.toArray.sorted.map(name => {
      db.context.getDropIndexSql(name, meta.table)
    }) ++ (needInsert ++ needUpdate).toArray.sorted.map(name => {
      val info = defMap(name)
      db.context.getCreateIndexSql(info)
    })
  }

  def checkEntity(conn: Connection, meta: EntityMeta, ignoreUnused: Boolean = false): Array[String] = {
    val sql = s"SHOW COLUMNS FROM `${meta.table}`"
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
      case true => Array[String]()
      case false => columnMap.keySet.diff(fieldMap.keySet).toArray.map(c => {
        db.context.getDropColumnSql(meta.table, c)
      }).sorted
    }
    //2. 实体里面有，表没有
    val needAdd = fieldMap.keySet.diff(columnMap.keySet).toArray.map(c => {
      db.context.getAddColumnSql(fieldMap(c))
    }).sorted
    //3. 都有的字段，但是类型不一致，需要alter
    val needAlter = columnMap.keySet.intersect(fieldMap.keySet).toArray.map(c => {
      val fieldMeta = fieldMap(c)
      val columnInfo = columnMap(c)
      if (columnInfo.matchs(fieldMeta)) {
        null
      } else {
        db.context.getModifyColumnSql(fieldMeta)
      }
    }).filter(_ != null).sorted
    needDrop ++ needAdd ++ needAlter ++ checkIndex(conn, meta)
  }
}

class SqliteChecker(db: Db, ignoreUnused: Boolean = false) {

  case class SchemeInfo(ty: String, name: String, sql: String)

  def check(): Unit = {
    // 获取已经存在的表
    // 与现有的表比较
    val tips = new ArrayBuffer[String]()
    val infos = db.query("select * from sqlite_master", Array(), rs => {
      Stream.continually({
        if (rs.next()) {
          val ty = rs.getString("type")
          val name = rs.getString("name")
          val sql = rs.getString("sql")
          SchemeInfo(ty, name, sql)
        } else {
          null
        }
      }).takeWhile(_ != null).filter(_.name != "sqlite_sequence").toArray
    })
    val infoNameMap = infos.map(info => (info.name, info)).toMap
    val entityMap = db.entities().map(e => (e.table, e)).toMap
    val tableInDb = infos.filter(_.ty == "table").map(_.name).toSet
    val tableInDef = db.entities().map(_.table).toSet
    // need drop
    if (!ignoreUnused) {
      tableInDb.diff(tableInDef).toArray.sorted.foreach(t => {
        tips += db.context.getDropTableSql(t)
      })
    }
    // need create
    tableInDef.diff(tableInDb).toArray.sorted.foreach(t => {
      tips += db.context.getCreateTableSql(entityMap(t))
    })
    // need update
    tableInDb.intersect(tableInDef).foreach(t => {
      val ctips = new ArrayBuffer[String]()
      case class ColumnInfo(name: String, ty: String, notnull: Boolean, dftValue: String, pk: Boolean)
      val cinfos: Array[ColumnInfo] = db.query(s"PRAGMA table_info(`${t}`)", Array(), rs => {
        Stream.continually({
          if (rs.next()) {
            val name = rs.getString("name")
            val ty = rs.getString("type")
            val notnull = rs.getInt("notnull") == 1
            val dftValue = rs.getString("dflt_value")
            val pk = rs.getInt("pk") == 1
            ColumnInfo(name, ty, notnull, dftValue, pk)
          } else {
            null
          }
        }).takeWhile(_ != null).toArray
      })
      val columnInDb = cinfos.map(_.name).toSet
      val columnInDefMap = entityMap(t).fieldVec.filter(_.isNormalOrPkey).map(f => (f.column, f)).toMap
      // need drop
      if (!ignoreUnused) {
        columnInDb.diff(columnInDefMap.keySet).toArray.sorted.foreach(c => {
          ctips += db.context.getDropColumnSql(t, c)
        })
      }
      // need add
      columnInDefMap.keySet.diff(columnInDb).toArray.sorted.foreach(c => {
        ctips += db.context.getAddColumnSql(columnInDefMap(c))
      })
      if (ctips.nonEmpty) {
        tips ++= ctips
      } else {
        // 最后对craetetable做一次判断
        val sqlInDb = infoNameMap(t).sql + ";"
        val sqlInDef = db.context.getCreateTableSql(entityMap(t))
          .replace(" IF NOT EXISTS", "")
        if (sqlInDb != sqlInDef) {
          tips += s"Table Define Not Match\nIn DB:\n${sqlInDb}\nYour Define:\n${sqlInDef}"
        }
      }
    })
    // check index
    val idxInDb = infos.filter(_.ty == "index").map(_.name).toSet
    val idxInDefMap = db.entities().flatMap(e => {
      e.indexVec.map(idx => (idx.name, idx))
    }).toMap
    // 缺失的索引
    idxInDefMap.keySet.diff(idxInDb).foreach(idx => {
      tips += db.context.getCreateIndexSql(idxInDefMap(idx))
    })
    if (tips.nonEmpty) {
      val info = "\n" + tips.mkString("\n")
      throw new RuntimeException(info)
    }
  }
}