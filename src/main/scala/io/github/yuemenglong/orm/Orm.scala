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
import io.github.yuemenglong.orm.operate.join.traits.{Cascade, Cond, Root, TypedSelectableCascade}
import io.github.yuemenglong.orm.operate.query._
import io.github.yuemenglong.orm.operate.query.traits.{Query, Selectable}
import io.github.yuemenglong.orm.sql.SelectCore

import scala.reflect.ClassTag

object Orm {

  def init(paths: Array[String]): Unit = {
    Scanner.scan(paths)
  }

  private[orm] def init(path: String): Unit = {
    Scanner.scan(path)
  }

  private def init(clazzs: Array[Class[_]]): Unit = {
    Scanner.scan(clazzs)
  }

  def reset(): Unit = {
    OrmMeta.reset()
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

  def insert[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.insert(convert(obj))

  def update[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.update(convert(obj))

  def delete[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.delete(convert(obj))

  def root[T](clazz: Class[T]): Root[T] = Root[T](clazz)

  def select[T](c: Selectable[T]): Query[T] = new Query[T] {
    override val targets = Array(c)
    override private[orm] val core = new SelectCore(c.getColumns: _*)
  }

  def selectFrom[T](r: TypedSelectableCascade[T]): Query[T] = select(r).from(r)

  //
  //  def cond(): Cond = new CondHolder
  //
  //  def select[T](s: Selectable[T]): QueryBuilder[T] = {
  //    new QueryBuilderImpl[T] {
  //      override val st = new SelectableTupleImpl[T](s.getType, s)
  //    }
  //  }
  //
  //  def select[T1, T2](s1: Selectable[T1], s2: Selectable[T2]): QueryBuilder[(T1, T2)] = {
  //    new QueryBuilderImpl[(T1, T2)] {
  //      override val st = new SelectableTupleImpl[(T1, T2)](classOf[(T1, T2)], s1, s2)
  //    }
  //  }
  //
  //  def select[T1, T2, T3](s1: Selectable[T1], s2: Selectable[T2], s3: Selectable[T3]): QueryBuilder[(T1, T2, T3)] = {
  //    new QueryBuilderImpl[(T1, T2, T3)] {
  //      override val st = new SelectableTupleImpl[(T1, T2, T3)](classOf[(T1, T2, T3)], s1, s2, s3)
  //    }
  //  }
  //
  //  def subSelect[T](s: Selectable[T]): SubQueryBuilder[T] = {
  //    new SubQueryBuilderImpl[T] {
  //      override val st = new SelectableTupleImpl[T](s.getType, s)
  //    }
  //  }
  //
  //  def subSelect[T1, T2](s1: Selectable[T1], s2: Selectable[T2]): SubQueryBuilder[(T1, T2)] = {
  //    new SubQueryBuilderImpl[(T1, T2)] {
  //      override val st = new SelectableTupleImpl[(T1, T2)](classOf[(T1, T2)], s1, s2)
  //    }
  //  }
  //
  //  def subSelect[T1, T2, T3](s1: Selectable[T1], s2: Selectable[T2], s3: Selectable[T3]): SubQueryBuilder[(T1, T2, T3)] = {
  //    new SubQueryBuilderImpl[(T1, T2, T3)] {
  //      override val st = new SelectableTupleImpl[(T1, T2, T3)](classOf[(T1, T2, T3)], s1, s2, s3)
  //    }
  //  }
  //
  //  def selectFrom[T](root: Root[T]): Query[T, T] = {
  //    val pRoot = root
  //    new QueryImpl[T, T] with QueryBuilderImpl[T] {
  //      override val root = pRoot
  //      override val st = new SelectableTupleImpl[T](root.getType, root)
  //    }
  //  }
  //
  //  @Deprecated
  //  def insert[T](clazz: Class[T]): ExecutableInsert[T] = new InsertImpl(clazz)
  //
  //  def inserts[T](arr: Array[T]): ExecutableInsert[T] = {
  //    arr.isEmpty match {
  //      case true => throw new RuntimeException("Batch Insert But Array Is Empty")
  //      case false => {
  //        val entityArr = Orm.convert(arr)
  //        val clazz = entityArr(0).asInstanceOf[Entity].$$core()
  //          .meta.clazz.asInstanceOf[Class[T]]
  //        new InsertImpl[T](clazz).values(entityArr)
  //      }
  //    }
  //  }
  //
  //  def update(root: Root[_]): ExecutableUpdate = new UpdateImpl(root)
  //
  //  def delete(joins: Cascade*): ExecutableDelete = new DeleteImpl(joins: _*)
  //
  //  def deleteFrom(root: Root[_]): ExecutableDelete = new DeleteImpl(root).from(root)
  //
  //  def set[V](obj: Object, field: String, value: V): Unit = obj.asInstanceOf[Entity].$$core().set(field, value.asInstanceOf[Object])
  //
  //  def get(obj: Object, field: String): Object = obj.asInstanceOf[Entity].$$core().get(field)
  //
  //  def clear(obj: Object, field: String): Unit = EntityManager.clear(obj, field)
  //
  //  def clear[T <: Object](obj: T)(fn: T => Any): Unit = EntityManager.clear(obj)(fn)
}
