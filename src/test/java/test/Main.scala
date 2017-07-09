package test

import orm.Orm
import orm.meta.OrmMeta
import orm.select.{RootSelector, Selector}
import test.model.Obj

/**
  * Created by yml on 2017/7/9.
  */
object Main {
  def main(args: Array[String]): Unit = {
    Orm.init("")
    val s = new RootSelector[Obj](OrmMeta.entityMap("Obj"))
    s.where().eq("id", new Integer(1))
    println(s.getSql)
    println(s.getParam)
    val db = Orm.openDb("localhost", 3306, "root", "root", "test")
    Selector.query(s, db.openConnection())
  }
}
