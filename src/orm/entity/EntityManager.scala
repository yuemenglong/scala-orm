package orm.entity

import java.lang.reflect.Method
import java.util

import net.sf.cglib.proxy.Enhancer
import orm.lang.interfaces.Entity
import orm.meta.{EntityMeta, FieldMetaTypeKind, OrmMeta}
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import orm.kit.Kit

import scala.util.parsing.json.{JSON, JSONArray, JSONObject, JSONType}

/**
  * Created by Administrator on 2017/5/18.
  */
object EntityManager {
  def empty[T](clazz: Class[T]): T = {
    require(OrmMeta.entityMap.size > 0)
    val meta = OrmMeta.entityMap(clazz.getSimpleName())
    val core = EntityCore.empty(meta)
    wrap(core).asInstanceOf[T]
  }

  def create[T](clazz: Class[T]): T = {
    require(OrmMeta.entityMap.size > 0)
    val meta = OrmMeta.entityMap(clazz.getSimpleName())
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
    require(obj.isInstanceOf[Entity])
    val entity = obj.asInstanceOf[Entity]
    return entity.$$core()
  }

  def parse[T](clazz: Class[T], json: String): T = {
    val meta = OrmMeta.entityMap(clazz.getSimpleName())
    val data = JSON.parseRaw(json).get
    parseInner(meta, data.asInstanceOf[JSONObject]).asInstanceOf[T]
  }

  def parseArray[T](clazz: Class[T], json: String): util.ArrayList[T] = {
    val meta = OrmMeta.entityMap(clazz.getSimpleName())
    val data = JSON.parseRaw(json).get
    val list = data.asInstanceOf[JSONArray].list.map(item => {
      parseInner(meta, item.asInstanceOf[JSONObject])
    })
    Kit.list(list).asInstanceOf[util.ArrayList[T]]
  }

  def stringify(obj: Object): String = {
    val jsonObj = stringifyInner(obj)
    return jsonObj.toString()
  }

  def stringifyArray(arr: util.ArrayList[Object]): String = {
    val list = Kit.array(arr).map(item => {
      stringifyInner(item)
    }).toList
    return new JSONArray(list).toString()
  }

  def stringifyInner(obj: Object): JSONObject = {
    val core = EntityManager.core(obj)
    val meta = core.meta
    val map: Map[String, Object] = meta.fieldVec.filter(fieldMeta => {
      core.fieldMap.contains(fieldMeta.name) && core.fieldMap(fieldMeta.name) != null
    }).map(fieldMeta => {
      val value = core.fieldMap(fieldMeta.name)
      val obj = fieldMeta.typeKind match {
        case FieldMetaTypeKind.BUILT_IN => fieldMeta.stringify(value)
        case FieldMetaTypeKind.REFER |
             FieldMetaTypeKind.POINTER |
             FieldMetaTypeKind.ONE_ONE => stringifyInner(value)
        case FieldMetaTypeKind.ONE_MANY => {
          val list = Kit.array(value.asInstanceOf[util.ArrayList[Object]]).map(item => {
            stringifyInner(item)
          }).toList
          new JSONArray(list)
        }
      }
      (fieldMeta.name, obj)
    })(collection.breakOut)
    return new JSONObject(map)
  }

  private def parseInner(meta: EntityMeta, data: JSONObject): Object = {
    val map: Map[String, Object] = meta.fieldVec.filter(fieldMeta => {
      val name = fieldMeta.name
      data.obj.contains(fieldMeta.name)
    }).map(fieldMeta => {
      val name = fieldMeta.name
      val value = data.obj(fieldMeta.name)
      val obj = fieldMeta.typeKind match {
        case FieldMetaTypeKind.BUILT_IN => fieldMeta.parse(value.toString())
        case FieldMetaTypeKind.REFER |
             FieldMetaTypeKind.POINTER |
             FieldMetaTypeKind.ONE_ONE => parseInner(fieldMeta.refer, value.asInstanceOf[JSONObject])
        case FieldMetaTypeKind.ONE_MANY => {
          val list = value.asInstanceOf[JSONArray].list.map(item => {
            parseInner(fieldMeta.refer, item.asInstanceOf[JSONObject])
          })
          Kit.list(list)
        }
      }
      (fieldMeta.name, obj)
    })(collection.breakOut)
    val core = new EntityCore(meta, map)
    wrap(core)
  }
}
