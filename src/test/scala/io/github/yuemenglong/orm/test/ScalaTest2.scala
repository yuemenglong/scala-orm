package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.test.model.Obj
import org.junit.{After, Assert, Before, Test}

/**
  * Created by <yuemenglong@126.com> on 2017/10/19.
  */
class ScalaTest2 {
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
      db.beginTransaction(session => {
        val root = Orm.root(classOf[Obj]).asSelect()
        session.query(Orm.from(root))
      })
    }
  }

  @Test
  def testIn(): Unit = {
    db.beginTransaction(session => {
      1.to(3).foreach(i => {
        val obj = new Obj()
        obj.setName(i.toString)
        session.execute(Orm.insert(Orm.convert(obj)))
      })
      val root = Orm.root(classOf[Obj]).asSelect()
      val res = session.query(Orm.select(root).from(root).where(root.get("id").in(Array(1, 2))))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0).getId, 1)
      Assert.assertEquals(res(1).getId, 2)
    })
  }

  @Test
  def testSpecField(): Unit = {
    db.beginTransaction(session => {
      val obj = new Obj()
      obj.setName("name")
      obj.setAge(10)
      session.execute(Orm.insert(Orm.convert(obj)))

      val root = Orm.root(classOf[Obj]).asSelect()
      root.fields("name")
      val res = session.first(Orm.select(root).from(root))
      Assert.assertEquals(res.getAge, null)
      Assert.assertEquals(res.getName, "name")
      Assert.assertEquals(res.getId, 1L)
    })
  }

}
