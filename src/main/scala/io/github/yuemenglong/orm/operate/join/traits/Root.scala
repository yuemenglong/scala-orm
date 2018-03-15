package io.github.yuemenglong.orm.operate.join.traits

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.query.{Count, Count_, Sum}
import io.github.yuemenglong.orm.operate.query.traits.{Selectable, SubQuery}

/**
  * Created by <yuemenglong@126.com> on 2018/3/13.
  */

trait RootOp {
  def count(): Count_

  def count(field: Field): Count

  def sum[R](field: Field, clazz: Class[R]): Sum[R]

  def sum[R](field: SelectableField[R]): Sum[R] = sum(field, field.getType)

  def max[R](field: Field, clazz: Class[R]): SelectableField[R]

  def max[R](field: SelectableField[R]): SelectableField[R] = max(field, field.getType)

  def min[R](field: Field, clazz: Class[R]): SelectableField[R]

  def min[R](field: SelectableField[R]): SelectableField[R] = min(field, field.getType)

  def exists(query: SubQuery[_, _]): Cond

  def notexs(query: SubQuery[_, _]): Cond
}

trait RootBase[T] extends TypedSelectCascade[T] with TypedCascade[T]
  with Selectable[T] with SelectFieldCascade with Cascade {
}

trait SubRoot[T] extends RootBase[T] {
}

trait Root[T] extends RootBase[T] with RootOp {
  def subRoot[R](clazz: Class[R]): SubRoot[R]
}
