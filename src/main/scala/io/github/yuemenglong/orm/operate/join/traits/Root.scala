//
//package io.github.yuemenglong.orm.operate.join.traits
//
//import io.github.yuemenglong.orm.meta.OrmMeta
//import io.github.yuemenglong.orm.operate.core.traits.JoinInner
//import io.github.yuemenglong.orm.operate.field.traits.{Field, SelectableField}
//import io.github.yuemenglong.orm.operate.join.{ExistsQ, NotExsQ}
//import io.github.yuemenglong.orm.operate.query.traits.SubQuery
//import io.github.yuemenglong.orm.operate.query.{Count_, _}
//
///**
//  * Created by <yuemenglong@126.com> on 2018/3/13.
//  */
//
//trait RootOp {
//  def count(): Count_ = new Count_
//
//  def count(field: Field): Count = new Count(field)
//
//  def sum[R](field: Field, clazz: Class[R]): Sum[R] = new Sum[R](field, clazz)
//
//  def sum[R](field: SelectableField[R]): Sum[R] = sum(field, field.getType)
//
//  def max[R](field: Field, clazz: Class[R]): SelectableField[R] = new Max(field, clazz)
//
//  def max[R](field: SelectableField[R]): SelectableField[R] = max(field, field.getType)
//
//  def min[R](field: Field, clazz: Class[R]): SelectableField[R] = new Min(field, clazz)
//
//  def min[R](field: SelectableField[R]): SelectableField[R] = min(field, field.getType)
//
//  def exists(query: SubQuery[_, _]): Cond = ExistsQ(query)
//
//  def notexs(query: SubQuery[_, _]): Cond = NotExsQ(query)
//}
//
////trait RootBase[T] extends TypedSelectableCascade[T] {
//trait RootBase[T] {
//
//}
//
//trait SubRoot[T] extends RootBase[T] {
//  private[orm] val no: Int
//}
//
//trait Root[T] extends RootBase[T] with RootOp {
////  var subCounter: Int = 0
////
////  def subRoot[R](clazz: Class[R]): SubRoot[R] = {
////    val subMeta = OrmMeta.entityMap(clazz)
////    subCounter += 1
////    val inn = new JoinInner(subMeta)
////    new SubRoot[R] {
////      override private[orm] val no = subCounter
////      override private[orm] val inner = inn
////    }
////  }
//}
//
////object Root {
////  def apply[T](clazz: Class[T]): Root[T] = {
////    val meta = OrmMeta.entityMap.get(clazz) match {
////      case Some(m) => m
////      case None => throw new RuntimeException(s"Not Entity Of [${clazz.getName}]")
////    }
////    val inn = new JoinInner(meta)
////    new Root[T] {
////      override private[orm] val inner = inn
////    }
////  }
////}
