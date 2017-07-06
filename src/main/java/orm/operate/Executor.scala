package orm.operate

import java.sql.{Connection, Statement}

import orm.entity.{EntityCore, EntityManager}
import orm.meta.EntityMeta

import scala.collection.mutable.ArrayBuffer

object Cascade {
  val NULL: Int = 0
  val INSERT: Int = 1
  val UPDATE: Int = 2
  val DELETE: Int = 3
}

class Executor(val meta: EntityMeta, val cascade: Int) {
  // 只有顶层有entity
  private var withs = new ArrayBuffer[(String, Executor)]()
  private var entity: Object = null
  private var cond: Cond = null
  private var spec = Map[Object, Executor]()

  private def setEntity(entity: Object): Unit = {
    this.entity = entity
  }

  def getEntity(): Object = {
    entity
  }

  def where(cond: Cond): Unit = {
    this.cond = cond
  }

  def insert(field: String): Executor = {
    require(meta.fieldMap.contains(field) && meta.fieldMap(field).isObject())
    val execute = new Executor(meta.fieldMap(field).refer, Cascade.INSERT)
    this.withs.+=((field, execute))
    this.withs.last._2
  }

  def update(field: String): Executor = {
    require(meta.fieldMap.contains(field) && meta.fieldMap(field).isObject())
    val execute = new Executor(meta.fieldMap(field).refer, Cascade.UPDATE)
    this.withs.+=((field, execute))
    this.withs.last._2
  }

  def delete(field: String): Executor = {
    require(meta.fieldMap.contains(field) && meta.fieldMap(field).isObject())
    val execute = new Executor(meta.fieldMap(field).refer, Cascade.DELETE)
    this.withs.+=((field, execute))
    this.withs.last._2
  }


