package io.github.yuemenglong.orm.operate.query

import java.lang
import java.sql.ResultSet

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.operate.field.SelectableFieldImpl
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.CondHolder
import io.github.yuemenglong.orm.operate.join.traits.{Cond, Root, SubRoot}
import io.github.yuemenglong.orm.operate.query.traits._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */

trait QueryBuilderImpl[T] extends QueryBuilder[T] {
  val st: Selectable[T]

  override def from[R](selectRoot: Root[R]): Query[R, T] = {
    val thisSt = st
    new QueryImpl[R, T] with QueryBuilderImpl[T] {
      override val root = selectRoot
      override val st = thisSt
    }
  }
}

trait SubQueryBuilderImpl[T] extends SubQueryBuilder[T] {
  val st: Selectable[T]

  override def from[R](subRoot: SubRoot[R]): SubQuery[R, T] = {
    val thisSt = st
    new SubQueryImpl[R, T] with QueryBaseImpl[R, T] with QueryBuilderImpl[T] {
      override val root = subRoot
      override val st = thisSt
    }
  }
}

trait SubQueryImpl[R, T] extends SubQuery[R, T] {
  self: QueryBaseImpl[R, T] =>

  override def getSql = s"(\n${getSql0}\n)"
}

trait QueryImpl[R, T] extends QueryBaseImpl[R, T] with Query[R, T] {
  self: QueryBuilderImpl[T] =>

  override def getSql: String = getSql0
}

trait QueryBaseImpl[R, T] extends QueryBase[R, T] {
  self: QueryBuilderImpl[T] =>
  val root: SubRoot[R]
  private[orm] var cond: Cond = new CondHolder()
  private[orm] var orders: ArrayBuffer[(Field, String)] = new ArrayBuffer[(Field, String)]()
  private[orm] var limitOffset: (Long, Long) = (-1, -1)
  private[orm] var groupByVar: Array[Field] = Array[Field]()
  private[orm] var havingVar: Cond = _

  def getParams: Array[Object] = {
    val loParams = limitOffset match {
      case (-1, -1) => Array()
      case (l, -1) => Array[Object](new java.lang.Long(l))
      case (-1, o) => Array[Object](new java.lang.Long(o))
      case (l, o) => Array[Object](new java.lang.Long(l), new java.lang.Long(o))
    }
    val groupByHavingParams = (groupByVar.length, havingVar) match {
      case (0, _) => Array()
      case (_, null) => Array()
      case (_, _) => havingVar.getParams
    }
    root.getParams ++ cond.getParams ++ loParams ++ groupByHavingParams
  }

  protected def getSql0: String = {
    val columnsSql = st.getColumnWithAs
    val tableSql = root.getSql
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
    val loSql = limitOffset match {
      case (-1, -1) => ""
      case (_, -1) => "\nLIMIT ?"
      case (-1, _) => "\nOFFSET ?"
      case (_, _) => "\nLIMIT ? OFFSET ?"
    }
    val groupByHavingSql = (groupByVar.length, havingVar) match {
      case (0, _) => ""
      case (_, null) => s"\nGROUP BY ${groupByVar.map(_.getColumn).mkString(", ")}"
      case (_, _) => s"\nGROUP BY ${groupByVar.map(_.getColumn).mkString(", ")}\nHAVING ${havingVar.getSql}"
    }
    s"SELECT\n$columnsSql\nFROM\n$tableSql\nWHERE\n$condSql$groupByHavingSql$orderBySql$loSql"
  }

  override def query(session: Session): Array[T] = {
    if (root == null || st == null) {
      throw new RuntimeException("Must Select <Tuple> From <Root>, Either Is Null")
    }
    var filterSet = Set[String]()
    val sql = getSql
    val params = getParams
    session.query(sql, params, rs => {
      var ab = ArrayBuffer[T]()
      val filterMap = mutable.Map[String, Entity]()
      while (rs.next()) {
        val value = st.pick(rs, filterMap)
        val key = st.getKey(value.asInstanceOf[Object])
        if (!filterSet.contains(key)) {
          ab += value
        }
        filterSet += key
      }
      ab.toArray(ClassTag(st.getType))
    })
  }

  override def limit(l: Long): this.type = {
    limitOffset = (l, limitOffset._2)
    this
  }

  override def offset(o: Long): this.type = {
    limitOffset = (limitOffset._1, o)
    this
  }

  override def asc(field: Field): this.type = {
    orders += ((field, "ASC"))
    this
  }

  override def desc(field: Field): this.type = {
    orders += ((field, "DESC"))
    this
  }

  override def where(c: Cond): this.type = {
    cond = c
    this
  }

  override def groupBy(field: Field, fields: Field*): this.type = {
    groupByVar = Array(field) ++ fields
    this
  }

  override def having(cond: Cond): this.type = {
    havingVar = cond
    this
  }

  override def getType: Class[T] = st.getType
}

