package orm.Session

import java.sql.Connection
import java.util

import orm.db.Db
import orm.entity.EntityManager
import orm.operate.{Executor, Selector}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/24.
  */

class Session(val conn: Connection) {
  var cache = new ArrayBuffer[Object]()
  var closed = false

  def injectSession(entity: Object, session: Session): Unit = {
    if (entity == null) {
      return
    }
    if (entity.isInstanceOf[util.ArrayList[_]]) {
      val list = entity.asInstanceOf[util.ArrayList[Object]]
      for (i <- 0 to list.size() - 1) {
        val core = EntityManager.core(list.get(i))
        core.setSession(this)
        injectSession(list.get(i), session)
      }
    } else {
      val core = EntityManager.core(entity)
      core.setSession(this)
      core.meta.fieldVec.filter(!_.isNormalOrPkey()).foreach(fieldMeta => {
        if (core.fieldMap.contains(fieldMeta.name)) {
          injectSession(core.fieldMap(fieldMeta.name), session)
        }
      })
    }

  }

  def execute(executor: Executor): Int = {
    val entity = executor.getEntity()
    require(entity != null)
    val ret = executor.execute(conn)
    injectSession(entity, this)
    return ret
  }

  def query[T](selector: Selector[T]): util.ArrayList[T] = {
    val ret = selector.query(conn)
    injectSession(ret, this)
    return ret
  }

  def addCache(obj: Object): Unit = {
    cache += obj
  }

  def isClosed(): Boolean = {
    closed
  }

  def flush(): Unit = {
    require(closed == false)
    cache.foreach(item => {
      val ex = Executor.createUpdate(item)
      this.execute(ex)
    })
    cache.clear()
  }

  def close(): Unit = {
    require(closed == false)
    conn.close()
    this.closed = true
  }
}
