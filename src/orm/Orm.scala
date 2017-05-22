package orm

import orm.db.Db
import orm.entity.EntityManager
import orm.init.Scanner
import orm.meta.OrmMeta


object Orm {

  def init(path: String): Unit = {
    Scanner.scan(path)
  }

  def openDb(host: String, port: Int, user: String, pwd: String, db: String): Db = {
    require(OrmMeta.entityVec.length > 0)
    return new Db(host, port, user, pwd, db)
  }

  def create[T](clazz: Class[T]): T = {
    EntityManager.create(clazz)
  }
}
