package orm


import orm.db.Db
import orm.entity.EntityManager
import orm.init.Scanner
import orm.meta.OrmMeta

object Orm {

  def init(path: String): Unit = {
    Scanner.scan(path)
  }


  def init(paths: Array[String]): Unit = {
    Scanner.scan(paths)
  }

  def openDb(host: String, port: Int, user: String, pwd: String, db: String): Db = {
    require(OrmMeta.entityVec.nonEmpty)
    new Db(host, port, user, pwd, db)
  }

  def create[T](clazz: Class[T]): T = {
    EntityManager.create(clazz)
  }

  def empty[T](clazz: Class[T]): T = {
    EntityManager.empty(clazz)
  }

  def convert[T](obj: T): T = {
    EntityManager.convert(obj.asInstanceOf[Object]).asInstanceOf[T]
  }
}
