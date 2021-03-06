package io.github.yuemenglong.orm.impl.tool

import java.io.OutputStream

import io.github.yuemenglong.orm.api.OrmTool
import io.github.yuemenglong.orm.api.operate.query.Query1
import io.github.yuemenglong.orm.api.operate.sql.table.{ResultTable, Root, Table, TypedResultTable}
import io.github.yuemenglong.orm.api.session.Session
import io.github.yuemenglong.orm.impl.Orm
import io.github.yuemenglong.orm.impl.entity.{Entity, EntityCore, EntityManager}
import io.github.yuemenglong.orm.impl.kit.Kit
import io.github.yuemenglong.orm.impl.meta._
import io.github.yuemenglong.orm.impl.operate.sql.table.TableImpl

import scala.reflect.ClassTag

/**
 * Created by <yuemenglong@126.com> on 2017/10/10.
 */
//noinspection ScalaFileName
class OrmToolImpl extends OrmTool {
  def getConstructors: Map[Class[_], () => Object] = {
    OrmMeta.entityVec.map(meta => {
      val fn = () => {
        Orm.obj(meta.clazz).asInstanceOf[Object]
      }
      (meta.clazz, fn)
    }).toMap
  }

  def exportTsClass(os: OutputStream, prefix: String = "", imports: String = ""): Unit = Export.exportTsClass(os, prefix, imports)

  def exportDtClass(os: OutputStream): Unit = Export.exportDtClass(os)

