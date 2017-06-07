package orm.entity

import java.lang.reflect.Method
import java.util
import java.util.regex.Pattern
import java.util.stream.Collectors

import net.sf.cglib.proxy.MethodProxy
import orm.Session.Session
import orm.kit.Kit
import orm.meta.{EntityMeta, FieldMetaTypeKind}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/18.
  */
class EntityCore(val meta: EntityMeta, var fieldMap: Map[String, Object]) {

  private val pattern = Pattern.compile("(get|set|clear)(.+)")
  private val coreFn = "$$core"
  private var session: Session = null

  def getPkey(): Object = {
    if (!fieldMap.contains(meta.pkey.name)) {
      return null
    }
    fieldMap(meta.pkey.name)
  }

  def get(field: String): Object = {
    val fieldMeta = this.meta.fieldMap(field)
    if (fieldMeta.isNormalOrPkey()) {
      return this.getValue(field)
    } else if (fieldMeta.isRefer()) {
      return this.getValue(field)
    } else if (fieldMeta.isPointer()) {
      return this.getValue(field)
    } else if (fieldMeta.isOneOne()) {
      return this.getValue(field)
    } else if (fieldMeta.isOneMany()) {
      return this.getValue(field)
    } else {
      throw new RuntimeException("Unknown Field Type")
    }
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
      case _ => throw new RuntimeException(s"Unknown Field Meta Type: [${field}]")
    }
    return null
  }

  def getValue(field: String): Object = {
    this.fieldMap.contains(field) match {
      case true => this.fieldMap(field)
      case false => null
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
    this.fieldMap.contains(field) match {
      case false => {}
      case true => this.fieldMap(field) match {
        case null => {}
        case _ => {
          val oldb = EntityManager.core(this.fieldMap(field))
          oldb.fieldMap += (fieldMeta.right -> null)
          addSessionCache(this.fieldMap(field))
        }
      }
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
    require(value != null)
    val a = this;
    val fieldMeta = a.meta.fieldMap(field)
    val coll = value.asInstanceOf[java.util.Collection[Object]]
    var newIds = Set[String]()
    coll.stream().forEach(item => {
      EntityManager.core(item).getPkey() match {
        case null => {}
        case pkey: Object => {
          newIds += pkey.toString
        }
      }
    })
    a.fieldMap.contains(field) match {
      case false => {}
      case true => a.fieldMap(field).asInstanceOf[util.Collection[Object]].forEach(item => {
        val core = EntityManager.core(item)
        // 不在新数组里，说明关系断开了
        if (core.getPkey() != null && !newIds.contains(core.getPkey().toString())) {
          // oldb.a_id = null
          core.fieldMap += ((fieldMeta.right, null))
          addSessionCache(item)
        }
      })
    }
    coll.stream().forEach(item => {
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
    require(!session.isClosed())
    session.addCache(obj)
  }

  override def toString: String = {
    val content = this.meta.fieldVec.filter(field => {
      this.fieldMap.contains(field.name)
    }).map(field => {
      field.typeKind match {
        case FieldMetaTypeKind.BUILT_IN |
             FieldMetaTypeKind.IGNORE_BUILT_IN => {
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
        }
        case FieldMetaTypeKind.IGNORE_REFER |
             FieldMetaTypeKind.REFER |
             FieldMetaTypeKind.POINTER |
             FieldMetaTypeKind.ONE_ONE => {
          s"""${field.name}: ${this.fieldMap(field.name)}"""
        }
        case FieldMetaTypeKind.ONE_MANY |
             FieldMetaTypeKind.IGNORE_MANY => {
          val bs = this.fieldMap(field.name).asInstanceOf[util.Collection[Object]]
          val joins = new ArrayBuffer[String]()
          bs.forEach(b => {
            joins += b.toString()
          })
          val content = joins.mkString(", ")
          s"${field.name}: [${content}]"
        }
      }
    }).mkString(", ")
    s"{${content}}"
  }

  def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
    if (method.getName() == coreFn) {
      return this
    }
    if (method.getName() == "toString") {
      return this.toString()
    }
    val matcher = pattern.matcher(method.getName())
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

    return op match {
      case "get" => this.get(field)
      case "set" => this.set(field, args(0))
      case "clear" => throw new RuntimeException("")
    }
  }
}

object EntityCore {
  def create(meta: EntityMeta): EntityCore = {
    val map: Map[String, Object] = meta.fieldVec.map((field) => {
      field.isOneMany() match {
        case false => (field.name, null)
        case true => (field.name, new util.ArrayList[Object]())
      }
    })(collection.breakOut)
    new EntityCore(meta, map)
  }

  def empty(meta: EntityMeta): EntityCore = {
    new EntityCore(meta, Map())
  }
}

