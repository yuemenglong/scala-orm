package io.github.yuemenglong.orm.meta

import java.lang.reflect.Field

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.anno._

//object FieldMetaTypeKind {
//  val BUILT_IN: Int = 0
//  val REFER: Int = 1
//  val POINTER: Int = 2
//  val ONE_ONE: Int = 3
//  val ONE_MANY: Int = 4
//  val IGNORE_BUILT_IN: Int = 100
//  val IGNORE_REFER: Int = 101
//  val IGNORE_MANY: Int = 102
//}

trait FieldMeta {
  val entity: EntityMeta
  val name: String
  val clazz: Class[_]
  val column: String
  val nullable: Boolean
  val isPkey: Boolean
  val isAuto: Boolean

  val dbType: String

  def getDbSql: String = {
    val notnull = if (nullable) "" else " NOT NULL"
    val pkey = (isPkey, isAuto) match {
      case (false, _) => ""
      case (true, false) => " PRIMARY KEY"
      case (true, true) => " PRIMARY KEY AUTO_INCREMENT"
    }
    s"$column $dbType$notnull$pkey"
  }

  def isNormalOrPkey: Boolean = !isRefer

  def isNormal: Boolean = isNormalOrPkey && !isPkey

  def isRefer: Boolean = this.isInstanceOf[FieldMetaRefer]

  def isPointer: Boolean = this.isInstanceOf[FieldMetaPointer]

  def isOneOne: Boolean = this.isInstanceOf[FieldMetaOneOne]

  def isOneMany: Boolean = this.isInstanceOf[FieldMetaOneMany]
}

trait FieldMetaBuildIn extends FieldMeta

class FieldMetaFkey(override val name: String,
                    override val entity: EntityMeta,
                    referMeta: FieldMetaRefer) extends FieldMeta with FieldMetaBuildIn {
  override val column: String = Kit.lodashCase(name)
  override val nullable: Boolean = referMeta.nullable
  override val isPkey: Boolean = referMeta.isPkey
  override val isAuto: Boolean = referMeta.isAuto
  override val dbType: String = "BIGINT"
  override val clazz: Class[_] = classOf[java.lang.Long]
}

abstract class FieldMetaDeclared(val field: Field, val entity: EntityMeta) extends FieldMeta {
  val annoColumn: Column = field.getAnnotation(classOf[Column])
  val annoId: Id = field.getAnnotation(classOf[Id])
  val annoDateTime: DateTime = field.getAnnotation(classOf[DateTime])
  val annoLongText: LongText = field.getAnnotation(classOf[LongText])
  val annoEnum: Enum = field.getAnnotation(classOf[Enum])
  val annoPointer: Pointer = field.getAnnotation(classOf[Pointer])
  val annoOneOne: OneToOne = field.getAnnotation(classOf[OneToOne])
  val annoOneMany: OneToMany = field.getAnnotation(classOf[OneToMany])

  override val name: String = field.getName
  override val clazz: Class[_] = field.getType
  override val column: String = annoColumn match {
    case null => Kit.lodashCase(name)
    case _ => annoColumn.name().length match {
      case 0 => Kit.lodashCase(name)
      case _ => annoColumn.name()
    }
  }
  override val nullable: Boolean = annoColumn match {
    case null => true
    case _ => annoColumn.nullable()
  }
  override val isPkey: Boolean = annoId != null
  override val isAuto: Boolean = isPkey && annoId.auto()
}

class FieldMetaInteger(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.Integer])
  override val dbType: String = "INT"
}

class FieldMetaTinyInt(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.Integer])
  override val dbType: String = "TINYINT"
}

class FieldMetaSmallInt(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.Integer])
  override val dbType: String = "SMALLINT"
}

class FieldMetaLong(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.Long])
  override val dbType: String = "BIGINT"
}

class FieldMetaDouble(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.Double])
  override val dbType: String = "DOUBLE"
}

class FieldMetaFloat(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.Float])
  override val dbType: String = "FLOAT"
}

class FieldMetaBoolean(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.Boolean])
  override val dbType: String = "BOOLEAN"
}

