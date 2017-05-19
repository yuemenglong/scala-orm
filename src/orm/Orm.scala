package orm

import modal.Person
import orm.db.Db
import orm.entity.EntityManager
import orm.execute.Execute
import orm.init.Scanner

object Orm {
  def open(): Unit = {
    Scanner.scan("")
  }

  def main(args: Array[String]): Unit = {
    Orm.open()
    Db.rebuild()

    var person = EntityManager.create(classOf[Person])
    person.setAge(1)
    Execute.insert(person, Db.getConn())
    println(person.getId())

    person = EntityManager.create(classOf[Person])
    Execute.insert(person, Db.getConn())
    println(person.getId())

//  var stmt = Db.getConn().prepareStatement("insert into person set age=?,name=?")
//    stmt.setObject(1, 100)
//    stmt.setObject(2, "Tom")
//    stmt.executeUpdate()
  }
}
