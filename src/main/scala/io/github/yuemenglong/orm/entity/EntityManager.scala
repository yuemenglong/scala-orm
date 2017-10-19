package io.github.yuemenglong.orm.entity

import java.lang.reflect.Method

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.meta._
import net.sf.cglib.proxy.{Enhancer, MethodInterceptor, MethodProxy}

import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/18.
  */
object EntityManager {
  def empty[T](clazz: Class[T]): T = {
    require(OrmMeta.entityMap.nonEmpty)
    val meta = OrmMeta.entityMap(clazz.getSimpleName)
    val core = EntityCore.empty(meta)
    wrap(core).asInstanceOf[T]
  }

  def create[T](clazz: Class[T]): T = {
    require(OrmMeta.entityMap.nonEmpty)
    val meta = OrmMeta.entityMap(clazz.getSimpleName)
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
    if (!OrmMeta.entityMap.contains(obj.getClass.getSimpleName)) {
      throw new RuntimeException(s"[${obj.getClass.getSimpleName}] Is Not Entity")
    }
    val meta = OrmMeta.entityMap(obj.getClass.getSimpleName)
    val map: Map[String, Object] = Kit.getDeclaredFields(obj.getClass)
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
    val map: Map[String, Object] = retEntity.$$core().fieldMap.map { case (name, value) =>
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
}
