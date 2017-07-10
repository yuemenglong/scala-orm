package test

import orm.Orm
import orm.meta.OrmMeta
import orm.operate.Executor
import orm.select.{RootSelector, Selector, TargetSelector}
import test.model.{OM, OO, Obj, Ptr}

/**
  * Created by yml on 2017/7/9.
  */
object Main {
  def main(args: Array[String]): Unit = {
    Orm.init("")
    val db = Orm.openDb("localhost", 3306, "root", "root", "test")
    db.rebuild()

    val session = db.openSession()
    var obj = new Obj()
    obj.setName("name")
    obj.setPtr(new Ptr())
    obj.setOo(new OO())
    obj.setOm(Array[OM](new OM(), new OM()))
    obj = Orm.convert(obj)
    val ex = Executor.createInsert(obj)
    ex.insert("ptr")
    ex.insert("oo")
    ex.insert("om")
    session.execute(ex)

    //    val s = new RootSelector[Obj](OrmMeta.entityMap("Obj"))
    //    s.where().eq("id", new Integer(1))
    //    s.select("ptr")
    //    s.select("oo")
    //    s.select("om")
    //    val os = Selector.query(s, db.openConnection())
    //    os.foreach(println)
    //

    val rs = new RootSelector[OM](OrmMeta.entityMap("OM"))
    val count = rs.count(classOf[Long])
    val res = Selector.query(Array[TargetSelector[_]](count), db.openConnection())
    res.foreach(_.foreach(println))
  }
}
