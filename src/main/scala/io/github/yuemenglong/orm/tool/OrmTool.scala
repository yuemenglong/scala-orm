package io.github.yuemenglong.orm.tool

import java.io.FileOutputStream
import java.lang.reflect.Field

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.impl.QueryImpl
import io.github.yuemenglong.orm.operate.impl.core.SelectJoinImpl
import io.github.yuemenglong.orm.operate.traits.Query
import io.github.yuemenglong.orm.operate.traits.core.{Join, Root, SelectJoin}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

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
  def stringifyTsClass(clazz: Class[_], relateMap: mutable.Map[String, Set[String]], init: Boolean): String = {
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

  def attach[T](obj: T, field: String, session: Session,
                joinFn: SelectJoin => Unit = _ => {},
                queryFn: Query[T] => Unit = (_: Query[T]) => {},
               ): T = {
    if (!obj.isInstanceOf[Entity]) {
      throw new RuntimeException("Not Entity")
    }
    val entity = obj.asInstanceOf[Entity]
    if (entity.$$core().getPkey == null) {
      throw new RuntimeException("Not Has Pkey")
    }
    val meta = entity.$$core().meta
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isNormalOrPkey) {
      throw new RuntimeException(s"Not Refer Feild, $field")
    }
    val root = Orm.root(entity.$$core().meta.clazz.asInstanceOf[Class[T]])
    root.fields(Array[String]())
    val pkeyName = entity.$$core().meta.pkey.name
    val pkeyValue = entity.$$core().getPkey

    val join = root.select(field)
    joinFn(join)

    val query = Orm.selectFrom(root).asInstanceOf[QueryImpl[T]]
    queryFn(query)
    query.where(query.cond.and(root.get(pkeyName).eql(pkeyValue)))

    val res = session.first(query).asInstanceOf[Entity].$$core().get(field)
    entity.$$core().set(field, res)
    obj
  }

  def updateById[T, V](clazz: Class[T], id: V, session: Session,
                       pairs: (String, Any)*): Unit = {
    val root = Orm.root(clazz)
    val assigns = pairs.map { case (f, v) => root.get(f).assign(v.asInstanceOf[Object]) }
    val pkey = root.getMeta.pkey.name
    session.execute(Orm.update(root).set(assigns: _*).where(root.get(pkey).eql(id)))
  }

  def selectById[T, V](clazz: Class[T], id: V, session: Session,
                       rootFn: (Root[T]) => Unit = (_: Root[T]) => {}): T = {
    val root = Orm.root(clazz)
    rootFn(root)
    val pkey = root.getMeta.pkey.name
    session.first(Orm.selectFrom(root).where(root.get(pkey).eql(id)))
  }

  def deleteById[T, V](clazz: Class[T], id: V, session: Session,
                       rootFn: (Root[T]) => Array[Join] = (_: Root[T]) => Array[Join]()): Int = {
    val root = Orm.root(clazz)
    val cascade = rootFn(root)
    val all: Array[Join] = Array(root) ++ cascade
    val pkey = root.getMeta.pkey.name
    val ex = Orm.delete(all: _*).from(root).where(root.get(pkey).eql(id))
    session.execute(ex)
  }

  def clearField(obj: Object, field: String): Unit = {
    if (!obj.isInstanceOf[Entity]) {
      throw new RuntimeException("Not Entity")
    }
    if (!obj.asInstanceOf[Entity].$$core().meta.fieldMap.contains(field)) {
      throw new RuntimeException(s"Not A Valid Field, $field")
    }
    obj.asInstanceOf[Entity].$$core().fieldMap -= field
  }
}
