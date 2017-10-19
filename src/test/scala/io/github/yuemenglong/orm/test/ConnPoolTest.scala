package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.test.model.Obj
import org.junit.{After, Before, Test}

/**
  * Created by <yuemenglong@126.com> on 2017/10/19.
  */
class ConnPoolTest {
  private var db: Db = _


  @SuppressWarnings(Array("Duplicates"))
  @Before def before(): Unit = {
    Orm.init("io.github.yuemenglong.orm.test.model")
    db = openDb()
    db.rebuild()
  }

  @After def after(): Unit = {
    Orm.clear()
    db.shutdown()
  }

  def openDb(): Db = Orm.openDb("localhost", 3306, "root", "root", "test")

  @Test
  def testConnPool(): Unit = {
    for (i <- 0.until(1000)) {
      println(i)
      db.beginTransaction(session => {
        val root = Orm.root(classOf[Obj]).asSelect()
        session.query(Orm.from(root))
      })
    }
  }

}
