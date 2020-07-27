package io.github.yuemenglong.orm.api

import java.io.OutputStream

import io.github.yuemenglong.orm.api.db.{Db, DbConfig}
import io.github.yuemenglong.orm.api.operate.execute.{ExecutableDelete, ExecutableInsert, ExecutableUpdate, TypedExecuteRoot}
import io.github.yuemenglong.orm.api.operate.query.{Query1, Query2, Query3, Selectable}
import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, ExprLike, ResultColumn}
import io.github.yuemenglong.orm.api.operate.sql.field.{FnExpr, SelectableFieldExpr}
import io.github.yuemenglong.orm.api.operate.sql.table.{ResultTable, Root, Table, TypedResultTable}
import io.github.yuemenglong.orm.api.session.Session

import scala.reflect.ClassTag

trait Orm {

  private[orm] def init(path: String): Unit

  def init(paths: Array[String]): Unit = {
    init(paths.map(Class.forName))
  }

  def init(clazzs: Array[Class[_]]): Unit

  def reset(): Unit

  def mysql(host: String, port: Int, username: String, password: String, db: String): DbConfig

  def mysql(url: String): DbConfig

  def sqlite(db: String): DbConfig

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

trait OrmFn {
  def count(): FnExpr[Long]

  def count(c: ResultColumn with ExprLike[_]): FnExpr[Long]

  def sum[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def min[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def max[T](f: SelectableFieldExpr[T]): FnExpr[T]

  def exists(e: ExprLike[_]): ExprLike[_]
}

trait OrmTool {
  def getConstructors: Map[Class[_], () => Object]

  def exportTsClass(os: OutputStream, prefix: String = "", imports: String = ""): Unit

  def attach[T, R](orig: T, session: Session)(fn: T => R): T = attachX(orig, session)(fn)(null, null)

  def attachOneMany[T, R](orig: T, session: Session)(fn: T => Array[R]): T = attachOneManyX(orig, session)(fn)(null, null)

  def attachArray[T, R](orig: Array[T], session: Session)(fn: T => R): Array[T] = attachArrayX(orig, session)(fn)(null, null)

  def attachArrayOneMany[T, R](orig: Array[T], session: Session)(fn: T => Array[R]): Array[T] = attachArrayOneManyX(orig, session)(fn)(null, null)

  def attachX[T, R](orig: T, session: Session)
                   (fn: T => R)
                   (joinFn: TypedResultTable[R] => Unit,
                    queryFn: Query1[R] => Unit
                   ): T

  def attachOneManyX[T, R](orig: T, session: Session)
                          (fn: T => Array[R])
                          (joinFn: TypedResultTable[R] => Unit,
                           queryFn: Query1[R] => Unit
                          ): T

  def attachArrayX[T, R](orig: Array[T], session: Session)
                        (fn: T => R)
                        (joinFn: TypedResultTable[R] => Unit,
                         queryFn: Query1[R] => Unit
                        ): Array[T]

  def attachArrayOneManyX[T, R](orig: Array[T], session: Session)
                               (fn: T => Array[R])
                               (joinFn: TypedResultTable[R] => Unit,
                                queryFn: Query1[R] => Unit
                               ): Array[T]

  def attach[T](obj: T, field: String, session: Session): T = attach(obj, field, session, null, null)

  def attach[T](obj: T, field: String, session: Session,
                joinFn: ResultTable => Unit,
                queryFn: Query1[_] => Unit
               ): T

  def updateById[T, V](clazz: Class[T], id: V, session: Session,
                       pair: (String, Any), pairs: (String, Any)*): Unit

  def updateById[T, V](clazz: Class[T], id: V, session: Session)
                      (fns: (T => Any)*)
                      (values: Any*): Unit

  def updateById[T, V](obj: T, session: Session)
                      (fns: (T => Any)*)

  def updateArray[T <: Object](root: Root[T], oldValues: Array[T], newValues: Array[T], session: Session): Unit

  def selectByIdEx[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                  (rootFn: Root[T] => Unit = null): T

  def selectById[T: ClassTag, V](clazz: Class[T], id: V, session: Session): T = selectByIdEx(clazz, id, session)()


  def deleteByIdEx[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                  (rootFn: Root[T] => Array[Table] = (_: Root[T]) => Array[Table]()
                                  ): Int

  def deleteById[T: ClassTag, V](clazz: Class[T], id: V, session: Session): Int
}

object OrmLoader {
  private val inst: Orm = Class.forName("io.github.yuemenglong.orm.impl.OrmImpl")
    .newInstance().asInstanceOf[Orm]

  def loadOrm(): Orm = inst
}