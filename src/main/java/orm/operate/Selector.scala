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
// select column from table where cond. param
trait SelectorTrait {
  def root: Root[_]
}

trait Target[T] extends SelectorTrait {
  def getColumn: Array[String]

  def pick(resultSet: ResultSet): T

  def key(value: Object): String = value.toString

  def classT(): Class[T]
}

abstract class Selector(parent: JoinImpl) extends SelectorTrait {
  def root: Root[_] = {
    if (parent == null) {
      this.asInstanceOf[Root[_]]
    } else {
      parent.root
    }
  }
}

// entity -------------------------------------------------------

class JoinImpl(val meta: EntityMeta, val joinField: FieldMeta, val parent: JoinImpl)
  extends Selector(parent) {
  // Boolean 表示是否关联查询，即是否为select
  protected var fields: Array[String] = meta.managedFieldVec().filter(_.isNormalOrPkey).map(_.name).toArray
  protected var joins: ArrayBuffer[(String, Boolean, JoinImpl)] = new ArrayBuffer[(String, Boolean, JoinImpl)]()
  protected var targets: ArrayBuffer[(String, Target[Object])] = new ArrayBuffer[(String, Target[Object])]()

  val alias: String = getEntityAlias

  def getEntityAlias: String = {
    if (parent == null) {
      Kit.lodashCase(meta.entity)
    } else {
      s"${parent.alias}_${joinField.name}"
    }
  }

  def getFieldColumn(field: String): String = {
    s"$alias.${meta.fieldMap(field).column}"
  }

  def getFieldAlias(field: String): String = {
    s"$alias$$$field"
  }

  def setFields(fields: Array[String]): Unit = {
    this.fields = fields
  }

  private def findExists(field: String): Option[(String, Boolean, JoinImpl)] = {
    if (!meta.fieldMap.contains(field) || !meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Join Non Object Field, $field")
    }
    joins.find(_._1 == field)
  }

  def select(field: String): JoinImpl = {
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
        val selector = new JoinImpl(fieldMeta.refer, fieldMeta, this)
        joins += ((field, flag, selector))
        selector
    }
  }

  def join[T](field: String, clazz: Class[T]): Join[T] = {
    val flag = false
    findExists(field) match {
      case Some(t) =>
        if (t._2 != flag) {
          throw new RuntimeException(s"Already Join $field")
        } else {
          if (t._3.meta.clazz != clazz) {
            throw new RuntimeException("Class Not Match")
          }
          t._3.asInstanceOf[Join[T]]
        }
      case None =>
        val fieldMeta = meta.fieldMap(field)
        if (fieldMeta.refer.clazz != clazz) {
          throw new RuntimeException("Class Not Match")
        }
        val selector = new Join[T](fieldMeta.refer, fieldMeta, this)
        joins += ((field, flag, selector))
        selector
    }
  }

  def join(field: String): JoinImpl = {
    if (!meta.fieldMap.contains(field)) {
      throw new RuntimeException(s"Unknown Field $field For ${meta.entity}")
    }
    join(field, meta.fieldMap(field).refer.clazz)
  }

  def get[T](field: String, clazz: Class[T]): Field[T] = {
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"No Normal Field $field In ${meta.entity}")
    }
    val fieldMeta = meta.fieldMap(field)
    if (clazz != fieldMeta.field.getType) {
      throw new RuntimeException("Class Not Match")
    }
    targets.find(_._1 == field) match {
      case Some((_, fs)) => fs.asInstanceOf[Field[T]]
      case None =>
        val ret = new Field[T](clazz, field, this)
        targets += ((field, ret.asInstanceOf[Field[Object]]))
        ret
    }
  }

  def get(field: String): FieldImpl = {
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"No Normal Field $field In ${meta.entity}")
    }
    val clazz = meta.fieldMap(field).field.getType
    get(field, clazz)
  }

  def getTable: Array[String] = {
    val selfTable = if (parent == null) {
      s"${meta.table} AS $alias"
    } else {
      val left = parent.meta.fieldMap(joinField.left).column
      val right = meta.fieldMap(joinField.right).column
      s"LEFT JOIN ${meta.table} AS $alias ON ${parent.alias}.$left = $alias.$right"
    }
    Array(selfTable) ++ joins.flatMap(_._3.getTable)
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

class Join[T](meta: EntityMeta, joinField: FieldMeta, parent: JoinImpl)
  extends JoinImpl(meta, null, parent)
    with Target[T] {

  private val filterMap = mutable.Map[String, Entity]()

  override def getColumn: Array[String] = {
    def go(entitySelector: JoinImpl): Array[String] = {
      val selfColumn = fields.map(field => s"${getFieldColumn(field)} AS ${getFieldAlias(field)}")
      // 1. 属于自己的字段 2. 关联的字段（聚合） 3. 级联的部分
      selfColumn ++ targets.flatMap(_._2.getColumn) ++ joins.flatMap(t => go(t._3))
    }

    go(this)
  }

  override def pick(resultSet: ResultSet): T = {
    val a = pick(resultSet, filterMap)
    if (a == null) {
      null.asInstanceOf[T]
    } else {
      a.asInstanceOf[T]
    }
  }

  override def key(obj: Object): String = {
    if (obj == null) {
      ""
    } else {
      EntityManager.core(obj.asInstanceOf[Object]).getPkey.toString
    }
  }

  override def classT(): Class[T] = meta.clazz.asInstanceOf[Class[T]]
}

class Root[T](meta: EntityMeta)
  extends Join[T](meta, null, null) {

  private var cond: Cond = _
  private var order: (String, Array[String]) = _
  private var limit: Int = -1
  private var offset: Int = -1

  def where(c: Cond): Root[T] = {
    cond = c
    this
  }

  def count[R](field: FieldImpl, clazz: Class[R]): Count[R] = Count(clazz, field, this)

  def count[R](clazz: Class[R]): Count_[R] = Count_(clazz, this)

  def asc(fields: Array[String]): Root[T] = {
    if (!fields.forall(meta.fieldMap.contains(_))) {
      throw new RuntimeException(s"Field Not Match When Call Asc On ${meta.entity}")
    }
    order = ("ASC", fields)
    this
  }

  def desc(fields: Array[String]): Root[T] = {
    if (!fields.forall(meta.fieldMap.contains(_))) {
      throw new RuntimeException(s"Field Not Match When Call Asc On ${meta.entity}")
    }
    order = ("DESC", fields)
    this
  }

  def asc(field: String): Root[T] = {
    asc(Array(field))
  }

  def desc(field: String): Root[T] = {
    desc(Array(field))
  }

  def limit(l: Int): Root[T] = {
    limit = l
    this
  }

  def offset(o: Int): Root[T] = {
    offset = o
    this
  }

  def getParam: Array[Object] = {
    cond match {
      case null => Array()
      case _ => cond.toParam
    }
  }

  def getSql(targets: Array[Target[_]]): String = {
    val columns = targets.flatMap(_.getColumn).mkString(",\n")
    val tables = getTable.mkString("\n")
    val conds = cond match {
      case null => "1 = 1"
      case c: Cond => c.toSql match {
        case "" => "1=1"
        case sql: String => sql
      }
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

// field -------------------------------------------------------

class FieldImpl(val clazz: Class[_], field: String, parent: JoinImpl)
  extends Selector(parent)
    with FieldOp {

  val column: String = parent.getFieldColumn(field)
  val alias: String = parent.getFieldAlias(field)

  // cond

  override def eql(v: Object): Cond = EqFV(this, v)

  override def eql(f: FieldImpl): Cond = EqFF(this, f)

  override def in(a: Array[Object]): Cond = InFA(this, a)
}

class Field[T](clazz: Class[T], field: String, parent: JoinImpl)
  extends FieldImpl(clazz, field, parent)
    with Target[T] {

  override def getColumn: Array[String] = {
    Array(s"$column AS $alias")
  }

  override def pick(resultSet: ResultSet): T = {
    resultSet.getObject(alias).asInstanceOf[T]
  }

  override def classT(): Class[T] = clazz
}

// aggre -------------------------------------------------------

case class Count[T](clazz: Class[T], field: FieldImpl, parent: JoinImpl)
  extends Selector(parent)
    with Target[T] {

  val column: String = s"COUNT(DISTINCT ${field.column})"
  val alias: String = s"count$$${field.alias}"

  override def getColumn: Array[String] = {
    Array(column)
  }

  override def pick(resultSet: ResultSet): T = {
    resultSet.getObject(alias).asInstanceOf[T]
  }

  override def classT(): Class[T] = clazz
}

case class Count_[T](clazz: Class[T], parent: JoinImpl)
  extends Selector(parent)
    with Target[T] {

  val column: String = s"COUNT(*)"
  val alias: String = s"count$$${parent.alias}"

  override def getColumn: Array[String] = {
    Array(column)
  }

  override def pick(resultSet: ResultSet): T = {
    resultSet.getObject(alias).asInstanceOf[T]
  }

  override def classT(): Class[T] = clazz
}

// static -------------------------------------------------------

object Selector {
  def createSelect[T](clazz: Class[T]): Root[T] = {
    val meta = OrmMeta.entityMap(clazz.getSimpleName)
    if (meta.clazz != clazz) {
      throw new RuntimeException("Class Not Match")
    }
    new Root[T](meta)
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

  def query[T](selector: Target[T], conn: Connection): Array[T] = {
    val ct: ClassTag[T] = selector match {
      case es: Join[_] => ClassTag(es.meta.clazz)
      case fs: Target[_] => ClassTag(fs.classT())
    }
    query(Array[Target[_]](selector), conn).map(row => row(0).asInstanceOf[T]).toArray(ct)
  }

  def query[T0, T1](s1: Target[T0], s2: Target[T1], conn: Connection): Array[(T0, T1)] = {
    val selectors = Array[Target[_]](s1, s2)
    query(selectors, conn).map(row => {
      (row(0).asInstanceOf[T0], row(1).asInstanceOf[T1])
    })
  }

  def query(selectors: Array[Target[_]], conn: Connection): Array[Array[Object]] = {
    if (selectors.length == 0) {
      throw new RuntimeException("No Selector")
    }
    var filterSet = Set[String]()
    val roots = selectors.map(_.root)
    if (roots.exists(_ != roots(0))) {
      throw new RuntimeException("Root Not Match")
    }
    val root = roots(0)
    val sql = root.getSql(selectors)
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

