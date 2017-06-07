package orm.meta

import java.lang
import java.lang.reflect.{Field, ParameterizedType}
import java.text.SimpleDateFormat
import java.util.Date

import orm.kit.Kit
import orm.lang.anno._

object FieldMetaTypeKind {
  val BUILT_IN: Int = 0
  val REFER: Int = 1
  val POINTER: Int = 2
  val ONE_ONE: Int = 3
  val ONE_MANY: Int = 4
  val IGNORE_BUILT_IN: Int = 100
  val IGNORE_REFER: Int = 101
  val IGNORE_MANY: Int = 102
}

class FieldMeta(val entity: EntityMeta,
                val field: Field,
                val pkey: Boolean,
                val auto: Boolean,

                val typeKind: Int,
                val typeName: String,

                val name: String,
                val column: String,
                val columnAnno: Column,
                val ignore: Boolean,

                val left: String,
                val right: String) {
  var refer: EntityMeta = null // 最后统一注入，因为第一遍扫描时可能还没有生成

  def getDbSql(): String = {
    var length = FieldMeta.DEFAULT_LEN
    var notnull = FieldMeta.DEFAULT_NOT_NULL;
    var bigDecimalDetail = ""

    if (columnAnno != null) {
      length = columnAnno.length()
      notnull = columnAnno.nullable() match {
        case true => ""
        case false => " NOT NULL"
      }
      bigDecimalDetail = (columnAnno.precision(), columnAnno.scale()) match {
        case (0, 0) => ""
        case (p, s) => s"(${p},${s})"
      }
    }

    val pkey = (this.pkey, this.auto) match {
      case (false, _) => ""
      case (true, false) => " PRIMARY KEY"
      case (true, true) => " PRIMARY KEY AUTO_INCREMENT"
    }
    this.typeName match {
      case "Integer" => s"`${this.column}` INTEGER${notnull}${pkey}"
      case "Long" => s"`${this.column}` BIGINT${notnull}${pkey}"
      case "Float" => s"`${this.column}` FLOAT${notnull}${pkey}"
      case "Double" => s"`${this.column}` DOUBLE${notnull}${pkey}"
      case "Boolean" => s"`${this.column}` BOOLEAN${notnull}${pkey}"
      case "BigDecimal" => s"`${this.column}` DECIMAL${bigDecimalDetail}${notnull}${pkey}"
      case "Date" => s"`${this.column}` DATE${notnull}${pkey}"
      case "DateTime" => s"`${this.column}` DATETIME${notnull}${pkey}"
      case "String" => s"`${this.column}` VARCHAR(${length})${notnull}${pkey}"
      case "LongText" => s"`${this.column}` LONGTEXT${notnull}${pkey}"
      case _ => throw new RuntimeException()
    }
  }

  def isNormal(): Boolean = {
    if (this.pkey) {
      return false
    }
    return this.isNormalOrPkey()
  }

  def isPkey(): Boolean = {
    this.pkey
  }

  def isNormalOrPkey(): Boolean = {
    this.typeKind == FieldMetaTypeKind.BUILT_IN
  }

  def isObject(): Boolean = {
    this.typeKind match {
      case FieldMetaTypeKind.BUILT_IN | FieldMetaTypeKind.IGNORE_BUILT_IN => false
      case _ => true
    }
  }

  def isRefer(): Boolean = {
    this.typeKind == FieldMetaTypeKind.REFER
  }

  def isPointer(): Boolean = {
    this.typeKind == FieldMetaTypeKind.POINTER
  }

  def isOneOne(): Boolean = {
    this.typeKind == FieldMetaTypeKind.ONE_ONE
  }

  def isOneMany(): Boolean = {
    this.typeKind == FieldMetaTypeKind.ONE_MANY
  }

  def parse(json: String): Object = {
    require(typeKind == FieldMetaTypeKind.BUILT_IN ||
      typeKind == FieldMetaTypeKind.IGNORE_BUILT_IN)
    if (json == "null") {
      return null
    }
    typeName match {
      case "Integer" => new lang.Integer(lang.Double.parseDouble(json).toInt)
      case "Long" => new lang.Long(lang.Double.parseDouble(json).toLong)
      case "Float" => new lang.Float(lang.Double.parseDouble(json).toFloat)
      case "Double" => new lang.Double(lang.Double.parseDouble(json))
      case "Boolean" => new lang.Boolean(lang.Boolean.parseBoolean(json))
      case "BigDecimal" => new java.math.BigDecimal(json)
      case "Date" => new SimpleDateFormat("yyyy-MM-dd").parse(json)
      case "DateTime" => new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(json)
      case "String" => json
      case "LongText" => json
      case _ => throw new RuntimeException()
    }
  }

  def stringify(data: Object): Object = {
    require(data != null)
    typeName match {
      case "Integer" => data
      case "Long" => data
      case "Float" => data
      case "Double" => data
      case "Boolean" => data
      case "BigDecimal" => new lang.Double(data.asInstanceOf[java.math.BigDecimal].doubleValue())
      case "Date" => new SimpleDateFormat("yyyy-MM-dd").format(data.asInstanceOf[Date])
      case "DateTime" => new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(data.asInstanceOf[Date])
      case "String" => data.toString()
      case "LongText" => data.toString()
      case _ => throw new RuntimeException()
    }
  }
}

object FieldMeta {
  val DEFAULT_LEN: Int = 255
  val DEFAULT_NOT_NULL: String = ""

  def pickColumn(field: Field, typeKind: Int): String = {
    // 非内建类型不对应数据库列
    if (typeKind != FieldMetaTypeKind.BUILT_IN) {
      return null
    }
    field.getDeclaredAnnotation(classOf[Column]) match {
      case null => Kit.lodashCase(field.getName())
      case column => column.name() match {
        case null | "" => Kit.lodashCase(field.getName())
        case name => name
      }
    }
  }

