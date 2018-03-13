package io.github.yuemenglong.orm.operate.join

import java.lang

import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.traits.{Root, RootOp}
import io.github.yuemenglong.orm.operate.query._
import io.github.yuemenglong.orm.operate.query.traits.Selectable

/**
  * Created by <yuemenglong@126.com> on 2018/3/13.
  */
trait RootOpImpl extends RootOp {
  override def count(): Selectable[java.lang.Long] = new Count_

  override def count(field: Field): SelectableField[lang.Long] = new Count(field)

  override def sum[R](field: Field, clazz: Class[R]): SelectableField[R] = new Sum[R](field, clazz)

  override def max[R](field: Field, clazz: Class[R]): SelectableField[R] = new Max(field, clazz)

  override def min[R](field: Field, clazz: Class[R]): SelectableField[R] = new Min(field, clazz)
}

trait RootImpl[T] extends Root[T] with RootOpImpl {
  self: SelectableImpl[T] with SelectFieldJoinImpl with JoinImpl =>
}
