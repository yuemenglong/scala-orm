package orm.Session

import orm.db.Db
import orm.operate.Executor

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/24.
  */

class Session(db:Db) {
  var conn = db.getConn()
  var cache = new ArrayBuffer[Object]()
  var close = false

  def execute(executor: Executor):Int={
0
  }
}
