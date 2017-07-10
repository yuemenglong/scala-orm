package orm.Session

import java.sql.Connection

import orm.entity.EntityManager
import orm.operate.{Executor, Selector}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/24.
  */

class Session(private val conn: Connection) {
  private var cache = new ArrayBuffer[Object]()
  private var closed = false
  private var tx: Transaction = _

  def injectSession(entity: Object, session: Session): Unit = {
    if (entity == null) {
      return
    }
    if (entity.getClass.isArray) {
      entity.asInstanceOf[Array[Object]].foreach(item => {
        val core = EntityManager.core(item)
        core.setSession(this)
        injectSession(item, session)
      })
    } else {
      val core = EntityManager.core(entity)
      core.setSession(this)
      core.meta.managedFieldVec().filter(!_.isNormalOrPkey).foreach(fieldMeta => {
        if (core.fieldMap.contains(fieldMeta.name)) {
          injectSession(core.fieldMap(fieldMeta.name), session)
        }
      })
    }
  }

  def execute(executor: Executor): Int = {
    val entity = executor.getEntity
    require(entity != null)
    val ret = executor.execute(conn)
    injectSession(entity, this)
    ret
  }

  def query[T](selector: Selector[T]): Array[T] = {
    val ret = selector.query(conn)
    injectSession(ret, this)
    ret
  }

  def first[T](selector: Selector[T]): T = {
    val ret = selector.first(conn)
    injectSession(ret.asInstanceOf[Object], this)
    ret.asInstanceOf[T]
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
}
