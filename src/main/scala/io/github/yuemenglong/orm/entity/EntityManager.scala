package io.github.yuemenglong.orm.entity

import java.lang.reflect.Method

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.lang.types.Types
import io.github.yuemenglong.orm.meta._
import net.sf.cglib.proxy.{Enhancer, MethodInterceptor, MethodProxy}

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

  def createMarker[T](meta: EntityMeta, ret: Array[String] = Array(null)): T = {
    val enhancer: Enhancer = new Enhancer
    enhancer.setSuperclass(meta.clazz)
    val prefix = ret(0)

    enhancer.setCallback(new MethodInterceptor() {
      @throws[Throwable]
      def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
        (method.getName, meta) match {
          case ("toString", _) => ret(0)
          case (_, null) => throw new RuntimeException(s"Not Refer Field [$prefix]")
          case (_, _) => meta.getterMap.get(method) match {
            case None => throw new RuntimeException(s"Invalid Method Name: ${method.getName}")
            case Some(fieldMeta) =>
              ret(0) = prefix match {
                case null => fieldMeta.name
                case _ => s"$prefix.${fieldMeta.name}"
              }
              fieldMeta match {
                // 返回一个有效的marker，可以继续迭代
                case referMeta: FieldMetaRefer => createMarker[Object](referMeta.refer, ret)
                // 返回一个只表示field的String
                case _ => Types.newInstance(fieldMeta.clazz)
              }
          }
        }
      }
    })
    enhancer.create().asInstanceOf[T]
  }

  //  def createMarkerRet(clazz: Class[_], value: String): Object = {
  //    val enhancer: Enhancer = new Enhancer
  //    enhancer.setSuperclass(clazz)
  //
  //    enhancer.setCallback(new MethodInterceptor() {
  //      @throws[Throwable]
  //      def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
  //        method.getName match {
  //          case "toString" => value
  //          case _ => throw new RuntimeException("Marker Only Can Call ToString")
  //        }
  //      }
  //    })
  //    enhancer.create()
  //  }
}
