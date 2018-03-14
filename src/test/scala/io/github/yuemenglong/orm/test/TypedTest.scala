package io.github.yuemenglong.orm.test

import java.text.SimpleDateFormat

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.test.entity._
import io.github.yuemenglong.orm.tool.OrmTool
import org.junit.{After, Assert, Before, Test}

/**
  * Created by <yuemenglong@126.com> on 2018/1/31.
  */
class TypedTest {
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

  def openDb(): Db = Orm.openDb("localhost", 3306, "root", "root", "test")

  def openDb2(): Db = Orm.openDb("localhost", 3306, "root", "root", "test2")

  @Test
  def testInsert(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
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
    Assert.assertEquals(r.id, 1L)
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
  })

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
      val ex = Orm.insert(obj)
      ex.insert(_.om)
      session.execute(ex)
    })
    val root = Orm.root(classOf[Obj])
    val cond = root.get(_.id).lt(2)
      .or(root.get(_.id).gt(9))
      .and(root.selects(_.om).get(_.id).gt(2))
    val objs = session.query(Orm.selectFrom(root).where(cond))
    Assert.assertEquals(objs.length, 2)
    Assert.assertEquals(objs(0).om.length, 1)
    Assert.assertEquals(objs(1).om.length, 3)
  })

  @Test
  def testNotNull(): Unit = db.beginTransaction(session => {
    var obj = new Obj
    obj.name = "name"
    session.execute(Orm.insert(Orm.convert(obj)))

    obj = new Obj
    obj.name = "name2"
    obj.age = 10
    session.execute(Orm.insert(Orm.convert(obj)))

    val root = Orm.root(classOf[Obj])
    var res = session.query(Orm.selectFrom(root).where(root.get(_.age).isNull))
    Assert.assertEquals(res.length, 1)
    Assert.assertEquals(res(0).id.longValue(), 1)
    Assert.assertEquals(res(0).age, null)

    res = session.query(Orm.selectFrom(root).where(root.get(_.age).notNull()))
    Assert.assertEquals(res.length, 1)
    Assert.assertEquals(res(0).id.longValue(), 2)
    Assert.assertEquals(res(0).age, 10)
  })

  @Test
  def testCount(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.om = Array(new OM, new OM, new OM, new OM, new OM, new OM)
    val ex = Orm.insert(Orm.convert(obj))
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.inserts(_.om)
    val ret = session.execute(ex)
    Assert.assertEquals(ret, 7)

    val root = Orm.root(classOf[OM])
    val c = session.first(Orm.select(root.count()).from(root))
    Assert.assertEquals(c.longValue(), 6)
  })

  @Test
  def testSelectField(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.om = Array(new OM, new OM, new OM, new OM, new OM, new OM)

    val ex = Orm.insert(obj)
    ex.insert(_.om)
    session.execute(ex)

    val root = Orm.root(classOf[Obj])
    val c = session.first(Orm.select(root.joins(_.om).get(_.id))
      .from(root).limit(1).offset(5))
    Assert.assertEquals(c.intValue(), 6)
  })

  @Test
  def testMultiTarget(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = "name"
    obj.ptr = new Ptr
    obj.oo = new OO
    obj.om = Array(new OM, new OM)
    val ex = Orm.insert(obj)
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.insert(_.om)
    session.execute(ex)

    val root = Orm.root(classOf[Obj])
    val s1 = root.joins(_.om).as()
    val res = session.query(Orm.select(root, s1).from(root))
    Assert.assertEquals(res.length, 2)
    Assert.assertEquals(res(0)._1.name, "name")
    Assert.assertEquals(res(0)._1.id.longValue(), 1)
    Assert.assertEquals(res(1)._1.id.longValue(), 1)
    Assert.assertEquals(res(0)._2.id.longValue(), 1)
    Assert.assertEquals(res(1)._2.id.longValue(), 2)
  })

  @Test
  def testJoin(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = "name"
    obj.ptr = new Ptr
    obj.oo = new OO
    obj.om = Array(new OM, new OM)
    obj.om(0).mo = new MO
    obj.om(0).mo.value = 100
    val ex = Orm.insert(obj)
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.inserts(_.om).insert(_.mo)
    val ret = session.execute(ex)
    Assert.assertEquals(ret, 6)

    {
      val root = Orm.root(classOf[Obj])
      val mo = root.joins(_.om).leftJoin(_.mo).as()
      val res = session.query(Orm.select(root, mo).from(root))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0)._1.om, null)
      Assert.assertEquals(res(0)._2.value.intValue(), 100)
      Assert.assertEquals(res(1)._2, null)
    }
    {
      val root = Orm.root(classOf[OM])
      root.select(_.mo).on(root.leftJoin(_.mo).get(_.value).eql(100))
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0).mo.value.longValue(), 100)
      Assert.assertEquals(res(1).mo, null)
    }
  })

  @Test
  def testSelectOOWithNull(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = "name"
    session.execute(Orm.insert(obj))

    val root = Orm.root(classOf[Obj])
    root.select(_.ptr)
    root.select(_.oo)
    root.select(_.om)
    val res = session.query(Orm.selectFrom(root))
    Assert.assertEquals(res.length, 1)
    Assert.assertEquals(res(0).ptr, null)
    Assert.assertEquals(res(0).oo, null)
    Assert.assertEquals(res(0).om.length, 0)
  })

  @Test
  def testDistinctCount(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = "name"
    obj.ptr = new Ptr
    obj.oo = new OO
    obj.om = Array(new OM, new OM, new OM)
    val ex = Orm.insert(obj)
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.inserts(_.om).insert(_.mo)
    val ret = session.execute(ex)
    Assert.assertEquals(ret, 6)

    {
      val root = Orm.root(classOf[OM])
      val res = session.first(Orm.select(root.count(root.get(_.objId))).from(root))
      Assert.assertEquals(res.longValue(), 3)
    }
    {
      val root = Orm.root(classOf[OM])
      val res = session.first(Orm.select(root.count(root.get(_.objId)).distinct()).from(root))
      Assert.assertEquals(res.longValue(), 1)
    }
    {
      val root = Orm.root(classOf[OM])
      val res = session.first(Orm.select(root.count()).from(root))
      Assert.assertEquals(res.longValue(), 3)
    }
  })

  @Test
  def testTarget(): Unit = db.beginTransaction(session => {
    (0 until 2).foreach(i => {
      val obj = new Obj
      obj.name = s"name${i}"
      val ret = session.execute(Orm.insert(obj))
      Assert.assertEquals(ret, 1)
    })

    val root = Orm.root(classOf[Obj])
    val id = root.get(_.id)
    val name = root.get(_.name)
    val res = session.query(Orm.select(id, name).from(root))
    Assert.assertEquals(res.length, 2)
    Assert.assertEquals(res(0)._1.longValue(), 1)
    Assert.assertEquals(res(1)._1.longValue(), 2)
    Assert.assertEquals(res(0)._2, "name0")
    Assert.assertEquals(res(1)._2, "name1")
  })

  @Test
  def testSumGroupBy(): Unit = db.beginTransaction(session => {
    {
      val obj = new Obj
      obj.name = "name0"
      obj.om = Array(new OM)
      val ex = Orm.insert(obj)
      ex.insert(_.om)
      val ret = session.execute(ex)
      Assert.assertEquals(ret, 2)
    }
    {
      val obj = new Obj
      obj.name = "name1"
      obj.om = Array(new OM, new OM)
      val ex = Orm.insert(obj)
      ex.insert(_.om)
      val ret = session.execute(ex)
      Assert.assertEquals(ret, 3)
    }
    {
      val root = Orm.root(classOf[Obj])
      val res = session.first(Orm.select(root.sum(root.joins(_.om).get(_.id))).from(root))
      Assert.assertEquals(res.longValue(), 6)
    }
    {
      val root = Orm.root(classOf[Obj])
      val om = root.joins(_.om)
      val res = session.query(Orm.select(
        root.get(_.id),
        root.sum(om.get(_.id)),
        root.count(om.get(_.id))
      ).from(root).groupBy(root.get(_.id)))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0)._1.longValue(), 1)
      Assert.assertEquals(res(0)._2.longValue(), 1)
      Assert.assertEquals(res(0)._3.longValue(), 1)
      Assert.assertEquals(res(1)._1.longValue(), 2)
      Assert.assertEquals(res(1)._2.longValue(), 5)
      Assert.assertEquals(res(1)._3.longValue(), 2)
    }
    {
      val root = Orm.root(classOf[Obj])
      val res = session.query(Orm.select(
        root.get(_.id),
        root.sum(root.joins(_.om).get(_.id)),
        root.count(root.joins(_.om).get(_.id))
      ).from(root)
        .groupBy(root.get(_.id))
        .having(root.count(root(_.id)).gt(1)))
      Assert.assertEquals(res.length, 1)
      Assert.assertEquals(res(0)._1.longValue(), 2)
      Assert.assertEquals(res(0)._2.longValue(), 5)
      Assert.assertEquals(res(0)._3.longValue(), 2)
    }
  })

  @Test
  def testCurd(): Unit = db.beginTransaction(session => {
    val longText = "loooooooooooooooooooooooooooooooooooooooooooooooooooong"
    val obj = new Obj

    obj.age = 10
    obj.name = "/TOM"
    obj.nowTime = new Date
    obj.doubleValue = 1.2
    obj.price = new BigDecimal(123.45)
    obj.longText = longText

    obj.ptr = new Ptr
    obj.ptr.value = 10
    obj.oo = new OO
    obj.oo.value = 100
    obj.om = Array(new OM, new OM)
    obj.om(0).value = 1000
    obj.om(1).value = 2000

    obj.ignValue = 0
    obj.ign = new Ign

    {
      val ex = Orm.insert(obj)
      ex.insert(_.ptr)
      ex.insert(_.oo)
      ex.insert(_.om)
      val ret = session.execute(ex)
      Assert.assertEquals(ret, 5)
      Assert.assertEquals(ex.root().id.longValue(), 1)
      obj.id = ex.root().id
    }

    {
      val o = Orm.empty(classOf[Obj])
      o.id = obj.id
      o.age = 20
      val ret = session.execute(Orm.update(o))
      Assert.assertEquals(ret, 1)
    }

    {
      val root = Orm.root(classOf[Obj])
      root.select(_.ptr)
      root.select(_.oo)
      root.select(_.om)

      val res = session.query(Orm.selectFrom(root).where(root.get(_.id).in(Array(1, 2))))
      Assert.assertEquals(res.length, 1)
      Assert.assertEquals(res(0).id.intValue(), 1)
      Assert.assertEquals(res(0).longText, longText)
      Assert.assertEquals(res(0).doubleValue, 1.2, 0.000000001)
      Assert.assertEquals(res(0).age.intValue(), 20)
      Assert.assertEquals(res(0).ptr.value.intValue(), 10)
      Assert.assertEquals(res(0).oo.value.intValue(), 100)
      Assert.assertEquals(res(0).om(0).value.intValue(), 1000)
      Assert.assertEquals(res(0).om(1).value.intValue(), 2000)
    }

    {
      val ret = session.execute(Orm.delete(obj))
      Assert.assertEquals(ret, 1)
    }
  })

  @Test
  def testUpdateSpec(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = "name"
    obj.age = 100
    val ex = Orm.insert(obj)
    session.execute(ex)

    {
      val obj = Orm.empty(classOf[Obj])
      obj.id = ex.root().id
      obj.age = 200
      session.execute(Orm.update(obj))
    }
    {
      val root = Orm.root(classOf[Obj])
      val obj = session.first(Orm.selectFrom(root))
      Assert.assertEquals(obj.name, "name")
      Assert.assertEquals(obj.age.longValue(), 200)
    }
  })

  @Test
  def testDeleteRefer(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.ptr = new Ptr
    obj.oo = new OO
    obj.om = Array(new OM, new OM)

    val ex = Orm.insert(obj)
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.insert(_.om)
    val ret = session.execute(ex)
    Assert.assertEquals(ret, 5)

    {
      val root = Orm.root(classOf[Obj])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 1)
    }
    {
      val root = Orm.root(classOf[Ptr])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 1)
    }
    {
      val root = Orm.root(classOf[OO])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 1)
    }
    {
      val root = Orm.root(classOf[OM])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 2)
    }

    {
      val dex = Orm.delete(ex.root())
      dex.delete(_.ptr)
      dex.delete(_.oo)
      dex.delete(_.om)
      session.execute(dex)
    }

    {
      val root = Orm.root(classOf[Obj])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 0)
    }
    {
      val root = Orm.root(classOf[Ptr])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 0)
    }
    {
      val root = Orm.root(classOf[OO])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 0)
    }
    {
      val root = Orm.root(classOf[OM])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 0)
    }
  })

  @Test
  def testOrderByLimitOffset(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.om = (1 to 6).map(_ => new OM).toArray

    val ex = Orm.insert(obj)
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.insert(_.om)
    val ret = session.execute(ex)
    Assert.assertEquals(ret, 7)

    val root = Orm.root(classOf[OM])
    val res = session.query(Orm.selectFrom(root)
      .desc(root.get(_.id)).limit(3).offset(2))
    Assert.assertEquals(res.length, 3)
    Assert.assertEquals(res(0).id.intValue(), 4)
    Assert.assertEquals(res(1).id.intValue(), 3)
    Assert.assertEquals(res(2).id.intValue(), 2)
  })

  @Test
  def testUpdate(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.om = (1 to 6).map(_ => new OM).toArray
    val ex = Orm.insert(obj)
    ex.insert(_.ptr)
    ex.insert(_.oo)
    ex.insert(_.om)

    {
      val ret = session.execute(ex)
      Assert.assertEquals(ret, 7)
    }

    {
      val root = Orm.root(classOf[OM])
      val ret = session.execute(Orm.update(root)
        .set(root.get(_.objId).assign(2))
        .where(root.get(_.id).gt(4)))
      Assert.assertEquals(ret, 2)
    }

    {
      val obj = Orm.empty(classOf[Obj])
      obj.name = "name2"
      obj.age = 10
      session.execute(Orm.insert(obj))

      {
        val root = Orm.root(classOf[Obj])
        session.execute(Orm.update(root).set(
          root.get(_.age) := null,
          root.get(_.doubleValue) := 20.0
        ).where(root.get(_.id) === obj.id))
      }
      {
        val root = Orm.root(classOf[Obj])
        val o = session.first(Orm.selectFrom(root).where(root.get(_.id) === obj.id))
        Assert.assertEquals(o.doubleValue, 20.0)
        Assert.assertNull(o.age)
      }
    }
    {
      val root = Orm.root(classOf[Obj])
      root.selects(_.om)
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0).om.length, 4)
      Assert.assertEquals(res(1).om.length, 2)
    }
  })

  @Test
  def testDelete(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.om = (1 to 6).map(_ => new OM).toArray

    (1 to 2).foreach(_ => {
      val ex = Orm.insert(obj)
      ex.insert(_.ptr)
      ex.insert(_.oo)
      ex.insert(_.om)
      val ret = session.execute(ex)
      Assert.assertEquals(ret, 7)
    })

    val root = Orm.root(classOf[OM])
    val ret = session.execute(
      Orm.deleteFrom(root).where(
        root.join(_.obj).get(_.id).gt(1)
          .or(root.get(_.id).lt(3))))
    Assert.assertEquals(ret, 8)
  })

  @Test
  def testBatchInsert(): Unit = db.beginTransaction(session => {
    val objs = (1 to 3).map(i => {
      val obj = Orm.empty(classOf[Obj])
      obj.name = i.toString
      obj
    }).toArray
    val ret = session.execute(Orm.inserts(objs))
    Assert.assertEquals(ret, 3)
    Assert.assertEquals(objs(0).id.intValue(), 1)
    Assert.assertEquals(objs(1).id.intValue(), 2)
    Assert.assertEquals(objs(2).id.intValue(), 3)
    Assert.assertEquals(objs(0).name, "1")
    Assert.assertEquals(objs(1).name, "2")
    Assert.assertEquals(objs(2).name, "3")
  })

  @Test
  def transactionTest(): Unit = {
    try {
      db.beginTransaction(session => {
        val obj = new Obj
        obj.name = ""
        session.execute(Orm.insert(obj))
        val root = Orm.root(classOf[Obj])
        val res = session.query(Orm.selectFrom(root))
        Assert.assertEquals(res.length, 1)
        throw new RuntimeException("ROLL BACK")
      })
    } catch {
      case _: Throwable =>
    }
    db.beginTransaction(session => {
      val root = Orm.root(classOf[Obj])
      val res = session.query(Orm.selectFrom(root))
      Assert.assertEquals(res.length, 0)
    })
  }

  @Test
  def dateTimeTest(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.nowTime = new SimpleDateFormat("yyyy-MM-dd").parse("2017-12-12")
    session.execute(Orm.insert(obj))

    {
      val root = Orm.root(classOf[Obj])
      val obj = session.first(Orm.selectFrom(root).where(root.get(_.id).eql(1)))
      Assert.assertEquals(new SimpleDateFormat("yyyy-MM-dd").format(obj.nowTime), "2017-12-12")
      Assert.assertEquals(obj.nowTime.getClass.getName, "java.sql.Timestamp")
    }
  })

  @Test
  def likeTest(): Unit = db.beginTransaction(session => {
    {
      val obj = new Obj
      obj.name = "like it"
      session.execute(Orm.insert(obj))
    }
    {
      val obj = new Obj
      obj.name = "dont like"
      session.execute(Orm.insert(obj))
    }
    {
      val root = Orm.root(classOf[Obj])
      val res = session.query(Orm.selectFrom(root).where(root.get(_.name).like("like%")))
      Assert.assertEquals(res.length, 1)
      Assert.assertEquals(res(0).name, "like it")
    }
    {
      val root = Orm.root(classOf[Obj])
      val res = session.query(Orm.selectFrom(root).where(root.get(_.name).like("%like")))
      Assert.assertEquals(res.length, 1)
      Assert.assertEquals(res(0).name, "dont like")
    }
  })

  @Test
  def testOrmTool(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.om = Array(new OM, new OM, new OM).map(om => {
      om.mo = new MO
      om
    })
    val ex = Orm.insert(obj)
    ex.inserts(_.om).insert(_.mo)
    session.execute(ex)

    {
      val obj = ex.root()
      obj.om = Array()
      OrmTool.attach(obj, session)(_.om)
      Assert.assertEquals(obj.om.length, 3)
    }
    {
      val obj = ex.root()
      obj.om = Array()
      OrmTool.attachsx(obj, session)(_.om)(j => {
        j.select(_.mo)
      }, null)
      Assert.assertEquals(obj.om.length, 3)
      Assert.assertEquals(obj.om(0).mo.id.intValue(), 1)
      Assert.assertEquals(obj.om(1).mo.id.intValue(), 2)
      Assert.assertEquals(obj.om(2).mo.id.intValue(), 3)
    }
    {
      val obj = ex.root()
      obj.om.foreach(om => {
        Orm.clear(om)(_.mo)
        Assert.assertNull(om.mo)
      })

      OrmTool.sattach(obj.om, session)(_.mo)
      Assert.assertEquals(obj.om(0).mo.id.intValue(), 1)
      Assert.assertEquals(obj.om(1).mo.id.intValue(), 2)
      Assert.assertEquals(obj.om(2).mo.id.intValue(), 3)
    }
    {
      val obj = ex.root()
      OrmTool.updateById(classOf[Obj], obj.id, session)(_.name, _.age)("name2", 10)
    }
    {
      val obj = ex.root()
      val res = OrmTool.selectById(classOf[Obj], obj.id, session)(root => {
        root.select(_.om)
      })
      Assert.assertEquals(res.om.length, 3)
      Assert.assertEquals(res.name, "name2")
      Assert.assertEquals(res.age, 10)
    }
    {
      val obj = ex.root()
      OrmTool.deleteById(classOf[Obj], obj.id, session)(root => {
        Array(root.leftJoin(_.om))
      })
      val res = OrmTool.selectById(classOf[Obj], obj.id, session)(root => {
        root.select(_.om)
      })
      Assert.assertNull(res)
      val root = Orm.root(classOf[OM])
      val count = session.first(Orm.select(root.count()).from(root))
      Assert.assertEquals(count.intValue(), 0)
    }
  })

  @Test
  def testCondEx(): Unit = db.beginTransaction(session => {
    {
      (1 to 3).foreach(f = i => {
        val obj = new Obj
        obj.name = i.toString
        val ex = Orm.insert(obj)
        session.execute(ex)
      })
    }
    {
      val root = Orm.root(classOf[Obj])
      val cond = (root.get(_.id) === 1).or(root.get(_.id) > 2)
      val res = session.query(Orm.selectFrom(root).where(cond))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0).name, "1")
      Assert.assertEquals(res(1).name, "3")
    }
    {
      val root = Orm.root(classOf[Obj])
      val cond = (root.get(_.id) <= 2).or(root.get(_.id) !== 3)
      val res = session.query(Orm.selectFrom(root).where(cond))
      Assert.assertEquals(res.length, 2)
      Assert.assertEquals(res(0).name, "1")
      Assert.assertEquals(res(1).name, "2")
    }
  })

  @Test
  def testSubQuery(): Unit = db.beginTransaction(session => {
    {
      val obj = new Obj
      obj.name = "name"
      obj.ptr = new Ptr
      obj.oo = new OO
      obj.om = Array(new OM, new OM, new OM)
      val ex = Orm.insert(obj)
      ex.insert(_.ptr)
      ex.insert(_.oo)
      ex.insert(_.om)
      session.execute(ex)
    }

    {
      val r = Orm.root(classOf[Obj])
      r.fields(_.name)
      val sr = r.subRoot(classOf[OM])
      val query = Orm.selectFrom(r).where(r(_.id).in(
        Orm.subSelect(sr(_.id)).from(sr)
      ))
      val res = session.query(query)
      Assert.assertEquals(res.length, 1)
    }
    {
      val r = Orm.root(classOf[OM])
      val sr = r.subRoot(classOf[Obj])
      val query = Orm.selectFrom(r).where(r(_.objId).===(
        Orm.subSelect(sr(_.id)).from(sr).where(sr.get(_.id).===(1))
      ))
      val res = session.query(query)
      Assert.assertEquals(res.length, 3)
    }
    {
      val r = Orm.root(classOf[OM])
      val sr = r.subRoot(classOf[Obj])
      val query = Orm.selectFrom(r).where(r.get(_.id).>(
        Orm.subSelect(sr.get(_.id)).from(sr).all
      ))
      val res = session.query(query)
      Assert.assertEquals(res(0).id.intValue(), 2)
      Assert.assertEquals(res(1).id.intValue(), 3)
    }
    {
      val r = Orm.root(classOf[OM])
      val sr = r.subRoot(classOf[Obj])
      val query = Orm.selectFrom(r).where(r.exists(
        Orm.subSelect(sr.get(_.id)).from(sr).where(sr.get(_.id).eql(r.get(_.objId)))
      ))
      val res = session.query(query)
      Assert.assertEquals(res.length, 3)
    }
  })

  @Test
  def testAssignEx(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.age = 10
    session.execute(Orm.insert(obj))

    val root = Orm.root(classOf[Obj])
    session.execute(Orm.update(root).set(root(_.age) := (root(_.age) + 10)))

    val res = session.first(Orm.select(root(_.age)).from(root))
    Assert.assertEquals(res.intValue(), 20)
  })
}
