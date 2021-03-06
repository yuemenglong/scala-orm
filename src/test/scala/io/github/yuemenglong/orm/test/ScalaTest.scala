package io.github.yuemenglong.orm.test

import java.io.ByteArrayOutputStream
import java.util.Date

import io.github.yuemenglong.orm.api.{Orm, OrmLoader}
import io.github.yuemenglong.orm.api.db.Db
import io.github.yuemenglong.orm.impl.entity.{Entity, EntityCore, EntityManager}
import io.github.yuemenglong.orm.test.entity._
import org.junit.{After, Assert, Before, Test}
import io.github.yuemenglong.orm.api.types.Types._

/**
 * Created by <yuemenglong@126.com> on 2017/10/19.
 */
class ScalaTest {
  val Orm: Orm = OrmLoader.loadOrm()
  private var db: Db = _
  private var db2: Db = _

  @SuppressWarnings(Array("Duplicates"))
  @Before def before(): Unit = {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
    Orm.init("io.github.yuemenglong.orm.test.entity")
    db = openDb()
    db.rebuild()
    db.check()

    db2 = openDb2()
    db2.rebuild()
    db2.check()
  }

  @After def after(): Unit = {
    Orm.reset()
    db.shutdown()
    db2.shutdown()
  }

  def openDb(): Db = Orm.open(Orm.mysql("localhost", 3306, "root", "root", "orm_test"))

  def openDb2(): Db = Orm.open(Orm.mysql("localhost", 3306, "root", "root", "orm_test2"))

  @Test
  def testConnPool(): Unit = {
    for (_ <- 0.until(1000)) {
      db.beginTransaction(session => {
        val root = Orm.root(classOf[Obj])
        session.query(Orm.selectFrom(root))
      })
    }
  }

  @Test
  def testIn(): Unit = {
    db.beginTransaction(session => {
      1.to(4).foreach(i => {
        val obj = new Obj()
        obj.name = i.toString
        session.execute(Orm.insert(Orm.convert(obj)))
      })

      {
        val root = Orm.root(classOf[Obj])
        val res = session.query(Orm.select(root).from(root).where(root.get("id").in(Array(1, 2))))
        Assert.assertEquals(res.length, 2)
        Assert.assertEquals(res(0).id.intValue(), 1)
        Assert.assertEquals(res(1).id.intValue(), 2)
      }
      {
        val root = Orm.root(classOf[Obj])
        val res = session.query(Orm.select(root).from(root).where(root.get("id").nin(Array(3, 4))))
        Assert.assertEquals(res.length, 2)
        Assert.assertEquals(res(0).id.intValue(), 1)
        Assert.assertEquals(res(1).id.intValue(), 2)
      }
    })
  }

  @Test
  def testSpecField(): Unit = {
    db.beginTransaction(session => {
      val obj = new Obj()
      obj.name = "name"
      obj.age = 10
      session.execute(Orm.insert(Orm.convert(obj)))

      val root = Orm.root(classOf[Obj])
      root.fields("name")
      val res = session.first(Orm.select(root).from(root))
      Assert.assertNull(res.age)
      Assert.assertEquals(res.name, "name")
      Assert.assertEquals(res.id.intValue(), 1L)
    })
  }

  @Test
  def testIgnoreField(): Unit = {
    db.beginTransaction(session => {
      {
        val obj = new Obj()
        obj.name = "name"
        obj.age = 10
        obj.birthday = new Date()
        obj.oo = new OO
        obj.oo.value = 10
        val ex = Orm.insert(Orm.convert(obj))
        ex.insert("oo")
        ex.ignore("age")
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        root.select("oo")
        val obj = session.first(Orm.select(root).from(root))
        Assert.assertNull(obj.age)
        Assert.assertEquals(obj.name, "name")
        Assert.assertEquals(obj.oo.value, 10)

        {
          obj.name = "name1"
          obj.oo.value = 20
          val ex = Orm.update(Orm.convert(obj))
          ex.update("oo").ignore("value")
          session.execute(ex)
        }
        {
          val root = Orm.root(classOf[Obj])
          root.select("oo")
          val obj = session.first(Orm.select(root).from(root))
          Assert.assertEquals(obj.name, "name1")
          Assert.assertEquals(obj.oo.value, 10)
        }
      }
    })
  }

