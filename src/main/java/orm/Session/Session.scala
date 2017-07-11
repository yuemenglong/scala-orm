package orm.Session

import java.sql.Connection

import orm.lang.interfaces.Entity
import orm.operate._

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/24.
  */

class Session(private val conn: Connection) {
  private var cache = new ArrayBuffer[Object]()
  private var closed = false
  private var tx: Transaction = _

  def injectSession(entity: Entity): Unit = {
    if (entity == null) {
      return
    }
    val core = entity.$$core()
    core.setSession(this)
    core.meta.managedFieldVec().filter(field => {
      !field.isNormalOrPkey && core.fieldMap.contains(field.name)
    }).foreach(field => {
      if (field.isOneMany) {
        core.fieldMap(field.name).asInstanceOf[Array[Object]]
          .map(_.asInstanceOf[Entity]).foreach(injectSession)
      } else {
        injectSession(core.fieldMap(field.name).asInstanceOf[Entity])
      }
    })
  }

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

  def addCache(obj: Object): Unit = {
    cache += obj
  }

  def isClosed: Boolean = {
    closed
  }

  def flush(): Unit = {
    require(!closed)
    cache.foreach(item => {
      val ex = Executor.createUpdate(item)
      this.execute(ex)
    })
    cache.clear()
  }

  def close(): Unit = {
    require(!closed)
    conn.close()
    this.closed = true
  }

  def getConnection: Connection = {
    conn
  }

  def execute(executor: Executor): Int = {
    require(executor.getEntity != null)
    val ret = executor.execute(conn)
    injectSession(executor.getEntity)
    ret
  }

  def query(selectors: Array[TargetSelector[_]]): Array[Array[Object]] = {
    val ret = Selector.query(selectors, conn)
    ret.foreach(_.filter(_.isInstanceOf[Entity]).map(_.asInstanceOf[Entity]).foreach(injectSession))
    ret
  }

  def query[T](selector: TargetSelector[T]): Array[T] = {
    val ct: ClassTag[T] = selector match {
      case es: Join[_] => ClassTag(es.meta.clazz)
      case fs: Field[_] => ClassTag(fs.clazz)
    }
    query(Array[TargetSelector[_]](selector))
      .map(row => row(0).asInstanceOf[T])
      .toArray(ct)
  }

  def first[T](selector: TargetSelector[T]): T = {
    query(selector) match {
      case Array() => null.asInstanceOf[T]
      case arr => arr(0)
    }
  }

  def query[T0, T1](s0: TargetSelector[T0], s1: TargetSelector[T1]): Array[(T0, T1)] = {
    val selectors = Array[TargetSelector[_]](s0, s1)
    query(selectors).map(row => {
      (row(0).asInstanceOf[T0], row(1).asInstanceOf[T1])
    })
  }

  def first[T0, T1](s0: TargetSelector[T0], s1: TargetSelector[T1]): (T0, T1) = {
    query(s0, s1) match {
      case Array() => null.asInstanceOf[(T0, T1)]
      case arr => arr(0)
    }
  }

  def query[T0, T1, T2](s0: TargetSelector[T0], s1: TargetSelector[T1], s2: TargetSelector[T2]): Array[(T0, T1, T2)] = {
    val selectors = Array[TargetSelector[_]](s0, s1, s2)
    query(selectors).map(row => {
      (row(0).asInstanceOf[T0], row(1).asInstanceOf[T1], row(2).asInstanceOf[T2])
    })
  }

  def first[T0, T1, T2](s0: TargetSelector[T0], s1: TargetSelector[T1], s2: TargetSelector[T2]): (T0, T1, T2) = {
    query(s0, s1, s2) match {
      case Array() => null.asInstanceOf[(T0, T1, T2)]
      case arr => arr(0)
    }
  }
}
