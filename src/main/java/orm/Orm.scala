package orm

import orm.db.Db
import orm.entity.EntityManager
import orm.init.Scanner
import orm.kit.Kit
import orm.meta.OrmMeta
import orm.operate.impl._
import orm.operate.traits.{DeleteBuilder, UpdateBuilder}
import orm.operate.traits.core._

object Orm {

  def init(path: String): Unit = {
    Scanner.scan(path)
  }

  def init(paths: Array[String]): Unit = {
    Scanner.scan(paths)
  }

  def init(clazzs: Array[Class[_]]): Unit = {
    Scanner.scan(clazzs)
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

  def getEmptyConstructorMap: Map[Class[_], () => Object] = Kit.getEmptyConstructorMap

  def insert(obj: Object): ExecuteRoot = ExecuteRootImpl.insert(obj)

  def update(obj: Object): ExecuteRoot = ExecuteRootImpl.update(obj)

  def delete(obj: Object): ExecuteRoot = ExecuteRootImpl.delete(obj)

  def root[T](clazz: Class[T]): Root[T] = {
    if (!OrmMeta.entityMap.contains(clazz.getSimpleName)) {
      throw new RuntimeException("Not Entity Class")
    }
    new RootImpl[T](clazz, OrmMeta.entityMap(clazz.getSimpleName))
  }

  def select[T](target: Selectable[T]): SelectBuilder1[T] = new SelectBuilder1Impl(target)

  def from[T](root: SelectRoot[T]): Query1[T] = select(root).from(root)

  def from[T](clazz: Class[T]): Query1[T] = from(Orm.root(clazz).asSelect())

  def update(root: Root[_]): UpdateBuilder = new UpdateBuilderImpl(root)

  def delete(root: Root[_]): DeleteBuilder = new DeleteBuilderImpl(root)
}