  @Test
  def testMinMax(): Unit = {
    db.beginTransaction(session => {
      1.to(10).foreach(i => {
        val obj = new Obj
        obj.name = i.toString
        session.execute(Orm.insert(Orm.convert(obj)))
      })
      val root = Orm.root(classOf[Obj])
      val query = Orm.select(Orm.Fn.max(root.get("id").to(classOf[java.lang.Long])),
        Orm.Fn.min(root.get("id").to(classOf[java.lang.Long]))).from(root)
      val (max, min) = session.first(query)
      Assert.assertEquals(max.intValue(), 10)
      Assert.assertEquals(min.intValue(), 1)
    })
  }

  @Test
  def testAttach(): Unit = {
    db.beginTransaction(session => {
      var obj = new Obj
      obj.name = ""
      obj.om = 1.to(5).map(_ => {
        val om = new OM()
        om.mo = new MO
        om
      }).toArray
      obj.oo = new OO
      obj = Orm.convert(obj)
      val ex = Orm.insert(obj)
      ex.insert("om").insert("mo")
      ex.insert("oo")
      session.execute(ex)
      obj.om = Array()
      obj = Orm.Tool.attach(obj, "om", session, join => join.select("mo"), null)
      Assert.assertEquals(obj.om.length, 5)
      Assert.assertEquals(obj.om(0).mo.id.intValue(), 1)
      obj = Orm.Tool.attach[Obj](obj, "oo", session)
      Assert.assertEquals(obj.oo.id.longValue(), 1)

      obj.om.foreach(om => {
        om.asInstanceOf[Entity].$$core().fieldMap += ("mo" -> null)
      })
      Orm.Tool.attach(obj.om, "mo", session).zipWithIndex.foreach { case (om, idx) =>
        Assert.assertEquals(om.id.longValue(), idx + 1)
      }
    })
  }

  @Test
  def testConvert(): Unit = db.beginTransaction(session => {
    val obj = Orm.convert(new Obj)
    obj.name = "Age10"
    obj.age = 10
    session.execute(Orm.insert(obj))
    val root = Orm.root(classOf[Obj])
    val res = session.first(Orm.selectFrom(root).where(root.get("age").eql(10)))
    Assert.assertEquals(res.name, "Age10")
  })

  @Test
  def testIgnore(): Unit = db.beginTransaction(session => {
    val obj = Orm.convert(new Obj)
    obj.name = "Tom"
    obj.age = 100
    obj.doubleValue = 10.0
    session.execute(Orm.insert(obj))

    {
      val root = Orm.root(classOf[Obj])
      val res = session.first(Orm.selectFrom(root))
      res.name = "Jack"
      res.age = 0
      res.doubleValue = 20.0
      session.execute(Orm.update(res).ignore("name", "age"))
    }
    {
      val root = Orm.root(classOf[Obj])
      val res = session.first(Orm.selectFrom(root))
      Assert.assertEquals(res.age.intValue(), 100)
      Assert.assertEquals(res.name, "Tom")
      Assert.assertEquals(res.doubleValue.doubleValue(), 20.0, 0.00001)
    }
  })

  @Test
  def testDeleteCascade(): Unit = {
    db.beginTransaction(session => {
      var obj = new Obj

      {
        obj.name = ""
        obj.oo = new OO
        obj.om = Array(new OM, new OM)
        obj.om(0).mo = new MO
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
        val query = Orm.select(Orm.Fn.count()).from(root)
        val ret = session.first(query)
        Assert.assertEquals(ret.longValue(), 0)
      }
    })
  }

  @Test
  def testDeleteCascadeCond(): Unit = {
    db.beginTransaction(session => {
      var obj = new Obj

      {
        obj.name = ""
        obj.oo = new OO
        obj.om = Array(new OM, new OM)
        obj.om(0).mo = new MO
        obj = Orm.convert(obj)
        val ex = Orm.insert(obj)
        ex.insert("oo")
        ex.insert("om").insert("mo")
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        val ex = Orm.delete(
          root.leftJoin("oo")
        ).from(root).where(root.get("id").eql(1))
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        root.select("oo")
        val query = Orm.selectFrom(root)
        val obj = session.first(query)
        Assert.assertNotNull(obj)
        Assert.assertNull(obj.oo)
      }
    })
  }

