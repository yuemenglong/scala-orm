package io.github.yuemenglong.orm

import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.entity.EntityManager
import io.github.yuemenglong.orm.init.Scanner
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.meta.OrmMeta
import io.github.yuemenglong.orm.operate.impl._
import io.github.yuemenglong.orm.operate.impl.core._
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

  def clear(): Unit = {
    OrmMeta.clear()
  }

  def openDb(host: String, port: Int, user: String, pwd: String, db: String,
             minConn: Int, maxConn: Int, partition: Int): Db = {
    if (OrmMeta.entityVec.isEmpty) throw new RuntimeException("Orm Not Init Yet")
    new Db(host, port, user, pwd, db, minConn, maxConn, partition)
  }

  def openDb(host: String, port: Int, user: String, pwd: String, db: String): Db = {
    if (OrmMeta.entityVec.isEmpty) throw new RuntimeException("Orm Not Init Yet")
    new Db(host, port, user, pwd, db, 5, 30, 3)
  }

  def create[T](clazz: Class[T]): T = {
    EntityManager.create(clazz)
  }

  def empty[T](clazz: Class[T]): T = {
    EntityManager.empty(clazz)
  }

  def convert[T](obj: T): T = {
    if (obj.getClass.isArray) {
      val arr = obj.asInstanceOf[Array[_]]
      if (arr.isEmpty) {
        obj
      } else {
        arr.map(convert).toArray(ClassTag(arr(0).getClass)).asInstanceOf[T]
      }
    } else {
      EntityManager.convert(obj.asInstanceOf[Object]).asInstanceOf[T]
    }
  }

  @Deprecated
  def converts[T](arr: Array[T]): Array[T] = {
    if (arr.isEmpty) {
      throw new RuntimeException("Converts Nothing")
    }
    arr.map(convert).toArray(ClassTag(arr(0).getClass))
  }

  def setLogger(b: Boolean): Unit = {
    Logger.setEnable(b)
  }

  def insert[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.insert(obj)

  def update[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.update(obj)

  def delete[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.delete(obj)

  def root[T](clazz: Class[T]): TypedRoot[T] = {
    if (!OrmMeta.entityMap.contains(clazz)) {
      throw new RuntimeException("Not Entity Class")
    }
    new TypedRootImpl[T](clazz, new TypedJoinImpl[T](OrmMeta.entityMap(clazz)))
  }

  def cond(): Cond = new CondHolder

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

  def selectFrom[T](root: Root[T]): Query[T] = {
    val st = new SelectableTupleImpl[T](root.getType, root)
    new QueryImpl[T](st, root)
  }

  @Deprecated
  def insert[T](clazz: Class[T]): ExecutableInsert[T] = new InsertImpl(clazz)

  def inserts[T](arr: Array[T]): ExecutableInsert[T] = {
    arr.isEmpty match {
      case true => throw new RuntimeException("Batch Insert But Array Is Empty")
      case false => new InsertImpl[T](arr(0).asInstanceOf[Entity].$$core()
        .meta.clazz.asInstanceOf[Class[T]]).values(arr)
    }
  }

  def update(root: Root[_]): ExecutableUpdate = new UpdateImpl(root)

  def delete(joins: Join*): ExecutableDelete = new DeleteImpl(joins: _*)

  def deleteFrom(root: Root[_]): ExecutableDelete = new DeleteImpl(root).from(root)
}
