package io.github.yuemenglong.orm.operate.query

import java.sql.ResultSet

import io.github.yuemenglong.orm.api.operate.sql.core._
import io.github.yuemenglong.orm.api.operate.sql.table.SubQuery
import io.github.yuemenglong.orm.impl.entity.Entity
import io.github.yuemenglong.orm.operate.sql.core._
import io.github.yuemenglong.orm.operate.sql.table.SubQueryImpl
import io.github.yuemenglong.orm.session.Session

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
 * Created by yml on 2017/7/14.
 */
trait Queryable[T] {
  def query(session: Session): Array[T]
}

trait Selectable[T] {
  def getColumns: Array[ResultColumn]

  def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T

  def getType: Class[T]

  def getKey(value: Object): String
}

trait AsSubQuery[T] {
  def all: ExprLike[_]

  def any: ExprLike[_]

  def asTable(alias: String): SubQuery
}

trait QueryBase[T] extends SelectStatement[T] with AsSubQuery[T]

private[orm] trait QueryBaseImpl[S] extends QueryBase[S] with SelectStatementImpl[S] {
  val targets: Array[Selectable[_]]

  def query0(session: Session): Array[Array[Any]] = {
    var filterSet = Set[String]()
    val sql = {
      val sb = new StringBuffer()
      genSql(sb)
      sb.toString
    }
    val params = {
      val ab = new ArrayBuffer[Object]()
      genParams(ab)
      ab.toArray
    }
    session.query(sql, params, rs => {
      var ab = new ArrayBuffer[Array[Any]]()
      val filterMap = mutable.Map[String, Entity]()
      while (rs.next()) {
        val row = targets.map(s => {
          val value = s.pick(rs, filterMap)
          val key = s.getKey(value.asInstanceOf[Object])
          (value, key)
        })
        val key = row.map(_._2).mkString("$")
        if (!filterSet.contains(key)) {
          ab += row.map(_._1)
        }
        filterSet += key
      }
      ab.toArray
    })
  }

  def all: ExprLike[_] = ExprUtil.create("ALL", this)

  def any: ExprLike[_] = ExprUtil.create("ANY", this)

  def asTable(alias: String): SubQuery = {
    val that = this
    new SubQueryImpl {
      override private[orm] val _on = Var[Expr](null)
      override private[orm] val _table = TableLikeUtil.create(that, alias)._table
      override private[orm] val _joins = new ArrayBuffer[(String, TableOrSubQuery, Var[Expr])]()
    }
  }
}

trait Query1[T] extends QueryBase[Query1[T]] with Queryable[T]

private[orm] class Query1Impl[T: ClassTag](s: Selectable[T]) extends Query1[T] with QueryBaseImpl[Query1[T]] {

  override def query(session: Session): Array[T] = {
    Array[T](query0(session).map(r => r(0).asInstanceOf[T]): _*)
  }

  override val targets: Array[Selectable[_]] = Array(s)
  override private[orm] val core = new SelectCore(s.getColumns)
}

trait Query2[T0, T1] extends QueryBase[Query2[T0, T1]] with Queryable[(T0, T1)]

private[orm] class Query2Impl[T0: ClassTag, T1: ClassTag](s0: Selectable[T0],
                                                          s1: Selectable[T1])
  extends Query2[T0, T1] with QueryBaseImpl[Query2[T0, T1]] {

  override def query(session: Session): Array[(T0, T1)] = {
    Array[(T0, T1)](query0(session).map(r => (
      r(0).asInstanceOf[T0],
      r(1).asInstanceOf[T1]
    )): _*)
  }

  override val targets: Array[Selectable[_]] = Array(s0, s1)
  override private[orm] val core = new SelectCore(s0.getColumns ++ s1.getColumns)
}

trait Query3[T0, T1, T2] extends QueryBase[Query3[T0, T1, T2]] with Queryable[(T0, T1, T2)]

private[orm] class Query3Impl[T0: ClassTag, T1: ClassTag, T2: ClassTag](s0: Selectable[T0],
                                                                        s1: Selectable[T1],
                                                                        s2: Selectable[T2]
                                                                       )
  extends Query3[T0, T1, T2] with QueryBaseImpl[Query3[T0, T1, T2]] {

  override def query(session: Session): Array[(T0, T1, T2)] = {
    Array[(T0, T1, T2)](query0(session).map(r => (
      r(0).asInstanceOf[T0],
      r(1).asInstanceOf[T1],
      r(2).asInstanceOf[T2]
    )): _*)
  }

  override val targets: Array[Selectable[_]] = Array(s0, s1, s2)
  override private[orm] val core = new SelectCore(s0.getColumns ++ s1.getColumns ++ s2.getColumns)
}