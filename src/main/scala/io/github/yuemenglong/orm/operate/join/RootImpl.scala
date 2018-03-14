package io.github.yuemenglong.orm.operate.join

import java.lang
import java.sql.ResultSet

import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.meta.OrmMeta
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.join.traits._
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

  var subCounter: Int = 0

  override def subRoot[R](clazz: Class[R]) = {
    val subMeta = OrmMeta.entityMap(clazz)
    subCounter += 1
    new SubRoot[R]
      with TypedSelectJoinImpl[R] with TypedJoinImpl[R]
      with SelectableImpl[R] with SelectFieldJoinImpl with JoinImpl {
      override val inner = new JoinInner {
        override val meta = subMeta
        override val parent = null
        override val left = null
        override val right = null
        override val joinName = null
        override val joinType = null
        override val subIdx = subCounter
      }
    }
  }
}
