package io.github.yuemenglong.orm.tool

import java.io.FileOutputStream

import io.github.yuemenglong.orm.Orm
import io.github.yuemenglong.orm.meta._

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
}
