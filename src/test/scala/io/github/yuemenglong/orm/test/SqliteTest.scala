package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.test.lite._
import org.junit.{After, Assert, Before, Test}

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
    db.check()
  }

  @After def after(): Unit = {
    Orm.reset()
    db.shutdown()
  }

  def openDb(): Db = Orm.openDb("test.db")

  @Test
  def testInsert(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.boolValue = true
    obj.ptr = new Ptr
    obj.oo = new OO
    obj.om = Array(new OM, new OM)
    obj.om(0).mo = new MO
    val ex = Orm.insert(obj)
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.inserts(_.om).insert(_.mo)
    session.execute(ex)
    val r = ex.root()
    Assert.assertEquals(r.id, 1)
    Assert.assertEquals(r.ptrId, r.ptr.id)
    Assert.assertEquals(r.oo.objId, r.id)
    Assert.assertEquals(r.om(0).mo.id, r.om(0).moId)
    r.om.foreach(m => {
      Assert.assertEquals(m.objId, r.id)
    })

    {
      r.name = "name"
      r.om = r.om ++ Array(Orm.create(classOf[OM]))
      val ex = Orm.update(r)
      (0 to 1).foreach(i => ex.ignoreFor(r.om(i)))
      ex.insert(_.om)
      session.execute(ex)
      Assert.assertEquals(r.om(2).objId, r.id)
    }
    {
      val root = Orm.root(classOf[Obj])
      val q = Orm.selectFrom(root)
      val res = session.query(q)
      Assert.assertEquals(res(0).boolValue, true)
    }

    {
      val root = Orm.root(classOf[Obj])
      val q = Orm.select(root.get(_.id)).from(root)
      session.query(q)
    }
  })
}
