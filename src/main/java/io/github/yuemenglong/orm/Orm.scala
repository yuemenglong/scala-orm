package io.github.yuemenglong.orm

import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.entity.EntityManager
import io.github.yuemenglong.orm.init.Scanner
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.meta.OrmMeta
import io.github.yuemenglong.orm.operate.impl._
import io.github.yuemenglong.orm.operate.impl.core.{ExecuteRootImpl, RootImpl}
import io.github.yuemenglong.orm.operate.traits.core._
import io.github.yuemenglong.orm.operate.traits.{ExecutableDelete, ExecutableInsert, ExecutableUpdate, Query}

import scala.reflect.ClassTag

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

  def clear(): Unit ={
    OrmMeta.clear()
  }

  def openDb(host: String, port: Int, user: String, pwd: String, db: String): Db = {
    require(OrmMeta.entityVec.nonEmpty)
    val ret = new Db(host, port, user, pwd, db)
    ret.check()
    ret
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

  def converts[T](arr: Array[T]): Array[T] = {
    if (arr.isEmpty) {
      throw new RuntimeException("Converts Nothing")
    }
    arr.map(convert).toArray(ClassTag(arr(0).getClass))
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

  def select[T](s: Selectable[T]): Query[T] = {
    val st = new SelectableTupleImpl[T](s.getType, s)
    new QueryImpl[T](st)
  }

  def select[T1, T2](s1: Selectable[T1], s2: Selectable[T2]): Query[(T1, T2)] = {
    val st = new SelectableTupleImpl[(T1, T2)](classOf[(T1, T2)], s1, s2)
    new QueryImpl[(T1, T2)](st)
  }

  def select[T1, T2, T3](s1: Selectable[T1], s2: Selectable[T2], s3: Selectable[T3]): Query[(T1, T2, T3)] = {
    val st = new SelectableTupleImpl[(T1, T2, T3)](classOf[(T1, T2, T3)], s1, s2, s3)
    new QueryImpl[(T1, T2, T3)](st)
  }

  def from[T](root: SelectRoot[T]): Query[T] = {
    val st = new SelectableTupleImpl[T](root.getType, root)
    new QueryImpl[T](st, root)
  }

  def insert[T](clazz: Class[T]): ExecutableInsert[T] = new InsertImpl(clazz)

  def update(root: Root[_]): ExecutableUpdate = new UpdateImpl(root)

  def delete(root: Root[_]): ExecutableDelete = new DeleteImpl(root)
}
