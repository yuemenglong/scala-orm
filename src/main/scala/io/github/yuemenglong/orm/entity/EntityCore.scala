package io.github.yuemenglong.orm.entity

import java.lang.reflect.Method
import java.text.SimpleDateFormat

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.types.Types.{BigDecimal, Date, DateTime}
import io.github.yuemenglong.orm.meta._
import net.sf.cglib.proxy.MethodProxy

/**
 * Created by Administrator on 2017/5/18.
 */
class EntityCore(val meta: EntityMeta, var fieldMap: Map[String, Object]) {
  private val coreFn = "$$core"

  def getPkey: Object = {
    if (!fieldMap.contains(meta.pkey.name)) {
      return null
    }
    fieldMap(meta.pkey.name)
  }

  def setPkey(id: Object): Unit = {
    fieldMap += (meta.pkey.name -> id)
  }

  def get(field: String): Object = {
    this.getValue(field)
  }

  def set(field: String, value: Object): Object = {
    this.setValue(field, value)
    null
  }

  def getValue(field: String): Object = this.fieldMap.get(field) match {
    case Some(v) => v
    case None => null
  }

  def setValue(field: String, value: Object): Unit = {
    this.fieldMap += (field -> value)
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
    } else if (meta.ignoreGetterMap.contains(method)) {
      val fieldMeta = meta.ignoreGetterMap(method)
      this.get(fieldMeta.name)
    } else if (meta.ignoreSetterMap.contains(method)) {
      val fieldMeta = meta.ignoreSetterMap(method)
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

  def shallowEqual(left: EntityCore, right: EntityCore): Boolean = {
    if (left.meta != right.meta) {
      return false
    }
    left.meta.fieldVec.filter(_.isNormal).foreach(fieldMeta => {
      val name = fieldMeta.name
      val leftVal = left.fieldMap.getOrElse(name, null)
      val rightVal = right.fieldMap.getOrElse(name, null)
      if ((leftVal != null && rightVal == null) ||
        (leftVal == null && rightVal != null)) {
        return false
      }
      // 都为null也认为相等
      val eq = if (leftVal == rightVal) {
        true
      } else (leftVal, rightVal) match {
        // boolean int long float double bigint date datetime string
        // timestamp bigint 特殊处理
        case (t1: Date, t2: Date) =>
          val sdf = new SimpleDateFormat("yyyyMMdd")
          sdf.format(t1) == sdf.format(t2)
        case (t1: DateTime, t2: DateTime) =>
          val t1t = t1.getTime % 1000 match {
            case n if n >= 500 => t1.getTime / 1000 + 1
            case n if n < 500 => t1.getTime / 1000
          }
          val t2t = t2.getTime % 1000 match {
            case n if n >= 500 => t2.getTime / 1000 + 1
            case n if n < 500 => t2.getTime / 1000
          }
          t1t == t2t
        case (t1: BigDecimal, t2: BigDecimal) => t1.doubleValue() == t2.doubleValue()
        case _ => false
      }
      if (!eq) {
        return false
      }
    })
    true
  }

  def setWithRefer(core: EntityCore, field: String, value: Object): Object = {
    val fieldMeta = core.meta.fieldMap(field)
    fieldMeta match {
      case _: FieldMetaBuildIn => setValue(core, field, value)
      case _: FieldMetaPointer => setPointer(core, field, value)
      case _: FieldMetaOneOne => setOneOne(core, field, value)
      case _: FieldMetaOneMany => setOneMany(core, field, value)
    }
    null
  }

  def setValue(core: EntityCore, str: String, value: Object): Unit = {
    core.fieldMap += (str -> value)
  }

  def setPointer(core: EntityCore, field: String, value: Object): Unit = {
    val a = core
    val fieldMeta = a.meta.fieldMap(field).asInstanceOf[FieldMetaPointer]
    // a.b = b
    core.fieldMap += (field -> value)
    if (value != null) {
      // a.b_id = b.id
      val b = EntityManager.core(value)
      syncField(a, fieldMeta.left, b, fieldMeta.right)
    } else {
      // a.b_id = null
      a.fieldMap += ((fieldMeta.left, null))
    }
  }

  // 不考虑设置老数据，与oneMany保持一致
  def setOneOne(core: EntityCore, field: String, value: Object): Unit = {
    val a = core
    val fieldMeta = core.meta.fieldMap(field).asInstanceOf[FieldMetaOneOne]
    //    // a.b.a_id = null
    //    if (core.fieldMap.contains(field)) core.fieldMap(field) match {
    //      case null =>
    //      case oldb => val oldbCore = EntityManager.core(oldb)
    //        oldbCore.fieldMap += (fieldMeta.right -> null)
    //    }
    // a.b = b
    a.fieldMap += (field -> value)
    if (value != null) {
      // b.a_id = a.id
      val b = EntityManager.core(value)
      syncField(b, fieldMeta.right, a, fieldMeta.left)
    }
  }

  def setOneMany(core: EntityCore, field: String, value: Object): Unit = {
    require(value != null && value.getClass.isArray, "Here Must Array And Not Null")
    val a = core
    val fieldMeta = a.meta.fieldMap(field).asInstanceOf[FieldMetaOneMany]
    val arr = value.asInstanceOf[Array[Object]]
    arr.foreach(item => {
      // b.a_id = a.id
      val b = EntityManager.core(item)
      syncField(b, fieldMeta.right, a, fieldMeta.left)
    })
    // a.bs = bs
    a.fieldMap += (field -> value)
  }

  def syncField(a: EntityCore, left: String, b: EntityCore, right: String): Unit = {
    // a.left = b.right
    if (b.fieldMap.contains(right)) {
      a.fieldMap += (left -> b.fieldMap(right))
    } else if (a.fieldMap.contains(left)) {
      a.fieldMap -= left
    }
  }
}

