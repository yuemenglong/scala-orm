package io.github.yuemenglong.orm.tool

import java.io.FileOutputStream

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.impl.QueryImpl
import io.github.yuemenglong.orm.operate.impl.core.SelectJoinImpl
import io.github.yuemenglong.orm.operate.traits.Query
import io.github.yuemenglong.orm.operate.traits.core.SelectJoin

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
    var referMap: Map[String, Set[String]] = Map()
    val checkWithMap = (meta: EntityMeta) => {
      require(!referMap.contains(meta.entity))
      val entity = meta.entity
      val refers = meta.fieldMap.values.filter(f => f.isPointer || f.isOneOne)
        .map(_.asInstanceOf[FieldMetaRefer].refer.entity)
      // 1. 获取不能new的字段
      val invalids = refers.filter(r => referMap.get(r) match {
        case Some(s) => s.contains(entity)
        case None => false
      }).toArray
      // 2. 所有能new的字段，打平set
      val set: Set[String] = refers.filter(!invalids.contains(_))
        .flatMap(e => referMap.get(e) match {
          case Some(s) => s ++ Set(e)
          case None => Set(e)
        }).toSet
      // 3. 所有已经存在的实体，将当前entity加入
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
          case _: FieldMetaLongText => "string = undefined"
          case _: FieldMetaDate => "string = undefined"
          case _: FieldMetaDateTime => "string = undefined"

          //          case field: FieldMetaPointer => s"${field.refer.entity} = new ${field.refer.entity}()"
          //          case field: FieldMetaOneOne => s"${field.refer.entity} = new ${field.refer.entity}()"
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

  def selectById[T, V](clazz: Class[T], id: V, session: Session): T = {
    val root = Orm.root(clazz)
    val pkey = root.getMeta.pkey.name
    session.first(Orm.selectFrom(root).where(root.get(pkey).eql(id)))
  }

  def deleteById[T, V](clazz: Class[T], id: V, session: Session): Int = {
    val obj = Orm.empty(clazz).asInstanceOf[Object]
    val pkey = OrmMeta.entityMap(clazz.getSimpleName).pkey.name
    obj.asInstanceOf[Entity].$$core().fieldMap += (pkey -> id.asInstanceOf[Object])
    session.execute(Orm.delete(obj))
  }

  def clearField(obj: Object, field: String): Unit = {
    if (!obj.isInstanceOf[Entity]) {
      throw new RuntimeException("Not Entity")
    }
    if (!obj.asInstanceOf[Entity].$$core().meta.fieldMap.contains(field)) {
      throw new RuntimeException(s"Not A Valid Field, ${field}")
    }
    obj.asInstanceOf[Entity].$$core().fieldMap -= field
  }
}
