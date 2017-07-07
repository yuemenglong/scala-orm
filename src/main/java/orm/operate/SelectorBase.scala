package orm.operate

import orm.meta.EntityMeta

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/7/7.
  */
abstract class SelectorBase(val meta: EntityMeta, val alias: String, val parent: SelectorBase = null) {
  protected var fields: Set[String] = Set[String]()
  protected val withs: ArrayBuffer[(String, SelectorBase)] = ArrayBuffer[(String, SelectorBase)]()
  protected var cond: Cond = _
  protected var on: JoinCond = _
  protected var order: (String, Array[String]) = _
  protected var limit: Int = -1
  protected var offset: Int = -1

  def newInstance(meta: EntityMeta, alias: String, parent: SelectorBase = null): SelectorBase

  def select(field: String): SelectorBase = {
    if (!meta.fieldMap.contains(field)) {
      throw new RuntimeException(s"[$field] Not Field Of [${meta.entity}]")
    }
    val fieldMeta = meta.fieldMap(field)
    if (fieldMeta.isNormalOrPkey) {
      throw new RuntimeException(s"$field Is Not Refer")
    }
    withs.find(_._1 == field) match {
      case None =>
        val selector = newInstance(fieldMeta.refer, s"${this.alias}_$field", this)
        this.withs += ((field, selector))
        selector
      case Some(p) => p._2
    }
  }

  def where(cond: Cond): SelectorBase = {
    cond.check(meta)
    this.cond = cond
    this
  }

  def asc(fields: Array[String]): SelectorBase = {
    if (!fields.forall(meta.fieldMap.contains(_))) {
      throw new RuntimeException(s"Field Not Match When Call Asc On ${meta.entity}")
    }
    if (parent != null) {
      throw new RuntimeException("OrderBy/Limit/Offset Only Call On Root Selector")
    }
    order = ("ASC", fields)
    this
  }

  def desc(fields: Array[String]): SelectorBase = {
    if (!fields.forall(meta.fieldMap.contains(_))) {
      throw new RuntimeException(s"Field Not Match When Call Asc On ${meta.entity}")
    }
    if (parent != null) {
      throw new RuntimeException("OrderBy/Limit/Offset Only Call On Root Selector")
    }
    order = ("DESC", fields)
    this
  }

  def asc(field: String): SelectorBase = {
    asc(Array(field))
  }

  def desc(field: String): SelectorBase = {
    desc(Array(field))
  }

  def limit(l: Int): SelectorBase = {
    if (parent != null) {
      throw new RuntimeException("OrderBy/Limit/Offset Only Call On Root Selector")
    }
    limit = l
    this
  }

  def offset(o: Int): SelectorBase = {
    if (parent != null) {
      throw new RuntimeException("OrderBy/Limit/Offset Only Call On Root Selector")
    }
    offset = o
    this
  }

  def getColumns: String = {
    val selfColumns = fields.map(meta.fieldMap(_)).map(field => {
      s"$alias.${field.column} AS $alias$$${field.name}"
    }).mkString(",\n\t")
    val withColumns = withs.map { case (_, selector) =>
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
    val orderBySql = if (order != null) {
      val fields = order._2.map(field => s"$alias$$$field").mkString(", ")
      s" ORDER By $fields ${order._1}"
    } else {
      ""
    }
    val limitSql = if (limit != -1) {
      s" LIMIT $limit"
    } else {
      ""
    }
    val offsetSql = if (offset != -1) {
      s" OFFSET $offset"
    } else {
      ""
    }
    s"SELECT\n\t$columns\nFROM\n\t$tables\nWHERE\n\t$cond$orderBySql$limitSql$offsetSql"
  }


}
