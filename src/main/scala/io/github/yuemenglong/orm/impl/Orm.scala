package io.github.yuemenglong.orm.impl

import io.github.yuemenglong.orm.api.db.{Db, DbConfig}
import io.github.yuemenglong.orm.api.operate.execute.{ExecutableDelete, ExecutableInsert, ExecutableUpdate, TypedExecuteRoot}
import io.github.yuemenglong.orm.api.operate.query.{Query1, Query2, Query3, Selectable}
import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprLike, ResultColumn}
import io.github.yuemenglong.orm.api.operate.sql.field.{FnExpr, SelectableFieldExpr}
import io.github.yuemenglong.orm.api.operate.sql.table.{Root, Table}
import io.github.yuemenglong.orm.api.{Orm, OrmFn, OrmTool}
import io.github.yuemenglong.orm.impl.db.DbImpl
import io.github.yuemenglong.orm.impl.entity.{Entity, EntityManager}
import io.github.yuemenglong.orm.impl.init.Scanner
import io.github.yuemenglong.orm.impl.logger.Logger
import io.github.yuemenglong.orm.impl.meta.OrmMeta
import io.github.yuemenglong.orm.impl.operate.execute._
import io.github.yuemenglong.orm.impl.operate.query._
import io.github.yuemenglong.orm.impl.operate.sql.core.ExprUtil
import io.github.yuemenglong.orm.impl.operate.sql.field.FnExprImpl
import io.github.yuemenglong.orm.impl.operate.sql.table._
import io.github.yuemenglong.orm.impl.tool.OrmToolImpl

import scala.reflect.ClassTag

object Orm extends OrmImpl

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

class OrmFnImpl extends OrmFn {
  def count(): FnExpr[Long] = new FnExprImpl[Long] {
    override private[orm] val uid = "$count$"
    override private[orm] val expr = ExprUtil.func("COUNT(*)", d = false, Array())
    override val clazz: Class[Long] = classOf[Long]
  }

  def count(c: ResultColumn with ExprLike[_]): FnExpr[Long] = new FnExprImpl[Long] {
    override val clazz: Class[Long] = classOf[Long]
    override private[orm] val uid = s"$$count$$${c.uid}"
    override private[orm] val expr = ExprUtil.func("COUNT", d = false, Array(c.toExpr))
  }

  def sum[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$sum$$${f.uid}"
    override private[orm] val expr = ExprUtil.func("SUM", d = false, Array(f.toExpr))
  }

  def min[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$min$$${f.uid}"
    override private[orm] val expr = ExprUtil.func("MIN", d = false, Array(f.toExpr))
  }

  def max[T](f: SelectableFieldExpr[T]): FnExpr[T] = new FnExprImpl[T] {
    override val clazz: Class[T] = f.getType
    override private[orm] val uid = s"$$max$$${f.uid}"
    override private[orm] val expr = ExprUtil.func("MAX", d = false, Array(f.toExpr))
  }

  def exists(e: ExprLike[_]): ExprLike[_] = ExprUtil.create("EXISTS", e)
}

