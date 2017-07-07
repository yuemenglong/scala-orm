package orm.operate

import java.sql.{Connection, ResultSet}

import orm.entity.{EntityCore, EntityManager}
import orm.meta.{EntityMeta, OrmMeta}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/22.
  */
class Selector[T](val meta: EntityMeta, val alias: String, val parent: Selector[_] = null) {
  require(meta != null)
  val withs: ArrayBuffer[(String, Selector[Object])] = ArrayBuffer[(String, Selector[Object])]()
  var cond: Cond = _
  var on: JoinCond = _

  // 过滤结果用
  // 1. 该表的对象 pkey
  // 2. 与该表有一对多关系的对象 field@pkey
  // 3. 最终的结果集 @pkey
  var filterMap: Map[String, EntityCore] = Map[String, EntityCore]()

  def select(field: String): Selector[Object] = {
    require(meta.fieldMap.contains(field))
    val fieldMeta = meta.fieldMap(field)
    require(!fieldMeta.isNormalOrPkey)
    val selector = new Selector[Object](fieldMeta.refer, s"${this.alias}_$field", this)
    this.withs += ((field, selector))
    selector
  }

  def where(cond: Cond): Selector[T] = {
    cond.check(meta)
    this.cond = cond
    this
  }

  private def resetFilterMap(): Unit = {
    filterMap = Map()
    withs.foreach { case (_, selector) =>
      selector.resetFilterMap()
    }
  }

  private def bufferToArray(ab: ArrayBuffer[Object], ct: ClassTag[Object]): Array[Object] = {
    ab.map(item => {
      val core = EntityManager.core(item)
      val pairs: Array[(String, Array[Object])] = core.fieldMap.toArray.map(pair => {
        val (name, value) = pair
        value match {
          case ab: ArrayBuffer[_] =>
            val abo = ab.asInstanceOf[ArrayBuffer[Object]]
            val entityName = core.meta.fieldMap(name).typeName
            val entityClass = OrmMeta.entityMap(entityName).clazz
            val ct = ClassTag(entityClass).asInstanceOf[ClassTag[Object]]
            val array = bufferToArray(abo, ct)
            (name, array)
          case _ =>
            null
        }
      }).filter(_ != null)
      pairs.foreach(p => core.fieldMap += p)
      item
    }).toArray(ct)
  }

  def getColumns: String = {
    val selfColumns = this.meta.managedFieldVec().filter(field => field.isNormalOrPkey).map(field => {
      s"${this.alias}.${field.column} AS ${this.alias}$$${field.name}"
    }).mkString(",\n\t")
    val withColumns = this.withs.map { case (_, selector) =>
      selector.getColumns
    }
    withColumns.insert(0, selfColumns)
    withColumns.mkString(",\n\n\t")
  }

  def getTables: String = {
    val selfWiths = this.withs.map { case (field, selector) =>
      val table = selector.meta.table
      val alias = selector.alias
      val fieldMeta = this.meta.fieldMap(field)
      val leftColumn = this.meta.fieldMap(fieldMeta.left).column
      val rightColumn = selector.meta.fieldMap(fieldMeta.right).column
      val cond = s"${this.alias}.$leftColumn = $alias.$rightColumn"
      var main = s"LEFT JOIN $table AS $alias ON $cond"
      val joinCond = this.on match {
        case null => ""
        case _ => this.on.toSql(parent.alias, alias, parent.meta, meta)
      }
      main = joinCond.length() match {
        case 0 => main
        case _ => s"$main AND $joinCond"
      }
      val subs = selector.getTables
      subs.length match {
        case 0 => s"$main"
        case _ => s"$main\n\t$subs"
      }
    }.mkString("\n\t")
    selfWiths
  }

  def getConds: String = {
    val subConds = this.withs.map { case (_, selector) =>
      selector.getConds
    }.filter(item => item != null)
    this.cond match {
      case null =>
      case _ => subConds.insert(0, this.cond.toSql(alias, meta))
    }
    if (subConds.isEmpty) {
      return null
    }
    subConds.mkString("\n\tAND ")
  }

  def getParams: Array[Object] = {
    val subParams = this.withs.flatMap { case (_, selector) =>
      selector.getParams
    }
    this.cond match {
      case null =>
      case _ =>
        for (item <- this.cond.toParams.reverse) {
          subParams.insert(0, item)
        }
    }
    subParams.toArray
  }

  def getSql: String = {
    val columns = this.getColumns
    var tables = this.getTables
    var cond = this.getConds
    if (cond == null) {
      cond = "1 = 1"
    }
    val table = s"${meta.table} AS $alias"
    if (tables.length() != 0) {
      tables = s"$table\n\t$tables"
    } else {
      tables = table
    }
    s"SELECT\n\t$columns\nFROM\n\t$tables\nWHERE\n\t$cond"
  }

  def query(conn: Connection): Array[T] = {
    val sql = this.getSql
    println(sql)
    val params = this.getParams.map(_.toString()).mkString(", ")
    println(s"[Params] => [$params]")
    val stmt = conn.prepareStatement(sql)
    this.getParams.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    val rs = stmt.executeQuery()
    var ab = ArrayBuffer[Object]()
    while (rs.next()) {
      val core = this.pick(rs)
      val key = s"@${core.getPkey}"
      if (!filterMap.contains(key)) {
        ab += EntityManager.wrap(core)
      }
      filterMap += (key -> core)
    }
    rs.close()
    stmt.close()
    resetFilterMap()
    bufferToArray(ab, ClassTag(meta.clazz)).asInstanceOf[Array[T]]
  }

  def first(conn: Connection): T = {
    val arr = query(conn)
    arr.length match {
      case 0 => null.asInstanceOf[T]
      case _ => arr(0)
    }
  }

  def pick(rs: ResultSet): EntityCore = {
    val core = pickSelf(rs)
    if (core == null) {
      return null
    }
    pickRefer(rs, core)
    core
  }

  def pickSelf(rs: ResultSet): EntityCore = {
    val core = new EntityCore(meta, Map())
    meta.managedFieldVec().filter(_.isNormalOrPkey).foreach(field => {
      val label = s"$alias$$${field.name}"
      val value = rs.getObject(label)
      core.fieldMap += (field.name -> value)
    })
    val key = core.getPkey
    if (key == null) {
      return null
    }
    if (filterMap.contains(key.toString)) {
      return filterMap(key.toString)
    }
    filterMap += (key.toString -> core)
    core
  }

  def pickRefer(rs: ResultSet, core: EntityCore): Unit = {
    withs.foreach { case (field, selector) =>
      val bCore = selector.pick(rs)
      val fieldMeta = meta.fieldMap(field)
      if (!fieldMeta.isOneMany) {
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
          val b = EntityManager.wrap(bCore)
          val key = s"$field@${bCore.getPkey.toString}"
          if (!filterMap.contains(key) && !core.fieldMap.contains(field)) {
            // 没有就创建
            core.fieldMap += (field -> new ArrayBuffer[Object]())
          }
          if (!filterMap.contains(key)) {
            // 有了就追加
            val arr = core.fieldMap(field).asInstanceOf[ArrayBuffer[Object]]
            arr += b
          }
          filterMap += (key -> bCore)
        }
      }
    }
  }

}

object Selector {
  def from[T](clazz: Class[T]): Selector[T] = {
    OrmMeta.check()
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName)
    new Selector[T](entityMeta, entityMeta.entity)
  }

  def createSelect[T](clazz: Class[T]): Selector[T] = {
    OrmMeta.check()
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName)
    new Selector[T](entityMeta, entityMeta.entity)
  }
}
