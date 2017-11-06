package io.github.yuemenglong.orm.test

import java.util.Date

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.test.model.{MO, OM, OO, Obj}
import io.github.yuemenglong.orm.tool.OrmTool
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
        val root = Orm.root(classOf[Obj])
        session.query(Orm.selectFrom(root))
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
      val root = Orm.root(classOf[Obj])
      val res = session.query(Orm.select(root).from(root).where(root.get("id").in(Array(1, 2))))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0).getId.intValue(), 1)
      Assert.assertEquals(res(1).getId.intValue(), 2)
    })
  }

  @Test
  def testSpecField(): Unit = {
    db.beginTransaction(session => {
      val obj = new Obj()
      obj.setName("name")
      obj.setAge(10)
      session.execute(Orm.insert(Orm.convert(obj)))

      val root = Orm.root(classOf[Obj])
      root.fields("name")
      val res = session.first(Orm.select(root).from(root))
      Assert.assertEquals(res.getAge, null)
      Assert.assertEquals(res.getName, "name")
      Assert.assertEquals(res.getId, 1L)
    })
  }

  @Test
  def testIgnoreField(): Unit = {
    db.beginTransaction(session => {
      {
        val obj = new Obj()
        obj.setName("name")
        obj.setAge(10)
        obj.setBirthday(new Date())
        obj.setOo(new OO)
        obj.getOo.setValue(10)
        val ex = Orm.insert(Orm.convert(obj))
        ex.insert("oo")
        ex.ignore("age")
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        root.select("oo")
        val obj = session.first(Orm.select(root).from(root))
        Assert.assertEquals(obj.getAge, null)
        Assert.assertEquals(obj.getName, "name")
        Assert.assertEquals(obj.getOo.getValue, 10)

        {
          obj.setName("name1")
          obj.getOo.setValue(20)
          val ex = Orm.update(Orm.convert(obj))
          ex.update("oo").ignore("value")
          session.execute(ex)
        }
        {
          val root = Orm.root(classOf[Obj])
          root.select("oo")
          val obj = session.first(Orm.select(root).from(root))
          Assert.assertEquals(obj.getName, "name1")
          Assert.assertEquals(obj.getOo.getValue, 10)
        }
      }
    })
  }

  @Test
  def testMinMax(): Unit = {
    db.beginTransaction(session => {
      1.to(10).foreach(i => {
        val obj = new Obj
        obj.setName(i.toString)
        session.execute(Orm.insert(Orm.convert(obj)))
      })
      val root = Orm.root(classOf[Obj])
      val query = Orm.select(root.max(root.get("id"), classOf[java.lang.Long]),
        root.min(root.get("id"), classOf[java.lang.Long])).from(root)
      val (max, min) = session.first(query)
      Assert.assertEquals(max.intValue(), 10)
      Assert.assertEquals(min.intValue(), 1)
    })
  }

  @Test
  def testAttach(): Unit = {
    db.beginTransaction(session => {
      var obj = new Obj
      obj.setName("")
      obj.setOm(1.to(5).map(i => {
        val om = new OM()
        om.setMo(new MO)
        om
      }).toArray)
      obj = Orm.convert(obj)
      val ex = Orm.insert(obj)
      ex.insert("om").insert("mo")
      session.execute(ex)
      obj.setOm(Array())
      obj = OrmTool.attach[Obj](obj, "om", session, join => join.select("mo"))
      Assert.assertEquals(obj.getOm.length, 5)
      Assert.assertEquals(obj.getOm()(0).getMo.getId.intValue(), 1)
    })
  }

  @Test
  def testConvert(): Unit = db.beginTransaction(session => {
    val obj = Orm.convert(new Obj)
    obj.setName("Age10")
    obj.setAge(10)
    session.execute(Orm.insert(obj))
    val root = Orm.root(classOf[Obj])
    val res = session.first(Orm.selectFrom(root).where(root.get("age").eql(10)))
    Assert.assertEquals(res.getName, "Age10")
  })

  @Test
  def testIgnore(): Unit = db.beginTransaction(session => {
    val obj = Orm.convert(new Obj)
    obj.setName("Tom")
    obj.setAge(100)
    obj.setDoubleValue(10.0)
    session.execute(Orm.insert(obj))

    {
      val root = Orm.root(classOf[Obj])
      val res = session.first(Orm.selectFrom(root))
      res.setName("Jack")
      res.setAge(0)
      res.setDoubleValue(20.0)
      session.execute(Orm.update(res).ignore("name", "age"))
    }
    {
      val root = Orm.root(classOf[Obj])
      val res = session.first(Orm.selectFrom(root))
      Assert.assertEquals(res.getAge.intValue(), 100)
      Assert.assertEquals(res.getName, "Tom")
      Assert.assertEquals(res.getDoubleValue.doubleValue(), 20.0, 0.00001)
    }
  })

  @Test
  def testDeleteCascade(): Unit = {
    db.beginTransaction(session => {
      var obj = new Obj

      {
        obj.setName("")
        obj.setOo(new OO)
        obj.setOm(Array(new OM, new OM))
        obj.getOm()(0).setMo(new MO)
        obj = Orm.convert(obj)
        val ex = Orm.insert(obj)
        ex.insert("oo")
        ex.insert("om").insert("mo")
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        val ex = Orm.delete(
          root,
          root.leftJoin("oo"),
          root.leftJoin("om"),
          root.leftJoin("om").leftJoin("mo")
        ).from(root).where(root.get("id").eql(1))
        val ret = session.execute(ex)
        Assert.assertEquals(ret, 5)
      }
      {
        val root = Orm.root(classOf[Obj])
        root.select("oo")
        root.select("om").select("mo")
        val query = Orm.select(root.count()).from(root)
        val ret = session.first(query)
      }
    })
  }

  @Test
  def testCascadeUpdate(): Unit = {
    db.beginTransaction(session => {
      var obj = new Obj

      {
        obj.setName("")
        obj.setOo(new OO)
        obj.setOm(Array(new OM, new OM))
        obj.getOm()(0).setMo(new MO)
        obj = Orm.convert(obj)
        val ex = Orm.insert(obj)
        ex.insert("oo")
        ex.insert("om").insert("mo")
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        val ex = Orm.update(root).set(
          root.get("name").assign("Tom"),
          root.leftJoin("oo").get("value").assign(100),
          root.leftJoin("om").get("value").assign(200)
        ).where(root.get("id").eql(obj.getId).and(
          root.leftJoin("om").get("id").eql(1)))
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        root.select("oo")
        root.select("om")
        val query = Orm.selectFrom(root).where(root.get("id").eql(1))
        val obj = session.first(query)
        Assert.assertEquals(obj.getName, "Tom")
        Assert.assertEquals(obj.getOo.getValue.intValue(), 100)
        Assert.assertEquals(obj.getOm()(0).getValue.intValue(), 200)
        Assert.assertEquals(obj.getOm()(1).getValue, null)
      }
    })
  }
}
