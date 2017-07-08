package orm.operate

import java.sql.{Connection, ResultSet}

import orm.entity.{EntityCore, EntityManager}
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
    val columnSql = super.getColumns

    (aggreSql.isEmpty, columnSql.isEmpty) match {
      case (true, true) => ""
      case (true, false) => columnSql
      case (false, true) => aggreSql
      case (true, true) => s"$aggreSql\n$columnSql"
    }
  }

  override def select(field: String): MultiSelector = {
    super.select(field).asInstanceOf[MultiSelector]
  }

  override def newInstance(meta: EntityMeta, alias: String, parent: SelectorBase): SelectorBase = {
    new MultiSelector(meta, alias, parent)
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

  def query(conn: Connection): Array[Array[Object]] = {
    val sql = this.getSql
    println(sql)
    val params = this.getParams.map(_.toString()).mkString(", ")
    println(s"[Params] => [$params]")
    val stmt = conn.prepareStatement(sql)
    this.getParams.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    val rs = stmt.executeQuery()
    var ab = ArrayBuffer[Array[Object]]()
    while (rs.next()) {
      ab += this.pickRow(rs)
    }
    rs.close()
    stmt.close()
    ab.toArray
  }

  def pickRow(rs: ResultSet): Array[Object] = {
    multiItems.map {
      case EntityItem(selector) => selector.pickEntity(rs)
      case FieldItem(alias) => rs.getObject(alias)
    }.toArray
  }

  def pickEntity(rs: ResultSet): Object = {
    val core = new EntityCore(meta, Map())
    meta.managedFieldVec().filter(_.isNormalOrPkey).foreach(field => {
      val label = s"$alias$$${field.name}"
      val value = rs.getObject(label)
      core.fieldMap += (field.name -> value)
    })
    EntityManager.wrap(core)
  }
}

