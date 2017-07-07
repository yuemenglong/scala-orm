package orm.operate

import java.sql.{Connection, ResultSet}

import orm.entity.{EntityCore, EntityManager}
import orm.meta.{EntityMeta, OrmMeta}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.ClassTag

/**
  * Created by Administrator on 2017/5/22.
  */

// table alias: entity_field
// column alias: entity$field
class Selector[T](meta: EntityMeta, alias: String, parent: SelectorBase = null) extends SelectorBase(meta, alias, parent) {
  require(meta != null)
  // 全部字段都要读取
  fields = meta.managedFieldVec().filter(_.isNormalOrPkey).map(_.name)(collection.breakOut)

  // 过滤结果用
  // 1. 该表的对象 pkey
  // 2. 与该表有一对多关系的对象 field@pkey
  // 3. 最终的结果集 @pkey
  var filterMap: Map[String, EntityCore] = Map[String, EntityCore]()

  override def select(field: String): Selector[Object] = {
    super.select(field).asInstanceOf[Selector[Object]]
    //    if (meta.fieldMap.contains(field)) {
    //      throw new RuntimeException(s"[$field] Not Field Of [${meta.entity}]")
    //    }
    //    val fieldMeta = meta.fieldMap(field)
    //    if (fieldMeta.isNormalOrPkey) {
    //      throw new RuntimeException(s"$field Is Not Refer")
    //    }
    //    withs.find(_._1 == field) match {
    //      case None =>
    //        val selector = new Selector[Object](fieldMeta.refer, s"${this.alias}_$field", this)
    //        this.withs += ((field, selector))
    //        selector
    //      case Some(p) => p._2.asInstanceOf[Selector[Object]]
    //    }
  }

  private def resetFilterMap(): Unit = {
    filterMap = Map()
    withs.foreach { case (_, selector) =>
      selector.asInstanceOf[Selector[_]].resetFilterMap()
    }
  }

  private def bufferToArray(ab: ArrayBuffer[Object], ct: ClassTag[Object]): Array[Object] = {
    ab.map(item => {
      val core = EntityManager.core(item)
      val pairs: Array[(String, Array[Object])] = core.fieldMap.toArray.map(pair => {
        val (name, value) = pair
        value match {
          case ab: ArrayBuffer[_] =>
            val abo = ab.asInstanceOf[ArrayBuffer[Object]]
            val entityName = core.meta.fieldMap(name).typeName
            val entityClass = OrmMeta.entityMap(entityName).clazz
            val ct = ClassTag(entityClass).asInstanceOf[ClassTag[Object]]
            val array = bufferToArray(abo, ct)
            (name, array)
          case _ =>
            null
        }
      }).filter(_ != null)
      pairs.foreach(p => core.fieldMap += p)
      item
    }).toArray(ct)
  }

  def query(conn: Connection): Array[T] = {
    val sql = this.getSql
    println(sql)
    val params = this.getParams.map(_.toString()).mkString(", ")
    println(s"[Params] => [$params]")
    val stmt = conn.prepareStatement(sql)
    this.getParams.zipWithIndex.foreach { case (param, i) =>
      stmt.setObject(i + 1, param)
    }
    val rs = stmt.executeQuery()
    var ab = ArrayBuffer[Object]()
    while (rs.next()) {
      val core = this.pick(rs)
      val key = s"@${core.getPkey}"
      if (!filterMap.contains(key)) {
        ab += EntityManager.wrap(core)
      }
      filterMap += (key -> core)
    }
    rs.close()
    stmt.close()
    resetFilterMap()
    bufferToArray(ab, ClassTag(meta.clazz)).asInstanceOf[Array[T]]
  }

  def first(conn: Connection): T = {
    val arr = query(conn)
    arr.length match {
      case 0 => null.asInstanceOf[T]
      case _ => arr(0)
    }
  }

  def pick(rs: ResultSet): EntityCore = {
    val core = pickSelf(rs)
    if (core == null) {
      return null
    }
    pickRefer(rs, core)
    core
  }

  def pickSelf(rs: ResultSet): EntityCore = {
    val core = new EntityCore(meta, Map())
    meta.managedFieldVec().filter(_.isNormalOrPkey).foreach(field => {
      val label = s"$alias$$${field.name}"
      val value = rs.getObject(label)
      core.fieldMap += (field.name -> value)
    })
    val key = core.getPkey
    if (key == null) {
      return null
    }
    if (filterMap.contains(key.toString)) {
      return filterMap(key.toString)
    }
    filterMap += (key.toString -> core)
    core
  }

  def pickRefer(rs: ResultSet, core: EntityCore): Unit = {
    withs.foreach { case (field, selector) =>
      val bCore = selector.asInstanceOf[Selector[_]].pick(rs)
      val fieldMeta = meta.fieldMap(field)
      if (!fieldMeta.isOneMany) {
        if (bCore != null) {
          val entity = EntityManager.wrap(bCore)
          core.fieldMap += (field -> entity)
        } else {
          core.fieldMap += (field -> null)
        }
      }
      else {
        // 一对多的情况
        if (bCore != null) {
          val b = EntityManager.wrap(bCore)
          val key = s"$field@${bCore.getPkey.toString}"
          if (!filterMap.contains(key) && !core.fieldMap.contains(field)) {
            // 没有就创建
            core.fieldMap += (field -> new ArrayBuffer[Object]())
          }
          if (!filterMap.contains(key)) {
            // 有了就追加
            val arr = core.fieldMap(field).asInstanceOf[ArrayBuffer[Object]]
            arr += b
          }
          filterMap += (key -> bCore)
        }
      }
    }
  }

  override def newInstance(meta: EntityMeta, alias: String, parent: SelectorBase = null): SelectorBase = {
    new Selector[T](meta, alias, parent)
  }
}

object Selector {
  def from[T](clazz: Class[T]): Selector[T] = {
    OrmMeta.check()
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName)
    new Selector[T](entityMeta, entityMeta.entity)
  }

  def createSelect[T](clazz: Class[T]): Selector[T] = {
    OrmMeta.check()
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName)
    new Selector[T](entityMeta, entityMeta.entity)
  }
}
