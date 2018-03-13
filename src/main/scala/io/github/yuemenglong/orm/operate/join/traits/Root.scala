package io.github.yuemenglong.orm.operate.join.traits

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.query.traits.Selectable

/**
  * Created by <yuemenglong@126.com> on 2018/3/13.
  */

trait RootOp {
  def count(): Selectable[java.lang.Long]

  def count(field: Field): SelectableField[java.lang.Long]

  def sum[R](field: Field, clazz: Class[R]): SelectableField[R]

  def sum[R](field: SelectableField[R]): SelectableField[R] = sum(field, field.getType)

  def max[R](field: Field, clazz: Class[R]): SelectableField[R]

  def max[R](field: SelectableField[R]): SelectableField[R] = max(field, field.getType)

  def min[R](field: Field, clazz: Class[R]): SelectableField[R]

  def min[R](field: SelectableField[R]): SelectableField[R] = min(field, field.getType)
}

trait IRoot[T] extends TypedSelectJoin[T] with TypedJoin[T]
  with Selectable[T] with SelectFieldJoin with Join

trait SubRoot[T] extends IRoot[T] {

  val index: Int

  override def getAlias = s"${Kit.lowerCaseFirst(getMeta.entity)}$$${index}"
}

trait Root[T] extends IRoot[T] with RootOp {
  def sub[R](clazz: Class[R]): SubRoot[R]
}
