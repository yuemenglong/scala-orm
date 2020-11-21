package io.github.yuemenglong.orm.impl.tool

import java.io.OutputStream

import io.github.yuemenglong.orm.api.anno.{ExportDT, ExportTS}
import io.github.yuemenglong.orm.api.anno.predef.Const
import io.github.yuemenglong.orm.api.types.Types
import io.github.yuemenglong.orm.impl.kit.Kit
import io.github.yuemenglong.orm.impl.meta.OrmMeta

object Export {
  def exportTsClass(os: OutputStream, prefix: String = "", imports: String = ""): Unit = {
    val classes = OrmMeta.entityVec.map(e => stringifyTsClass(e.clazz, prefix)).mkString("\n\n")
    val content = s"${imports}\r\n\r\n${classes}"
    os.write(content.getBytes())
    os.close()
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
      val anno = f.getDeclaredAnnotation(classOf[ExportTS])
      val ty = f.getType match {
        case Types.IntegerClass => s"number"
        case Types.LongClass => s"number"
        case Types.FloatClass => s"number"
        case Types.DoubleClass => s"number"
        case Types.BooleanClass => s"boolean"
        case Types.StringClass => s"string"
        case Types.DateClass => s"string"
        case Types.DateTimeClass => s"string"
        case Types.BigDecimalClass => s"number"
        case _ => s"$typeName"
      }
      val init = anno != null && anno.value() != Const.ANNOTATION_STRING_NULL match {
        case true => anno.value()
        case false => "null"
      }
      val value = f.getType match {
        case Types.IntegerClass => init
        case Types.LongClass => init
        case Types.FloatClass => init
        case Types.DoubleClass => init
        case Types.BooleanClass => init
        case Types.StringClass => init
        case Types.DateClass => init
        case Types.DateTimeClass => init
        case Types.BigDecimalClass => init
        case `clazz` => "null" // 自己引用自己
        case _ => f.getType.isArray match {
          case true => init match { // 数组的情况
            case "null" => "[]" // 默认的情况
            case s => s
          }
          case false => init match { // 对象的情况
            case "" => s"new $typeName()" // 特殊指定的情况
            case s => s
          }
        }
      }
      s"\t${pfx}$name: $ty = ${value};"
    }).mkString("\n")
    val ret = s"export class $className {\n$content\n}"
    ret
  }

  def exportDtClass(os: OutputStream): Unit = {
    val classes = OrmMeta.entityVec.map(e => stringifyDtClass(e.clazz)).mkString("\n\n")
    val content = s"${classes}"
    os.write(content.getBytes())
    os.close()
  }

  private def stringifyDtClass(clazz: Class[_]): String = {
    def getType(clazz: Class[_]): String = {
      clazz match {
        case Types.IntegerClass => s"int"
        case Types.LongClass => s"int"
        case Types.FloatClass => s"double"
        case Types.DoubleClass => s"double"
        case Types.BooleanClass => s"bool"
        case Types.StringClass => s"String"
        case Types.DateClass => s"String"
        case Types.DateTimeClass => s"String"
        case Types.BigDecimalClass => s"double"
        case t if t.isArray => s"List<${getType(Kit.getArrayType(t))}>"
        case t => t.getSimpleName
      }
    }

    val className = clazz.getSimpleName
    val fields = Kit.getDeclaredFields(clazz).filter(p = f => {
      f.getDeclaredAnnotation(classOf[ExportDT]) match {
        case anno if anno != null && anno.ignore() => false
        case _ => true
      }
    })
    val filedsContent = fields.map(f => {
      val name = f.getName
      val anno = f.getDeclaredAnnotation(classOf[ExportDT])
      val ty = getType(f.getType)
      val initValue = anno != null && anno.value() != Const.ANNOTATION_STRING_NULL match {
        case true => " = " + anno.value()
        case false => f.getType.isArray match {
          case true => " = []"
          case false => ""
        }
      }
      s"  ${ty} ${name}${initValue};"
    }).mkString("\n")
    val toJsonBody = fields.map(f => {
      val name = f.getName
      val ty = getType(f.getType)
      ty match {
        case "int" | "double" | "bool" | "String" => s"""      "${name}": ${name},"""
        case x if x.startsWith("List") => s"""      "${name}": ${name}?.map((x) => x.toJson())?.toList(),"""
        case x => s"""      "${name}": ${name}?.toJson(),"""
      }
    }).mkString("\n")
    val toJsonFn =
      s"""  Map toJson() {
         |    return {
         |${toJsonBody}
         |    };
         |  }""".stripMargin
    val fromJsonBody = fields.map(f => {
      val name = f.getName
      val ty = getType(f.getType)
      ty match {
        case "int" | "double" | "bool" | "String" => s"""    ${name} = $$map["${name}"];"""
        case x if x.startsWith("List") =>
          val T = Kit.getArrayType(f.getType).getSimpleName
          s"""    ${name} = List<${T}>.from($$map["${name}"]?.map((x) => ${T}.fromJson(x)) ?? []);"""
        case x => s"""    ${name} = $$map["${name}"] != null ? ${x}.fromJson($$map["${name}"]) : null;"""
      }
    }).mkString("\n")
    val fromJsonFn =
      s"""  ${className}.fromJson(Map $$map) {
         |${fromJsonBody}
         |  }""".stripMargin
    val ret =
      s"""class ${className} {
         |${filedsContent}
         |  ${className}() {}
         |${fromJsonFn}
         |${toJsonFn}
         |}""".stripMargin
    ret
  }

  def main(args: Array[String]): Unit = {
    val res = stringifyDtClass(classOf[T])
    println(res)
  }
}

class T {
  @ExportDT(ignore = true)
  var i: Integer = _
  var date: Types.Date = _
  var ii: Array[T] = _
  var t: T = _
}