package orm.operate.impl

import java.sql.Connection

import orm.lang.interfaces.Entity
import orm.meta.OrmMeta
import orm.operate.traits.core._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */
class SelectBuilderImpl(targets: Array[Selectable[_]]) extends SelectBuilder {
  override def from(root: SelectRoot[_]): Query = new QueryImpl(targets, root)
}

class SelectBuilder1Impl[T](target: Selectable[T]) extends SelectBuilder1[T] {
  override def from(root: SelectRoot[_]): Query1[T] = new Query1Impl(target, root)
}

class QueryableImpl(targets: Array[Selectable[_]], root: SelectRoot[_]) extends Queryable {
  protected var cond: Cond = new CondRoot()
  protected var limitVar: Long = -1
  protected var offsetVar: Long = -1
  protected var orders: ArrayBuffer[(Field, String)] = new ArrayBuffer[(Field, String)]()

  def getParams: Array[Object] = {
    val loParams = (limitVar, offsetVar) match {
      case (-1, -1) => Array()
      case (l, -1) => Array[Object](new java.lang.Long(l))
      case (-1, o) => Array[Object](new java.lang.Long(o))
      case (l, o) => Array[Object](new java.lang.Long(l), new java.lang.Long(o))
    }
    root.getParams ++ cond.getParams ++ loParams
  }

  def getSql: String = {
    val columnsSql = targets.map(_.getColumnWithAs).mkString(",\n")
    val tableSql = root.getTableWithJoinCond
    val condSql = cond.getSql match {
      case "" => "1 = 1"
      case s => s
    }
    val orderBySql = orders.length match {
      case 0 => ""
      case _ =>
        val content = orders.map { case (f, s) => s"${f.getColumn} $s" }.mkString(", ")
        s"\nORDER BY $content"
    }
    val loSql = (limitVar, offsetVar) match {
      case (-1, -1) => ""
      case (_, -1) => "\nLIMIT ?"
      case (-1, _) => "\nOFFSET ?"
      case (_, _) => "\nLIMIT ? OFFSET ?"
    }
    s"SELECT\n$columnsSql\nFROM\n$tableSql\nWHERE\n$condSql$orderBySql$loSql"
  }

  override def query(conn: Connection): Array[Array[Object]] = {
    if (targets.length == 0) {
      throw new RuntimeException("No Selector")
    }
    if (targets.map(_.getRoot.asInstanceOf[SelectRoot[_]]).exists(_ != root)) {
      throw new RuntimeException("Root Not Match")
    }
    var filterSet = Set[String]()
    val sql = getSql
    println(sql)
    val params = getParams
    println(s"""[Params] => [${params.map(_.toString).mkString(", ")}]""")
    val stmt = conn.prepareStatement(sql)
    params.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    val rs = stmt.executeQuery()
    var ab = ArrayBuffer[Array[Object]]()
    val filterMap = mutable.Map[String, Entity]()
    while (rs.next()) {
      val values = targets.map(_.pick(rs, filterMap).asInstanceOf[Object])
      val key = (targets, values).zipped.map(_.getKey(_)).mkString("$")
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
}

class Query1Impl[T](val t: Selectable[T], root: SelectRoot[_])
  extends QueryableImpl(Array(t), root) with Query1[T] {
  def transform(res: Array[Array[Object]]): Array[T] = {
    val ct = ClassTag(t.getType)
    res.map(r => r(0).asInstanceOf[T]).toArray(ct.asInstanceOf[ClassTag[T]])
  }

  override def limit(l: Long): Query1[T] = {
    limitVar = l
    this
  }

  override def offset(l: Long): Query1[T] = {
    offsetVar = l
    this
  }

  override def asc(field: Field): Query1[T] = {
    orders += ((field, "ASC"))
    this
  }

  override def desc(field: Field): Query1[T] = {
    orders += ((field, "DESC"))
    this
  }

  override def where(c: Cond): Query1[T] = {
    cond = c
    this
  }
}

class QueryImpl(ts: Array[Selectable[_]], root: SelectRoot[_])
  extends QueryableImpl(ts, root) with Query {

  override def limit(l: Long): Query = {
    limitVar = l
    this
  }

  override def offset(l: Long): Query = {
    offsetVar = l
    this
  }

  override def asc(field: Field): Query = {
    orders += ((field, "ASC"))
    this
  }

  override def desc(field: Field): Query = {
    orders += ((field, "DESC"))
    this
  }

  override def where(c: Cond): Query = {
    cond = c
    this
  }
}
