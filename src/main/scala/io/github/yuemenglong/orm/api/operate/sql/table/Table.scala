package io.github.yuemenglong.orm.api.operate.sql.table

import java.sql.ResultSet

import io.github.yuemenglong.orm.api.operate.sql.core.TableLike
import io.github.yuemenglong.orm.api.operate.sql.field.{FieldExpr, SelectableFieldExpr}
import io.github.yuemenglong.orm.api.operate.sql.table.JoinType.JoinType
import io.github.yuemenglong.orm.impl.entity.Entity
import io.github.yuemenglong.orm.impl.meta.EntityMeta
import io.github.yuemenglong.orm.operate.query.Selectable
import io.github.yuemenglong.orm.operate.sql.table.TypedResultTableImpl

import scala.collection.mutable

object JoinType extends Enumeration {
  type JoinType = Value
  val INNER, LEFT, RIGHT, OUTER = Value
}

trait Table extends TableLike {

  def getMeta: EntityMeta

  def get(field: String): FieldExpr

  def join(field: String, joinType: JoinType): Table

  def join(field: String): Table = join(field, JoinType.INNER)

  def leftJoin(field: String): Table = join(field, JoinType.LEFT)

  def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): TypedResultTable[T]

  def joinAs[T](left: String, right: String, clazz: Class[T]): TypedResultTable[T] = this.joinAs(left, right, clazz, JoinType.INNER)

  def leftJoinAs[T](left: String, right: String, clazz: Class[T]): TypedResultTable[T] = this.joinAs(left, right, clazz, JoinType.LEFT)

  def as[T](clazz: Class[T]): TypedResultTableImpl[T]
}

trait ResultTable extends Table {
  def select(field: String): ResultTable

  def fields(fields: String*): ResultTable

  def ignore(fields: String*): ResultTable

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity
}

trait TypedTable[T] extends Table {

  def apply[R](fn: T => R): SelectableFieldExpr[R] = get(fn)

  def get[R](fn: (T => R)): SelectableFieldExpr[R]

  def join[R](fn: (T => R), joinType: JoinType): TypedTable[R]

  def join[R](fn: (T => R)): TypedTable[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedTable[R] = join(fn, JoinType.LEFT)

  def joinAs[R](fn: (T => R), joinType: JoinType): TypedResultTable[R]

  def joinAs[R](fn: (T => R)): TypedResultTable[R] = joinAs(fn, JoinType.INNER)

  def leftJoinAs[R](fn: (T => R)): TypedResultTable[R] = joinAs(fn, JoinType.LEFT)

  def joinsAs[R](fn: (T => Array[R]), joinType: JoinType): TypedResultTable[R]

  def joinsAs[R](fn: (T => Array[R])): TypedResultTable[R] = joinsAs(fn, JoinType.INNER)

  def leftJoinsAs[R](fn: (T => Array[R])): TypedTable[R] = joinsAs(fn, JoinType.LEFT)

  def joins[R](fn: (T => Array[R]), joinType: JoinType): TypedTable[R]

  def joins[R](fn: (T => Array[R])): TypedTable[R] = joins(fn, JoinType.INNER)

  def joinArray[R](fn: (T => Array[R])): TypedTable[R] = joins(fn)

  def leftJoins[R](fn: (T => Array[R])): TypedTable[R] = joins(fn, JoinType.LEFT)

  def leftJoinArray[R](fn: (T => Array[R])): TypedTable[R] = leftJoins(fn)

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedTable[R]

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedTable[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedTable[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)
}

trait TypedResultTable[T] extends TypedTable[T]
  with ResultTable with Selectable[T] {

  def select[R](fn: T => R): TypedResultTable[R]

  def selects[R](fn: T => Array[R]): TypedResultTable[R]

  def selectArray[R](fn: T => Array[R]): TypedResultTable[R] = selects(fn)

  def fields(fns: (T => Object)*): TypedResultTable[T]

  def ignore(fns: (T => Object)*): TypedResultTable[T]
}

trait SubQuery extends TableLike {
  def get(alias: String): FieldExpr

  def join(t: TableLike, joinType: JoinType): TableLike

  def join(t: TableLike): TableLike = join(t, JoinType.INNER)
}
