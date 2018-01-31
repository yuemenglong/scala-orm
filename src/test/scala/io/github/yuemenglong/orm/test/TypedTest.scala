package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.db.Db
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
      val ex = Orm.insert(Orm.convert(obj))
      ex.insert()(_.om)
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
  def testField(): Unit = db.beginTransaction(session => {
    val obj = new Obj
    obj.name = ""
    obj.om = Array(new OM, new OM, new OM, new OM, new OM, new OM)

    val ex = Orm.insert(Orm.convert(obj))
    ex.insert()(_.om)
    session.execute(ex)

    val root = Orm.root(classOf[Obj])
    val c = session.first(Orm.select(root.join()(_.om).get(_.id).asLong())
      .from(root).limit(1).offset(5))
    Assert.assertEquals(c.intValue(), 6)
  })
}