  def insert(obj: Object): Executor = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new Executor(referMeta, Cascade.INSERT)
    this.spec += ((obj, executor))
    executor
  }

  def update(obj: Object): Executor = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new Executor(referMeta, Cascade.UPDATE)
    this.spec += ((obj, executor))
    executor
  }

  def delete(obj: Object): Executor = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new Executor(referMeta, Cascade.DELETE)
    this.spec += ((obj, executor))
    executor
  }

  def ignore(obj: Object): Executor = {
    this.spec += ((obj, null))
    null
  }


  def execute(conn: Connection): Int = {
    require(entity != null)
    execute(entity, conn)
  }

  private def execute(entity: Object, conn: Connection): Int = {
    if (entity == null) {
      return 0
    }
    val core = EntityManager.core(entity)
    require(meta == core.meta)
    return (this.executePointer(core, conn)
      + this.executeSelf(core, conn)
      + this.executeOneOne(core, conn)
      + this.executeOneMany(core, conn)
      )
  }

  private def executeInsert(core: EntityCore, conn: Connection): Int = {
    val validFields = core.meta.managedFieldVec().filter(field => {
      field.isNormalOrPkey() && core.fieldMap.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}`"
    }).mkString(", ")
    val values = validFields.map(_ => {
      "?"
    }).mkString(", ")
    val sql = s"INSERT INTO ${core.meta.table}(${columns}) values(${values})"
    val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    validFields.zipWithIndex.foreach {
      case (field, i) => stmt.setObject(i + 1, core.get(field.name))
    }
    println(sql)
    val params = validFields.map(item => {
      core.get(item.name) match {
        case null => "null"
        case v => v.toString()
      }
    }).mkString(", ")
    println(s"[Params] => [${params}]")

    val affected = stmt.executeUpdate()
    if (!core.meta.pkey.auto) {
      stmt.close()
      return affected
    }
    val rs = stmt.getGeneratedKeys()
    if (rs.next()) {
      val id = rs.getObject(1)
      core.fieldMap += (core.meta.pkey.name -> id)
    }
    rs.close()
    stmt.close()
    affected
  }

  private def executeUpdate(core: EntityCore, conn: Connection): Int = {
    require(core.getPkey() != null)
    val validFields = core.meta.managedFieldVec().filter(field => {
      field.isNormal() && core.fieldMap.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}` = ?"
    }).mkString(", ")
    val idCond = s"${core.meta.pkey.name} = ?"
    val sql = s"UPDATE ${core.meta.table} SET ${columns} WHERE ${idCond}"
    val stmt = conn.prepareStatement(sql)
    validFields.zipWithIndex.foreach { case (field, i) =>
      stmt.setObject(i + 1, core.get(field.name))
    }
    stmt.setObject(validFields.length + 1, core.getPkey())
    println(sql)
    val ret = stmt.executeUpdate()
    stmt.close()
    ret
  }

  private def executeDelete(core: EntityCore, conn: Connection): Int = {
    require(core.getPkey() != null)
    val idCond = s"${core.meta.pkey.name} = ?"
    val sql = s"DELETE FROM ${core.meta.table} WHERE ${idCond}"
    val stmt = conn.prepareStatement(sql)
    stmt.setObject(1, core.getPkey())
    println(sql)
    val ret = stmt.executeUpdate()
    stmt.close()
    ret
  }

  private def executeSelf(core: EntityCore, conn: Connection): Int = {
    this.cascade match {
      case Cascade.INSERT => executeInsert(core, conn)
      case Cascade.UPDATE => executeUpdate(core, conn)
      case Cascade.DELETE => executeDelete(core, conn)
    }
  }

  private def executePointer(core: EntityCore, conn: Connection): Int = {
    var ret = 0
    this.withs.filter { case (field, _) =>
      core.meta.fieldMap(field).isPointer() &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) => {
      // insert(b)
      val b = core.fieldMap(field)
      val bCore = EntityManager.core(b)
      ret += ex.execute(b, conn)
      // a.b_id = b.id
      require(bCore != null)
      val fieldMeta = core.meta.fieldMap(field)
      core.fieldMap += (fieldMeta.left -> bCore.fieldMap(fieldMeta.right))
    }
    }
    ret
  }

  private def executeOneOne(core: EntityCore, conn: Connection): Int = {
    var ret = 0
    this.withs.filter { case (field, _) =>
      core.meta.fieldMap(field).isOneOne() &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) => {
      // b.a_id = a.id
      val fieldMeta = core.meta.fieldMap(field)
      val b = core.fieldMap(field)
      val bCore = EntityManager.core(b)
      bCore.fieldMap += (fieldMeta.right -> core.fieldMap(fieldMeta.left))
      // insert(b)
      ret += ex.execute(b, conn)
    }
    }
    ret
  }

  private def executeOneMany(core: EntityCore, conn: Connection): Int = {
    var ret = 0
    this.withs.filter { case (field, _) =>
      core.meta.fieldMap(field).isOneMany() &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) => {
      val bs = core.fieldMap(field).asInstanceOf[Array[Object]]
      bs.foreach(b => {
        // 配置了特殊处理的方法
        if (spec.contains(b)) {
          val specEx = spec(b)
          if (specEx != null) {
            ret += specEx.execute(b, conn)
          }
        } else {
          // b.a_id = a.id
          val fieldMeta = core.meta.fieldMap(field)
          val bCore = EntityManager.core(b)
          bCore.fieldMap += (fieldMeta.right -> core.fieldMap(fieldMeta.left))
          // insert(b)
          ret += ex.execute(b, conn)
        }
      })
    }
    }
    ret
  }

}

object Executor {
  def createInsert(entity: Object): Executor = {
    val meta = EntityManager.core(entity).meta
    val ret = new Executor(meta, Cascade.INSERT)
    ret.setEntity(entity)
    return ret
  }

  def createUpdate(entity: Object): Executor = {
    val meta = EntityManager.core(entity).meta
    val ret = new Executor(meta, Cascade.UPDATE)
    ret.setEntity(entity)
    return ret
  }

  def createDelete(entity: Object): Executor = {
    val meta = EntityManager.core(entity).meta
    val ret = new Executor(meta, Cascade.DELETE)
    ret.setEntity(entity)
    return ret
  }
}
