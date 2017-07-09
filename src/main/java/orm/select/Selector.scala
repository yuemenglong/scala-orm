package orm.select

import java.sql.{Connection, ResultSet}

import orm.entity.{EntityCore, EntityManager}
import orm.kit.Kit
import orm.lang.interfaces.Entity
import orm.meta.{EntityMeta, FieldMeta, OrmMeta}
import orm.operate.{Cond, EntityItem}

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

// select column from table where cond. param
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

  def get[T](field: String): EntitySelector[T] = {
    val flag = false
    findExists(field) match {
      case Some(t) =>
        if (t._2 != flag) {
          throw new RuntimeException(s"Already Join $field")
        } else {
          t._3.asInstanceOf[EntitySelector[T]]
        }
      case None =>
        val fieldMeta = meta.fieldMap(field)
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

  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, EntityCore]): EntityCore = {
    val core = pickSelf(resultSet, filterMap)
    if (core == null) {
      return null
    }
    pickRefer(core, resultSet, filterMap)
    core
  }

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, EntityCore]): EntityCore = {
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
    filterMap += (key -> core)
    core
  }

  def pickRefer(a: EntityCore, resultSet: ResultSet, filterMap: mutable.Map[String, EntityCore]) {
    joins.filter(_._2).foreach { case (field, _, subSelector) =>
      val fieldMeta = meta.fieldMap(field)
      val bCore = subSelector.pick(resultSet, filterMap)
      (bCore, fieldMeta.isOneMany) match {
        case (null, false) => a.fieldMap += (field -> null)
        case (null, true) => a.fieldMap += (field -> new ArrayBuffer[Object]())
        case (_, false) => a.fieldMap += (field -> EntityManager.wrap(bCore))
        case (_, true) =>
          val b = EntityManager.wrap(bCore)
          val key = getOneManyFilterKey(field, bCore)
          if (filterMap.contains(key)) {
            //
          } else if (!a.fieldMap.contains(field)) {
            a.fieldMap += (field -> new ArrayBuffer[Object]())
            a.fieldMap(field).asInstanceOf[ArrayBuffer[Object]] += b
          } else {
            a.fieldMap(field).asInstanceOf[ArrayBuffer[Object]] += b
          }
      }
    }
  }
}

trait TargetSelector[T] extends SelectorNode {
  def pick(resultSet: ResultSet): T

  def key(value: T): String
}

class EntitySelector[T](override val meta: EntityMeta, override val joinField: FieldMeta, override val parent: SelectorImpl)
  extends SelectorImpl(meta, null, null)
    with TargetSelector[T] {

  override def setTarget(value: Boolean): Unit = {
    super.setTarget(value)
    joins.foreach(_._3.setTarget(value))
  }

  private val filterMap = mutable.Map[String, EntityCore]()

  override def pick(resultSet: ResultSet): T = {
    val core = pick(resultSet, filterMap)
    if (core == null) {
      null.asInstanceOf[T]
    } else {
      EntityManager.wrap(core).asInstanceOf[T]
    }
  }

  override def key(obj: T): String = {
    EntityManager.core(obj.asInstanceOf[Object]).getPkey.toString
  }
}


class RootSelector[T](meta: EntityMeta)
  extends EntitySelector[T](meta, null, null) {

  def getSql: String = {
    val columns = getColumn.mkString(",\n")
    val tables = getTable.mkString("\n")
    val conds = getCond.mkString(" \nAND ") match {
      case "" => "1 = 1"
      case s => s
    }
    s"SELECT\n$columns\nFROM\n$tables\nWHERE\n$conds"
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

  override def key(value: T): String = {
    value.toString
  }
}

object Selector {

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

  def query[T](selector: TargetSelector[T], conn: Connection): Array[T] = {
    selector.setTarget(true)
    var filterSet = Set[String]()
    val root = selector.root
    val sql = root.getSql
    println(sql)
    val params = root.getParam
    println(s"""[Params] => [${params.map(_.toString).mkString(", ")}]""")
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    val rs = stmt.executeQuery()
    var ab = ArrayBuffer[T]()
    while (rs.next()) {
      val value = selector.pick(rs)
      //      val core = EntityManager.core(entity.asInstanceOf[Object])
      //      val key = core.getPkey.toString
      val key = selector.key(value)
      if (!filterSet.contains(key)) {
        ab += value
      }
      filterSet += key
    }
    rs.close()
    stmt.close()
    selector match {
      case es: EntitySelector[T] => bufferToArray(ab.asInstanceOf[ArrayBuffer[Object]],
        ClassTag(es.meta.clazz)).asInstanceOf[Array[T]]
      case fs: FieldSelector[T] => ab.toArray(ClassTag(fs.clazz))
    }
  }
}