  @Test
  def testCascadeUpdate(): Unit = {
    db.beginTransaction(session => {
      var obj = new Obj

      {
        obj.name = ""
        obj.oo = new OO
        obj.om = Array(new OM, new OM)
        obj.om(0).mo = new MO
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
        ).where(root.get("id").eql(obj.id).and(
          root.leftJoin("om").get("id").eql(1)))
        session.execute(ex)
      }
      {
        val root = Orm.root(classOf[Obj])
        root.select("oo")
        root.select("om")
        val query = Orm.selectFrom(root).where(root.get("id").eql(1))
        val obj = session.first(query)
        Assert.assertEquals(obj.name, "Tom")
        Assert.assertEquals(obj.oo.value.intValue(), 100)
        Assert.assertEquals(obj.om(0).value.intValue(), 200)
        Assert.assertEquals(obj.om(1).value, null)
      }
    })
  }

  @Test
  def testExport(): Unit = {
    {
      val bs = new ByteArrayOutputStream()
      Orm.Tool.exportTsClass(bs)
      println(new String(bs.toByteArray))
    }

    {
      val bs = new ByteArrayOutputStream()
      Orm.Tool.exportTsClass(bs, "@observable", "import {observable} from 'mobx'")
      println(new String(bs.toByteArray))
    }
  }

  @Test
  def testFields(): Unit = {
    db.beginTransaction(session => {
      val obj = new Obj
      obj.name = "name"
      obj.age = 10
      val ex = Orm.insert(Orm.convert(obj))
      ex.fields("name")
      session.execute(ex)

      val obj2 = session.first(Orm.selectFrom(Orm.root(classOf[Obj])))
      Assert.assertEquals(obj2.name, "name")
      Assert.assertNull(obj2.age)

      obj2.name = "name2"
      obj2.age = 20
      val ex2 = Orm.update(obj2)
      ex2.fields("age")
      session.execute(ex2)

      val obj3 = session.first(Orm.selectFrom(Orm.root(classOf[Obj])))
      Assert.assertEquals(obj3.name, "name")
      Assert.assertEquals(obj3.age.intValue(), 20)
    })
  }

  @Test
  def testJoinAs(): Unit = {
    db.beginTransaction(session => {
      {
        val obj = new Obj()
        obj.name = ""
        obj.ptr = new Ptr
        obj.oo = new OO
        obj.om = Array(new OM, new OM)
        val ex = Orm.insert(Orm.convert(obj))
        ex.insert("ptr")
        ex.insert("oo")
        ex.insert("om")
        session.execute(ex)
      }

      {
        val root = Orm.root(classOf[Obj])
        val p = root.leftJoinAs("ptrId", "id", classOf[Ptr])
        val query = Orm.select(root, p).from(root)
        val res = session.query(query)
        Assert.assertEquals(res.length, 1)
        val (obj, ptr) = res(0)
        Assert.assertEquals(obj.id.longValue(), 1L)
        Assert.assertEquals(ptr.id.longValue(), 1L)
      }
      {
        val root = Orm.root(classOf[Obj])
        val o = root.leftJoinAs("id", "objId", classOf[OM])
        val query = Orm.select(root, o).from(root)
        val res = session.query(query)
        Assert.assertEquals(res.length, 2)
        Assert.assertEquals(res(0)._1.toString, res(1)._1.toString)
        Assert.assertEquals(res(0)._2.id.longValue(), 1L)
        Assert.assertEquals(res(1)._2.id.longValue(), 2L)
      }
    })
  }

  @Test
  def testSelectIgnore(): Unit = {
    db.beginTransaction(session => {
      val obj = new Obj()
      obj.name = "name"
      session.execute(Orm.insert(Orm.convert(obj)))

      val root = Orm.root(classOf[Obj])
      root.ignore("name")
      val o = session.first(Orm.selectFrom(root))
      Assert.assertEquals(o.id.longValue(), 1)
      Assert.assertNull(o.name)
    })
  }

  @Test
  def testSelectDeleteById(): Unit = {
    db.beginTransaction(session => {
      val obj = new Obj()
      obj.name = "name"
      obj.oo = new OO
      val ex = Orm.insert(Orm.convert(obj))
      ex.insert("oo")
      session.execute(ex)

      {
        val o = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)(root => {
          root.select("oo")
        })
        Assert.assertEquals(o.name, "name")
        Assert.assertEquals(o.oo.id.longValue(), 1)
      }
      {
        Orm.Tool.deleteByIdEx(classOf[Obj], 1, session)()
        val o = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
        Assert.assertNull(o)
      }
    })
  }

  @Test
  def testAssignAddSub(): Unit = {
    db.beginTransaction(session => {
      val obj = new Obj()
      obj.name = "name"
      obj.doubleValue = 1.5
      obj.age = 10
      session.execute(Orm.insert(Orm.convert(obj)))

      {
        val root = Orm.root(classOf[Obj])
        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("doubleValue").add(1.2))))
        val o = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
        Assert.assertEquals(o.doubleValue.doubleValue(), 2.7, 0.00001)
      }
      {
        val root = Orm.root(classOf[Obj])
        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("doubleValue").sub(1.5))))
        val o = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
        Assert.assertEquals(o.doubleValue.doubleValue(), 1.2, 0.00001)
      }
      {
        val root = Orm.root(classOf[Obj])
        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("age").add(1.2))))
        val o = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
        Assert.assertEquals(o.doubleValue.doubleValue(), 11.2, 0.00001)
      }
      {
        val root = Orm.root(classOf[Obj])
        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("age").sub(1.5))))
        val o = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
        Assert.assertEquals(o.doubleValue.doubleValue(), 8.5, 0.00001)
      }
    })
  }

  @Test
  def testClearField(): Unit = {
    val obj = Orm.obj(classOf[Obj])
    Assert.assertEquals(obj.toString, "{}")
    obj.id = 1L
    Assert.assertEquals(obj.toString, """{id: 1}""")
    Orm.clear(obj, "id")
    Assert.assertEquals(obj.toString, "{}")
  }

  @Test
  def testTransaction(): Unit = {
    try {
      db.beginTransaction(fn = session => {
        val obj = Orm.obj(classOf[Obj])
        obj.name = ""
        session.execute(Orm.insert(obj))
        throw new RuntimeException("Test")
      })
      Assert.assertFalse(true)
    } catch {
      case _: Throwable =>
        db.beginTransaction(session => {
          val obj = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
          Assert.assertNull(obj)
        })
    }
  }

  @Test
  def testTransaction2(): Unit = {
    try {
      db.beginTransaction(fn = session => {
        val obj = Orm.obj(classOf[Obj])
        obj.name = ""
        session.execute(Orm.insertArray(Array(obj)))
        throw new RuntimeException("Test")
      })
      Assert.assertFalse(true)
    } catch {
      case _: Throwable =>
        db.beginTransaction(session => {
          val obj = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
          Assert.assertNull(obj)
        })
    }
  }

  @Test
  def testEnum(): Unit = {
    db.beginTransaction(session => {
      db.check()
      val obj = new Obj
      obj.name = "enum"
      obj.status = "succ"
      session.execute(Orm.insert(Orm.convert(obj)))
      val root = Orm.root(classOf[Obj])
      val res = session.first(Orm.selectFrom(root).where(root.get("status").eql("succ")))
      Assert.assertEquals(res.id.longValue(), 1)
      Assert.assertEquals(res.status, "succ")
    })
  }

  @Test
  def testDefaultValue(): Unit = {
    db.beginTransaction(session => {
      val obj = Orm.obj(classOf[Obj])
      obj.name = "dft"
      session.execute(Orm.insert(obj))
      val res = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
      Assert.assertEquals(res.dftValue.intValue(), 10)
    })
  }

  @Test
  def testOrmToolUpdate(): Unit = {
    db.beginTransaction(session => {
      val obj = Orm.obj(classOf[Obj])
      obj.name = "update"
      session.execute(Orm.insert(obj))
      Orm.Tool.updateById(classOf[Obj], 1, session, ("name", "update2"), ("age", 10))
      val res = Orm.Tool.selectByIdEx(classOf[Obj], 1, session)()
      Assert.assertEquals(res.age.intValue(), 10)
      Assert.assertEquals(res.name, "update2")
    })
  }

  @Test
  def testCrossDb(): Unit = {
    db.beginTransaction(session => {
      val mos = Orm.convert(1.to(5).map(_ => {
        val mo = new MO()
        mo
      }).toArray)
      session.execute(Orm.insertArray(mos))
      val oms = mos.map(mo => {
        val om = Orm.obj(classOf[OM])
        om.subId = 1L
        om.moId = mo.id
        om
      })
      session.execute(Orm.insertArray(oms))
    })
    db.beginTransaction(session => {
      var sub = Orm.obj(classOf[Sub])
      sub.id = 1L
      sub = Orm.Tool.attach(sub, "om", session, join => join.select("mo"), null)
      Assert.assertEquals(sub.om.length, 5)
      Assert.assertEquals(sub.om(0).mo.id.intValue(), 1)
    })
  }

  @Test
  def testText(): Unit = {
    db.beginTransaction(session => {
      val s = 1.to(10000).map(_.toString).mkString("")
      val obj = new Obj
      obj.name = ""
      obj.text = s
      session.execute(Orm.insert(Orm.convert(obj)))

      val root = Orm.root(classOf[Obj])
      val o2 = session.first(Orm.selectFrom(root).where(root.get("id").eql(1)))
      Assert.assertEquals(o2.text, s)
    })
  }

  @Test
  def testShallowEqual(): Unit = {
    db.beginTransaction(session => {
      val now = new Date().getTime
      val obj = new Obj
      obj.name = "name"
      obj.smallIntValue = 100
      obj.tinyIntValue = 10
      obj.longValue = 1000L
      obj.age = 20
      obj.price = java.math.BigDecimal.valueOf(1.5)
      obj.datetimeValue = new DateTime(now)
      obj.birthday = new java.sql.Date(now)
      val obj2 = Orm.convert(obj)
      session.execute(Orm.insert(obj2))
      val obj3 = Orm.Tool.selectById(classOf[Obj], obj2.id, session)
      val core2 = EntityManager.core(obj2)
      val core3 = EntityManager.core(obj3)

      {
        val ret = EntityCore.shallowEqual(core2, core3)
        Assert.assertTrue(ret)
      }
    })
  }

  @Test
  def testUpdateArray(): Unit = {
    db.beginTransaction(session => {
      val obj = Orm.convert(new Obj)
      obj.name = "name"
      obj.om = Orm.convert(Array(new OM, new OM, new OM))
      obj.om(0).value = 10 // delete
      obj.om(1).value = 20 // same
      obj.om(2).value = 30 // update

      {
        val ex = Orm.insert(obj)
        ex.insertArray(_.om)
        session.execute(ex)
      }

      {
        val oms = Array(new OM, new OM, new OM)
        oms(0).id = 2L
        oms(0).value = 20 // same
        oms(0).objId = obj.id

        oms(1).id = 3L
        oms(1).value = 300 // update
        oms(1).objId = obj.id

        oms(2).value = 40 // insert
        oms(2).objId = obj.id

        Orm.Tool.updateArray(Orm.root(classOf[OM]), obj.om, oms, session)
      }

      {
        val obj2 = Orm.Tool.selectByIdEx(classOf[Obj], obj.id, session)(root => root.select(_.om))
        Assert.assertEquals(obj2.om(0).id, 2L)
        Assert.assertEquals(obj2.om(1).id, 3L)
        Assert.assertEquals(obj2.om(2).id, 4L)

        Assert.assertEquals(obj2.om(0).value, 20)
        Assert.assertEquals(obj2.om(1).value, 300)
        Assert.assertEquals(obj2.om(2).value, 40)
      }
    })
  }

  @Test
  def testUpdateByIdAndField(): Unit = {
    db.beginTransaction(session => {
      val obj = Orm.create(classOf[Obj])
      obj.name = "name"
      obj.longValue = 100L
      session.execute(Orm.insert(obj))

      Orm.Tool.updateById(classOf[Obj], obj.id, session)(_.name)("name1")
      val obj1 = Orm.Tool.selectById(classOf[Obj], obj.id, session)
      Assert.assertEquals(obj1.name, "name1")

      Orm.Tool.updateByField(classOf[Obj], session)(_.longValue, _.name)(100, "name2")
      val obj2 = Orm.Tool.selectById(classOf[Obj], obj.id, session)
      Assert.assertEquals(obj2.name, "name2")
    })
  }
}
