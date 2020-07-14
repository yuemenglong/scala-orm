package io.github.yuemenglong.orm.impl.entity

import java.lang.reflect.Method

import io.github.yuemenglong.orm.impl.kit.Kit
import io.github.yuemenglong.orm.impl.meta.{FieldMetaOneOne, _}
import net.sf.cglib.proxy.{Enhancer, MethodInterceptor, MethodProxy}

import scala.collection.mutable
import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/18.
  */
object EntityManager {
  def empty[T](clazz: Class[T]): T = {
    require(OrmMeta.entityMap.nonEmpty)
    val meta = OrmMeta.entityMap(clazz)
    val core = EntityCore.empty(meta)
    wrap(core).asInstanceOf[T]
  }

  def create[T](clazz: Class[T]): T = {
    require(OrmMeta.entityMap.nonEmpty)
    val meta = OrmMeta.entityMap(clazz)
    val core = EntityCore.create(meta)
    wrap(core).asInstanceOf[T]
  }

  def wrap(core: EntityCore): Entity = {
    val enhancer: Enhancer = new Enhancer
    enhancer.setSuperclass(core.meta.clazz)
    enhancer.setInterfaces(Array(classOf[Entity]))

    enhancer.setCallback(new MethodInterceptor() {
      @throws[Throwable]
      def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
        core.intercept(obj, method, args, proxy)
      }
    })
    enhancer.create().asInstanceOf[Entity]
  }

  def core(obj: Object): EntityCore = {
    require(obj != null)
    if (!obj.isInstanceOf[Entity]) throw new RuntimeException("Not Entity, Need Use Orm.create/parse/convert To Get Entity")
    val entity = obj.asInstanceOf[Entity]
    entity.$$core()
  }

  def isEntity(obj: Object): Boolean = {
    obj.isInstanceOf[Entity]
  }

  def convert(obj: Object): Entity = {
    if (obj == null) {
      return null
    }
    if (isEntity(obj)) {
      return obj.asInstanceOf[Entity]
    }
    if (!OrmMeta.entityMap.contains(obj.getClass)) {
      throw new RuntimeException(s"[${obj.getClass.getSimpleName}] Is Not Entity")
    }
    val meta = OrmMeta.entityMap(obj.getClass)
    val map: mutable.Map[String, Object] = Kit.getDeclaredFields(obj.getClass)
      .filter(field => meta.fieldMap.contains(field.getName)) //不做ignore的
      .map(field => {
      field.setAccessible(true)
      val name = field.getName
      val fieldMeta = meta.fieldMap(name)
      val value = fieldMeta match {
        case _: FieldMetaBuildIn => field.get(obj)
        case _: FieldMetaPointer => convert(field.get(obj))
        case _: FieldMetaOneOne => convert(field.get(obj))
        case f: FieldMetaOneMany =>
          val bs = field.get(obj).asInstanceOf[Array[Object]]
          if (bs == null) {
            throw new RuntimeException("Array Must Init To Empty Rather Than Null")
          }
          val ct = ClassTag[Entity](f.refer.clazz)
          bs.map(convert).toArray(ct)
      }
      (name, value)
    })(collection.breakOut)
    val core = new EntityCore(meta, map)
    wrap(core)
  }

  def walk(entity: Entity, fn: (Entity) => Entity): Entity {} = {
    val retEntity = fn(entity)
    val map: mutable.Map[String, Object] = retEntity.$$core().fieldMap.map { case (name, value) =>
      if (value == null) {
        (name, value)
      } else {
        val fieldMeta = entity.$$core().meta.fieldMap(name)
        val newValue: Object = if (value.getClass.isArray) {
          val ct = ClassTag[Entity](fieldMeta.asInstanceOf[FieldMetaRefer].refer.clazz)
          value.asInstanceOf[Array[_]].map(_.asInstanceOf[Entity])
            .map(walk(_, fn)).toArray(ct)
        } else value match {
          case e: Entity => walk(e, fn)
          case _ => value
        }
        (name, newValue)
      }
    }
    val newCore = new EntityCore(entity.$$core().meta, map)
    EntityManager.wrap(newCore)
  }

  def createMarker[T](meta: EntityMeta): T = {
    val enhancer: Enhancer = new Enhancer
    enhancer.setSuperclass(meta.clazz)
    var fnName: String = null
    require(meta != null)

    enhancer.setCallback(new MethodInterceptor() {
      @throws[Throwable]
      def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
        method.getName match {
          case "toString" => fnName
          case _ => meta.getterMap.get(method) match {
            case None => throw new RuntimeException(s"Invalid Method Name: ${method.getName}")
            case Some(fieldMeta) =>
              fnName = fieldMeta.name
              null
          }
        }
      }
    })
    enhancer.create().asInstanceOf[T]
  }

  def clear(obj: Object, field: String): Unit = {
    if (!isEntity(obj)) {
      throw new RuntimeException("Not Entity")
    }
    val entity = obj.asInstanceOf[Entity]
    if (!entity.$$core().meta.fieldMap.contains(field)) {
      throw new RuntimeException(s"[${field}] Not Field Of Entity [${entity.$$core().meta.entity}]")
    }
    entity.$$core().fieldMap -= field
  }

  def clear[T <: Object](obj: T)(fn: T => Any): Unit = {
    if (!isEntity(obj)) {
      throw new RuntimeException("Not Entity")
    }
    val entity = obj.asInstanceOf[Entity]
    val marker = createMarker[T](entity.$$core().meta)
    fn(marker)
    clear(obj, marker.toString)
  }
}