  def pickTypeName(field: Field): String = {
    val typeName = Kit.getGenericType(field).getSimpleName()
    typeName match {
      case "Date" => {
        field.getDeclaredAnnotation(classOf[DateTime]) match {
          case null => "Date"
          case _ => "DateTime"
        }
      }
      case "String" => {
        field.getDeclaredAnnotation(classOf[LongText]) match {
          case null => "String"
          case _ => "LongText"
        }
      }
      case _ => typeName
    }
  }


  def pickLeftRight(entity: EntityMeta, field: Field, typeKind: Int): (String, String) = {
    typeKind match {
      case FieldMetaTypeKind.BUILT_IN |
           FieldMetaTypeKind.IGNORE_BUILT_IN |
           FieldMetaTypeKind.IGNORE_REFER |
           FieldMetaTypeKind.IGNORE_MANY => {
        return (null, null)
      }
      case FieldMetaTypeKind.REFER => {
        val anno = field.getAnnotation(classOf[Refer])
        return (anno.left(), anno.right())
      }
      case FieldMetaTypeKind.POINTER => {
        val anno = field.getAnnotation(classOf[Pointer])
        val left = anno.left() match {
          case "" => field.getName().toLowerCase() + "_id"
          case _ => anno.left()
        }
        val right = anno.right() match {
          case "" => "id"
          case _ => anno.right()
        }
        return (left, right)
      }
      case FieldMetaTypeKind.ONE_ONE => {
        val anno = field.getAnnotation(classOf[OneToOne])
        val left = anno.left() match {
          case "" => "id"
          case _ => anno.left()
        }
        val right = anno.right() match {
          case "" => entity.entity.toLowerCase() + "_id"
          case _ => anno.right()
        }
        return (left, right)
      }
      case FieldMetaTypeKind.ONE_MANY => {
        val anno = field.getAnnotation(classOf[OneToMany])
        val left = anno.left() match {
          case "" => "id"
          case _ => anno.left()
        }
        val right = anno.right() match {
          case "" => entity.entity.toLowerCase() + "_id"
          case _ => anno.right()
        }
        return (left, right)
      }
    }
  }

  def pickTypeKind(entity: EntityMeta, field: Field): Int = {
    val builtIn = field.getType().getSimpleName() match {
      case "Integer" |
           "Long" |
           "Float" |
           "Double" |
           "Boolean" |
           "BigDecimal" |
           "Date" |
           "String" => true
      case _ => false
    }
    if (field.getDeclaredAnnotation(classOf[Ignore]) != null || entity.ignore) {
      if (builtIn) {
        return FieldMetaTypeKind.IGNORE_BUILT_IN
      } else if (!Kit.isGenericType(field)) {
        return FieldMetaTypeKind.IGNORE_REFER
      } else {
        return FieldMetaTypeKind.IGNORE_MANY
      }
    }
    if (builtIn) {
      return FieldMetaTypeKind.BUILT_IN
    }
    if (field.getDeclaredAnnotation(classOf[Refer]) != null) {
      return FieldMetaTypeKind.REFER
    }
    if (field.getDeclaredAnnotation(classOf[Pointer]) != null) {
      return FieldMetaTypeKind.POINTER
    }
    if (field.getDeclaredAnnotation(classOf[OneToOne]) != null) {
      return FieldMetaTypeKind.ONE_ONE
    }
    if (field.getDeclaredAnnotation(classOf[OneToMany]) != null) {
      return FieldMetaTypeKind.ONE_MANY
    }
    if (field.getDeclaredAnnotation(classOf[Ignore]) != null || entity.ignore) {
      return FieldMetaTypeKind.IGNORE_BUILT_IN
    }
    throw new RuntimeException(s"[${field.getName()}] Must Has A Refer Type")
  }

  def pickIdAuto(field: Field): Boolean = {
    field.getDeclaredAnnotation(classOf[Id]).auto()
  }

  def pickId(field: Field): Boolean = {
    field.getDeclaredAnnotation(classOf[Id]) != null
  }

  def createFieldMeta(entity: EntityMeta, field: Field): FieldMeta = {
    val pkey: Boolean = FieldMeta.pickId(field)
    val auto: Boolean = pkey && FieldMeta.pickIdAuto(field)

    val typeKind: Int = FieldMeta.pickTypeKind(entity, field)
    val typeName: String = FieldMeta.pickTypeName(field)

    val name: String = field.getName()
    val column: String = FieldMeta.pickColumn(field, typeKind)
    val columnAnno: Column = field.getDeclaredAnnotation(classOf[Column])
    val ignore = typeKind >= FieldMetaTypeKind.IGNORE_BUILT_IN

    val (left, right) = FieldMeta.pickLeftRight(entity, field, typeKind)

    return new FieldMeta(entity, field,
      pkey, auto,
      typeKind, typeName,
      name, column, columnAnno, ignore,
      left, right)
  }

  def createReferMeta(entity: EntityMeta, fieldName: String): FieldMeta = {
    // 默认Long
    val field: Field = null
    val pkey: Boolean = false
    val auto: Boolean = false

    val typeKind: Int = FieldMetaTypeKind.BUILT_IN
    val typeName: String = "Long"

    val name: String = fieldName
    val column: String = fieldName
    val columnAnno: Column = null
    val ignore = false

    val left: String = null
    val right: String = null

    return new FieldMeta(entity, field,
      pkey, auto,
      typeKind, typeName,
      name, column, columnAnno, ignore,
      left, right)
  }
}
