package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import org.junit.{After, Before, Test}

/**
  * Created by <yuemenglong@126.com> on 2018/1/31.
  */
class SqliteTest {
  private var db: Db = _

  @SuppressWarnings(Array("Duplicates"))
  @Before def before(): Unit = {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
    Orm.init("io.github.yuemenglong.orm.test.lite")
    db = openDb()
    db.rebuild()
    //    db.check()
  }

  @After def after(): Unit = {
    Orm.reset()
    db.shutdown()
  }

  def openDb(): Db = Orm.openDb("root", "root", "test.db")

  @Test
  def testInsert(): Unit = db.beginTransaction(session => {

  })
}
