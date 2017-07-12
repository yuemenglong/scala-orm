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
  def getColumnWithAs: Array[String]

  def pick(resultSet: ResultSet): T

  def key(value: Object): String = value.toString

  def classT(): Class[T]
}

abstract class Selector(parent: Join) extends SelectorTrait {
  def root: Root[_] = {
    if (parent == null) {
      this.asInstanceOf[Root[_]]
    } else {
      parent.root
    }
  }
}

// entity -------------------------------------------------------

class Join(val meta: EntityMeta, val joinField: FieldMeta, val parent: Join)
  extends Selector(parent) {
  // Boolean 表示是否关联查询，即是否为select
  protected var fields: Array[String] = meta.managedFieldVec().filter(_.isNormalOrPkey).map(_.name).toArray
  protected var joins: ArrayBuffer[(String, Boolean, Join)] = new ArrayBuffer[(String, Boolean, Join)]()
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

  private def findExists(field: String): Option[(String, Boolean, Join)] = {
    if (!meta.fieldMap.contains(field) || !meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Join Non Object Field, $field")
    }
    joins.find(_._1 == field)
  }

  def select(field: String): Join = {
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
        val selector = new Join(fieldMeta.refer, fieldMeta, this)
        joins += ((field, flag, selector))
        selector
    }
  }

  def join[T](field: String, clazz: Class[T]): JoinT[T] = {
    val flag = false
    findExists(field) match {
      case Some(t) =>
        if (t._2 != flag) {
          throw new RuntimeException(s"Already Join $field")
        } else {
          if (t._3.meta.clazz != clazz) {
            throw new RuntimeException("Class Not Match")
          }
          t._3.asInstanceOf[JoinT[T]]
        }
      case None =>
        val fieldMeta = meta.fieldMap(field)
        if (fieldMeta.refer.clazz != clazz) {
          throw new RuntimeException("Class Not Match")
        }
        val selector = new JoinT[T](fieldMeta.refer, fieldMeta, this)
        joins += ((field, flag, selector))
        selector
    }
  }

  def join(field: String): Join = {
    if (!meta.fieldMap.contains(field)) {
      throw new RuntimeException(s"Unknown Field $field For ${meta.entity}")
    }
    join(field, meta.fieldMap(field).refer.clazz)
  }

  def get[T](field: String, clazz: Class[T]): FieldT[T] = {
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"No Normal Field $field In ${meta.entity}")
    }
    val fieldMeta = meta.fieldMap(field)
    if (clazz != fieldMeta.clazz) {
      throw new RuntimeException("Class Not Match")
    }
    targets.find(_._1 == field) match {
      case Some((_, fs)) => fs.asInstanceOf[FieldT[T]]
      case None =>
        val ret = new FieldT[T](clazz, field, this)
        targets += ((field, ret.asInstanceOf[FieldT[Object]]))
        ret
    }
  }

  def get(field: String): Field = {
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"No Normal Field $field In ${meta.entity}")
    }
    val clazz = meta.fieldMap(field).clazz
    get(field, clazz)
  }

  def getTableWithJoin: Array[String] = {
    val selfTable = if (parent == null) {
      s"${meta.table} AS $alias"
    } else {
      val left = parent.meta.fieldMap(joinField.left).column
      val right = meta.fieldMap(joinField.right).column
      s"LEFT JOIN ${meta.table} AS $alias ON ${parent.alias}.$left = $alias.$right"
    }
    Array(selfTable) ++ joins.flatMap(_._3.getTableWithJoin)
  }

  def getColumnWithAs: Array[String] = {
    def go(join: Join): Array[String] = {
      val selfColumn = join.fields.map(field => s"${join.getFieldColumn(field)} AS ${join.getFieldAlias(field)}")
      // 1. 属于自己的字段 2. 关联的字段（聚合） 3. 级联的部分
      selfColumn ++ join.targets.flatMap(_._2.getColumnWithAs) ++ join.joins.flatMap(t => go(t._3))
    }

    go(this)
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

class JoinT[T](meta: EntityMeta, joinField: FieldMeta, parent: Join)
  extends Join(meta, joinField, parent)
    with Target[T] {

  private val filterMap = mutable.Map[String, Entity]()

  override def getColumnWithAs: Array[String] = super.getColumnWithAs

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

class Root[T](clazz: Class[T])
  extends JoinT[T](OrmMeta.entityMap(clazz.getSimpleName), null, null) {

  private var cond: Cond = _
  private var orders: ArrayBuffer[(Field, String)] = ArrayBuffer[(Field, String)]()
  private var limit: Int = -1
  private var offset: Int = -1

  def where(c: Cond): Root[T] = {
    cond = c
    this
  }

  def count[R](field: Field, clazz: Class[R]): Count[R] = Count(clazz, field, this)

  def count[R](clazz: Class[R]): Count_[R] = Count_(clazz, this)

  def asc(field: Field): Root[T] = {
    orders += ((field, "ASC"))
    this
  }

  def desc(field: Field): Root[T] = {
    orders += ((field, "DESC"))
    this
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
    val columns = targets.flatMap(_.getColumnWithAs).mkString(",\n")
    val tables = getTableWithJoin.mkString("\n")
    val conds = cond match {
      case null => "1 = 1"
      case c: Cond => c.toSql match {
        case "" => "1=1"
        case sql: String => sql
      }
    }
    val orderBySql = orders.size match {
      case 0 => ""
      case _ => " ORDER BY " + orders.map { case (f, o) => s"${f.column} $o" }.mkString(", ")
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
    val postfix = s"$orderBySql$limitSql$offsetSql" match {
      case "" => ""
      case s => "\n" + s
    }
    s"SELECT\n$columns\nFROM\n$tables\nWHERE\n$conds$postfix"
  }

}

// field -------------------------------------------------------

class Field(val clazz: Class[_], field: String, parent: Join)
  extends Selector(parent)
    with FieldOp {

  val column: String = parent.getFieldColumn(field)
  val alias: String = parent.getFieldAlias(field)

  // cond

  override def eql(v: Object): Cond = EqFV(this, v)

  override def eql(f: Field): Cond = EqFF(this, f)

  override def in(a: Array[Object]): Cond = InFA(this, a)
}

class FieldT[T](clazz: Class[T], field: String, parent: Join)
  extends Field(clazz, field, parent)
    with Target[T] {

  override def getColumnWithAs: Array[String] = {
    Array(s"$column AS $alias")
  }

  override def pick(resultSet: ResultSet): T = {
    resultSet.getObject(alias).asInstanceOf[T]
  }

  override def classT(): Class[T] = clazz
}

// aggre -------------------------------------------------------

case class Count[T](clazz: Class[T], field: Field, parent: Join)
  extends Selector(parent)
    with Target[T] {

  val alias: String = s"count$$${field.alias}"
  val column: String = s"COUNT(DISTINCT ${field.column})"

  override def getColumnWithAs: Array[String] = {
    Array(s"$column AS $alias")
  }

  override def pick(resultSet: ResultSet): T = {
    resultSet.getObject(alias).asInstanceOf[T]
  }

  override def classT(): Class[T] = clazz
}

case class Count_[T](clazz: Class[T], parent: Join)
  extends Selector(parent)
    with Target[T] {

  val column: String = s"COUNT(*)"
  val alias: String = s"count$$"

  override def getColumnWithAs: Array[String] = {
    Array(s"$column AS $alias")
  }

  override def pick(resultSet: ResultSet): T = {
    resultSet.getObject(alias).asInstanceOf[T]
  }

  override def classT(): Class[T] = clazz
}

// static -------------------------------------------------------

object Selector {

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
      case es: JoinT[_] => ClassTag(es.meta.clazz)
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

