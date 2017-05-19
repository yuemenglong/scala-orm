package orm.entity

import java.lang.reflect.Method
import java.util.regex.Pattern

import net.sf.cglib.proxy.{Enhancer, InvocationHandler}
import orm.java.interfaces.Entity
import orm.meta.OrmMeta

/**
  * Created by Administrator on 2017/5/18.
  */
object EntityManager {

  private val pattern = Pattern.compile("(get|set|clear)(.+)")
  private val coreFn = "$$core"

  def create[T](clazz: Class[T]): T = {
    require(OrmMeta.entityMap.size > 0)
    val meta = OrmMeta.entityMap(clazz.getSimpleName())
    val core = EntityCore.create(meta)
    val enhancer: Enhancer = new Enhancer
    enhancer.setSuperclass(clazz)
    enhancer.setInterfaces(Array(classOf[Entity]))
    import net.sf.cglib.proxy.MethodInterceptor
    import net.sf.cglib.proxy.MethodProxy
    enhancer.setCallback(new MethodInterceptor() {
      @throws[Throwable]
      def intercept(obj: Object, method: Method, args: Array[Object], proxy: MethodProxy): Object = {
        if (method.getName() == coreFn) {
          return core
        }
        if (method.getName() == "toString"){
          return core.toString()
        }
        val matcher = pattern.matcher(method.getName())
        if (!matcher.matches()) {
          //          return clazz.getDeclaredMethod(method.getName()).invoke(proxy, args)
          return proxy.invokeSuper(obj, args)
        }
        val op = matcher.group(1)
        var field = matcher.group(2)
        field = field.substring(0, 1).toLowerCase() + field.substring(1)

        return op match {
          case "get" => core.get(field)
          case "set" => core.set(field, args(0))
          case "clear" => throw new RuntimeException("")
        }
      }

    })
    enhancer.create().asInstanceOf[T]
  }

  def core(obj: Object): EntityCore = {
    if (!obj.isInstanceOf[Entity]) {
      return null
    }
    val entity = obj.asInstanceOf[Entity]
    return entity.$$core()
  }
}
