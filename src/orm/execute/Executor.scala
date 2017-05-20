package orm.execute

import java.sql.{Connection, Statement}

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
  var withs = new ArrayBuffer[(String, Executor)]()

  def insert(field: String): Executor = {
    require(meta.fieldMap.contains(field) && meta.fieldMap(field).isObject())
    val execute = new Executor(meta.fieldMap(field).referMeta, Cascade.INSERT)
    this.withs.+=((field, execute))
    this.withs.last._2
  }

  def execute(entity: Object, conn: Connection): Int = {
    val core = EntityManager.core(entity)
    require(core != null)
    require(meta == core.meta)
    this.executePointer(core, conn)
    +this.executeSelf(core, conn)
    +this.executeOneOne(core, conn)
    +this.executeOneMany(core, conn)
  }

  private def executeInsert(core: EntityCore, conn: Connection): Int = {
    val validFields = core.meta.fieldVec.filter(field => {
      field.isNormal() && core.fieldMap.contains(field.name)
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
      core.meta.fieldMap(field).isPointer() && core.fieldMap.contains(field)
    }.foreach { case (field, ex) => {
      val obj = core.fieldMap(field)
      ret += ex.execute(obj, conn)
      val objCore = EntityManager.core(obj)
      val fieldMeta = core.meta.fieldMap(field)
      // a.b_id = b_id
      core.fieldMap += (fieldMeta.left -> objCore.fieldMap(fieldMeta.right))
    }
    }
    ret
  }

  private def executeOneOne(core: EntityCore, conn: Connection): Int

  = {
    0
  }

  private def executeOneMany(core: EntityCore, conn: Connection): Int

  = {
    0
  }

}

object Executor {
  def insert(clazz: Class[_]): Executor = {
    var meta = OrmMeta.entityMap(clazz.getSimpleName())
    return new Executor(meta, Cascade.INSERT)
  }
}
