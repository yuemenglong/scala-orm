package orm.operate

import orm.meta.EntityMeta

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/5/22.
  */
class JoinCond {
  var items = new ArrayBuffer[JoinItem]()

  def toSql(a1: String, a2: String, m1: EntityMeta, m2: EntityMeta): String = {
    items.map(_.toSql(a1, a2, m1, m2)).mkString(" AND ")
  }

  def toParams(): Array[Object] = {
    items.flatMap(_.toParams()).toArray
  }

  def check(m1: EntityMeta, m2: EntityMeta): Unit = {
    items.foreach(_.check(m1, m2))
  }

  def eq(f1: String, f2: String): JoinCond = {
    items += new Eq(f1, f2)
    this
  }

  trait JoinItem {
    def toSql(a1: String, a2: String, m1: EntityMeta, m2: EntityMeta): String

    def toParams(): Array[Object]

    def check(m1: EntityMeta, m2: EntityMeta): Unit
  }

  class Eq(val f1: String, val f2: String) extends JoinItem {
    override def toSql(a1: String, a2: String, m1: EntityMeta, m2: EntityMeta): String = {
      val c1 = m1.fieldMap(f1).column
      val c2 = m1.fieldMap(f2).column
      s"${a1}.${c1} = ${a2}.${c2}"
    }

    override def toParams(): Array[Object] = {
      Array()
    }

    override def check(m1: EntityMeta, m2: EntityMeta): Unit = {
      require(m1.fieldMap.contains(f1) && m2.fieldMap.contains(f2))
    }
  }
}

object JoinCond {
  def byEq(f1: String, f2: String): JoinCond = {
    new JoinCond().eq(f1, f2)
  }
}