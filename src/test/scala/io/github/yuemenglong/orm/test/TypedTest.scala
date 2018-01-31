package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.test.entity._
import org.junit.{After, Before, Test}

/**
  * Created by <yuemenglong@126.com> on 2018/1/31.
  */
class TypedTest {
  private var db: Db = _
  private var db2: Db = _

  @SuppressWarnings(Array("Duplicates"))
  @Before def before(): Unit = {
    Orm.init("io.github.yuemenglong.orm.test.entity")
    db = openDb()
    db.rebuild()
    db.check()

    db2 = openDb2()
    db2.rebuild()
    db2.check()
  }

  @After def after(): Unit = {
    Orm.clear()
    db.shutdown()
    db2.shutdown()
  }

  def openDb(): Db = Orm.openDb("localhost", 3306, "root", "root", "test")

  def openDb2(): Db = Orm.openDb("localhost", 3306, "root", "root", "test2")

  @Test
  def testOr(): Unit = db.beginTransaction(session => {
    (1 to 10).foreach(i => {
      val obj = new Obj
      obj.name = s"name$i"
      obj.ptr = new Ptr
      obj.ptr.value = i
      obj.om = Array(new OM, new OM, new OM)
      obj.om.foreach(om => {
        om.value = i * i
      })
      val ex = Orm.insert(Orm.convert(obj))
      ex.insert()(_.om)
      session.execute(ex)
    })
  })

}
