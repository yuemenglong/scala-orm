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
import io.github.yuemenglong.orm.operate.traits.core.{Root, SelectJoin}

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

  def exportTsClass3(path: String): Unit = {
    var referMap: Map[String, Set[String]] = Map()
    val checkWithMap = (meta: EntityMeta) => {
      require(!referMap.contains(meta.entity))
      val entity = meta.entity
      // 所有一对一的类型
      val refers = meta.fieldMap.values.filter(f => f.isPointer || f.isOneOne)
        .map(_.asInstanceOf[FieldMetaRefer].refer.entity)
      // 1. 获取不能new的字段(对方的展开结合里包含该类型,说明对方能new出该类型)
      val invalids = refers.filter(r => referMap.get(r) match {
        case Some(s) => s.contains(entity)
        case None => false
      }).toArray ++ Array(entity)
      // 2. 所有能new的字段(即一对一的)，打平set
      val set: Set[String] = refers.filter(!invalids.contains(_))
        .flatMap(e => referMap.get(e) match {
          case Some(s) => s ++ Set(e)
          case None => Set(e)
        }).toSet
      // 3. 所有已经存在的实体，且能new出当前类型的，将当前的集合并入
      referMap.mapValues(s => {
        if (s.contains(entity)) {
          s ++ set
        } else {
          s
        }
      })
      referMap += (entity -> set)
      invalids
    }
    val checkInvalid = (invalids: Array[String], field: FieldMetaRefer) => {
      if (invalids.contains(field.refer.entity)) {
        s"${field.refer.entity} = undefined"
      } else {
        s"${field.refer.entity} = new ${field.refer.entity}()"
      }
    }
    val classes = OrmMeta.entityVec.map(meta => {
      val invalids = checkWithMap(meta)
      val fields = meta.fieldVec.filter(field => {
        field match {
          case _: FieldMetaFkey => false
          case _ => true
        }
      }).map(field => {
        val name = field.name
        val ty = field match {
          case _: FieldMetaBoolean => "boolean = undefined"
          case _: FieldMetaInteger => "number = undefined"
          case _: FieldMetaSmallInt => "number = undefined"
          case _: FieldMetaTinyInt => "number = undefined"
          case _: FieldMetaLong => "number = undefined"
          case _: FieldMetaDouble => "number = undefined"
          case _: FieldMetaFloat => "number = undefined"
          case _: FieldMetaDecimal => "number = undefined"

          case _: FieldMetaString => "string = undefined"
          case _: FieldMetaEnum => "string = undefined"
          case _: FieldMetaLongText => "string = undefined"
          case _: FieldMetaDate => "string = undefined"
          case _: FieldMetaDateTime => "string = undefined"

          case field: FieldMetaPointer => checkInvalid(invalids, field)
          case field: FieldMetaOneOne => checkInvalid(invalids, field)
          case field: FieldMetaOneMany => s"Array<${field.refer.entity}> = []"
        }
        s"\t$name: $ty;"
      }).mkString("\n")
      s"export class ${meta.entity} {\n$fields\n}"
    }).mkString("\n\n")
    val fs = new FileOutputStream(path)
    fs.write(classes.getBytes())
    fs.close()
  }

  def exportTsClass(path: String): Unit = {
    val relateMap = mutable.Map[String, Set[String]]()
    val classes = OrmMeta.entityVec.map(e => stringifyTsClass(e.clazz, relateMap)).mkString("\n\n")
    val fs = new FileOutputStream(path)
    fs.write(classes.getBytes())
    fs.close()
  }

  // relateMap保存所有与之相关的类型
  def stringifyTsClass(clazz: Class[_], relateMap: mutable.Map[String, Set[String]]): String = {
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
                buildJoin: SelectJoin => Unit = _ => {},
                buildQuery: Query[T] => Unit = (_: Query[T]) => {},
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

    val join = root.select(field).asInstanceOf[SelectJoinImpl]
    buildJoin(join)

    val query = Orm.selectFrom(root).asInstanceOf[QueryImpl[T]]
    buildQuery(query)
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
                       rootFn: (Root[T]) => Unit = (_: Root[T]) => {}): Int = {
    val root = Orm.root(clazz)
    rootFn(root)
    val pkey = root.getMeta.pkey.name
    session.execute(Orm.deleteFrom(root).where(root.get(pkey).eql(id)))
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
