package yy.orm.entity

import java.lang.reflect.Method
import java.util.regex.Pattern

import net.sf.cglib.proxy.MethodProxy
import yy.orm.Session.Session
import yy.orm.meta.{EntityMeta, FieldMetaTypeKind}

/**
  * Created by Administrator on 2017/5/18.
  */
class EntityCore(val meta: EntityMeta, var fieldMap: Map[String, Object]) {

  private val pattern = Pattern.compile("(get|set|clear)(.+)")
  private val coreFn = "$$core"
  private var session: Session = _

  def getPkey: Object = {
    if (!fieldMap.contains(meta.pkey.name)) {
      return null
    }
    fieldMap(meta.pkey.name)
  }

  def get(field: String): Object = {
    this.getValue(field)
  }

  def set(field: String, value: Object): Object = {
    val fieldMeta = this.meta.fieldMap(field)
    fieldMeta.typeKind match {
      case FieldMetaTypeKind.BUILT_IN |
           FieldMetaTypeKind.REFER |
           FieldMetaTypeKind.IGNORE_BUILT_IN |
           FieldMetaTypeKind.IGNORE_REFER |
           FieldMetaTypeKind.IGNORE_MANY => this.setValue(field, value)
      case FieldMetaTypeKind.POINTER => this.setPointer(field, value)
      case FieldMetaTypeKind.ONE_ONE => this.setOneOne(field, value)
      case FieldMetaTypeKind.ONE_MANY => this.setOneMany(field, value)
      case _ => throw new RuntimeException(s"Unknown Field Meta Type: [$field]")
    }
    null
  }

  def getValue(field: String): Object = {
    if (this.fieldMap.contains(field)) {
      this.fieldMap(field)
    } else {
      null
    }
  }

  def setValue(field: String, value: Object): Unit = {
    this.fieldMap += (field -> value)
  }

  def setPointer(field: String, value: Object): Unit = {
    val a = this
    val fieldMeta = a.meta.fieldMap(field)
    // a.b = b
    this.fieldMap += (field -> value)
    if (value != null) {
      // a.b_id = b.id
      val b = EntityManager.core(value)
      a.syncField(fieldMeta.left, b, fieldMeta.right)
    } else {
      // a.b_id = null
      a.fieldMap += ((fieldMeta.left, null))
    }
  }

  def setOneOne(field: String, value: Object): Unit = {
    val a = this
    val fieldMeta = this.meta.fieldMap(field)
    // oldb.a_id = null
    if (this.fieldMap.contains(field)) {
      this.fieldMap(field) match {
        case null =>
        case _ =>
          val oldb = EntityManager.core(this.fieldMap(field))
          oldb.fieldMap += (fieldMeta.right -> null)
          addSessionCache(this.fieldMap(field))
      }
    } else {
      {}
    }
    // a.b = b
    a.fieldMap += (field -> value)
    if (value != null) {
      // b.a_id = a.id
      val b = EntityManager.core(value)
      b.syncField(fieldMeta.right, a, fieldMeta.left)
    }
  }

  def setOneMany(field: String, value: Object): Unit = {
    require(value != null && value.getClass.isArray)
    val a = this
    val fieldMeta = a.meta.fieldMap(field)
    val arr = value.asInstanceOf[Array[Object]]
    // 新id的集合，用来判断老数据是否需要断开id
    val newIds: Set[String] = arr.map(EntityManager.core(_).getPkey)
      .filter(_ != null)
      .map(_.toString)(collection.breakOut)
    // 老数据断开id
    if (a.fieldMap.contains(field)) {
      a.fieldMap(field).asInstanceOf[Array[Object]].foreach(item => {
        val core = EntityManager.core(item)
        // 不在新数组里，说明关系断开了
        if (core.getPkey != null && !newIds.contains(core.getPkey.toString)) {
          // oldb.a_id = null
          core.fieldMap += ((fieldMeta.right, null))
          addSessionCache(item)
        }
      })
    }
    arr.foreach(item => {
      // b.a_id = a.id
      val b = EntityManager.core(item)
      b.syncField(fieldMeta.right, a, fieldMeta.left)
    })
    // a.bs = bs
    a.fieldMap += (field -> value)
  }

  def syncField(left: String, b: EntityCore, right: String): Unit = {
    // a.left = b.right
    if (b.fieldMap.contains(right)) {
      this.fieldMap += (left -> b.fieldMap(right))
    } else if (this.fieldMap.contains(left)) {
      this.fieldMap -= left
    }
  }

  def setSession(session: Session): Unit = {
    this.session = session
  }

  def addSessionCache(obj: Object): Unit = {
    if (session == null) {
      return
    }
    require(!session.isClosed)
    session.addCache(obj)
  }

  override def toString: String = {
    val content = this.meta.fieldVec.filter(field => {
      this.fieldMap.contains(field.name)
    }).map(field => {
      field.typeKind match {
        case FieldMetaTypeKind.BUILT_IN |
             FieldMetaTypeKind.IGNORE_BUILT_IN =>
          val value = this.fieldMap(field.name)
          if (value == null) {
            s"${field.name}: null"
          } else if (field.typeName == "String" ||
            field.typeName == "Date" ||
            field.typeName == "DateTime") {
            s"""${field.name}: "${this.fieldMap(field.name)}""""
          } else {
            s"""${field.name}: ${this.fieldMap(field.name)}"""
          }
        case FieldMetaTypeKind.IGNORE_REFER |
             FieldMetaTypeKind.REFER |
             FieldMetaTypeKind.POINTER |
             FieldMetaTypeKind.ONE_ONE =>
          s"""${field.name}: ${this.fieldMap(field.name)}"""
        case FieldMetaTypeKind.ONE_MANY |
             FieldMetaTypeKind.IGNORE_MANY =>
          val content = this.fieldMap(field.name).asInstanceOf[Array[Object]]
            .map(_.toString).mkString(", ")
          s"${field.name}: [$content]"
      }
    }).mkString(", ")
    s"{$content}"
  }

  def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
    if (method.getName == coreFn) {
      return this
    }
    if (method.getName == "toString") {
      return this.toString()
    }
    val matcher = pattern.matcher(method.getName)
    if (!matcher.matches()) {
      return proxy.invokeSuper(obj, args)
    }
    val op = matcher.group(1)
    var field = matcher.group(2)
    field = field.substring(0, 1).toLowerCase() + field.substring(1)
    // 没有这个字段
    if (!this.meta.fieldMap.contains(field)) {
      return proxy.invokeSuper(obj, args)
    }

    op match {
      case "get" => this.get(field)
      case "set" => this.set(field, args(0))
      case "clear" => throw new RuntimeException("")
    }
  }
}

object EntityCore {
  def create(meta: EntityMeta): EntityCore = {
    val map: Map[String, Object] = meta.fieldVec.map((field) => {
      field.typeKind match {
        case FieldMetaTypeKind.ONE_MANY
             | FieldMetaTypeKind.IGNORE_MANY => (field.name, Array[Object]())
        case _ => (field.name, null)
      }
    })(collection.breakOut)
    new EntityCore(meta, map)
  }

  def empty(meta: EntityMeta): EntityCore = {
    new EntityCore(meta, Map())
  }
}

