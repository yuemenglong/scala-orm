package orm.operate

import java.sql.{Connection, ResultSet}
import java.util

import orm.entity.{EntityCore, EntityManager}
import orm.kit.Kit
import orm.meta.{EntityMeta, OrmMeta}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/22.
  */
class Selector[T](val meta: EntityMeta, val alias: String, val parent: Selector[_] = null) {
  require(meta != null)
  val withs = ArrayBuffer[(String, Selector[Object])]()
  var cond: Cond = null
  var on: JoinCond = null
  var map = Map[String, EntityCore]() // 过滤结果用

  def select(field: String): Selector[Object] = {
    require(meta.fieldMap.contains(field))
    val fieldMeta = meta.fieldMap(field)
    require(!fieldMeta.isNormalOrPkey())
    val selector = new Selector[Object](fieldMeta.refer, s"${this.alias}_${field}", this)
    this.withs += ((field, selector))
    return selector
  }

  def where(cond: Cond): Selector[T] = {
    cond.check(meta)
    this.cond = cond
    this
  }

  private def reset(): Unit = {
    map = Map()
    withs.foreach { case (_, selector) =>
      selector.reset()
    }
  }

  def getColumns(): String = {
    val selfColumns = this.meta.managedFieldVec.filter(field => field.isNormalOrPkey()).map(field => {
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
      val leftColumn = this.meta.fieldMap(fieldMeta.left).column
      val rightColumn = selector.meta.fieldMap(fieldMeta.right).column
      val cond = s"${this.alias}.${leftColumn} = ${alias}.${rightColumn}"
      var main = s"LEFT JOIN ${table} AS ${alias} ON ${cond}"
      val joinCond = this.on match {
        case null => ""
        case _ => this.on.toSql(parent.alias, alias, parent.meta, meta)
      }
      main = joinCond.length() match {
        case 0 => main
        case _ => s"${main} AND ${joinCond}"
      }
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
      case _ => subConds.insert(0, this.cond.toSql(alias, meta))
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
    var tables = this.getTables()
    var cond = this.getConds()
    if (cond == null) {
      cond = "1 = 1"
    }
    val table = s"${meta.table} AS ${alias}"
    if (tables.length() != 0) {
      tables = s"${table}\n\t${tables}"
    } else {
      tables = table
    }
    s"SELECT\n\t${columns}\nFROM\n\t${tables}\nWHERE\n\t${cond}"
  }

  def query(conn: Connection): util.Collection[T] = {
    val sql = this.getSql()
    println(sql)
    val params = this.getParams().map(_.toString()).mkString(", ")
    println(s"[Params] => [${params}]")
    val stmt = conn.prepareStatement(sql)
    this.getParams().zipWithIndex.foreach { case (param, i) => {
      stmt.setObject(i + 1, param)
    }
    }
    val rs = stmt.executeQuery()
    val ret = new util.ArrayList[T]()
    while (rs.next()) {
      val core = this.pick(rs)
      val key = s"@${core.getPkey()}"
      if (!map.contains(key)) {
        ret.add(EntityManager.wrap(core).asInstanceOf[T])
      }
      map += (key -> core)
    }
    rs.close()
    stmt.close()
    reset()
    return ret
  }

  def first(conn: Connection): T = {
    val coll = query(conn)
    coll.size() match {
      case 0 => null.asInstanceOf[T]
      case _ => coll.iterator().next()
    }
  }

  def pick(rs: ResultSet): EntityCore = {
    val core = pickSelf(rs)
    if (core == null) {
      return null
    }
    pickRefer(rs, core)
    return core
  }

  def pickSelf(rs: ResultSet): EntityCore = {
    val core = new EntityCore(meta, Map())
    meta.managedFieldVec().filter(_.isNormalOrPkey()).foreach(field => {
      val label = s"${alias}$$${field.name}"
      val value = rs.getObject(label)
      core.fieldMap += (field.name -> value)
    })
    val key = core.getPkey()
    if (key == null) {
      return null
    }
    if (map.contains(key.toString())) {
      return map(key.toString())
    }
    map += (key.toString() -> core)
    return core
  }

  def pickRefer(rs: ResultSet, core: EntityCore): Unit = {
    withs.foreach { case (field, selector) => {
      val bCore = selector.pick(rs)
      val fieldMeta = meta.fieldMap(field)
      if (!fieldMeta.isOneMany()) {
        if (bCore != null) {
          val entity = EntityManager.wrap(bCore)
          core.fieldMap += (field -> entity)
        } else {
          core.fieldMap += (field -> null)
        }
      }
      else {
        // 一对多的情况
        if (bCore != null) {
          val key = s"${field}@${bCore.getPkey().toString()}"
          if (!map.contains(key) && !core.fieldMap.contains(field)) {
            core.fieldMap += (field -> Kit.newInstance(fieldMeta.field.getType))
          }
          if (!map.contains(key)) {
            val list = core.fieldMap(field).asInstanceOf[util.Collection[Object]]
            list.add(EntityManager.wrap(bCore))
          }
          map += (key -> bCore)
        }
      }
    }
    }
  }

}

object Selector {
  def from[T](clazz: Class[T]): Selector[T] = {
    OrmMeta.check()
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName())
    new Selector[T](entityMeta, entityMeta.entity)
  }

  def createSelect[T](clazz: Class[T]): Selector[T] = {
    OrmMeta.check()
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName())
    new Selector[T](entityMeta, entityMeta.entity)
  }
}
