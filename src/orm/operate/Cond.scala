package orm.operate

import orm.meta.EntityMeta

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/22.
  */
class Cond {
  var items = new ArrayBuffer[CondItem]()

  def eq(field: String, param: Object): Cond = {
    items += new Eq(field, param)
    this
  }

  def toSql(alias: String): String = {
    if(items.length == 0){
      return null
    }
    return items.map(item => item.toSql(alias)).mkString("\n\tAND ")
  }

  def toParams(): Array[Object] = {
    items.flatMap(item => item.toParams()).toArray
  }

  def check(entityMeta: EntityMeta): Unit = {
    items.map(item => item.check(entityMeta))
  }
}

object Cond {
  def byEq(field: String, param: Object): Cond = {
    new Cond().eq(field, param)

  }
}

trait CondItem {
  def toSql(alias: String): String

  def toParams(): Array[Object]

  def check(entity: EntityMeta): Unit
}

class Eq(val field: String, val param: Object) extends CondItem {
  override def toSql(alias: String): String = {
    s"${alias}.${field} = ?"
  }

  override def toParams(): Array[Object] = {
    Array(param)
  }

  override def check(entity: EntityMeta): Unit = {
    require(entity.fieldMap.contains(field))
  }
}

