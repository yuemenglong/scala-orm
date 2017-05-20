package orm

import modal.{Person, Ptr}
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

    //    var person = EntityManager.create(classOf[Person])
    //    println(person)
    //    person.setAge(1)
    //    Execute.insert(person, Db.getConn())
    //    println(person.getId())
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
