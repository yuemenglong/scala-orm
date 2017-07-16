package orm.operate.impl

import java.sql.{Connection, ResultSet}

import orm.lang.interfaces.Entity
import orm.meta.OrmMeta
import orm.operate.traits.core._

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

class Query1Impl[T](val t: Selectable[T], root: SelectRoot[_])
  extends QueryImpl(Array(t), root) with Query1[T] {
  def transform(res: Array[Array[Object]]): Array[T] = {
    val ct = ClassTag(t.getType)
    res.map(r => r(0).asInstanceOf[T]).toArray(ct.asInstanceOf[ClassTag[T]])
  }
}

class QueryImpl(targets: Array[Selectable[_]], root: SelectRoot[_]) extends Query {
  var cond: Cond = new CondRoot()
  var limit: Long = -1
  var offset: Long = -1
  var orders: ArrayBuffer[(Field, String)] = new ArrayBuffer[(Field, String)]()

  override def limit(l: Long): Query = {
    limit = l
    this
  }

  override def offset(l: Long): Query = {
    offset = l
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
    while (rs.next()) {
      val values = targets.map(_.pick(rs).asInstanceOf[Object])
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

  def getParams: Array[Object] = {
    // TODO: order by limit offset
    root.getParams ++ cond.getParams
  }

  def getSql: String = {
    val columns = targets.map(_.getColumnWithAs).mkString(",\n")
    val table = root.getTableWithJoinCond
    val condSql = cond.getSql match {
      case "" => "1 = 1"
      case s => s
    }
    s"SELECT\n$columns\nFROM\n$table\nWHERE\n$condSql"
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
