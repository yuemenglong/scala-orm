package orm.operate

import java.sql.Connection
import java.util

import orm.meta.{EntityMeta, OrmMeta}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/22.
  */
class Selector[T](val meta: EntityMeta, val alias: String) {
  require(meta != null)
  val withs = ArrayBuffer[(String, Selector[Object])]()
  var cond: Cond = null

  def select(field: String): Selector[Object] = {
    require(meta.fieldMap.contains(field))
    val fieldMeta = meta.fieldMap(field)
    require(!fieldMeta.isNormalOrPkey())
    val selector = new Selector[Object](fieldMeta.refer, s"${this.alias}_${field}")
    this.withs += ((field, selector))
    return selector
  }

  def where(cond: Cond): Selector[T] = {
    cond.check(meta)
    this.cond = cond
    this
  }

  def getColumns(): String = {
    val selfColumns = this.meta.fieldVec.filter(field => field.isNormalOrPkey()).map(field => {
      s"${this.alias}.${field.column} AS ${this.alias}$$${field.name}"
    }).mkString(",\n\t")
    val withColumns = this.withs.map { case (_, selector) => {
      selector.getColumns()
    }
    }
    withColumns.insert(0, selfColumns)
    withColumns.mkString(",\n\n\t")
  }

  def getTables(): String = {
    val selfWiths = this.withs.map { case (field, selector) => {
      val table = selector.meta.table
      val alias = selector.alias
      val fieldMeta = this.meta.fieldMap(field)
      val cond = s"${this.alias}.${fieldMeta.left} = ${alias}.${fieldMeta.right}"
      val main = s"LEFT JOIN ${table} AS ${alias} ON ${cond}"
      val subs = selector.getTables()
      subs.length match {
        case 0 => s"${main}"
        case _ => s"${main}\n\t${subs}"
      }
    }
    }.mkString("\n\t")
    return selfWiths
  }

  def getConds(): String = {
    val subConds = this.withs.map { case (_, selector) => {
      selector.getConds()
    }
    }.filter(item => item != null)
    this.cond match {
      case null => {}
      case _ => subConds.insert(0, this.cond.toSql(alias))
    }
    if (subConds.length == 0) {
      return null
    }
    return subConds.mkString("\n\tAND ")
  }

  def getParams(): Array[Object] = {
    val subParams = this.withs.flatMap { case (_, selector) => {
      selector.getParams()
    }
    }
    this.cond match {
      case null => {}
      case _ => {
        for (item <- this.cond.toParams().reverse) {
          subParams.insert(0, item)
        }
      }
    }
    return subParams.toArray
  }

  def getSql(): String = {
    val columns = this.getColumns()
    val tables = this.getTables()
    var cond = this.getConds()
    val params = this.getParams()
    if (cond == null) {
      cond = "1 = 1"
    }
    val table = s"${meta.table} AS ${alias}"
    s"SELECT\n\t${columns}\nFROM\n\t${table}\n\t${tables}\nWHERE\n\t${cond}"
  }

  def query(conn: Connection): util.ArrayList[T] = {
    null
  }

}

object Selector {
  def from[T](clazz: Class[T]): Selector[T] = {
    OrmMeta.check()
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName())
    new Selector[T](entityMeta, entityMeta.entity)
  }
}
