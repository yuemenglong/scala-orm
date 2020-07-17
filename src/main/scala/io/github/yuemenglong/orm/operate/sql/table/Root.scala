
package io.github.yuemenglong.orm.operate.sql.table

import io.github.yuemenglong.orm.api.operate.sql.core.Expr
import io.github.yuemenglong.orm.impl.kit.Kit
import io.github.yuemenglong.orm.impl.meta.OrmMeta
import io.github.yuemenglong.orm.operate.sql.core.{TableLike, Var}

trait Root[T] extends TypedResultTable[T]

trait RootImpl[T] extends Root[T] with TypedResultTableImpl[T]

object Root {
  def apply[T](clazz: Class[T]): Root[T] = {
    val rootMeta = OrmMeta.entityMap.get(clazz) match {
      case Some(m) => m
      case None => throw new RuntimeException(s"Not Entity Of [${clazz.getName}]")
    }
    val table = TableLike(rootMeta.table, Kit.lowerCaseFirst(rootMeta.entity))
    new RootImpl[T] {
      override val meta = rootMeta
      override private[orm] val _table = table._table
      override private[orm] val _joins = table._joins
      override private[orm] val _on = Var[Expr](null)
    }
  }
}
