package orm.operate

import java.sql.{Connection, ResultSet}

import orm.entity.{EntityCore, EntityManager}
import orm.kit.Kit
import orm.lang.interfaces.Entity
import orm.meta.{EntityMeta, FieldMeta, OrmMeta}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by yml on 2017/7/9.
  */
trait SelectorNode {
  def root: RootSelector[_]

  def setTarget(value: Boolean)
}

abstract class Selector(parent: SelectorImpl) extends SelectorNode {
  protected var isTarget = false

  def getColumn: Array[String]

  def getTable: Array[String]

  def getCond: Array[String]

  def getParam: Array[Object]

  def setTarget(value: Boolean): Unit = {
    isTarget = value
  }

  def root: RootSelector[_] = {
    if (parent == null) {
      this.asInstanceOf[RootSelector[_]]
    } else {
      parent.root
    }
  }
}

// select column from table where cond. param
class SelectorImpl(val meta: EntityMeta, val joinField: FieldMeta, val parent: SelectorImpl)
  extends Selector(parent) {
  protected var joins: ArrayBuffer[(String, Boolean, SelectorImpl)] = new ArrayBuffer[(String, Boolean, SelectorImpl)]()
  protected var fields: Array[String] = meta.managedFieldVec().filter(_.isNormalOrPkey).map(_.name).toArray
  protected var aggres: ArrayBuffer[(String, FieldSelector[Object])] = new ArrayBuffer[(String, FieldSelector[Object])]()

  protected val alias: String = getAlias
  protected val cond: Cond = new Cond

  def getAlias: String = {
    if (parent == null) {
      Kit.lowerCaseFirst(meta.entity)
    } else {
      s"${parent.alias}_${joinField.name}"
    }
  }

  def getFieldAlias(field: String): String = {
    s"$alias$$$field"
  }

  def setFields(fields: Array[String]): Unit = {
    this.fields = fields
  }

  private def findExists(field: String): Option[(String, Boolean, SelectorImpl)] = {
    if (!meta.fieldMap.contains(field) || !meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Join Non Object Field, $field")
    }
    joins.find(_._1 == field)
  }

  def select(field: String): SelectorImpl = {
    val flag = true
    findExists(field) match {
      case Some(t) =>
        if (t._2 != flag) {
          throw new RuntimeException(s"Already Get $field")
        } else {
          t._3
        }
      case None =>
        val fieldMeta = meta.fieldMap(field)
        val selector = new SelectorImpl(fieldMeta.refer, fieldMeta, this)
        joins += ((field, flag, selector))
        selector
    }
  }

  def get[T](field: String, clazz: Class[T]): EntitySelector[T] = {
    val flag = false
    findExists(field) match {
      case Some(t) =>
        if (t._2 != flag) {
          throw new RuntimeException(s"Already Join $field")
        } else {
          if (t._3.meta.clazz != clazz) {
            throw new RuntimeException("Class Not Match")
          }
          t._3.asInstanceOf[EntitySelector[T]]
        }
      case None =>
        val fieldMeta = meta.fieldMap(field)
        if (fieldMeta.refer.clazz != clazz) {
          throw new RuntimeException("Class Not Match")
        }
        val selector = new EntitySelector[T](fieldMeta.refer, fieldMeta, this)
        joins += ((field, flag, selector))
        selector
    }
  }

  def where(): Cond = {
    cond
  }

  def count[T](clazz: Class[T]): FieldSelector[T] = {
    val fieldAlias = s"$$count_$alias"
    val sql = s"COUNT(*) AS $fieldAlias"
    val ret = new FieldSelector[T](clazz, sql, fieldAlias, this)
    aggres += ((fieldAlias, ret.asInstanceOf[FieldSelector[Object]]))
    ret
  }

  def count[T](field: String, clazz: Class[T]): FieldSelector[T] = {
    val fieldAlias = s"$$count_$alias$$$field"
    val sql = s"COUNT(*) AS $fieldAlias"
    val ret = new FieldSelector[T](clazz, sql, fieldAlias, this)
    aggres += ((fieldAlias, ret.asInstanceOf[FieldSelector[Object]]))
    ret
  }

  override def getColumn: Array[String] = {
    val selfColumn = if (isTarget) {
      fields.map(meta.fieldMap(_))
        .map(field => s"$alias.${field.column} AS ${getFieldAlias(field.name)}")
    } else {
      Array[String]()
    }
    selfColumn ++ aggres.flatMap(_._2.getColumn) ++ joins.flatMap(_._3.getColumn)
  }

  override def getTable: Array[String] = {
    val selfTable = if (parent == null) {
      s"${meta.table} AS $alias"
    } else {
      val left = parent.meta.fieldMap(joinField.left).column
      val right = meta.fieldMap(joinField.right).column
      s"LEFT JOIN ${meta.table} AS $alias ON ${parent.alias}.$left = $alias.$right"
    }
    Array(selfTable) ++ joins.flatMap(_._3.getTable)
  }

  override def getCond: Array[String] = {
    val selfCond = cond.toSql(alias, meta)
    (Array(selfCond) ++ joins.flatMap(_._3.getCond)).filter(_ != null)
  }

  override def getParam: Array[Object] = {
    val selfParam = cond.toParams
    selfParam ++ joins.flatMap(_._3.getParam)
  }


  def getFilterKey(core: EntityCore): String = {
    s"$alias@${core.getPkey.toString}"
  }

  def getOneManyFilterKey(field: String, core: EntityCore): String = {
    s"$alias@$field@${core.getPkey.toString}"
  }

  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val a = pickSelf(resultSet, filterMap)
    if (a == null) {
      return null
    }
    pickRefer(a, resultSet, filterMap)
    a
  }

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val map: Map[String, Object] = fields.map(field => {
      val alias = getFieldAlias(field)
      val value = resultSet.getObject(alias)
      (field, value)
    })(collection.breakOut)
    val core = new EntityCore(meta, map)
    if (core.getPkey == null) {
      return null
    }
    val key = getFilterKey(core)
    if (filterMap.contains(key)) {
      return filterMap(key)
    }
    val a = EntityManager.wrap(core)
    filterMap += (key -> a)
    a
  }

  def pickRefer(a: Object, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]) {
    val aCore = EntityManager.core(a)
    joins.filter(_._2).foreach { case (field, _, subSelector) =>
      val fieldMeta = meta.fieldMap(field)
      val b = subSelector.pick(resultSet, filterMap)
      (b, fieldMeta.isOneMany) match {
        case (null, false) => aCore.fieldMap += (field -> null)
        case (null, true) => aCore.fieldMap += (field -> new ArrayBuffer[Entity]())
        case (_, false) => aCore.fieldMap += (field -> b)
        case (_, true) =>
          val key = getOneManyFilterKey(field, b.$$core())
          if (filterMap.contains(key)) {
            //
          } else if (!aCore.fieldMap.contains(field)) {
            aCore.fieldMap += (field -> new ArrayBuffer[Entity]())
            aCore.fieldMap(field).asInstanceOf[ArrayBuffer[Entity]] += b
          } else {
            aCore.fieldMap(field).asInstanceOf[ArrayBuffer[Entity]] += b
          }
      }
    }
  }
}

