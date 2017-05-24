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

  def ne(field: String, param: Object): Cond = {
    items += new Ne(field, param)
    this
  }

  def gt(field: String, param: Object): Cond = {
    items += new Gt(field, param)
    this
  }

  def lt(field: String, param: Object): Cond = {
    items += new Lt(field, param)
    this
  }

  def gte(field: String, param: Object): Cond = {
    items += new Gte(field, param)
    this
  }

  def lte(field: String, param: Object): Cond = {
    items += new Lte(field, param)
    this
  }

  def toSql(alias: String, meta: EntityMeta): String = {
    if (items.length == 0) {
      return null
    }
    return items.map(item => item.toSql(alias, meta)).mkString("\n\tAND ")
  }

  def toParams(): Array[Object] = {
    items.flatMap(item => item.toParams()).toArray
  }

  def check(entityMeta: EntityMeta): Unit = {
    items.map(item => item.check(entityMeta))
  }

  //////////////////////////////////////////////////////

  trait CondItem {
    def toSql(alias: String, meta: EntityMeta): String

    def toParams(): Array[Object]

    def check(meta: EntityMeta): Unit
  }

  class Eq(val field: String, val param: Object) extends CondItem {
    override def toSql(alias: String, meta: EntityMeta): String = {
      val column = meta.fieldMap(field).column
      s"${alias}.${column} = ?"
    }

    override def toParams(): Array[Object] = {
      Array(param)
    }

    override def check(meta: EntityMeta): Unit = {
      require(meta.fieldMap.contains(field))
    }
  }

  class Ne(val field: String, val param: Object) extends CondItem {
    override def toSql(alias: String, meta: EntityMeta): String = {
      val column = meta.fieldMap(field).column
      s"${alias}.${column} <> ?"
    }

    override def toParams(): Array[Object] = {
      Array(param)
    }

    override def check(meta: EntityMeta): Unit = {
      require(meta.fieldMap.contains(field))
    }
  }

  class Gt(val field: String, val param: Object) extends CondItem {
    override def toSql(alias: String, meta: EntityMeta): String = {
      val column = meta.fieldMap(field).column
      s"${alias}.${column} > ?"
    }

    override def toParams(): Array[Object] = {
      Array(param)
    }

    override def check(meta: EntityMeta): Unit = {
      require(meta.fieldMap.contains(field))
    }
  }

  class Lt(val field: String, val param: Object) extends CondItem {
    override def toSql(alias: String, meta: EntityMeta): String = {
      val column = meta.fieldMap(field).column
      s"${alias}.${column} < ?"
    }

    override def toParams(): Array[Object] = {
      Array(param)
    }

    override def check(meta: EntityMeta): Unit = {
      require(meta.fieldMap.contains(field))
    }
  }

  class Gte(val field: String, val param: Object) extends CondItem {
    override def toSql(alias: String, meta: EntityMeta): String = {
      val column = meta.fieldMap(field).column
      s"${alias}.${column} >= ?"
    }

    override def toParams(): Array[Object] = {
      Array(param)
    }

    override def check(meta: EntityMeta): Unit = {
      require(meta.fieldMap.contains(field))
    }
  }

  class Lte(val field: String, val param: Object) extends CondItem {
    override def toSql(alias: String, meta: EntityMeta): String = {
      val column = meta.fieldMap(field).column
      s"${alias}.${column} <= ?"
    }

    override def toParams(): Array[Object] = {
      Array(param)
    }

    override def check(meta: EntityMeta): Unit = {
      require(meta.fieldMap.contains(field))
    }
  }
}

object Cond {
  def byEq(field: String, param: Object): Cond = {
    new Cond().eq(field, param)
  }
}
