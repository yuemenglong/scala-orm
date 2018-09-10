package io.github.yuemenglong.orm.test

import java.text.SimpleDateFormat

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.operate.field.Fn
import io.github.yuemenglong.orm.operate.join.TypedSelectableCascade
import io.github.yuemenglong.orm.sql.Expr
import io.github.yuemenglong.orm.test.entity._
import io.github.yuemenglong.orm.test.lite.LiteObj
import io.github.yuemenglong.orm.tool.OrmTool
import org.junit.{After, Assert, Before, Test}

/**
  * Created by <yuemenglong@126.com> on 2018/1/31.
  */
class SqliteTest {
  private var db: Db = _

  @SuppressWarnings(Array("Duplicates"))
  @Before def before(): Unit = {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
//    Orm.init(Array(classOf[SqliteObj].asInstanceOf[Class[_]]))
    Orm.init(Array("io.github.yuemenglong.orm.test.lite.LiteObj"))
    db = openDb()
    db.rebuild()
    //    db.check()
  }

  @After def after(): Unit = {
    Orm.reset()
    db.shutdown()
  }

  def openDb(): Db = Orm.openDb("root", "root", "test")

  @Test
  def testInsert(): Unit = db.beginTransaction(session => {
    val obj = new LiteObj

  })
}
