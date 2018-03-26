
package io.github.yuemenglong.orm.operate.join

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.meta.OrmMeta
import io.github.yuemenglong.orm.sql.{Expr, Table, Var}

trait Root[T] extends TypedSelectableCascade[T] {}

object Root {
  def apply[T](clazz: Class[T]): Root[T] = {
    val rootMeta = OrmMeta.entityMap.get(clazz) match {
      case Some(m) => m
      case None => throw new RuntimeException(s"Not Entity Of [${clazz.getName}]")
    }
    val table = Table(rootMeta.table, Kit.lowerCaseFirst(rootMeta.entity))
    new Root[T] {
      override val meta = rootMeta
      override private[orm] val _table = table._table
      override private[orm] val _joins = table._joins
      override private[orm] val _on = Var[Expr](null)
    }
  }
}
