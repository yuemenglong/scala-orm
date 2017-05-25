package orm.operate

import java.sql.{Connection, Statement}
import java.util

import orm.entity.{EntityCore, EntityManager}
import orm.meta.{EntityMeta, OrmMeta}

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

  private def setEntity(entity: Object): Unit = {
    this.entity = entity
  }

  def getEntity(): Object = {
    entity
  }

  def insert(field: String): Executor = {
    require(meta.fieldMap.contains(field) && meta.fieldMap(field).isObject())
    val execute = new Executor(meta.fieldMap(field).refer, Cascade.INSERT)
    this.withs.+=((field, execute))
    this.withs.last._2
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
    val validFields = core.meta.fieldVec.filter(field => {
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
    val affected = stmt.executeUpdate()
    if (!core.meta.pkey.auto) {
      return affected
    }
    val rs = stmt.getGeneratedKeys()
    if (rs.next()) {
      val id = rs.getObject(1)
      core.fieldMap += (core.meta.pkey.name -> id)
    }
    affected
  }

  private def executeUpdate(core: EntityCore, conn: Connection): Int = {
    0
  }

  private def executeDelete(core: EntityCore, conn: Connection): Int = {
    0
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
      val bs = core.fieldMap(field).asInstanceOf[util.ArrayList[Object]]
      for (i <- 0 to bs.size() - 1) {
        val b = bs.get(i)
        // b.a_id = a.id
        val fieldMeta = core.meta.fieldMap(field)
        val bCore = EntityManager.core(b)
        bCore.fieldMap += (fieldMeta.right -> core.fieldMap(fieldMeta.left))
        // insert(b)
        ret += ex.execute(b, conn)
      }
    }
    }
    ret
  }

}

object Executor {
  def createUpdate(entity: Object): Executor = {
    val meta = EntityManager.core(entity).meta
    val ret = new Executor(meta, Cascade.UPDATE)
    ret.setEntity(entity)
    return ret
  }

  def createInsert(entity: Object): Executor = {
    val meta = EntityManager.core(entity).meta
    val ret = new Executor(meta, Cascade.INSERT)
    ret.setEntity(entity)
    return ret
  }
}
