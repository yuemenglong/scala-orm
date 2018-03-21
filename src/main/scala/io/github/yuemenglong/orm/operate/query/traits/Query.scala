package io.github.yuemenglong.orm.operate.query.traits

import java.sql.ResultSet

import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.core.traits.Expr2
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.traits.Cond
import io.github.yuemenglong.orm.sql.{ResultColumn, SelectStatement}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by yml on 2017/7/14.
  */
trait Queryable[T] {
  def query(session: Session): Array[T]

  //  def getType: Class[T]
}

trait Selectable[T] {
  def getColumns: List[ResultColumn]

  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getType: Class[T]

  def getKey(value: Object): String
}

//trait QueryBuilder[T] {
//  def from[R](selectRoot: Root[R]): Query[R, T]
//
//  def distinct: this.type
//}
//
//trait SubQueryBuilder[T] {
//  def from[R](subRoot: SubRoot[R]): SubQuery[R, T]
//
//  def distinct: this.type
//}

//trait QueryBase[R, T] extends Queryable[T] with Expr2 {
//
//  def limit(l: Long): this.type
//
//  def offset(l: Long): this.type
//
//  def asc(field: Field): this.type
//
//  def desc(field: Field): this.type
//
//  def where(cond: Cond): this.type
//
//  def groupBy(field: Field, fields: Field*): this.type
//
//  def having(cond: Cond): this.type
//
//  def distinct(): this.type
//}
//
//trait SubQuery[R, T] extends QueryBase[R, T] {
//  def all: this.type
//
//  def any: this.type
//}

//trait Query[R, T] extends QueryBase[R, T] {
//
//}

//trait Query[T] extends Queryable[T] with SelectStatement {
//  val target: Array[Selectable[_]]
//
//  override def query(session: Session): Array[T] = {
//    var filterSet = Set[String]()
//    val sql = {
//      val sb = new StringBuffer()
//      genSql(sb)
//      sb.toString
//    }
//    val params = getParams
//    session.query(sql, params, rs => {
//      var ab = ArrayBuffer[T]()
//      val filterMap = mutable.Map[String, Entity]()
//      while (rs.next()) {
//        val value = st.pick(rs, filterMap)
//        val key = st.getKey(value.asInstanceOf[Object])
//        if (!filterSet.contains(key)) {
//          ab += value
//        }
//        filterSet += key
//      }
//      ab.toArray(ClassTag(st.getType))
//    })
//  }
//}

trait QueryBase extends SelectStatement {
  val targets: Array[Selectable[_]]

  def query0(session: Session): Array[_] = {
    var filterSet = Set[String]()
    val sql = {
      val sb = new StringBuffer()
      genSql(sb)
      sb.toString
    }
    val params = getParams.toArray
    session.query(sql, params, rs => {
      var list = List[Any]()
      val filterMap = mutable.Map[String, Entity]()
      while (rs.next()) {
        val row = targets.map(s => {
          val value = s.pick(rs, filterMap)
          val key = s.getKey(value.asInstanceOf[Object])
          (value, key)
        })
        val key = row.map(_._2).mkString("$")
        if (!filterSet.contains(key)) {
          list ::= row.map(_._1)
        }
        filterSet += key
      }
      list.toArray
    })
  }
}

trait Query[T] extends QueryBase with Queryable[T] {
  //noinspection ScalaRedundantCast
  override def query(session: Session): Array[T] = {
    query0(session).map(_.asInstanceOf[T]).asInstanceOf[Array[T]]
  }
}