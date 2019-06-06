package io.github.yuemenglong.orm

import io.github.yuemenglong.orm.db.Db
import io.github.yuemenglong.orm.entity.EntityManager
import io.github.yuemenglong.orm.init.Scanner
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.meta.OrmMeta
import io.github.yuemenglong.orm.operate.execute._
import io.github.yuemenglong.orm.operate.execute.traits.{ExecutableDelete, ExecutableInsert, ExecutableUpdate, TypedExecuteRoot}
import io.github.yuemenglong.orm.operate.join._
import io.github.yuemenglong.orm.operate.query._
import io.github.yuemenglong.orm.sql.Expr

import scala.reflect.ClassTag

object Orm {

  def init(paths: Array[String]): Unit = {
    Scanner.scan(paths)
  }

  def init(clazzs: Array[_ <: Class[_]]): Unit = {
    Scanner.scan(clazzs)
  }

  private[orm] def init(path: String): Unit = {
    Scanner.scan(path)
  }

  def reset(): Unit = {
    OrmMeta.reset()
  }

  def openMysqlDb(host: String, port: Int, user: String, pwd: String, db: String,
                  minConn: Int, maxConn: Int, partition: Int): Db = {
    if (OrmMeta.entityVec.isEmpty) throw new RuntimeException("Orm Not Init Yet")
    Db.mysql(host, port, user, pwd, db, minConn, maxConn, partition)
  }

  def openMysqlDb(host: String, port: Int, user: String, pwd: String, db: String): Db = {
    if (OrmMeta.entityVec.isEmpty) throw new RuntimeException("Orm Not Init Yet")
    Db.mysql(host, port, user, pwd, db, 5, 30, 3)
  }

  def openSqliteDb(db: String, minConn: Int, maxConn: Int, partition: Int): Db = {
    if (OrmMeta.entityVec.isEmpty) throw new RuntimeException("Orm Not Init Yet")
    Db.sqlite(db, minConn, maxConn, partition)
  }

  def openSqliteDb(db: String): Db = {
    if (OrmMeta.entityVec.isEmpty) throw new RuntimeException("Orm Not Init Yet")
    Db.sqlite(db, 5, 30, 3)
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

  def insert[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.insert(convert(obj))

  def update[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.update(convert(obj))

  def delete[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.delete(convert(obj))

  def root[T](clazz: Class[T]): Root[T] = Root[T](clazz)

  def table[T](clazz: Class[T]): Root[T] = Root[T](clazz)

  def select[T: ClassTag](c: Selectable[T]): Query1[T] = new Query1[T](c)

  def select[T0: ClassTag, T1: ClassTag](s0: Selectable[T0], s1: Selectable[T1]): Query2[T0, T1] = new Query2[T0, T1](s0, s1)

  def select[T0: ClassTag, T1: ClassTag, T2: ClassTag](s0: Selectable[T0], s1: Selectable[T1], s2: Selectable[T2]): Query3[T0, T1, T2] = new Query3[T0, T1, T2](s0, s1, s2)

  def selectFrom[T: ClassTag](r: TypedSelectableCascade[T]): Query1[T] = select(r).from(r)

  def cond(): Expr = Expr("1 = 1")

  @Deprecated
  def insert[T](clazz: Class[T]): ExecutableInsert[T] = new InsertImpl(clazz)

  def inserts[T](arr: Array[T]): ExecutableInsert[T] = {
    arr.isEmpty match {
      case true => throw new RuntimeException("Batch Insert But Array Is Empty")
      case false => {
        val entityArr = Orm.convert(arr)
        val clazz = entityArr(0).asInstanceOf[Entity].$$core()
          .meta.clazz.asInstanceOf[Class[T]]
        new InsertImpl[T](clazz).values(entityArr)
      }
    }
  }

  def update(root: Root[_]): ExecutableUpdate = new UpdateImpl(root)

  def delete(joins: Cascade*): ExecutableDelete = new DeleteImpl(joins: _*)

  def deleteFrom(root: Root[_]): ExecutableDelete = new DeleteImpl(root).from(root)

  def set[V](obj: Object, field: String, value: V): Unit = obj.asInstanceOf[Entity].$$core().set(field, value.asInstanceOf[Object])

  def get(obj: Object, field: String): Object = obj.asInstanceOf[Entity].$$core().get(field)

  def clear(obj: Object, field: String): Unit = EntityManager.clear(obj, field)

  def clear[T <: Object](obj: T)(fn: T => Any): Unit = EntityManager.clear(obj)(fn)
}
