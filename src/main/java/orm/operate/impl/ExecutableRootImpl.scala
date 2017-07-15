package orm.operate.impl

import java.sql.{Connection, Statement}

import orm.entity.{EntityCore, EntityManager}
import orm.lang.interfaces.Entity
import orm.meta.{EntityMeta, OrmMeta}
import orm.operate.traits.core.{ExecutableJoin, ExecutableRoot}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by yml on 2017/7/15.
  */
abstract class ExecutableJoinImpl(meta: EntityMeta) extends ExecutableJoin {
  private var withs = new ArrayBuffer[(String, ExecutableJoinImpl)]()
  private var spec = Map[Object, ExecutableJoinImpl]()

  def execute(entity: Entity, conn: Connection): Int = {
    if (entity.$$core().meta != meta) {
      throw new RuntimeException(s"Meta Info Not Match, ${entity.$$core().meta.entity}:${meta.entity}")
    }
    if (entity == null) {
      return 0
    }
    val core = entity.$$core()
    require(meta == core.meta)
    (executePointer(core, conn)
      + executeSelf(core, conn)
      + executeOneOne(core, conn)
      + executeOneMany(core, conn)
      )
  }

  def executeSelf(core: EntityCore, conn: Connection): Int

  private def executePointer(core: EntityCore, conn: Connection): Int = {
    var ret = 0
    withs.filter { case (field, _) =>
      core.meta.fieldMap(field).isPointer &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) =>
      // insert(b)
      val b = core.fieldMap(field).asInstanceOf[Entity]
      val bCore = b.$$core()
      ret += ex.execute(b, conn)
      // a.b_id = b.id
      val fieldMeta = core.meta.fieldMap(field)
      core.fieldMap += (fieldMeta.left -> bCore.fieldMap(fieldMeta.right))
    }
    ret
  }

  private def executeOneOne(core: EntityCore, conn: Connection): Int = {
    var ret = 0
    withs.filter { case (field, _) =>
      core.meta.fieldMap(field).isOneOne &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) =>
      // b.a_id = a.id
      val fieldMeta = core.meta.fieldMap(field)
      val b = core.fieldMap(field).asInstanceOf[Entity]
      val bCore = b.$$core()
      bCore.fieldMap += (fieldMeta.right -> core.fieldMap(fieldMeta.left))
      // insert(b)
      ret += ex.execute(b, conn)
    }
    ret
  }

  private def executeOneMany(core: EntityCore, conn: Connection): Int = {
    var ret = 0
    withs.filter { case (field, _) =>
      core.meta.fieldMap(field).isOneMany &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) =>
      val bs = core.fieldMap(field).asInstanceOf[Array[Object]]
      bs.map(_.asInstanceOf[Entity]).foreach(b => {
        // 配置了特殊处理的方法
        if (spec.contains(b)) {
          val specEx = spec(b)
          if (specEx != null) {
            ret += specEx.execute(b, conn)
          }
        } else {
          // b.a_id = a.id
          val fieldMeta = core.meta.fieldMap(field)
          val bCore = b.$$core()
          bCore.fieldMap += (fieldMeta.right -> core.fieldMap(fieldMeta.left))
          // insert(b)
          ret += ex.execute(b, conn)
        }
      })
    }
    ret
  }

  private def checkField(field: String): Unit = {
    if (!meta.fieldMap.contains(field)) throw new RuntimeException(s"Unknown Field $field In ${meta.entity}")
    if (!meta.fieldMap(field).isObject) throw new RuntimeException(s"$field Is Not Object")
  }

  override def insert(field: String): ExecutableJoin = {
    checkField(field)
    val execute = new Insert(meta.fieldMap(field).refer)
    withs.+=((field, execute))
    withs.last._2
  }

  override def update(field: String): ExecutableJoin = {
    checkField(field)
    val execute = new Update(meta.fieldMap(field).refer)
    withs.+=((field, execute))
    withs.last._2
  }

  override def delete(field: String): ExecutableJoin = {
    checkField(field)
    val execute = new Delete(meta.fieldMap(field).refer)
    withs.+=((field, execute))
    withs.last._2
  }

  override def ignore(field: String): ExecutableJoin = {
    checkField(field)
    withs.remove(withs.indexWhere(_._1 == field))
    this
  }

  override def insert(obj: Object): ExecutableJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new Insert(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def update(obj: Object): ExecutableJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new Update(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def delete(obj: Object): ExecutableJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new Delete(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def ignore(obj: Object): ExecutableJoin = {
    spec += ((obj, null))
    this
  }

}

class Insert(meta: EntityMeta) extends ExecutableJoinImpl(meta) {
  override def executeSelf(core: EntityCore, conn: Connection): Int = {
    val validFields = core.meta.managedFieldVec().filter(field => {
      field.isNormalOrPkey && core.fieldMap.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}`"
    }).mkString(", ")
    val values = validFields.map(_ => {
      "?"
    }).mkString(", ")
    val sql = s"INSERT INTO ${core.meta.table}($columns) values($values)"
    val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    validFields.zipWithIndex.foreach {
      case (field, i) => stmt.setObject(i + 1, core.get(field.name))
    }
    println(sql)
    val params = validFields.map(item => {
      core.get(item.name) match {
        case null => "null"
        case v => v.toString
      }
    }).mkString(", ")
    println(s"[Params] => [$params]")

    val affected = stmt.executeUpdate()
    if (!core.meta.pkey.auto) {
      stmt.close()
      return affected
    }
    val rs = stmt.getGeneratedKeys
    if (rs.next()) {
      val id = rs.getObject(1)
      core.fieldMap += (core.meta.pkey.name -> id)
    }
    rs.close()
    stmt.close()
    affected
  }
}

class Update(meta: EntityMeta) extends ExecutableJoinImpl(meta) {
  override def executeSelf(core: EntityCore, conn: Connection): Int = {
    if (core.getPkey == null) throw new RuntimeException("Update Entity Must Has Pkey")
    val validFields = core.meta.managedFieldVec().filter(field => {
      field.isNormal && core.fieldMap.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}` = ?"
    }).mkString(", ")
    val idCond = s"${core.meta.pkey.column} = ?"
    val sql = s"UPDATE ${core.meta.table} SET $columns WHERE $idCond"
    val stmt = conn.prepareStatement(sql)
    validFields.zipWithIndex.foreach { case (field, i) =>
      stmt.setObject(i + 1, core.get(field.name))
    }
    stmt.setObject(validFields.length + 1, core.getPkey)
    println(sql)
    // id作为条件出现
    val params = validFields.++(Array(core.meta.pkey)).map(item => {
      core.get(item.name) match {
        case null => "null"
        case v => v.toString
      }
    }).mkString(", ")
    println(s"[Params] => [$params]")
    val ret = stmt.executeUpdate()
    stmt.close()
    ret
  }
}

class Delete(meta: EntityMeta) extends ExecutableJoinImpl(meta) {
  override def executeSelf(core: EntityCore, conn: Connection): Int = {
    if (core.getPkey == null) throw new RuntimeException("Delete Entity Must Has Pkey")
    val idCond = s"${core.meta.pkey.name} = ?"
    val sql = s"DELETE FROM ${core.meta.table} WHERE $idCond"
    val stmt = conn.prepareStatement(sql)
    stmt.setObject(1, core.getPkey)
    println(sql)
    println(s"[Params] => [${core.getPkey}]")
    val ret = stmt.executeUpdate()
    stmt.close()
    ret
  }
}

class ExecutableRootImpl(obj: Object, impl: ExecutableJoinImpl) extends ExecutableRoot {

  override def execute(conn: Connection): Int = impl.execute(obj.asInstanceOf[Entity], conn)

  override def insert(field: String): ExecutableJoin = impl.insert(field)

  override def update(field: String): ExecutableJoin = impl.update(field)

  override def delete(field: String): ExecutableJoin = impl.delete(field)

  override def ignore(field: String): ExecutableJoin = impl.ignore(field)

  override def insert(obj: Object): ExecutableJoin = impl.insert(obj)

  override def update(obj: Object): ExecutableJoin = impl.update(obj)

  override def delete(obj: Object): ExecutableJoin = impl.delete(obj)

  override def ignore(obj: Object): ExecutableJoin = impl.ignore(obj)

  override def postExecute(fn: (Entity) => Unit): Unit = fn(obj.asInstanceOf[Entity])
}

object ExecutableRootImpl {
  private def check(obj: Object): Unit = {
    if (!obj.isInstanceOf[Entity]) {
      throw new RuntimeException("Not Entity")
    }
  }

  def insert(obj: Object): ExecutableRoot = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new Insert(meta)
    new ExecutableRootImpl(obj, ex)
  }

  def update(obj: Object): ExecutableRoot = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new Update(meta)
    new ExecutableRootImpl(obj, ex)
  }

  def delete(obj: Object): ExecutableRoot = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new Delete(meta)
    new ExecutableRootImpl(obj, ex)
  }
}
