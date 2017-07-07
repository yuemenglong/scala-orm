package orm.operate

import orm.meta.EntityMeta

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/7/7.
  */

class MultiItem

case class EntityItem(selector: MultiSelector) extends MultiItem

case class FieldItem(alias: String) extends MultiItem

class MultiSelector(meta: EntityMeta, alias: String, parent: SelectorBase = null) extends SelectorBase(meta, alias, parent) {
  private var aggres = ArrayBuffer[(String, String, String)]()
  private var multiItems = ArrayBuffer[MultiItem]()

  override def getColumns: String = {
    val aggreSql = aggres.map(_._2).mkString(",\n")
    if (aggreSql.nonEmpty) {
      s"$aggreSql\n${super.getColumns}"
    } else {
      super.getColumns
    }
  }

  override def select(field: String): MultiSelector = {
    super.select(field).asInstanceOf[MultiSelector]
  }

  private def getRoot: MultiSelector = {
    if (parent != null) {
      parent.asInstanceOf[MultiSelector].getRoot
    } else {
      this
    }
  }

  def count(): Unit = {
    val fieldAlias = s"$$count_$alias"
    val sql = s"COUNT(*) AS $fieldAlias"
    aggres += (("*", sql, fieldAlias))
    getRoot.multiItems += FieldItem(fieldAlias)
  }

  def count(field: String = "*"): Unit = {
    if (!meta.fieldMap.contains(field)) {
      throw new RuntimeException()
    }
    val fieldAlias = s"$$count_${alias}_$field"
    val sql = s"COUNT(DISTINCT $alias.$field) AS $fieldAlias"
    aggres += ((field, sql, fieldAlias))
    getRoot.multiItems += FieldItem(fieldAlias)
  }

  override def newInstance(meta: EntityMeta, alias: String, parent: SelectorBase): SelectorBase = {
    new MultiSelector(meta, alias, parent)
  }
}

