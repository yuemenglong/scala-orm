package orm

import modal.{OM, OO, Person, Ptr}
import orm.db.Db
import orm.entity.EntityManager
import orm.execute.Executor
import orm.init.Scanner
import orm.meta.OrmMeta

import scala.collection.mutable.ArrayBuffer

object Orm {

  def init(path: String): Unit = {
    Scanner.scan(path)
  }

  def openDb(host: String, port: Int, user: String, pwd: String, db: String): Db = {
    require(OrmMeta.entityVec.length > 0)
    return new Db(host, port, user, pwd, db)
  }

  def main(args: Array[String]): Unit = {
    Orm.init("")
    val db = Orm.openDb("localhost", 3306, "root", "root", "test")
    db.rebuild()

    var person = EntityManager.create(classOf[Person])
    var ptr = EntityManager.create(classOf[Ptr])
    var oo = EntityManager.create(classOf[OO])
    var om = EntityManager.create(classOf[OM])
    var om2 = EntityManager.create(classOf[OM])
    var ex = Executor.createInsert(classOf[Person])

    person.setAge(1)
    ptr.setValue(10)
    oo.setValue(100)
    om.setValue(1000)
    om2.setValue(2000)
    person.setPtr(ptr)
    person.setOo(oo)
    ex.insert("ptr")
    ex.insert("oo")
    //    ex.insert("om")
    val ret = ex.execute(person, db.getConn())


    println(ret)
    //
    //    person = EntityManager.create(classOf[Person])
    //    Execute.insert(person, Db.getConn())
    //    println(person.getId())

    //  var stmt = Db.getConn().prepareStatement("insert into person set age=?,name=?")
    //    stmt.setObject(1, 100)
    //    stmt.setObject(2, "Tom")
    //    stmt.executeUpdate()
  }
}
