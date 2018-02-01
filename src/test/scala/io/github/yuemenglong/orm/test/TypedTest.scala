package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.lang.types.Types._
import io.github.yuemenglong.orm.test.entity._
import org.junit.{After, Assert, Before, Test}

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
    val c = session.first(Orm.select(root.joins(_.om).get(_.id).asLong())
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
      val res = session.first(Orm.select(root.count(_.objId)).from(root))
      Assert.assertEquals(res.longValue(), 3)
    }
    {
      val root = Orm.root(classOf[OM])
      val res = session.first(Orm.select(root.count(_.objId).distinct()).from(root))
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
      ).from(root).groupBy(_.id))
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
        .groupBy(_.id)
        .having(root.count(_.id).gt(1)))
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
      obj.age = 20
      val ret = session.execute(Orm.update(obj))
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
}
