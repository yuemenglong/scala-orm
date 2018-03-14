package io.github.yuemenglong.orm.operate.join

import java.lang

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.meta.OrmMeta
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.traits._
import io.github.yuemenglong.orm.operate.query._
import io.github.yuemenglong.orm.operate.query.traits.{Selectable, SubQuery}

/**
  * Created by <yuemenglong@126.com> on 2018/3/13.
  */
trait RootOpImpl extends RootOp {
  override def count(): Selectable[java.lang.Long] = new Count_

  override def count(field: Field): SelectableField[lang.Long] = new Count(field)

  override def sum[R](field: Field, clazz: Class[R]): SelectableField[R] = new Sum[R](field, clazz)

  override def max[R](field: Field, clazz: Class[R]): SelectableField[R] = new Max(field, clazz)

  override def min[R](field: Field, clazz: Class[R]): SelectableField[R] = new Min(field, clazz)

  override def exists(query: SubQuery[_, _]): Cond = ExistsQ(query)

  override def notexs(query: SubQuery[_, _]): Cond = NotExsQ(query)
}

trait RootImpl[T] extends Root[T] with RootOpImpl {
  self: SelectableImpl[T] with SelectFieldJoinImpl with JoinImpl =>

  var subCounter: Int = 0

  override def subRoot[R](clazz: Class[R]): SubRoot[R] = {
    val subMeta = OrmMeta.entityMap(clazz)
    subCounter += 1
    new TypedSelectJoinImpl[R] with TypedJoinImpl[R]
      with SelectableImpl[R] with SelectFieldJoinImpl with JoinImpl
      with SubRootImpl[R] {
      override val inner = new JoinInner {
        override val meta = subMeta
        override val parent = null
        override val left = null
        override val right = null
        override val joinName = null
        override val joinType = null
      }
      override val no = subCounter
    }
  }
}

trait SubRootImpl[T] extends SubRoot[T] {
  val no: Int

  override def getAlias = {
    s"${no}$$${Kit.lowerCaseFirst(getMeta.entity)}"
  }
}
