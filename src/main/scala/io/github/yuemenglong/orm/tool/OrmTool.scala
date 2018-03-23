package io.github.yuemenglong.orm.tool

import java.io.FileOutputStream
import java.lang.reflect.Field

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.entity.EntityManager
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.join.traits.{Cascade, Root, SelectFieldCascade, TypedSelectableCascade}
import io.github.yuemenglong.orm.operate.query.traits.Query1

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
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
    exportTsClass(path, init = false)
  }

  def exportTsClass(path: String, init: Boolean): Unit = {
    val relateMap = mutable.Map[String, Set[String]]()
    val classes = OrmMeta.entityVec.map(e => stringifyTsClass(e.clazz, relateMap, init)).mkString("\n\n")
    val fs = new FileOutputStream(path)
    fs.write(classes.getBytes())
    fs.close()
  }

  // relateMap保存所有与之相关的类型
  private def stringifyTsClass(clazz: Class[_], relateMap: mutable.Map[String, Set[String]], init: Boolean): String = {
    val className = clazz.getSimpleName
    val newFields = new ArrayBuffer[Field]()
    val content = Kit.getDeclaredFields(clazz).map(f => {
      val name = f.getName
      val typeName = f.getType.getSimpleName
      val ty = f.getType match {
        case Types.IntegerClass => "number = undefined"
        case Types.LongClass => "number = undefined"
        case Types.FloatClass => "number = undefined"
        case Types.DoubleClass => "number = undefined"
        case Types.BooleanClass => "boolean = undefined"
        case Types.StringClass => "string = undefined"
        case Types.DateClass => "string = undefined"
        case Types.BigDecimalClass => "number = undefined"
        case `clazz` => s"$typeName = undefined" // 自己引用自己
        case _ => f.getType.isArray match {
          case true => s"$typeName = []" // 数组
          case false => // 非数组
            init match {
              case false => s"$typeName = undefined"
              case true => //需要初始化
                relateMap.contains(typeName) match {
                  case false => // 该类型还没有导出过，安全的
                    newFields += f
                    s"$typeName = new $typeName()"
                  case true => relateMap(typeName).contains(className) match { // 导出过
                    case false => // 该类型与自己没有关系，安全的
                      newFields += f
                      s"$typeName = new $typeName()"
                    case true => s"$typeName = undefined" // 自己已经被该类型导出过，避免循环引用
                  }
                }
            }
        }
      }
      s"\t$name: $ty;"
    }).mkString("\n")
    // 与自己有关系的类型
    val relateSet = newFields.flatMap(f => {
      val name = f.getType.getSimpleName
      relateMap.get(name).orElse(Some(Set[String]())).get ++ Set(name)
    }).toSet ++ Set(className)
    relateMap += (className -> relateSet)
    // 包含自己的类型把这部分并进去
    relateMap.foreach { case (n, s) =>
      if (s.contains(className)) {
        relateMap += (n -> (s ++ relateSet))
      }
    }
    val ret = s"export class $className {\n$content\n}"
    ret
  }

  def attach[T, R](orig: T, session: Session)(fn: T => R): T = attachx(orig, session)(fn)(null, null)

  def attachs[T, R](orig: T, session: Session)(fn: T => Array[R]): T = attachsx(orig, session)(fn)(null, null)

  def sattach[T, R](orig: Array[T], session: Session)(fn: T => R): Array[T] = sattachx(orig, session)(fn)(null, null)

  def sattachs[T, R](orig: Array[T], session: Session)(fn: T => Array[R]): Array[T] = sattachsx(orig, session)(fn)(null, null)

  def attachx[T, R](orig: T, session: Session)
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

  def attachsx[T, R](orig: T, session: Session)
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

  def sattachx[T, R](orig: Array[T], session: Session)
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

  def sattachsx[T, R](orig: Array[T], session: Session)
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

  def selectById[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                (rootFn: (Root[T]) => Unit = null): T = {
    val root = Orm.root(clazz)
    if (rootFn != null) rootFn(root)
    val pkey = root.getMeta.pkey.name
    session.first(Orm.selectFrom(root).where(root.get(pkey).eql(id)))
  }

  def deleteById[T: ClassTag, V](clazz: Class[T], id: V, session: Session)
                                (rootFn: (Root[T]) => Array[Cascade] = (_: Root[T]) => Array[Cascade]()): Int = {
    val root = Orm.root(clazz)
    val cascade = rootFn(root)
    val all: Array[Cascade] = Array(root) ++ cascade
    val pkey = root.getMeta.pkey.name
    val ex = Orm.delete(all: _*).from(root).where(root.get(pkey).eql(id))
    session.execute(ex)
  }
}
