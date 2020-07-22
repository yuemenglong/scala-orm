
package io.github.yuemenglong.orm.impl.operate.sql.table

import io.github.yuemenglong.orm.api.operate.sql.core.{Expr, Var}
import io.github.yuemenglong.orm.api.operate.sql.table.Root
import io.github.yuemenglong.orm.impl.kit.Kit
import io.github.yuemenglong.orm.impl.meta.{EntityMeta, OrmMeta}
import io.github.yuemenglong.orm.impl.operate.sql.core.TableLikeUtil

object RootUtil {
  def create[T](clazz: Class[T]): Root[T] = {
    val rootMeta = OrmMeta.entityMap.get(clazz) match {
      case Some(m) => m
      case None => throw new RuntimeException(s"Not Entity Of [${clazz.getName}]")
    }
    val table = TableLikeUtil.create(rootMeta.table, Kit.lowerCaseFirst(rootMeta.entity))
    new RootImpl[T] {
      override val meta: EntityMeta = rootMeta
      override private[orm] val _table = table._table
      override private[orm] val _joins = table._joins
      override private[orm] val _on = Var[Expr](null)
    }
  }
}

trait RootImpl[T] extends Root[T] with TypedResultTableImpl[T]


