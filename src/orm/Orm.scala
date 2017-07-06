package orm

import java.util

import model.Male
import org.jcp.xml.dsig.internal.dom.DOMExcC14NMethod
import orm.db.Db
import orm.entity.EntityManager
import orm.init.Scanner
import orm.kit.Kit
import orm.meta.OrmMeta

object Orm {

  def init(path: String): Unit = {
    Scanner.scan(path)
  }

  def init(paths: util.Collection[String]): Unit = {
    Scanner.scan(paths)
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

  def convert[T](obj: T): T = {
    EntityManager.convert(obj.asInstanceOf[Object]).asInstanceOf[T]
  }

  def main(args: Array[String]): Unit = {
    //    Orm.init("")
    Kit.getDeclaredFields(classOf[Male]).foreach(println(_))
  }
}
