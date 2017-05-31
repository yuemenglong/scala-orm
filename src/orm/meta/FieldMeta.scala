package orm.meta

import java.lang.reflect.{Field, ParameterizedType}

import orm.java.anno._

object FieldMetaType {
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
    this.typeKind == FieldMetaType.BUILT_IN
  }

  def isObject(): Boolean = {
    this.typeKind != FieldMetaType.BUILT_IN
  }

  def isRefer(): Boolean = {
    this.typeKind == FieldMetaType.REFER
  }

  def isPointer(): Boolean = {
    this.typeKind == FieldMetaType.POINTER
  }

  def isOneOne(): Boolean = {
    this.typeKind == FieldMetaType.ONE_ONE
  }

  def isOneMany(): Boolean = {
    this.typeKind == FieldMetaType.ONE_MANY
  }
}

object FieldMeta {
  def pickColumn(field: Field, typeKind: Int): String = {
    // 非内建类型不对应数据库列
    if (typeKind != FieldMetaType.BUILT_IN) {
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
      case FieldMetaType.ONE_MANY => {
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
    if (typeKind == FieldMetaType.BUILT_IN) {
      return (null, null)
    }
    typeKind match {
      case FieldMetaType.REFER => {
        val anno = field.getAnnotation(classOf[Refer])
        return (anno.left(), anno.right())
      }
      case FieldMetaType.POINTER => {
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
      case FieldMetaType.ONE_ONE => {
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
      case FieldMetaType.ONE_MANY => {
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
           "String" => return FieldMetaType.BUILT_IN
      case _ => {}
    }
    if (field.getDeclaredAnnotation(classOf[Refer]) != null) {
      return FieldMetaType.REFER
    }
    if (field.getDeclaredAnnotation(classOf[Pointer]) != null) {
      return FieldMetaType.POINTER
    }
    if (field.getDeclaredAnnotation(classOf[OneToOne]) != null) {
      return FieldMetaType.ONE_ONE
    }
    if (field.getDeclaredAnnotation(classOf[OneToMany]) != null) {
      return FieldMetaType.ONE_MANY
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

    val typeKind: Int = FieldMetaType.BUILT_IN
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
