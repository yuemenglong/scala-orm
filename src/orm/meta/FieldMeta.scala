package orm.meta

import java.lang.reflect.{Field, ParameterizedType}
import java.text.SimpleDateFormat
import java.util.Date

import orm.lang.anno._

object FieldMetaTypeKind {
  val BUILT_IN: Int = 0
  val REFER: Int = 1
  val POINTER: Int = 2
  val ONE_ONE: Int = 3
  val ONE_MANY: Int = 4
}

class FieldMeta(val entity: EntityMeta,
                val field: Field,
                val pkey: Boolean,
                val auto: Boolean,

                val typeKind: Int,
                val typeName: String,
                val name: String,
                val column: String,

                val nullable: Boolean,
                val length: Int,

                val left: String,
                val right: String) {
  var refer: EntityMeta = null // 最后统一注入，因为第一遍扫描时可能还没有生成

  def getDbSql(): String = {
    val notnull = this.nullable match {
      case true => "";
      case false => " NOT NULL";
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
      case "BigDecimal" => s"`${this.column}` DECIMAL${notnull}${pkey}"
      case "Date" => s"`${this.column}` DATE${notnull}${pkey}"
      case "DateTime" => s"`${this.column}` DATETIME${notnull}${pkey}"
      case "String" => s"`${this.column}` VARCHAR(${this.length})${notnull}${pkey}"
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
    this.typeKind != FieldMetaTypeKind.BUILT_IN
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
    require(typeKind == FieldMetaTypeKind.BUILT_IN)
    if (json == "null") {
      return null
    }
    typeName match {
      case "Integer" => new java.lang.Integer(json)
      case "Long" => new java.lang.Long(json)
      case "Float" => new java.lang.Float(json)
      case "Double" => new java.lang.Double(json)
      case "BigDecimal" => new java.math.BigDecimal(json)
      case "Date" => new SimpleDateFormat("yyyy-MM-dd").parse(json)
      case "DateTime" => new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(json)
      case "String" => json
      case _ => throw new RuntimeException()
    }
  }

  def stringify(data: Object): String = {
    if (data == null) {
      return "null"
    }
    typeName match {
      case "Integer" => data.toString()
      case "Long" => data.toString()
      case "Float" => data.toString()
      case "Double" => data.toString()
      case "BigDecimal" => data.toString()
      case "Date" => new SimpleDateFormat("yyyy-MM-dd").format(data.asInstanceOf[Date])
      case "DateTime" => new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(data.asInstanceOf[Date])
      case "String" => data.toString()
      case _ => throw new RuntimeException()
    }
  }
}

object FieldMeta {
  def pickColumn(field: Field, typeKind: Int): String = {
    // 非内建类型不对应数据库列
    if (typeKind != FieldMetaTypeKind.BUILT_IN) {
      return null
    }
    field.getDeclaredAnnotation(classOf[Column]) match {
      case null => field.getName().toLowerCase()
      case column => column.name() match {
        case null | "" => field.getName().toLowerCase()
        case name => name
      }
    }
  }

  def pickTypeName(field: Field, typeKind: Int): String = {
    val typeName = typeKind match {
      case FieldMetaTypeKind.ONE_MANY => {
        field.getGenericType().asInstanceOf[ParameterizedType].getActualTypeArguments()(0).asInstanceOf[Class[_]].getSimpleName()
      }
      case _ => {
        field.getType().getSimpleName()
      }
    }
    typeName match {
      case "Date" => {
        field.getDeclaredAnnotation(classOf[DateTime]) match {
          case null => "Date"
          case _ => "DateTime"
        }
      }
      case _ => typeName
    }
  }

  val DEFAULT_LEN: Int = 128

  def pickLeftRight(entity: EntityMeta, field: Field, typeKind: Int): (String, String) = {
    if (typeKind == FieldMetaTypeKind.BUILT_IN) {
      return (null, null)
    }
    typeKind match {
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

  def pickTypeKind(field: Field): Int = {
    field.getType().getSimpleName() match {
      case "Integer" |
           "Long" |
           "Float" |
           "Double" |
           "BigDecimal" |
           "Date" |
           "String" => return FieldMetaTypeKind.BUILT_IN
      case _ => {}
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
    throw new RuntimeException(field.getName())
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

    val typeKind: Int = FieldMeta.pickTypeKind(field)
    val typeName: String = FieldMeta.pickTypeName(field, typeKind)
    val name: String = field.getName()
    val column: String = FieldMeta.pickColumn(field, typeKind)
    val nullable = true

    val length: Int = FieldMeta.DEFAULT_LEN

    val (left, right) = FieldMeta.pickLeftRight(entity, field, typeKind)

    return new FieldMeta(entity, field,
      pkey, auto,
      typeKind, typeName, name, column,
      nullable, length,
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
    val nullable: Boolean = true

    val length: Int = FieldMeta.DEFAULT_LEN

    val left: String = null
    val right: String = null

    return new FieldMeta(entity, field,
      pkey, auto,
      typeKind, typeName, name, column,
      nullable, length,
      left, right)
  }
}
