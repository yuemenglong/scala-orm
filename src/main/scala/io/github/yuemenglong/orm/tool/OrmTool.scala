package io.github.yuemenglong.orm.tool

import java.io.FileOutputStream

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.entity.EntityManager
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.anno.ExportTS
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.join.{Cascade, Root, SelectFieldCascade, TypedSelectableCascade}
import io.github.yuemenglong.orm.operate.query.Query1

import scala.reflect.ClassTag

/**
  * Created by <yuemenglong@126.com> on 2017/10/10.
  */
object OrmTool {
  def getEmptyConstructorMap: Map[Class[_], () => Object] = {
    OrmMeta.entityVec.map(meta => {
      val fn = () => {
        Orm.empty(meta.clazz).asInstanceOf[Object]
      }
      (meta.clazz, fn)
    }).toMap
  }

  def exportTsClass(path: String): Unit = {
    exportTsClass(path, "", "")
  }

  def exportTsClass(path: String, prefix: String, imports: String): Unit = {
    val classes = OrmMeta.entityVec.map(e => stringifyTsClass(e.clazz, prefix)).mkString("\n\n")
    val content = s"${imports}\r\n\r\n${classes}"
    val fs = new FileOutputStream(path)
    fs.write(content.getBytes())
    fs.close()
  }

  // relateMap保存所有与之相关的类型
  private def stringifyTsClass(clazz: Class[_], prefix: String): String = {
    val pfx = prefix match {
      case null | "" => ""
      case s => s"${s} "
    }
    val className = clazz.getSimpleName
    val content = Kit.getDeclaredFields(clazz).filter(p = f => {
      f.getDeclaredAnnotation(classOf[ExportTS]) match {
        case anno if anno != null && anno.ignore() => false
        case _ => true
      }
    }).map(f => {
      val name = f.getName
      val typeName = f.getType.getSimpleName
      val ty = f.getType match {
        case Types.IntegerClass => s"number = undefined"
        case Types.LongClass => s"number = undefined"
        case Types.FloatClass => s"number = undefined"
        case Types.DoubleClass => s"number = undefined"
        case Types.BooleanClass => s"boolean = undefined"
        case Types.StringClass => s"string = undefined"
        case Types.DateClass => s"string = undefined"
        case Types.DateTimeClass => s"string = undefined"
        case Types.BigDecimalClass => s"number = undefined"
        case `clazz` => s"$typeName = undefined" // 自己引用自己
        case _ => f.getType.isArray match {
          case true => s"$typeName = []" // 数组
          case false => f.getDeclaredAnnotation(classOf[ExportTS]) match {
            case anno if anno != null && anno.init() => s"$typeName = new $typeName()"
            case _ => s"$typeName = undefined"
          }
        }
      }
      s"\t${pfx}$name: $ty;"
    }).mkString("\n")
    val ret = s"export class $className {\n$content\n}"
    ret
  }

  def attach[T, R](orig: T, session: Session)(fn: T => R): T = attachX(orig, session)(fn)(null, null)

  def attachOneMany[T, R](orig: T, session: Session)(fn: T => Array[R]): T = attachOneManyX(orig, session)(fn)(null, null)

  def attachArray[T, R](orig: Array[T], session: Session)(fn: T => R): Array[T] = attachArrayX(orig, session)(fn)(null, null)

  def attachArrayOneMany[T, R](orig: Array[T], session: Session)(fn: T => Array[R]): Array[T] = attachArrayOneManyX(orig, session)(fn)(null, null)

