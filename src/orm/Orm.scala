package orm

import java.util
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

  def empty[T](clazz: Class[T]): T = {
    EntityManager.empty(clazz)
  }

  def parse[T](clazz: Class[T], json: String): T = {
    EntityManager.parse[T](clazz, json)
  }

  def parseArray[T](clazz: Class[T], json: String): util.Collection[T] = {
    EntityManager.parseArray[T](clazz, json)
  }

  def stringify(obj: Object): String = {
    EntityManager.stringify(obj)
  }

  def stringifyArray(arr: util.Collection[Object]): String = {
    EntityManager.stringifyArray(arr)
  }

}