class SelectableTupleImpl[T](clazz: Class[T], ss: Selectable[_]*) extends Selectable[T] {
  val selects: Array[Selectable[_]] = ss.toArray
  val tuple2Class: Class[(_, _)] = classOf[(_, _)]
  val tuple3Class: Class[(_, _, _)] = classOf[(_, _, _)]
  val tuple4Class: Class[(_, _, _, _)] = classOf[(_, _, _, _)]
  val tuple5Class: Class[(_, _, _, _, _)] = classOf[(_, _, _, _, _)]
  val tuple6Class: Class[(_, _, _, _, _, _)] = classOf[(_, _, _, _, _, _)]
  val tuple7Class: Class[(_, _, _, _, _, _, _)] = classOf[(_, _, _, _, _, _, _)]
  val tuple8Class: Class[(_, _, _, _, _, _, _, _)] = classOf[(_, _, _, _, _, _, _, _)]

  override def pick(rs: ResultSet, filterMap: mutable.Map[String, Entity]): T = {
    val row = selects.map(_.pick(rs, filterMap).asInstanceOf[Object])
    arrayToTuple(row)
  }

  override def getColumnWithAs: String = selects.map(_.getColumnWithAs).mkString(",\n")

  override def getType: Class[T] = clazz

  override def getKey(value: Object): String = {
    val values = tupleToArray(value.asInstanceOf[T])
    (selects, values).zipped.map(_.getKey(_)).mkString("$")
  }

  def tupleToArray(tuple: T): Array[Object] = {
    val ret: Array[Any] = tuple match {
      case (t1, t2, t3, t4, t5, t6, t7, t8) => Array(t1, t2, t3, t4, t5, t6, t7, t8)
      case (t1, t2, t3, t4, t5, t6, t7) => Array(t1, t2, t3, t4, t5, t6, t7)
      case (t1, t2, t3, t4, t5, t6) => Array(t1, t2, t3, t4, t5, t6)
      case (t1, t2, t3, t4, t5) => Array(t1, t2, t3, t4, t5)
      case (t1, t2, t3, t4) => Array(t1, t2, t3, t4)
      case (t1, t2, t3) => Array(t1, t2, t3)
      case (t1, t2) => Array(t1, t2)
      case t => Array(t)
    }
    ret.map(_.asInstanceOf[Object])
  }

  def arrayToTuple(arr: Array[Object]): T = {
    clazz match {
      case `tuple2Class` => (arr(0), arr(1)).asInstanceOf[T]
      case `tuple3Class` => (arr(0), arr(1), arr(2)).asInstanceOf[T]
      case `tuple4Class` => (arr(0), arr(1), arr(2), arr(3)).asInstanceOf[T]
      case `tuple5Class` => (arr(0), arr(1), arr(2), arr(3), arr(4)).asInstanceOf[T]
      case `tuple6Class` => (arr(0), arr(1), arr(2), arr(3), arr(4), arr(5)).asInstanceOf[T]
      case `tuple7Class` => (arr(0), arr(1), arr(2), arr(3), arr(4), arr(5), arr(6)).asInstanceOf[T]
      case `tuple8Class` => (arr(0), arr(1), arr(2), arr(3), arr(4), arr(5), arr(6), arr(7)).asInstanceOf[T]
      case _ => arr(0).asInstanceOf[T]
    }
  }
}

class Count_ extends Selectable[java.lang.Long] {
  def getAlias: String = "$count$"

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): java.lang.Long = resultSet.getObject(getAlias, classOf[java.lang.Long])

  override def getColumnWithAs: String = s"COUNT(*) AS $getAlias"

  override def getType: Class[java.lang.Long] = classOf[java.lang.Long]

  override def getKey(value: Object): String = value.toString
}

class Count(impl: Field) extends SelectableFieldImpl[java.lang.Long](classOf[java.lang.Long], impl) {
  private var distinctVar = ""

  override def getType: Class[lang.Long] = classOf[java.lang.Long]

  override def getColumn: String = s"COUNT($distinctVar${impl.getColumn})"

  override def getAlias: String = s"$$count$$${impl.getAlias}"

  override def distinct(): SelectableField[lang.Long] = {
    distinctVar = "DISTINCT "
    this
  }
}

class Sum[T](impl: Field, clazz: Class[T]) extends SelectableFieldImpl[T](clazz, impl) {
  private var distinctVar = ""

  override def getColumn: String = s"SUM($distinctVar${impl.getColumn})"

  override def getAlias: String = s"$$sum$$${impl.getAlias}"

  override def distinct(): SelectableField[T] = {
    distinctVar = "DISTINCT "
    this
  }
}

class Max[T](impl: Field, clazz: Class[T]) extends SelectableFieldImpl[T](clazz, impl) {
  override def getColumn: String = s"MAX(${impl.getColumn})"

  override def getAlias: String = s"$$max$$${impl.getAlias}"

  override def distinct(): SelectableField[T] = {
    throw new RuntimeException("Max() Not Need Distinct")
  }
}

class Min[T](impl: Field, clazz: Class[T]) extends SelectableFieldImpl[T](clazz, impl) {
  override def getColumn: String = s"MIN(${impl.getColumn})"

  override def getAlias: String = s"$$min$$${impl.getAlias}"

  override def distinct(): SelectableField[T] = {
    throw new RuntimeException("Min() Not Need Distinct")
  }
}