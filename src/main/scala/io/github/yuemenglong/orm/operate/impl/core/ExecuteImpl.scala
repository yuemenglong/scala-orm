package io.github.yuemenglong.orm.operate.impl.core

import java.sql.{Connection, Statement}

import io.github.yuemenglong.orm.entity.{EntityCore, EntityManager}
import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.interfaces.Entity
import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.traits.core.{ExecuteJoin, ExecuteRoot}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by yml on 2017/7/15.
  */
abstract class ExecuteJoinImpl(meta: EntityMeta) extends ExecuteJoin {
  private var cascades = new ArrayBuffer[(String, ExecuteJoinImpl)]()
  private var spec = Map[Object, ExecuteJoinImpl]()
  protected var ignoreFields = Set[String]()

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
    cascades.filter { case (field, _) =>
      core.meta.fieldMap(field).isPointer &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) =>
      // insert(b)
      val b = core.fieldMap(field).asInstanceOf[Entity]
      val bCore = b.$$core()
      ret += ex.execute(b, conn)
      // a.b_id = b.id
      val fieldMeta = core.meta.fieldMap(field).asInstanceOf[FieldMetaPointer]
      core.fieldMap += (fieldMeta.left -> bCore.fieldMap(fieldMeta.right))
    }
    ret
  }

  private def executeOneOne(core: EntityCore, conn: Connection): Int = {
    var ret = 0
    cascades.filter { case (field, _) =>
      core.meta.fieldMap(field).isOneOne &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) =>
      // b.a_id = a.id
      val fieldMeta = core.meta.fieldMap(field).asInstanceOf[FieldMetaOneOne]
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
    cascades.filter { case (field, _) =>
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
          val fieldMeta = core.meta.fieldMap(field).asInstanceOf[FieldMetaOneMany]
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
    if (!meta.fieldMap(field).isRefer) throw new RuntimeException(s"$field Is Not Object")
  }

  override def insert(field: String): ExecuteJoin = {
    checkField(field)
    cascades.find(_._1 == field) match {
      case None =>
        val execute = new InsertJoin(meta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer)
        cascades.+=((field, execute))
        execute
      case Some(pair) => pair._2
    }
  }

  override def update(field: String): ExecuteJoin = {
    checkField(field)
    cascades.find(_._1 == field) match {
      case None =>
        val execute = new UpdateJoin(meta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer)
        cascades.+=((field, execute))
        execute
      case Some(pair) => pair._2
    }
  }

  override def delete(field: String): ExecuteJoin = {
    checkField(field)
    cascades.find(_._1 == field) match {
      case None =>
        val execute = new DeleteJoin(meta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer)
        cascades.+=((field, execute))
        execute
      case Some(pair) => pair._2
    }
  }

  override def ignore(field: String): ExecuteJoin = {
    if (!meta.fieldMap.contains(field)) {
      throw new RuntimeException(s"""Invalid Field: $field""")
    } else if (meta.fieldMap(field).isNormalOrPkey) {
      ignoreFields += field
    } else {
      cascades.remove(cascades.indexWhere(_._1 == field))
    }
    this
  }

  override def insert(obj: Object): ExecuteJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new InsertJoin(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def update(obj: Object): ExecuteJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new UpdateJoin(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def delete(obj: Object): ExecuteJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new DeleteJoin(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def ignore(obj: Object): ExecuteJoin = {
    spec += ((obj, null))
    this
  }

}

class InsertJoin(meta: EntityMeta) extends ExecuteJoinImpl(meta) {
  override def executeSelf(core: EntityCore, conn: Connection): Int = {
    val validFields = core.meta.fields().filter(field => {
      field.isNormalOrPkey &&
        core.fieldMap.contains(field.name) &&
        !ignoreFields.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}`"
    }).mkString(", ")
    val values = validFields.map(_ => {
      "?"
    }).mkString(", ")

    val sql = s"INSERT INTO `${core.meta.table}`($columns) values($values)"
    val params = validFields.map(f => core.get(f.name)).toArray

    Kit.logSql(sql, params)

    val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    validFields.zipWithIndex.foreach {
      case (field, i) => stmt.setObject(i + 1, core.get(field.name))
    }

    val affected = stmt.executeUpdate()
    if (!core.meta.pkey.isAuto) {
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

class UpdateJoin(meta: EntityMeta) extends ExecuteJoinImpl(meta) {
  override def executeSelf(core: EntityCore, conn: Connection): Int = {
    if (core.getPkey == null) throw new RuntimeException("Update Entity Must Has Pkey")
    val validFields = core.meta.fields().filter(field => {
      field.isNormal &&
        core.fieldMap.contains(field.name) &&
        !ignoreFields.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}` = ?"
    }).mkString(", ")
    val idCond = s"${core.meta.pkey.column} = ?"
    val sql = s"UPDATE `${core.meta.table}` SET $columns WHERE $idCond"
    val params = validFields.map(f => core.get(f.name)).toArray ++ Array(core.getPkey)
    //    val stmt = conn.prepareStatement(sql)
    //    validFields.zipWithIndex.foreach { case (field, i) =>
    //      stmt.setObject(i + 1, core.get(field.name))
    //    }
    //    stmt.setObject(validFields.length + 1, core.getPkey)
    //
    //    // id作为条件出现
    //    val params = validFields.++(Array(core.meta.pkey)).map(item => {
    //      core.get(item.name) match {
    //        case null => "null"
    //        case v => v.toString
    //      }
    //    }).mkString(", ")
    if (validFields.isEmpty) {
      Kit.logSql(sql, params)
      Logger.warn("No Field To Update")
      //      stmt.close()
      return 0
    }
    Kit.execute(conn, sql, params)
    //    val ret = stmt.executeUpdate()
    //    stmt.close()
    //    ret
  }
}

class DeleteJoin(meta: EntityMeta) extends ExecuteJoinImpl(meta) {
  override def executeSelf(core: EntityCore, conn: Connection): Int = {
    if (core.getPkey == null) throw new RuntimeException("Delete Entity Must Has Pkey")
    val idCond = s"${core.meta.pkey.name} = ?"
    val sql = s"DELETE FROM `${core.meta.table}` WHERE $idCond"
    Kit.execute(conn, sql, Array(core.getPkey))
    //    val stmt = conn.prepareStatement(sql)
    //    stmt.setObject(1, core.getPkey)
    //    val ret = stmt.executeUpdate()
    //    stmt.close()
    //    ret
  }
}

class ExecuteRootImpl(obj: Object, impl: ExecuteJoinImpl) extends ExecuteRoot {

  override def execute(conn: Connection): Int = impl.execute(obj.asInstanceOf[Entity], conn)

  override def insert(field: String): ExecuteJoin = impl.insert(field)

  override def update(field: String): ExecuteJoin = impl.update(field)

  override def delete(field: String): ExecuteJoin = impl.delete(field)

  override def ignore(field: String): ExecuteJoin = impl.ignore(field)

  override def insert(obj: Object): ExecuteJoin = impl.insert(obj)

  override def update(obj: Object): ExecuteJoin = impl.update(obj)

  override def delete(obj: Object): ExecuteJoin = impl.delete(obj)

  override def ignore(obj: Object): ExecuteJoin = impl.ignore(obj)

  override def walk(fn: (Entity) => Entity): Unit = {
    EntityManager.walk(obj.asInstanceOf[Entity], fn)
  }
}

object ExecuteRootImpl {
  private def check(obj: Object): Unit = {
    if (!obj.isInstanceOf[Entity]) {
      throw new RuntimeException("Not Entity, Maybe Need Convert")
    }
  }

  def insert(obj: Object): ExecuteRoot = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new InsertJoin(meta)
    new ExecuteRootImpl(obj, ex)
  }

  def update(obj: Object): ExecuteRoot = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new UpdateJoin(meta)
    new ExecuteRootImpl(obj, ex)
  }

  def delete(obj: Object): ExecuteRoot = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new DeleteJoin(meta)
    new ExecuteRootImpl(obj, ex)
  }
}
