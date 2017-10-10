package io.github.yuemenglong.orm.entity

import java.lang.reflect.Method

import net.sf.cglib.proxy.MethodProxy
import io.github.yuemenglong.orm.Session.Session
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.meta._

import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/18.
  */
class EntityCore(val meta: EntityMeta, var fieldMap: Map[String, Object]) {
  private val coreFn = "$$core"
  private var session: Session = _

  // 保证所有数组被初始化
  //  meta.fieldVec.filter(_.isOneMany).map(_.asInstanceOf[FieldMetaOneMany]).foreach(f => {
  //    if (!fieldMap.contains(f.name) || fieldMap(f.name) == null) {
  //      val ct = ClassTag[Any](f.refer.clazz)
  //      val array = Array.newBuilder(ct).result()
  //      fieldMap += (f.name -> array)
  //    }
  //  })

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
    fieldMeta match {
      case _: FieldMetaBuildIn => this.setValue(field, value)
      case _: FieldMetaPointer => this.setPointer(field, value)
      case _: FieldMetaOneOne => this.setOneOne(field, value)
      case _: FieldMetaOneMany => this.setOneMany(field, value)
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
    val fieldMeta = a.meta.fieldMap(field).asInstanceOf[FieldMetaPointer]
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
    val fieldMeta = this.meta.fieldMap(field).asInstanceOf[FieldMetaOneOne]
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
    val fieldMeta = a.meta.fieldMap(field).asInstanceOf[FieldMetaOneMany]
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
    }).map {
      case field: FieldMetaBuildIn =>
        val value = this.fieldMap(field.name)
        if (value == null) {
          s"${field.name}: null"
        } else if (field.clazz == classOf[String]) {
          s"""${field.name}: "${this.fieldMap(field.name)}""""
        } else {
          s"""${field.name}: ${this.fieldMap(field.name)}"""
        }
      case field: FieldMetaPointer =>
        s"""${field.name}: ${this.fieldMap(field.name)}"""
      case field: FieldMetaOneOne =>
        s"""${field.name}: ${this.fieldMap(field.name)}"""
      case field: FieldMetaOneMany =>
        val content = this.fieldMap(field.name).asInstanceOf[Array[Object]]
          .map(_.toString).mkString(", ")
        s"${field.name}: [$content]"
    }.mkString(", ")
    s"{$content}"
  }

  def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
    if (method.getName == coreFn) {
      return this
    }
    if (method.getName == "toString") {
      return this.toString()
    }
    if (meta.getterMap.contains(method)) {
      val fieldMeta = meta.getterMap(method)
      this.get(fieldMeta.name)
    } else if (meta.setterMap.contains(method)) {
      val fieldMeta = meta.setterMap(method)
      this.set(fieldMeta.name, args(0))
    } else {
      // 交给对象自己处理
      proxy.invokeSuper(obj, args)
    }
  }
}

object EntityCore {
  // 数组初始化为空数组，其他全部为null
  def create(meta: EntityMeta): EntityCore = {
    val map: Map[String, Object] = meta.fieldVec.map {
      case f: FieldMetaOneMany => (f.name, Kit.newArray(f.refer.clazz).asInstanceOf[Object])
      case f => (f.name, null)
    }(collection.breakOut)
    new EntityCore(meta, map)
  }

  // 纯空对象
  def empty(meta: EntityMeta): EntityCore = {
    new EntityCore(meta, Map())
  }
}

