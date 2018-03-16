//package io.github.yuemenglong.orm.test
//
//import java.io.File
//import java.util.Date
//
//import io.github.yuemenglong.orm.Orm
//import io.github.yuemenglong.orm.db.Db
//import io.github.yuemenglong.orm.lang.interfaces.Entity
//import io.github.yuemenglong.orm.test.entity._
//import io.github.yuemenglong.orm.tool.OrmTool
//import org.junit.{After, Assert, Before, Test}
//
///**
//  * Created by <yuemenglong@126.com> on 2017/10/19.
//  */
//class ScalaTest {
//  private var db: Db = _
//  private var db2: Db = _
//
//  @SuppressWarnings(Array("Duplicates"))
//  @Before def before(): Unit = {
//    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
//    Orm.init("io.github.yuemenglong.orm.test.entity")
//    db = openDb()
//    db.rebuild()
//    db.check()
//
//    db2 = openDb2()
//    db2.rebuild()
//    db2.check()
//  }
//
//  @After def after(): Unit = {
//    Orm.reset()
//    db.shutdown()
//    db2.shutdown()
//  }
//
//  def openDb(): Db = Orm.openDb("localhost", 3306, "root", "root", "test")
//
//  def openDb2(): Db = Orm.openDb("localhost", 3306, "root", "root", "test2")
//
//  @Test
//  def testConnPool(): Unit = {
//    for (_ <- 0.until(1000)) {
//      db.beginTransaction(session => {
//        val root = Orm.root(classOf[Obj])
//        session.query(Orm.selectFrom(root))
//      })
//    }
//  }
//
//  @Test
//  def testIn(): Unit = {
//    db.beginTransaction(session => {
//      1.to(4).foreach(i => {
//        val obj = new Obj()
//        obj.name = i.toString
//        session.execute(Orm.insert(Orm.convert(obj)))
//      })
//
//      {
//        val root = Orm.root(classOf[Obj])
//        val res = session.query(Orm.select(root).from(root).where(root.get("id").in(Array(1, 2))))
//        Assert.assertEquals(res.length, 2)
//        Assert.assertEquals(res(0).id.intValue(), 1)
//        Assert.assertEquals(res(1).id.intValue(), 2)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        val res = session.query(Orm.select(root).from(root).where(root.get("id").nin(Array(3, 4))))
//        Assert.assertEquals(res.length, 2)
//        Assert.assertEquals(res(0).id.intValue(), 1)
//        Assert.assertEquals(res(1).id.intValue(), 2)
//      }
//    })
//  }
//
//  @Test
//  def testSpecField(): Unit = {
//    db.beginTransaction(session => {
//      val obj = new Obj()
//      obj.name = "name"
//      obj.age = 10
//      session.execute(Orm.insert(Orm.convert(obj)))
//
//      val root = Orm.root(classOf[Obj])
//      root.fields("name")
//      val res = session.first(Orm.select(root).from(root))
//      Assert.assertNull(res.age)
//      Assert.assertEquals(res.name, "name")
//      Assert.assertEquals(res.id.intValue(), 1L)
//    })
//  }
//
//  @Test
//  def testIgnoreField(): Unit = {
//    db.beginTransaction(session => {
//      {
//        val obj = new Obj()
//        obj.name = "name"
//        obj.age = 10
//        obj.birthday = new Date()
//        obj.oo = new OO
//        obj.oo.value = 10
//        val ex = Orm.insert(Orm.convert(obj))
//        ex.insert("oo")
//        ex.ignore("age")
//        session.execute(ex)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        root.select("oo")
//        val obj = session.first(Orm.select(root).from(root))
//        Assert.assertNull(obj.age)
//        Assert.assertEquals(obj.name, "name")
//        Assert.assertEquals(obj.oo.value, 10)
//
//        {
//          obj.name = "name1"
//          obj.oo.value = 20
//          val ex = Orm.update(Orm.convert(obj))
//          ex.update("oo").ignore("value")
//          session.execute(ex)
//        }
//        {
//          val root = Orm.root(classOf[Obj])
//          root.select("oo")
//          val obj = session.first(Orm.select(root).from(root))
//          Assert.assertEquals(obj.name, "name1")
//          Assert.assertEquals(obj.oo.value, 10)
//        }
//      }
//    })
//  }
//
//  @Test
//  def testMinMax(): Unit = {
//    db.beginTransaction(session => {
//      1.to(10).foreach(i => {
//        val obj = new Obj
//        obj.name = i.toString
//        session.execute(Orm.insert(Orm.convert(obj)))
//      })
//      val root = Orm.root(classOf[Obj])
//      val query = Orm.select(root.max(root.get("id"), classOf[java.lang.Long]),
//        root.min(root.get("id"), classOf[java.lang.Long])).from(root)
//      val (max, min) = session.first(query)
//      Assert.assertEquals(max.intValue(), 10)
//      Assert.assertEquals(min.intValue(), 1)
//    })
//  }
//
//  @Test
//  def testAttach(): Unit = {
//    db.beginTransaction(session => {
//      var obj = new Obj
//      obj.name = ""
//      obj.om = 1.to(5).map(_ => {
//        val om = new OM()
//        om.mo = new MO
//        om
//      }).toArray
//      obj.oo = new OO
//      obj = Orm.convert(obj)
//      val ex = Orm.insert(obj)
//      ex.insert("om").insert("mo")
//      ex.insert("oo")
//      session.execute(ex)
//      obj.om = Array()
//      obj = OrmTool.attach(obj, "om", session, join => join.select("mo"), null)
//      Assert.assertEquals(obj.om.length, 5)
//      Assert.assertEquals(obj.om(0).mo.id.intValue(), 1)
//      obj = OrmTool.attach[Obj](obj, "oo", session)
//      Assert.assertEquals(obj.oo.id.longValue(), 1)
//
//      obj.om.foreach(om => {
//        om.asInstanceOf[Entity].$$core().fieldMap += ("mo" -> null)
//      })
//      OrmTool.attach(obj.om, "mo", session).zipWithIndex.foreach { case (om, idx) =>
//        Assert.assertEquals(om.id.longValue(), idx + 1)
//      }
//    })
//  }
//
//  @Test
//  def testConvert(): Unit = db.beginTransaction(session => {
//    val obj = Orm.convert(new Obj)
//    obj.name = "Age10"
//    obj.age = 10
//    session.execute(Orm.insert(obj))
//    val root = Orm.root(classOf[Obj])
//    val res = session.first(Orm.selectFrom(root).where(root.get("age").eql(10)))
//    Assert.assertEquals(res.name, "Age10")
//  })
//
//  @Test
//  def testIgnore(): Unit = db.beginTransaction(session => {
//    val obj = Orm.convert(new Obj)
//    obj.name = "Tom"
//    obj.age = 100
//    obj.doubleValue = 10.0
//    session.execute(Orm.insert(obj))
//
//    {
//      val root = Orm.root(classOf[Obj])
//      val res = session.first(Orm.selectFrom(root))
//      res.name = "Jack"
//      res.age = 0
//      res.doubleValue = 20.0
//      session.execute(Orm.update(res).ignore("name", "age"))
//    }
//    {
//      val root = Orm.root(classOf[Obj])
//      val res = session.first(Orm.selectFrom(root))
//      Assert.assertEquals(res.age.intValue(), 100)
//      Assert.assertEquals(res.name, "Tom")
//      Assert.assertEquals(res.doubleValue.doubleValue(), 20.0, 0.00001)
//    }
//  })
//
//  @Test
//  def testDeleteCascade(): Unit = {
//    db.beginTransaction(session => {
//      var obj = new Obj
//
//      {
//        obj.name = ""
//        obj.oo = new OO
//        obj.om = Array(new OM, new OM)
//        obj.om(0).mo = new MO
//        obj = Orm.convert(obj)
//        val ex = Orm.insert(obj)
//        ex.insert("oo")
//        ex.insert("om").insert("mo")
//        session.execute(ex)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        val ex = Orm.delete(
//          root,
//          root.leftJoin("oo"),
//          root.leftJoin("om"),
//          root.leftJoin("om").leftJoin("mo")
//        ).from(root).where(root.get("id").eql(1))
//        val ret = session.execute(ex)
//        Assert.assertEquals(ret, 5)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        root.select("oo")
//        root.select("om").select("mo")
//        val query = Orm.select(root.count()).from(root)
//        val ret = session.first(query)
//        Assert.assertEquals(ret.longValue(), 0)
//      }
//    })
//  }
//
//  @Test
//  def testDeleteCascadeCond(): Unit = {
//    db.beginTransaction(session => {
//      var obj = new Obj
//
//      {
//        obj.name = ""
//        obj.oo = new OO
//        obj.om = Array(new OM, new OM)
//        obj.om(0).mo = new MO
//        obj = Orm.convert(obj)
//        val ex = Orm.insert(obj)
//        ex.insert("oo")
//        ex.insert("om").insert("mo")
//        session.execute(ex)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        val ex = Orm.delete(
//          root.leftJoin("oo")
//        ).from(root).where(root.get("id").eql(1))
//        session.execute(ex)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        root.select("oo")
//        val query = Orm.selectFrom(root)
//        val obj = session.first(query)
//        Assert.assertNotNull(obj)
//        Assert.assertNull(obj.oo)
//      }
//    })
//  }
//
//  @Test
//  def testCascadeUpdate(): Unit = {
//    db.beginTransaction(session => {
//      var obj = new Obj
//
//      {
//        obj.name = ""
//        obj.oo = new OO
//        obj.om = Array(new OM, new OM)
//        obj.om(0).mo = new MO
//        obj = Orm.convert(obj)
//        val ex = Orm.insert(obj)
//        ex.insert("oo")
//        ex.insert("om").insert("mo")
//        session.execute(ex)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        val ex = Orm.update(root).set(
//          root.get("name").assign("Tom"),
//          root.leftJoin("oo").get("value").assign(100),
//          root.leftJoin("om").get("value").assign(200)
//        ).where(root.get("id").eql(obj.id).and(
//          root.leftJoin("om").get("id").eql(1)))
//        session.execute(ex)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        root.select("oo")
//        root.select("om")
//        val query = Orm.selectFrom(root).where(root.get("id").eql(1))
//        val obj = session.first(query)
//        Assert.assertEquals(obj.name, "Tom")
//        Assert.assertEquals(obj.oo.value.intValue(), 100)
//        Assert.assertEquals(obj.om(0).value.intValue(), 200)
//        Assert.assertEquals(obj.om(1).value, null)
//      }
//    })
//  }
//
//  @Test
//  def testExport(): Unit = {
//    OrmTool.exportTsClass("export2.ts")
//    new File("eport2.ts").deleteOnExit()
//  }
//
//  @Test
//  def testFields(): Unit = {
//    db.beginTransaction(session => {
//      val obj = new Obj
//      obj.name = "name"
//      obj.age = 10
//      val ex = Orm.insert(Orm.convert(obj))
//      ex.fields("name")
//      session.execute(ex)
//
//      val obj2 = session.first(Orm.selectFrom(Orm.root(classOf[Obj])))
//      Assert.assertEquals(obj2.name, "name")
//      Assert.assertNull(obj2.age)
//
//      obj2.name = "name2"
//      obj2.age = 20
//      val ex2 = Orm.update(obj2)
//      ex2.fields("age")
//      session.execute(ex2)
//
//      val obj3 = session.first(Orm.selectFrom(Orm.root(classOf[Obj])))
//      Assert.assertEquals(obj3.name, "name")
//      Assert.assertEquals(obj3.age.intValue(), 20)
//    })
//  }
//
//  @Test
//  def testJoinAs(): Unit = {
//    db.beginTransaction(session => {
//      {
//        val obj = new Obj()
//        obj.name = ""
//        obj.ptr = new Ptr
//        obj.oo = new OO
//        obj.om = Array(new OM, new OM)
//        val ex = Orm.insert(Orm.convert(obj))
//        ex.insert("ptr")
//        ex.insert("oo")
//        ex.insert("om")
//        session.execute(ex)
//      }
//
//      {
//        val root = Orm.root(classOf[Obj])
//        val p = root.leftJoinAs("ptrId", "id", classOf[Ptr])
//        val query = Orm.select(root, p).from(root)
//        val res = session.query(query)
//        Assert.assertEquals(res.length, 1)
//        val (obj, ptr) = res(0)
//        Assert.assertEquals(obj.id.longValue(), 1L)
//        Assert.assertEquals(ptr.id.longValue(), 1L)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        val o = root.leftJoinAs("id", "objId", classOf[OM])
//        val query = Orm.select(root, o).from(root)
//        val res = session.query(query)
//        Assert.assertEquals(res.length, 2)
//        Assert.assertEquals(res(0)._1.toString, res(1)._1.toString)
//        Assert.assertEquals(res(0)._2.id.longValue(), 1L)
//        Assert.assertEquals(res(1)._2.id.longValue(), 2L)
//      }
//    })
//  }
//
//  @Test
//  def testSelectIgnore(): Unit = {
//    db.beginTransaction(session => {
//      val obj = new Obj()
//      obj.name = "name"
//      session.execute(Orm.insert(Orm.convert(obj)))
//
//      val root = Orm.root(classOf[Obj])
//      root.ignore("name")
//      val o = session.first(Orm.selectFrom(root))
//      Assert.assertEquals(o.id.longValue(), 1)
//      Assert.assertNull(o.name)
//    })
//  }
//
//  @Test
//  def testSelectDeleteById(): Unit = {
//    db.beginTransaction(session => {
//      val obj = new Obj()
//      obj.name = "name"
//      obj.oo = new OO
//      val ex = Orm.insert(Orm.convert(obj))
//      ex.insert("oo")
//      session.execute(ex)
//
//      {
//        val o = OrmTool.selectById(classOf[Obj], 1, session)(root => {
//          root.select("oo")
//        })
//        Assert.assertEquals(o.name, "name")
//        Assert.assertEquals(o.oo.id.longValue(), 1)
//      }
//      {
//        OrmTool.deleteById(classOf[Obj], 1, session)()
//        val o = OrmTool.selectById(classOf[Obj], 1, session)()
//        Assert.assertNull(o)
//      }
//    })
//  }
//
//  @Test
//  def testAssignAddSub(): Unit = {
//    db.beginTransaction(session => {
//      val obj = new Obj()
//      obj.name = "name"
//      obj.doubleValue = 1.5
//      obj.age = 10
//      session.execute(Orm.insert(Orm.convert(obj)))
//
//      {
//        val root = Orm.root(classOf[Obj])
//        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("doubleValue").add(1.2))))
//        val o = OrmTool.selectById(classOf[Obj], 1, session)()
//        Assert.assertEquals(o.doubleValue.doubleValue(), 2.7, 0.00001)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("doubleValue").sub(1.5))))
//        val o = OrmTool.selectById(classOf[Obj], 1, session)()
//        Assert.assertEquals(o.doubleValue.doubleValue(), 1.2, 0.00001)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("age").add(1.2))))
//        val o = OrmTool.selectById(classOf[Obj], 1, session)()
//        Assert.assertEquals(o.doubleValue.doubleValue(), 11.2, 0.00001)
//      }
//      {
//        val root = Orm.root(classOf[Obj])
//        session.execute(Orm.update(root).set(root.get("doubleValue").assign(root.get("age").sub(1.5))))
//        val o = OrmTool.selectById(classOf[Obj], 1, session)()
//        Assert.assertEquals(o.doubleValue.doubleValue(), 8.5, 0.00001)
//      }
//    })
//  }
//
//  @Test
//  def testClearField(): Unit = {
//    val obj = Orm.empty(classOf[Obj])
//    Assert.assertEquals(obj.toString, "{}")
//    obj.id = 1L
//    Assert.assertEquals(obj.toString, """{id: 1}""")
//    Orm.clear(obj, "id")
//    Assert.assertEquals(obj.toString, "{}")
//  }
//
//  @Test
//  def testTransaction(): Unit = {
//    try {
//      db.beginTransaction(fn = session => {
//        val obj = Orm.create(classOf[Obj])
//        obj.name = ""
//        session.execute(Orm.insert(obj))
//        throw new RuntimeException("Test")
//      })
//      Assert.assertFalse(true)
//    } catch {
//      case _: Throwable =>
//        db.beginTransaction(session => {
//          val obj = OrmTool.selectById(classOf[Obj], 1, session)()
//          Assert.assertNull(obj)
//        })
//    }
//  }
//
//  @Test
//  def testTransaction2(): Unit = {
//    try {
//      db.beginTransaction(fn = session => {
//        val obj = Orm.create(classOf[Obj])
//        obj.name = ""
//        session.execute(Orm.inserts(Array(obj)))
//        throw new RuntimeException("Test")
//      })
//      Assert.assertFalse(true)
//    } catch {
//      case _: Throwable =>
//        db.beginTransaction(session => {
//          val obj = OrmTool.selectById(classOf[Obj], 1, session)()
//          Assert.assertNull(obj)
//        })
//    }
//  }
//
//  @Test
//  def testEnum(): Unit = {
//    db.beginTransaction(session => {
//      db.check()
//      val obj = new Obj
//      obj.name = "enum"
//      obj.status = "succ"
//      session.execute(Orm.insert(Orm.convert(obj)))
//      val root = Orm.root(classOf[Obj])
//      val res = session.first(Orm.selectFrom(root).where(root.get("status").eql("succ")))
//      Assert.assertEquals(res.id.longValue(), 1)
//      Assert.assertEquals(res.status, "succ")
//    })
//  }
//
//  @Test
//  def testDefaultValue(): Unit = {
//    db.beginTransaction(session => {
//      val obj = Orm.empty(classOf[Obj])
//      obj.name = "dft"
//      session.execute(Orm.insert(obj))
//      val res = OrmTool.selectById(classOf[Obj], 1, session)()
//      Assert.assertEquals(res.dftValue.intValue(), 10)
//    })
//  }
//
//  @Test
//  def testOrmToolUpdate(): Unit = {
//    db.beginTransaction(session => {
//      val obj = Orm.empty(classOf[Obj])
//      obj.name = "update"
//      session.execute(Orm.insert(obj))
//      OrmTool.updateById(classOf[Obj], 1, session, ("name", "update2"), ("age", 10))
//      val res = OrmTool.selectById(classOf[Obj], 1, session)()
//      Assert.assertEquals(res.age.intValue(), 10)
//      Assert.assertEquals(res.name, "update2")
//    })
//  }
//
//  @Test
//  def testCrossDb(): Unit = {
//    db.beginTransaction(session => {
//      val mos = Orm.convert(1.to(5).map(_ => {
//        val mo = new MO()
//        mo
//      }).toArray)
//      session.execute(Orm.inserts(mos))
//      val oms = mos.map(mo => {
//        val om = Orm.empty(classOf[OM])
//        om.subId = 1L
//        om.moId = mo.id
//        om
//      })
//      session.execute(Orm.inserts(oms))
//    })
//    db.beginTransaction(session => {
//      var sub = Orm.empty(classOf[Sub])
//      sub.id = 1L
//      sub = OrmTool.attach(sub, "om", session, join => join.select("mo"), null)
//      Assert.assertEquals(sub.om.length, 5)
//      Assert.assertEquals(sub.om(0).mo.id.intValue(), 1)
//    })
//  }
//
//  @Test
//  def testText(): Unit = {
//    db.beginTransaction(session => {
//      val s = 1.to(10000).map(_.toString).mkString("")
//      val obj = new Obj
//      obj.name = ""
//      obj.text = s
//      session.execute(Orm.insert(Orm.convert(obj)))
//
//      val root = Orm.root(classOf[Obj])
//      val o2 = session.first(Orm.selectFrom(root).where(root.get("id").eql(1)))
//      Assert.assertEquals(o2.text, s)
//    })
//  }
//
//  //  @Test
//  //  def testTypedInsert(): Unit = {
//  //    db.beginTransaction(session => {
//  //      val obj = new Obj
//  //      obj.setName("")
//  //      obj.setAge(10)
//  //      obj.setOo(new OO)
//  //      val ex = Orm.insert(Orm.convert(obj))
//  //      ex.insert(_.getOo)
//  //      ex.fields(_.getName)
//  //      session.execute(ex)
//  //
//  //      val root = Orm.root(classOf[Obj])
//  //      root.select("oo")
//  //      val res = session.first(Orm.selectFrom(root))
//  //      Assert.assertEquals(res.getId.intValue(), 1)
//  //      Assert.assertEquals(res.getAge, null)
//  //      Assert.assertEquals(res.getOo.getId.intValue(), 1)
//  //    })
//  //  }
//
//  //  @Test
//  //  def testTypedJoin(): Unit = {
//  //    db.beginTransaction(session => {
//  //      val obj = new Obj
//  //      obj.setName("")
//  //      obj.setAge(10)
//  //      obj.setOo(new OO)
//  //      val ex = Orm.insert(Orm.convert(obj))
//  //      ex.insert(_.getOo)
//  //      session.execute(ex)
//  //
//  //      {
//  //        val root = Orm.root(classOf[Obj])
//  //        val res = session.first(Orm.selectFrom(root).where(root.join(_.getOo).get("id").eql(1)))
//  //        Assert.assertEquals(res.getId.intValue(), 1)
//  //      }
//  //      {
//  //        val root = Orm.root(classOf[Obj])
//  //        val res = session.first(Orm.selectFrom(root).where(root.get(_.getOo.getId).eql(1)))
//  //        Assert.assertEquals(res.getId.intValue(), 1)
//  //      }
//  //      {
//  //        val root = Orm.root(classOf[Obj])
//  //        val oo = root.leftJoinAs(classOf[OO])(_.getId)(_.getObjId)
//  //        val res = session.first(Orm.select(oo).from(root))
//  //        Assert.assertEquals(res.getId.intValue(), 1)
//  //      }
//  //      {
//  //        val root = Orm.root(classOf[Obj])
//  //        root.fields(_.getAge)
//  //        val res = session.first(Orm.selectFrom(root))
//  //        Assert.assertEquals(res.getId.intValue(), 1)
//  //        Assert.assertEquals(res.getName, null)
//  //        Assert.assertEquals(res.getAge.intValue(), 10)
//  //      }
//  //    })
//  //  }
//}
