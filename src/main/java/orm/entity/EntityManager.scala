package orm.entity

import java.lang.reflect.Method

import net.sf.cglib.proxy.{Enhancer, MethodInterceptor, MethodProxy}
import orm.kit.Kit
import orm.lang.interfaces.Entity
import orm.meta.{FieldMetaTypeKind, OrmMeta}

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

  def wrap(core: EntityCore): Object = {
    val enhancer: Enhancer = new Enhancer
    enhancer.setSuperclass(core.meta.clazz)
    enhancer.setInterfaces(Array(classOf[Entity]))

    enhancer.setCallback(new MethodInterceptor() {
      @throws[Throwable]
      def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
        core.intercept(obj, method, args, proxy)
      }
    })
    enhancer.create()
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

  def convert(obj: Object): Object = {
    if (obj == null) {
      return null
    }
    if (isEntity(obj)) {
      return obj
    }
    if (!OrmMeta.entityMap.contains(obj.getClass.getSimpleName)) {
      throw new RuntimeException(s"[${obj.getClass.getSimpleName}] Is Not Entity")
    }
    val meta = OrmMeta.entityMap(obj.getClass.getSimpleName)
    val map: Map[String, Object] = Kit.getDeclaredFields(obj.getClass).map(field => {
      field.setAccessible(true)
      val name = field.getName
      val fieldMeta = meta.fieldMap(name)
      val value = fieldMeta.typeKind match {
        case FieldMetaTypeKind.BUILT_IN
             | FieldMetaTypeKind.IGNORE_BUILT_IN
        => field.get(obj)
        case FieldMetaTypeKind.REFER
             | FieldMetaTypeKind.POINTER
             | FieldMetaTypeKind.ONE_ONE
             | FieldMetaTypeKind.IGNORE_REFER
        => convert(field.get(obj))
        case FieldMetaTypeKind.ONE_MANY
             | FieldMetaTypeKind.IGNORE_MANY
        => val bs = field.get(obj).asInstanceOf[Array[Object]]
          if (bs == null) {
            throw new RuntimeException("Array Must Init To Empty Rather Than Null")
          }
          bs.map(convert)
      }
      (name, value)
    })(collection.breakOut)
    val core = new EntityCore(meta, map)
    wrap(core)
  }
}