trait TargetSelector[T] extends SelectorNode {
  def pick(resultSet: ResultSet): T

  def key(value: Object): String
}

class EntitySelector[T](override val meta: EntityMeta, override val joinField: FieldMeta, override val parent: SelectorImpl)
  extends SelectorImpl(meta, null, parent)
    with TargetSelector[T] {

  override def setTarget(value: Boolean): Unit = {
    super.setTarget(value)
    joins.foreach(_._3.setTarget(value))
  }

  private val filterMap = mutable.Map[String, Entity]()

  override def pick(resultSet: ResultSet): T = {
    val a = pick(resultSet, filterMap)
    if (a == null) {
      null.asInstanceOf[T]
    } else {
      a.asInstanceOf[T]
    }
  }

  override def key(obj: Object): String = {
    EntityManager.core(obj.asInstanceOf[Object]).getPkey.toString
  }
}


class RootSelector[T](meta: EntityMeta)
  extends EntitySelector[T](meta, null, null) {
  protected var order: (String, Array[String]) = _
  protected var limit: Int = -1
  protected var offset: Int = -1

  def asc(fields: Array[String]): RootSelector[T] = {
    if (!fields.forall(meta.fieldMap.contains(_))) {
      throw new RuntimeException(s"Field Not Match When Call Asc On ${meta.entity}")
    }
    order = ("ASC", fields)
    this
  }

  def desc(fields: Array[String]): RootSelector[T] = {
    if (!fields.forall(meta.fieldMap.contains(_))) {
      throw new RuntimeException(s"Field Not Match When Call Asc On ${meta.entity}")
    }
    order = ("DESC", fields)
    this
  }

  def asc(field: String): RootSelector[T] = {
    asc(Array(field))
  }

  def desc(field: String): RootSelector[T] = {
    desc(Array(field))
  }

  def limit(l: Int): RootSelector[T] = {
    limit = l
    this
  }

  def offset(o: Int): RootSelector[T] = {
    offset = o
    this
  }

  def getSql: String = {
    val columns = getColumn.mkString(",\n")
    val tables = getTable.mkString("\n")
    val conds = getCond.mkString(" \nAND ") match {
      case "" => "1 = 1"
      case s => s
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
    s"SELECT\n$columns\nFROM\n$tables\nWHERE\n$conds$orderBySql$limitSql$offsetSql"
  }

}

class FieldSelector[T](val clazz: Class[T], sql: String, alias: String, parent: SelectorImpl)
  extends Selector(parent)
    with TargetSelector[T] {
  override def getColumn: Array[String] = {
    if (isTarget) {
      Array(sql)
    } else {
      Array()
    }
  }

  override def getTable: Array[String] = {
    Array()
  }

  override def getCond: Array[String] = {
    Array()
  }

  override def getParam: Array[Object] = {
    Array()
  }

  override def pick(resultSet: ResultSet): T = {
    resultSet.getObject(alias).asInstanceOf[T]
  }

  override def key(value: Object): String = {
    value.toString
  }
}

object Selector {

  def createSelect[T](clazz: Class[T]): RootSelector[T] = {
    val meta = OrmMeta.entityMap(clazz.getSimpleName)
    if (meta.clazz != clazz) {
      throw new RuntimeException("Class Not Match")
    }
    new RootSelector[T](meta)
  }

  private def bufferToArray(entity: Entity): Entity = {
    val core = entity.$$core()
    core.fieldMap.toArray.map(pair => {
      val (name, value) = pair
      value match {
        case ab: ArrayBuffer[_] =>
          val entityName = core.meta.fieldMap(name).typeName
          val entityClass = OrmMeta.entityMap(entityName).clazz
          val ct = ClassTag(entityClass).asInstanceOf[ClassTag[Object]]
          val array = ab.map(_.asInstanceOf[Entity]).map(bufferToArray).toArray(ct)
          (name, array)
        case _ =>
          null
      }
    }).filter(_ != null).foreach(p => core.fieldMap += p)
    entity
  }

  def query[T](selector: TargetSelector[T], conn: Connection): Array[T] = {
    val ct: ClassTag[T] = selector match {
      case es: EntitySelector[_] => ClassTag(es.meta.clazz)
      case fs: FieldSelector[_] => ClassTag(fs.clazz)
    }
    query(Array[TargetSelector[_]](selector), conn).map(row => row(0).asInstanceOf[T]).toArray(ct)
  }

  def query[T0, T1](s1: TargetSelector[T0], s2: TargetSelector[T1], conn: Connection): Array[(T0, T1)] = {
    val selectors = Array[TargetSelector[_]](s1, s2)
    query(selectors, conn).map(row => {
      (row(0).asInstanceOf[T0], row(1).asInstanceOf[T1])
    })
  }

  def query(selectors: Array[TargetSelector[_]], conn: Connection): Array[Array[Object]] = {
    if (selectors.length == 0) {
      throw new RuntimeException("No Selector")
    }
    selectors.foreach(_.setTarget(true))
    var filterSet = Set[String]()
    val roots = selectors.map(_.root)
    if (roots.exists(_ != roots(0))) {
      throw new RuntimeException("Root Not Match")
    }
    val root = roots(0)
    val sql = root.getSql
    println(sql)
    val params = root.getParam
    println(s"""[Params] => [${params.map(_.toString).mkString(", ")}]""")
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    val rs = stmt.executeQuery()
    var ab = ArrayBuffer[Array[Object]]()
    while (rs.next()) {
      val values = selectors.map(_.pick(rs).asInstanceOf[Object])
      val key = (selectors, values).zipped.map(_.key(_)).mkString("$")
      if (!filterSet.contains(key)) {
        ab += values
      }
      filterSet += key
    }
    rs.close()
    stmt.close()
    ab.foreach(_.filter(_.isInstanceOf[Entity]).map(_.asInstanceOf[Entity]).foreach(bufferToArray))
    ab.toArray
  }
}

