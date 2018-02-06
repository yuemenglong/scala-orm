package io.github.yuemenglong.orm.Session

import java.sql.Connection

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.operate.traits.core.{Executable, Queryable}

import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/24.
  */

class Session(private val conn: Connection) {
  private var closed = false
  private var tx: Transaction = _

  def inTransaction(): Boolean = {
    tx != null
  }

  def beginTransaction(): Transaction = {
    if (tx == null) {
      tx = new Transaction(this)
    }
    tx
  }

  def clearTransaction(): Unit = {
    tx = null
  }


  def isClosed: Boolean = {
    closed
  }

  def close(): Unit = {
    require(!closed)
    conn.close()
    this.closed = true
  }

  def getConnection: Connection = {
    conn
  }

  def execute(executor: Executable): Int = {
    executor.execute(conn)
  }

  def query[T](query: Queryable[T]): Array[T] = {
    query.query(conn).toArray(ClassTag(query.getType))
  }

  def first[T](q: Queryable[T]): T = {
    query(q) match {
      case Array() => null.asInstanceOf[T]
      case arr => arr(0)
    }
  }

  //  def query[T](selector: Target[T]): Array[T] = {
  //    val ct: ClassTag[T] = selector match {
  //      case es: JoinT[_] => ClassTag(es.meta.clazz)
  //      case fs: Target[_] => ClassTag(fs.classT())
  //    }
  //    query(Array[Target[_]](selector))
  //      .map(row => row(0).asInstanceOf[T])
  //      .toArray(ct)
  //  }

  //  def first[T](selector: Target[T]): T = {
  //    query(selector) match {
  //      case Array() => null.asInstanceOf[T]
  //      case arr => arr(0)
  //    }
  //  }
  //
  //  def query[T0, T1](s0: Target[T0], s1: Target[T1]): Array[(T0, T1)] = {
  //    val selectors = Array[Target[_]](s0, s1)
  //    query(selectors).map(row => {
  //      (row(0).asInstanceOf[T0], row(1).asInstanceOf[T1])
  //    })
  //  }
  //
  //  def first[T0, T1](s0: Target[T0], s1: Target[T1]): (T0, T1) = {
  //    query(s0, s1) match {
  //      case Array() => null.asInstanceOf[(T0, T1)]
  //      case arr => arr(0)
  //    }
  //  }
  //
  //  def query[T0, T1, T2](s0: Target[T0], s1: Target[T1], s2: Target[T2]): Array[(T0, T1, T2)] = {
  //    val selectors = Array[Target[_]](s0, s1, s2)
  //    query(selectors).map(row => {
  //      (row(0).asInstanceOf[T0], row(1).asInstanceOf[T1], row(2).asInstanceOf[T2])
  //    })
  //  }
  //
  //  def first[T0, T1, T2](s0: Target[T0], s1: Target[T1], s2: Target[T2]): (T0, T1, T2) = {
  //    query(s0, s1, s2) match {
  //      case Array() => null.asInstanceOf[(T0, T1, T2)]
  //      case arr => arr(0)
  //    }
  //  }
}
