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
          case _: FieldMetaBoolean => "boolean"

          case _: FieldMetaInteger => "number"
          case _: FieldMetaSmallInt => "number"
          case _: FieldMetaTinyInt => "number"
          case _: FieldMetaLong => "number"
          case _: FieldMetaDouble => "number"
          case _: FieldMetaFloat => "number"
          case _: FieldMetaDecimal => "number"

          case _: FieldMetaString => "string"
          case _: FieldMetaLongText => "string"
          case _: FieldMetaDate => "string"
          case _: FieldMetaDateTime => "string"

          case field: FieldMetaPointer => field.refer.entity
          case field: FieldMetaOneOne => field.refer.entity
          case field: FieldMetaOneMany => s"Array<${field.refer.entity}>"
        }
        s"\t$name: $ty = null;"
      }).mkString("\n")
      s"export class ${meta.entity} {\n$fields\n}"
    }).mkString("\n\n")
    val fs = new FileOutputStream(path)
    fs.write(classes.getBytes())
    fs.close()
  }
}
