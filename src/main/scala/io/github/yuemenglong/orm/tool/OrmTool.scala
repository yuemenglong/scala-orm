package io.github.yuemenglong.orm.tool

import java.io.FileOutputStream

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.meta._
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
    val classes = OrmMeta.entityVec.map(meta => {
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

          case field: FieldMetaPointer => s"${field.refer.entity} = new ${field.refer.entity}()"
          case field: FieldMetaOneOne => s"${field.refer.entity} = new ${field.refer.entity}()"
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
    val cond = join.impl.cond.and(root.get(pkeyName).eql(pkeyValue))
    join.on(cond)

    val query = Orm.selectFrom(root)
    buildQuery(query)
    val res = session.first(query).asInstanceOf[Entity].$$core().get(field)
    entity.$$core().set(field, res)
    obj
  }
}