  def attachX[T, R](orig: T, session: Session)
                   (fn: T => R)
                   (joinFn: TypedSelectableCascade[R] => Unit,
                    queryFn: Query1[R] => Unit
                   ): T = {
    val obj = Orm.convert(orig)
    val entity = obj.asInstanceOf[Entity]
    val marker = EntityManager.createMarker[T](entity.$$core().meta)
    fn(marker)
    val field = marker.toString
    val jf = joinFn match {
      case null => null
      case _ => (join: SelectFieldCascade) => joinFn(join.asInstanceOf[TypedSelectableCascade[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[R]) => queryFn(query)
    }
    attach(obj, field, session, jf, qf.asInstanceOf[Query1[_] => Unit])
  }

  def attachOneManyX[T, R](orig: T, session: Session)
                          (fn: T => Array[R])
                          (joinFn: TypedSelectableCascade[R] => Unit,
                           queryFn: Query1[R] => Unit
                          ): T = {
    val obj = Orm.convert(orig)
    val entity = obj.asInstanceOf[Entity]
    val marker = EntityManager.createMarker[T](entity.$$core().meta)
    fn(marker)
    val field = marker.toString
    val jf = joinFn match {
      case null => null
      case _ => (join: SelectFieldCascade) => joinFn(join.asInstanceOf[TypedSelectableCascade[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[R]) => queryFn(query)
    }
    attach(obj, field, session, jf, qf.asInstanceOf[Query1[_] => Unit])
  }

  def attachArrayX[T, R](orig: Array[T], session: Session)
                        (fn: T => R)
                        (joinFn: TypedSelectableCascade[R] => Unit,
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
      case _ => (join: SelectFieldCascade) => joinFn(join.asInstanceOf[TypedSelectableCascade[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[_]) => queryFn(query.asInstanceOf[Query1[R]])
    }
    attach(arr, field, session, jf, qf)
  }

  def attachArrayOneManyX[T, R](orig: Array[T], session: Session)
                               (fn: T => Array[R])
                               (joinFn: TypedSelectableCascade[R] => Unit,
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
      case _ => (join: SelectFieldCascade) => joinFn(join.asInstanceOf[TypedSelectableCascade[R]])
    }
    val qf = queryFn match {
      case null => null
      case _ => (query: Query1[_]) => queryFn(query.asInstanceOf[Query1[R]])
    }
    attach(arr, field, session, jf, qf)
  }

  def attach[T](obj: T, field: String, session: Session): T = attach(obj, field, session, null, null)

  def attach[T](obj: T, field: String, session: Session,
                joinFn: SelectFieldCascade => Unit,
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
    val where = query.core._where match {
      case null => cond
      case w => w.and(cond)
    }
    query.where(where)

    val res = refer.isOneMany match {
      case true => session.query(query).toArray(ClassTag(refer.refer.clazz)).asInstanceOf[Object]
      case false => session.first(query).asInstanceOf[Object]
    }

    entity.$$core().setRefer(field, res)
    obj
  }

  private def attachArray(arr: Array[_], field: String, session: Session,
                          joinFn: SelectFieldCascade => Unit = null,
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
    val where = query.core._where match {
      case null => cond
      case w => w.and(cond)
    }
    query.where(where)

    val res: Map[Object, Object] = refer.isOneMany match {
      case true => session.query(query).map(_.asInstanceOf[Entity])
        .groupBy(_.$$core().get(rightField)).mapValues(_.toArray(ClassTag(refer.refer.clazz)))
      case false => session.query(query).map(_.asInstanceOf[Entity])
        .map(e => (e.$$core().get(rightField), e)).toMap
    }
    entities.foreach(e => {
      val leftValue = e.$$core().get(refer.left)
      if (res.contains(leftValue)) {
        e.$$core().setRefer(field, res(leftValue))
      }
    })
    arr
  }

  def updateById[T, V](clazz: Class[T], id: V, session: Session,
                       pair: (String, Any), pairs: (String, Any)*): Unit = {
    val root = Orm.root(clazz)
    val assigns = (Array(pair) ++ pairs).map {
      case (f, v) => root.get(f).assign(v.asInstanceOf[Object])
    }
    val pkey = root.getMeta.pkey.name
    session.execute(Orm.update(root).set(assigns: _*).where(root.get(pkey).eql(id)))
  }

  def updateById[T, V](clazz: Class[T], id: V, session: Session)
                      (fns: (T => Any)*)
                      (values: Any*): Unit = {
    val meta = OrmMeta.entityMap(clazz)
    if (fns.length != values.length) {
      throw new RuntimeException("Fields And Values Length Not Match")
    }
    val names = fns.map(fn => {
      val marker = EntityManager.createMarker[T](meta)
      fn(marker)
      marker.toString
    })
    val ps = names.zip(values)
    ps.length match {
      case 0 =>
      case 1 => updateById(clazz, id, session, ps.head)
      case _ => updateById(clazz, id, session, ps.head, ps.drop(1): _*)
    }
  }

  def selectByIdEx[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                  (rootFn: Root[T] => Unit = null): T = {
    val root = Orm.root(clazz)
    if (rootFn != null) rootFn(root)
    val pkey = root.getMeta.pkey.name
    session.first(Orm.selectFrom(root).where(root.get(pkey).eql(id)))
  }

  def selectById[T: ClassTag, V](clazz: Class[T], id: V, session: Session): T = {
    selectByIdEx(clazz, id, session)()
  }

  def deleteByIdEx[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                  (rootFn: Root[T] => Array[Cascade] = (_: Root[T]) => Array[Cascade]()
                                  ): Int = {
    val root = Orm.root(clazz)
    val cascade = rootFn(root)
    val all: Array[Cascade] = Array(root) ++ cascade
    val pkey = root.getMeta.pkey.name
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
}