  def attachX[T, R](orig: T, session: Session)
                   (fn: T => R)
                   (joinFn: TypedResultTable[R] => Unit,
                    queryFn: Query1[R] => Unit
                   ): T = {
    val obj = Orm.convert(orig)
    val entity = obj.asInstanceOf[Entity]
    val marker = EntityManager.createMarker[T](entity.$$core().meta)
    fn(marker)
    val field = marker.toString
    val jf = joinFn match {
      case null => null
      case _ => (join: ResultTable) => joinFn(join.asInstanceOf[TypedResultTable[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[R]) => queryFn(query)
    }
    attach(obj, field, session, jf, qf.asInstanceOf[Query1[_] => Unit])
  }

  def attachOneManyX[T, R](orig: T, session: Session)
                          (fn: T => Array[R])
                          (joinFn: TypedResultTable[R] => Unit,
                           queryFn: Query1[R] => Unit
                          ): T = {
    val obj = Orm.convert(orig)
    val entity = obj.asInstanceOf[Entity]
    val marker = EntityManager.createMarker[T](entity.$$core().meta)
    fn(marker)
    val field = marker.toString
    val jf = joinFn match {
      case null => null
      case _ => (join: ResultTable) => joinFn(join.asInstanceOf[TypedResultTable[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[R]) => queryFn(query)
    }
    attach(obj, field, session, jf, qf.asInstanceOf[Query1[_] => Unit])
  }

  def attachArrayX[T, R](orig: Array[T], session: Session)
                        (fn: T => R)
                        (joinFn: TypedResultTable[R] => Unit,
                         queryFn: Query1[R] => Unit
                        ): Array[T] = {
    if (orig.isEmpty) {
      return orig
    }
    val arr = Orm.convert(orig)
    val obj = arr(0)
    val entity = obj.asInstanceOf[Entity]
    val marker = EntityManager.createMarker[T](entity.$$core().meta)
    fn(marker)
    val field = marker.toString
    val jf = joinFn match {
      case null => null
      case _ => (join: ResultTable) => joinFn(join.asInstanceOf[TypedResultTable[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[_]) => queryFn(query.asInstanceOf[Query1[R]])
    }
    attach(arr, field, session, jf, qf)
  }

  def attachArrayOneManyX[T, R](orig: Array[T], session: Session)
                               (fn: T => Array[R])
                               (joinFn: TypedResultTable[R] => Unit,
                                queryFn: Query1[R] => Unit
                               ): Array[T] = {
    if (orig.isEmpty) {
      return orig
    }
    val arr = Orm.convert(orig)
    val obj = arr(0)
    val entity = obj.asInstanceOf[Entity]
    val marker = EntityManager.createMarker[T](entity.$$core().meta)
    fn(marker)
    val field = marker.toString
    val jf = joinFn match {
      case null => null
      case _ => (join: ResultTable) => joinFn(join.asInstanceOf[TypedResultTable[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[_]) => queryFn(query.asInstanceOf[Query1[R]])
    }
    attach(arr, field, session, jf, qf)
  }

  def attach[T](obj: T, field: String, session: Session,
                joinFn: ResultTable => Unit,
                queryFn: Query1[_] => Unit
               ): T = {
    if (obj.getClass.isArray) {
      return attachArray(obj.asInstanceOf[Array[_]], field, session, joinFn, queryFn.asInstanceOf[Query1[_] => Unit])
        .asInstanceOf[T]
    }
    if (!obj.isInstanceOf[Entity]) {
      throw new RuntimeException("Not Entity")
    }
    val entity = obj.asInstanceOf[Entity]
    val core = entity.$$core()
    val meta = core.meta
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isNormalOrPkey) {
      throw new RuntimeException(s"Not Refer Feild, $field")
    }
    val refer = meta.fieldMap(field).asInstanceOf[FieldMetaRefer]
    val root = Orm.root(refer.refer.clazz)
    val leftValue = core.get(refer.left)
    val rightField = refer.right
    if (joinFn != null) joinFn(root)

    val query = Orm.selectFrom(root)
    if (queryFn != null) queryFn(query.asInstanceOf[Query1[T]])
    val cond = root.get(rightField).eql(leftValue)
    query.where(cond)

    val res = refer.isOneMany match {
      case true => session.query(query).toArray(ClassTag(refer.refer.clazz)).asInstanceOf[Object]
      case false => session.first(query).asInstanceOf[Object]
    }

    EntityCore.setWithRefer(entity.$$core(), field, res)
    //    entity.$$core().setRefer(field, res)
    obj
  }

  private def attachArray(arr: Array[_], field: String, session: Session,
                          joinFn: ResultTable => Unit = null,
                          queryFn: Query1[_] => Unit = null
                         ): Array[_] = {
    if (arr.exists(!_.isInstanceOf[Entity])) {
      throw new RuntimeException("Array Has Item Not Entity")
    }
    if (arr.isEmpty) {
      return arr
    }

    val entities = arr.map(_.asInstanceOf[Entity])
    val meta = entities(0).$$core().meta
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isNormalOrPkey) {
      throw new RuntimeException(s"Not Refer Feild, $field")
    }
    val refer = meta.fieldMap(field).asInstanceOf[FieldMetaRefer]
    val root = Orm.root(refer.refer.clazz)
    val leftValues = entities.map(_.$$core().get(refer.left))
    val rightField = refer.right

    if (joinFn != null) joinFn(root)

    val query = Orm.selectFrom(root)
    if (queryFn != null) queryFn(query)
    val cond = root.get(rightField).in(leftValues)
    query.where(cond)

    val res: Map[Object, Object] = refer.isOneMany match {
      case true => session.query(query).map(_.asInstanceOf[Entity])
        .groupBy(_.$$core().get(rightField)).mapValues(_.toArray(ClassTag(refer.refer.clazz)))
      case false => session.query(query).map(_.asInstanceOf[Entity])
        .map(e => (e.$$core().get(rightField), e)).toMap
    }
    entities.foreach(e => {
      val leftValue = e.$$core().get(refer.left)
      if (res.contains(leftValue)) {
        EntityCore.setWithRefer(e.$$core(), field, res(leftValue))
        //        e.$$core().setRefer(field, res(leftValue))
      } else if (refer.isOneMany) {
        EntityCore.setWithRefer(e.$$core(), field, Kit.newArray(refer.refer.clazz))
      }
    })
    arr
  }

  override def updateByField[T, V](clazz: Class[T], session: Session,
                                   cond: (String, Any), pairs: (String, Any)*): Unit = {
    val root = Orm.root(clazz)
    val sets = pairs.map { case (k, v) =>
      root.get(k) === v
    }
    val ex = Orm.update(root).set(sets: _*).where(root.get(cond._1) === cond._2)
    session.execute(ex)
    //    val obj = Orm.create(clazz).asInstanceOf[Object]
    //    Orm.set(obj, cond._1, cond._2)
    //    pairs.foreach {
    //      case (f, v) => Orm.set(obj, f, v)
    //    }
    //    session.execute(Orm.update(obj))
  }

  override def updateByField[T, V](clazz: Class[T], session: Session)
                                  (condField: T => Any, fields: (T => Any)*)
                                  (condValue: Any, values: Any*): Unit = {
    val meta = OrmMeta.entityMap(clazz)
    val condName = {
      val marker = EntityManager.createMarker[T](meta)
      //noinspection ScalaUnusedExpression
      condField(marker)
      marker.toString
    }
    val fieldNames = fields.map(fn => {
      val marker = EntityManager.createMarker[T](meta)
      fn(marker)
      marker.toString
    })
    val newCond = (condName, condValue)
    val newPairs = fieldNames.zip(values)
    updateByField(clazz, session, newCond, newPairs: _*)
  }

  override def updateById[T, V](clazz: Class[T], id: V, session: Session)
                               (fields: (T => Any)*)(values: Any*): Unit = {
    val meta = OrmMeta.entityMap(clazz)
    val fieldNames = fields.map(fn => {
      val marker = EntityManager.createMarker[T](meta)
      fn(marker)
      marker.toString
    })
    val newCond = (meta.pkey.name, id)
    val newPairs = fieldNames.zip(values)
    updateByField(clazz, session, newCond, newPairs: _*)
  }

  override def updateById[T, V](clazz: Class[T], id: V, session: Session,
                                pairs: (String, Any)*): Unit = {
    val meta = OrmMeta.entityMap(clazz)
    updateByField(clazz, session, (meta.pkey.name, id), pairs: _*)
  }

  override def updateById[T, V](obj: T, session: Session)
                               (fns: (T => Any)*) {
    val clazz: Class[T] = obj.isInstanceOf[Entity] match {
      case true => obj.getClass.getSuperclass.asInstanceOf[Class[T]]
      case false => obj.getClass.asInstanceOf[Class[T]]
    }
    val meta = OrmMeta.entityMap(clazz)
    val names = fns.map(fn => {
      val marker = EntityManager.createMarker[T](meta)
      fn(marker)
      marker.toString
    })
    val values = names.map(name => {
      obj.asInstanceOf[Entity].$$core().getValue(name)
    })
    val id = obj.asInstanceOf[Entity].$$core().getPkey
    val pairs: Seq[(String, Any)] = names.zip(values)
    updateById(clazz, id, session, pairs: _*)
  }

  override def updateByField[T, V](obj: T, session: Session)
                                  (cond: T => Any)(fns: (T => Any)*) {
    val clazz: Class[T] = obj.isInstanceOf[Entity] match {
      case true => obj.getClass.getSuperclass.asInstanceOf[Class[T]]
      case false => obj.getClass.asInstanceOf[Class[T]]
    }
    val meta = OrmMeta.entityMap(clazz)
    val names = (Array(cond) ++ fns).map(fn => {
      val marker = EntityManager.createMarker[T](meta)
      fn(marker)
      marker.toString
    })
    val values = names.map(name => {
      obj.asInstanceOf[Entity].$$core().getValue(name)
    })
    val pairs = names.zip(values)
    val condPair = pairs(0)
    val kvPairs = pairs.drop(0)
    updateByField(clazz, session, condPair, kvPairs: _*)
  }

  def updateArray[T <: Object](root: Root[T], oldValues: Array[T], newValues: Array[T], session: Session): Unit = {
    val meta = root.asInstanceOf[TableImpl].getMeta
    val oldvs = Orm.convert(oldValues)
    val newvs = Orm.convert(newValues)
    val oldMap = oldvs.map(v => (v.asInstanceOf[Entity].$$core().getPkey, v)).toMap
    val newMap = newvs.map(v => (v.asInstanceOf[Entity].$$core().getPkey, v)).filter(_._1 != null).toMap

    // 没有id或者id不在老数据内 说明是新增的
    val toInsert = newvs.filter(obj => {
      val id = obj.asInstanceOf[Entity].$$core().getPkey
      id == null || !oldMap.keySet.contains(id)
    })
    if (toInsert.nonEmpty) {
      session.execute(Orm.insertArray(toInsert))
    }

    oldMap.keySet.intersect(newMap.keySet).foreach(key => {
      val ov = oldMap(key).asInstanceOf[Entity].$$core()
      val nv = newMap(key).asInstanceOf[Entity].$$core()
      if (!EntityCore.shallowEqual(ov, nv)) {
        val obj: T = newMap(key)
        session.execute(Orm.update(obj))
      }
    })

    val toDelete = oldMap.keySet.diff(newMap.keySet).toArray
    if (toDelete.nonEmpty) {
      session.execute(Orm.deleteFrom(root).where(root.get(meta.pkey.name).in(toDelete)))
    }
  }

  def selectByIdEx[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                  (rootFn: Root[T] => Unit = null): T = {
    val root = Orm.root(clazz)
    if (rootFn != null) rootFn(root)
    val pkey = root.asInstanceOf[TableImpl].getMeta.pkey.name
    session.first(Orm.selectFrom(root).where(root.get(pkey).eql(id)))
  }

  def deleteByIdEx[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                  (rootFn: Root[T] => Array[Table] = (_: Root[T]) => Array[Table]()
                                  ): Int = {
    val root = Orm.root(clazz)
    val cascade = rootFn(root)
    val all: Array[Table] = Array(root) ++ cascade
    val pkey = root.asInstanceOf[TableImpl].getMeta.pkey.name
    val ex = Orm.delete(all: _*).from(root).where(root.get(pkey).eql(id))
    session.execute(ex)
  }

  def deleteById[T: ClassTag, V](clazz: Class[T], id: V, session: Session): Int = {
    val entity = OrmMeta.entityMap(clazz)
    val table = entity.table
    val pkey = entity.pkey.column
    val sql = s"DELETE FROM `${table}` WHERE `${pkey}` = ?"
    session.execute(sql, Array(id.asInstanceOf[Object]))
  }

  def withRoot[T, R](clazz: Class[T]): (Root[T] => R) => R = {
    (fn: Root[T] => R) => {
      fn(Orm.root(clazz))
    }
  }
}

