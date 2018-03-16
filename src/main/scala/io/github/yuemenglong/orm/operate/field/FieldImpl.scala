package io.github.yuemenglong.orm.operate.field

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.meta.FieldMeta
import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
import io.github.yuemenglong.orm.operate.join.traits.Cascade

//import io.github.yuemenglong.orm.kit.Kit
//import io.github.yuemenglong.orm.meta.FieldMeta
//import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
//import io.github.yuemenglong.orm.operate.join.CascadeImpl

/**
  * Created by <yuemenglong@126.com> on 2017/7/15.
  */
class FieldImpl(val meta: FieldMeta, val parent: Cascade) extends Field {
  override def getField: String = meta.name

  override def getColumn: String = s"${parent.getAlias}.${meta.column}"

  override def getAlias: String = s"${parent.getAlias}$$${Kit.lodashCase(meta.name)}"

  override def as[T](clazz: Class[T]): SelectableField[T] = new SelectableFieldImpl[T](clazz, this)
}

class SelectableFieldImpl[T](clazz: Class[T], val impl: Field) extends SelectableField[T] {

  override def getColumn: String = s"${impl.getColumn}"

  override def getField: String = impl.getField

  override def getAlias: String = impl.getAlias

  override def getType: Class[T] = clazz

  override def as[R](clazz: Class[R]): SelectableField[R] = throw new RuntimeException("Already Selectable")
}
