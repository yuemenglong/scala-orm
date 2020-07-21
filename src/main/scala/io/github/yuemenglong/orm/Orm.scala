package io.github.yuemenglong.orm

import io.github.yuemenglong.orm.api.db.{Db, DbConfig}
import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprLike}
import io.github.yuemenglong.orm.api.operate.sql.field.OrmFn
import io.github.yuemenglong.orm.api.operate.sql.table.Root
import io.github.yuemenglong.orm.impl.db.DbImpl
import io.github.yuemenglong.orm.impl.entity.{Entity, EntityManager}
import io.github.yuemenglong.orm.impl.init.Scanner
import io.github.yuemenglong.orm.impl.logger.Logger
import io.github.yuemenglong.orm.impl.meta.OrmMeta
import io.github.yuemenglong.orm.operate.execute._
import io.github.yuemenglong.orm.operate.execute.traits.{ExecutableDelete, ExecutableInsert, ExecutableUpdate, TypedExecuteRoot}
import io.github.yuemenglong.orm.operate.query._
import io.github.yuemenglong.orm.operate.sql.core.ExprUtil
import io.github.yuemenglong.orm.operate.sql.field.OrmFnImpl
import io.github.yuemenglong.orm.operate.sql.table._
import io.github.yuemenglong.orm.tool.{OrmTool, OrmToolImpl}

import scala.reflect.ClassTag

trait Orm {

  def init(paths: Array[String]): Unit = {
    init(paths.map(Class.forName))
  }

  def init(clazzs: Array[Class[_]]): Unit

  def reset(): Unit

  def open(config: DbConfig): Db

  def obj[T](clazz: Class[T]): T

  def convert[T](obj: T): T

  def setLogger(enable: Boolean): Unit

  def setLoggerLevel(level: String = "DEBUG"): Unit = {
    System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", level)
  }

  def insert[T <: Object](obj: T): TypedExecuteRoot[T]

  def update[T <: Object](obj: T): TypedExecuteRoot[T]

  def delete[T <: Object](obj: T): TypedExecuteRoot[T]

  def root[T](clazz: Class[T]): Root[T]

  def table[T](clazz: Class[T]): Root[T] = root(clazz)

  def select[T: ClassTag](c: Selectable[T]): Query1[T]

  def select[T0: ClassTag, T1: ClassTag](s0: Selectable[T0], s1: Selectable[T1]): Query2[T0, T1]

  def select[T0: ClassTag, T1: ClassTag, T2: ClassTag](s0: Selectable[T0], s1: Selectable[T1], s2: Selectable[T2]): Query3[T0, T1, T2]

  def selectFrom[T: ClassTag](r: TypedResultTable[T]): Query1[T] = select(r).from(r)

  def cond(): Expr

  def insertArray[T](arr: Array[T]): ExecutableInsert[T]

  def update(root: Root[_]): ExecutableUpdate

  def delete(joins: Table*): ExecutableDelete

  def deleteFrom(root: Root[_]): ExecutableDelete = delete(root).from(root)

  def set[V](obj: Object, field: String, value: V): Unit

  def get(obj: Object, field: String): Object

  def clear(obj: Object, field: String): Unit

  def clear[T <: Object](obj: T)(fn: T => Any): Unit

  def const[T](v: T): Expr

  def expr(op: String, e: ExprLike[_]): Expr

  def expr(e: ExprLike[_], op: String): Expr

  def expr(l: ExprLike[_], op: String, r: ExprLike[_]): Expr

  def expr(e: ExprLike[_], l: ExprLike[_], r: ExprLike[_]): Expr

  def expr(es: ExprLike[_]*): Expr

  def expr(sql: String, params: Array[Object] = Array()): Expr

  val Fn: OrmFn
  val Tool: OrmTool
}

class OrmImpl extends Orm {

  def init(clazzs: Array[Class[_]]): Unit = {
    Scanner.scan(clazzs)
  }

  private[orm] def init(path: String): Unit = {
    Scanner.scan(path)
  }

  def reset(): Unit = {
    OrmMeta.reset()
  }

  def open(config: DbConfig): Db = {
    if (OrmMeta.entityVec.isEmpty) throw new RuntimeException("Orm Not Init Yet")
    new DbImpl(config)
  }

  def obj[T](clazz: Class[T]): T = {
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

  def setLogger(enable: Boolean): Unit = {
    Logger.setEnable(enable)
  }

  def insert[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.insert(convert(obj))

  def update[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.update(convert(obj))

  def delete[T <: Object](obj: T): TypedExecuteRoot[T] = ExecuteRootImpl.delete(convert(obj))

  def root[T](clazz: Class[T]): Root[T] = RootUtil.create[T](clazz)

  def select[T: ClassTag](c: Selectable[T]): Query1[T] = new Query1Impl[T](c)

  def select[T0: ClassTag, T1: ClassTag](s0: Selectable[T0], s1: Selectable[T1]): Query2[T0, T1] = new Query2Impl[T0, T1](s0, s1)

  def select[T0: ClassTag, T1: ClassTag, T2: ClassTag](s0: Selectable[T0], s1: Selectable[T1], s2: Selectable[T2]): Query3[T0, T1, T2] = new Query3Impl[T0, T1, T2](s0, s1, s2)

  def cond(): Expr = ExprUtil.create("1 = 1")

  def insertArray[T](arr: Array[T]): ExecutableInsert[T] = {
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

  def delete(joins: Table*): ExecutableDelete = new DeleteImpl(joins: _*)

  def set[V](obj: Object, field: String, value: V): Unit = obj.asInstanceOf[Entity].$$core().set(field, value.asInstanceOf[Object])

  def get(obj: Object, field: String): Object = obj.asInstanceOf[Entity].$$core().get(field)

  def clear(obj: Object, field: String): Unit = EntityManager.clear(obj, field)

  def clear[T <: Object](obj: T)(fn: T => Any): Unit = EntityManager.clear(obj)(fn)

  override def const[T](v: T): Expr = ExprUtil.const(v)

  override def expr(op: String, e: ExprLike[_]): Expr = ExprUtil.create(op, e)

  override def expr(e: ExprLike[_], op: String): Expr = ExprUtil.create(e, op)

  override def expr(l: ExprLike[_], op: String, r: ExprLike[_]): Expr = ExprUtil.create(l, op, r)

  override def expr(e: ExprLike[_], l: ExprLike[_], r: ExprLike[_]): Expr = ExprUtil.create(e, l, r)

  override def expr(es: ExprLike[_]*): Expr = ExprUtil.create(es: _*)

  override def expr(sql: String, params: Array[Object]): Expr = ExprUtil.create(sql, params)

  val Fn: OrmFn = new OrmFnImpl()

  val Tool: OrmTool = new OrmToolImpl()
}

object Orm extends OrmImpl