class FieldMetaString(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.String])
  val length: Int = if (annoColumn != null) {
    annoColumn.length()
  } else {
    255
  }
  override val dbType: String = "VARCHAR"
  override val getDbSql: String = {
    val notnull = if (nullable) "" else " NOT NULL"
    val pkey = (isPkey, isAuto) match {
      case (false, _) => ""
      case (true, false) => " PRIMARY KEY"
      case (true, true) => " PRIMARY KEY AUTO_INCREMENT"
    }
    s"$column $dbType($length)$notnull$pkey"
  }
}

class FieldMetaEnum(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.String])
  val values: String = annoEnum.value().map(s => '"' + s + '"').mkString(",")
  override val dbType: String = "ENUM"
  override val getDbSql: String = {
    val notnull = if (nullable) "" else " NOT NULL"
    val pkey = (isPkey, isAuto) match {
      case (false, _) => ""
      case (true, false) => " PRIMARY KEY"
      case (true, true) => " PRIMARY KEY AUTO_INCREMENT"
    }
    s"$column $dbType($values)$notnull$pkey"
  }
}

class FieldMetaDecimal(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.math.BigDecimal])
  val (precision, scale) = if (annoColumn != null) {
    (annoColumn.precision(), annoColumn.scale())
  } else {
    (0, 0)
  }
  override val dbType: String = "DECIMAL"
  override val getDbSql: String = {
    val notnull = if (nullable) "" else " NOT NULL"
    val pkey = (isPkey, isAuto) match {
      case (false, _) => ""
      case (true, false) => " PRIMARY KEY"
      case (true, true) => " PRIMARY KEY AUTO_INCREMENT"
    }
    val ps = (precision, scale) match {
      case (0, 0) => ""
      case (p, s) => s"($p,$s)"
    }
    s"$column $dbType$ps$notnull$pkey"
  }
}

class FieldMetaLongText(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.lang.String])
  override val dbType: String = "LONGTEXT"
}

class FieldMetaDate(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.util.Date])
  require(annoDateTime == null)
  override val dbType: String = "DATE"
}

class FieldMetaDateTime(field: Field, entity: EntityMeta) extends FieldMetaDeclared(field, entity) with FieldMetaBuildIn {
  require(field.getType == classOf[java.util.Date])
  require(annoDateTime != null)
  override val dbType: String = "DATETIME"
}

abstract class FieldMetaRefer(field: Field, entity: EntityMeta, val refer: EntityMeta) extends FieldMetaDeclared(field, entity) {
  protected def getLeftRight: (String, String)

  override val column: String = null
  override val dbType: String = null

  val (left, right) = getLeftRight
}

class FieldMetaPointer(field: Field, entity: EntityMeta, refer: EntityMeta) extends FieldMetaRefer(field, entity, refer) {
  require(annoPointer != null)
  require(!field.getType.isArray)

  override def getLeftRight: (String, String) = {
    var (left, right) = (annoPointer.left(), annoPointer.right())
    if (left.isEmpty) {
      left = name + "Id"
    }
    if (right.isEmpty) {
      right = refer.pkey.name
    }
    (left, right)
  }
}

class FieldMetaOneOne(field: Field, entity: EntityMeta, refer: EntityMeta) extends FieldMetaRefer(field, entity, refer) {
  require(annoOneOne != null)
  require(!field.getType.isArray)

  override def getLeftRight: (String, String) = {
    var (left, right) = (annoOneOne.left(), annoOneOne.right())
    if (left.isEmpty) {
      left = entity.pkey.name
    }
    if (right.isEmpty) {
      right = Kit.lowerCaseFirst(entity.entity) + "Id"
    }
    (left, right)
  }
}

class FieldMetaOneMany(field: Field, entity: EntityMeta, refer: EntityMeta) extends FieldMetaRefer(field, entity, refer) {
  require(annoOneMany != null)
  require(field.getType.isArray)

  override def getLeftRight: (String, String) = {
    var (left, right) = (annoOneMany.left(), annoOneMany.right())
    if (left.isEmpty) {
      left = entity.pkey.name
    }
    if (right.isEmpty) {
      right = Kit.lowerCaseFirst(entity.entity) + "Id"
    }
    (left, right)
  }
}
