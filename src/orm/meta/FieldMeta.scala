package orm.meta

import java.lang.reflect.Field

import orm.java.anno.Id

class FieldMeta(val field: Field) {
  val id: Boolean = FieldMeta.pickId(field)
  val auto: Boolean = id && FieldMeta.pickIdAuto(field)

  val tp: String = field.getType().getSimpleName()
  var name: String = field.getName()
  var column: String = field.getName()
  var length: Int = 128
  val nullable = true


  def getDbSql(): String = {
    val notnull = this.nullable match {
      case true => "";
      case false => " NOT NULL";
    }
    val pkey = (this.id, this.auto) match{
      case (false, _)=>""
      case (true, false)=>" PRIMARY KEY"
      case (true, true)=>" PRIMARY KEY AUTO_INCREMENT"
    }
    this.tp match {
      case "Integer" => s"`${this.column}` INTEGER${notnull}${pkey}"
      case "Long" => s"`${this.column}` BIGINT${notnull}${pkey}"
      case "String" => s"`${this.column}` VARCHAR(${this.length})${notnull}${pkey}"
      case _ => throw new RuntimeException()
    }
  }

  def isNormal(): Boolean = {
    if (this.id) {
      return false
    }
    this.tp match {
      case "Integer" => true
      case "Long" => true
      case "String" => true
      case _ => throw new RuntimeException()
    }
  }

  def isId(): Boolean = {
    this.id
  }

  def isPkeyOrNormal(): Boolean = {
    this.isId() || this.isNormal()
  }
}

private object FieldMeta {
  def pickIdAuto(field: Field): Boolean = {
    field.getDeclaredAnnotation(classOf[Id]).auto()
  }

  def pickId(field: Field): Boolean = {
    field.getDeclaredAnnotation(classOf[Id]) != null
  }
}
