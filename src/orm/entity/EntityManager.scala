package orm.entity

import java.lang.reflect.Method
import java.util
import java.util.stream.Collectors

import net.sf.cglib.proxy.Enhancer
import orm.lang.interfaces.Entity
import orm.meta.{EntityMeta, FieldMetaTypeKind, OrmMeta}
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import orm.kit.Kit

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.JavaConversions
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

  def parseArray[T](clazz: Class[T], json: String): util.Collection[T] = {
    val meta = OrmMeta.entityMap(clazz.getSimpleName())
    val data = JSON.parseRaw(json).get
    val ret = new util.ArrayList[T]()
    data.asInstanceOf[JSONArray].list.foreach(item => {
      ret.add(parseInner(meta, item.asInstanceOf[JSONObject]).asInstanceOf[T])
    })
    return ret;
  }

  def stringify(obj: Object): String = {
    val jsonObj = stringifyInner(obj)
    return jsonObj.toString()
  }

  def stringifyArray(arr: util.Collection[Object]): String = {
    var ab: ArrayBuffer[JSONObject] = new ArrayBuffer[JSONObject]()
    arr.stream().forEach(item => {
      ab += stringifyInner(item)
    })
    return new JSONArray(ab.toList).toString()
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
        case FieldMetaTypeKind.IGNORE |
             FieldMetaTypeKind.REFER |
             FieldMetaTypeKind.POINTER |
             FieldMetaTypeKind.ONE_ONE => stringifyInner(value)
        case FieldMetaTypeKind.ONE_MANY => {
          val ab = new ArrayBuffer[Object]()
          value.asInstanceOf[util.Collection[Object]].forEach(item => {
            ab += stringifyInner(item)
          })
          new JSONArray(ab.toList)
        }
      }
      (fieldMeta.name, obj)
    })(collection.breakOut)
    return new JSONObject(map)
  }

  private def parseInner(meta: EntityMeta, data: JSONObject): Object = {
    val map: Map[String, Object] = meta.fieldVec.filter(fieldMeta => {
      data.obj.contains(fieldMeta.name)
    }).map(fieldMeta => {
      val value = data.obj(fieldMeta.name)
      val obj = fieldMeta.typeKind match {
        case FieldMetaTypeKind.BUILT_IN => fieldMeta.parse(value.toString())
        case FieldMetaTypeKind.IGNORE |
             FieldMetaTypeKind.REFER |
             FieldMetaTypeKind.POINTER |
             FieldMetaTypeKind.ONE_ONE => parseInner(fieldMeta.refer, value.asInstanceOf[JSONObject])
        case FieldMetaTypeKind.ONE_MANY => {
          val list = new util.ArrayList[Object]()
          value.asInstanceOf[JSONArray].list.foreach(item => {
            list.add(parseInner(fieldMeta.refer, item.asInstanceOf[JSONObject]))
          })
          list
        }
      }
      (fieldMeta.name, obj)
    })(collection.breakOut)
    val core = new EntityCore(meta, map)
    wrap(core)
  }
}
