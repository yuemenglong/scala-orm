package io.github.yuemenglong.orm.operate.execute

import io.github.yuemenglong.orm.session.Session
import io.github.yuemenglong.orm.impl.entity.{Entity, EntityCore, EntityManager}
import io.github.yuemenglong.orm.logger.Logger
import io.github.yuemenglong.orm.meta._
import io.github.yuemenglong.orm.operate.execute.traits.{ExecuteJoin, ExecuteRoot, TypedExecuteJoin, TypedExecuteRoot}

import scala.collection.mutable.ArrayBuffer

/**
 * Created by yml on 2017/7/15.
 */
abstract class ExecuteCascadeImpl(meta: EntityMeta) extends ExecuteJoin {
  protected var fields: Array[FieldMeta] = meta.fields().filter(_.isNormalOrPkey).toArray
  protected var cascades = new ArrayBuffer[(String, ExecuteJoin)]()
  protected var spec: Map[Object, ExecuteCascadeImpl] = Map[Object, ExecuteCascadeImpl]()
  protected var ignoreFields: Set[String] = Set[String]()


  override def execute(entity: Entity, session: Session): Int = {
    if (entity.$$core().meta != meta) {
      throw new RuntimeException(s"Meta Info Not Match, ${entity.$$core().meta.entity}:${meta.entity}")
    }
    if (entity == null) {
      return 0
    }
    val core = entity.$$core()
    require(meta == core.meta)
    (executePointer(core, session)
      + executeSelf(core, session)
      + executeOneOne(core, session)
      + executeOneMany(core, session)
      )
  }

  def executeSelf(core: EntityCore, session: Session): Int

  private def executePointer(core: EntityCore, session: Session): Int = {
    var ret = 0
    cascades.filter { case (field, _) =>
      core.meta.fieldMap(field).isPointer &&
        core.fieldMap.contains(field) &&
        core.fieldMap(field) != null
    }.foreach { case (field, ex) =>
      // insert(b)
      val b = core.fieldMap(field).asInstanceOf[Entity]
      val bCore = b.$$core()
      ret += ex.execute(b, session)
      // a.b_id = b.id
      val fieldMeta = core.meta.fieldMap(field).asInstanceOf[FieldMetaPointer]
      core.fieldMap += (fieldMeta.left -> bCore.fieldMap(fieldMeta.right))
    }
    ret
  }

  private def executeOneOne(core: EntityCore, session: Session): Int = {
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
      ret += ex.execute(b, session)
    }
    ret
  }

  private def executeOneMany(core: EntityCore, session: Session): Int = {
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
            ret += specEx.execute(b, session)
          }
        } else {
          // b.a_id = a.id
          val fieldMeta = core.meta.fieldMap(field).asInstanceOf[FieldMetaOneMany]
          val bCore = b.$$core()
          bCore.fieldMap += (fieldMeta.right -> core.fieldMap(fieldMeta.left))
          // insert(b)
          ret += ex.execute(b, session)
        }
      })
    }
    ret
  }

  protected def checkField(field: String): Unit = {
    if (!meta.fieldMap.contains(field)) throw new RuntimeException(s"Unknown Field $field In ${meta.entity}")
    if (!meta.fieldMap(field).isRefer) throw new RuntimeException(s"$field Is Not Object")
    if (meta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer.db != meta.db) throw new RuntimeException(s"$field Is Not The Same DB")
  }

  private def cascade(field: String, creator: (EntityMeta) => ExecuteJoin): ExecuteJoin = {
    checkField(field)
    cascades.find(_._1 == field) match {
      case None =>
        val execute = creator(meta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer)
        cascades.+=((field, execute))
        execute
      case Some((_, e)) => e
    }
  }

  override def insert(field: String): ExecuteJoin = {
    cascade(field, (meta) => new InsertJoin(meta))
  }

  override def update(field: String): ExecuteJoin = {
    cascade(field, (meta) => new UpdateJoin(meta))
  }

  override def delete(field: String): ExecuteJoin = {
    cascade(field, (meta) => new DeleteJoin(meta))
  }

  override def fields(fields: String*): ExecuteJoin = {
    fields.foreach(field => {
      if (!meta.fieldMap.contains(field)) throw new RuntimeException(s"Unknown Field $field In ${meta.entity}")
      if (!meta.fieldMap(field).isNormalOrPkey) throw new RuntimeException(s"$field Is Not Normal Field")
    })
    this.fields = fields.map(meta.fieldMap(_)).toArray
    this
  }

  override def ignore(fields: String*): ExecuteJoin = {
    fields.toArray.foreach(field => {
      if (!meta.fieldMap.contains(field)) {
        throw new RuntimeException(s"""Invalid Field: $field""")
      } else if (meta.fieldMap(field).isNormalOrPkey) {
        ignoreFields += field
      } else {
        throw new RuntimeException(s"""Not Normal Field: $field""")
      }
    })
    this
  }

  override def insertFor(obj: Object): ExecuteJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new InsertJoin(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def updateFor(obj: Object): ExecuteJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new UpdateJoin(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def deleteFor(obj: Object): ExecuteJoin = {
    val core = EntityManager.core(obj)
    val referMeta = core.meta
    val executor = new DeleteJoin(referMeta)
    spec += ((obj, executor))
    executor
  }

  override def ignoreFor(obj: Object): ExecuteJoin = {
    spec += ((obj, null))
    this
  }
}

class InsertJoin(meta: EntityMeta) extends ExecuteCascadeImpl(meta) {
  override def executeSelf(core: EntityCore, session: Session): Int = {
    val validFields = this.fields.filter(field => {
      field.isNormalOrPkey &&
        core.fieldMap.contains(field.name) &&
        !ignoreFields.contains(field.name)
      //       && (field.defaultValue match {
      //          case "" => true // 没有默认值的情况没有影响
      //          case _ => field.nullable || core.get(field.name) != null // 有默认值的情况value不能为null
      //        })
    })
    val columns = validFields.map(field => {
      s"`${field.column}`"
    }).mkString(", ")
    val values = validFields.map(_ => {
      "?"
    }).mkString(", ")

    val sql = s"INSERT INTO `${core.meta.table}`($columns) values($values)"
    val params = validFields.map(f => core.get(f.name))
    session.execute(sql, params, stmt => {
      if (core.meta.pkey.isAuto) {
        val rs = stmt.getGeneratedKeys
        if (rs.next()) {
          val id = rs.getObject(1)
          core.fieldMap += (core.meta.pkey.name -> id)
        }
        rs.close()
      }
    })
  }
}

class UpdateJoin(meta: EntityMeta) extends ExecuteCascadeImpl(meta) {
  override def executeSelf(core: EntityCore, session: Session): Int = {
    if (core.getPkey == null) throw new RuntimeException("Update Entity Must Has Pkey")
    val validFields = this.fields.filter(field => {
      field.isNormal &&
        core.fieldMap.contains(field.name) &&
        !ignoreFields.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}` = ?"
    }).mkString(", ")
    val idCond = s"${core.meta.pkey.column} = ?"
    val sql = s"UPDATE `${core.meta.table}` SET $columns WHERE $idCond"
    val params = validFields.map(f => core.get(f.name)) ++ Array(core.getPkey)

    if (validFields.isEmpty) {
      Logger.warn("No Field To Update")
      return 0
    }
    session.execute(sql, params)
  }
}

class DeleteJoin(meta: EntityMeta) extends ExecuteCascadeImpl(meta) {
  override def executeSelf(core: EntityCore, session: Session): Int = {
    if (core.getPkey == null) throw new RuntimeException("Delete Entity Must Has Pkey")
    val idCond = s"${core.meta.pkey.column} = ?"
    val sql = s"DELETE FROM `${core.meta.table}` WHERE $idCond"
    session.execute(sql, Array(core.getPkey))
  }
}

class ExecuteRootImpl(obj: Object, impl: ExecuteJoin) extends ExecuteRoot {

  override def execute(session: Session): Int = impl.execute(obj.asInstanceOf[Entity], session)

  override def insert(field: String): ExecuteJoin = impl.insert(field)

  override def update(field: String): ExecuteJoin = impl.update(field)

  override def delete(field: String): ExecuteJoin = impl.delete(field)

  override def ignore(fields: String*): ExecuteRoot = {
    impl.ignore(fields: _*)
    this
  }

  override def insertFor(obj: Object): ExecuteJoin = impl.insertFor(obj)

  override def updateFor(obj: Object): ExecuteJoin = impl.updateFor(obj)

  override def deleteFor(obj: Object): ExecuteJoin = impl.deleteFor(obj)

  override def ignoreFor(obj: Object): ExecuteRoot = {
    impl.ignoreFor(obj)
    this
  }

  override def fields(fields: String*): ExecuteRootImpl = {
    impl.fields(fields: _*)
    this
  }

  override def execute(entity: Entity, session: Session): Int = impl.execute(entity, session)
}

trait TypedExecuteCascadeImpl[T] extends TypedExecuteJoin[T] {
  def getMeta: EntityMeta

  def getCascades: ArrayBuffer[(String, ExecuteJoin)]

  private def cascade[R](fn: (T) => R, creator: (EntityMeta) => TypedExecuteJoin[R]): TypedExecuteJoin[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    val fieldMeta = getMeta.fieldMap(field)
    if (!fieldMeta.isRefer) {
      throw new RuntimeException(s"[${getMeta.entity}]'s Field [$field] Is Not Refer")
    }

    getCascades.find(_._1 == field) match {
      case None =>
        val execute: TypedExecuteJoin[R] = creator(getMeta.fieldMap(field)
          .asInstanceOf[FieldMetaRefer].refer)
        getCascades += ((field, execute))
        execute
      case Some(pair) => pair._2.asInstanceOf[TypedExecuteJoin[R]]
    }
  }

  private def cascades[R](fn: (T) => Array[R], creator: (EntityMeta) => TypedExecuteJoin[R]): TypedExecuteJoin[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    val fieldMeta = getMeta.fieldMap(field)
    if (!fieldMeta.isOneMany) {
      throw new RuntimeException(s"[${getMeta.entity}]'s Field [$field] Is Not OneMany")
    }

    getCascades.find(_._1 == field) match {
      case None =>
        val execute: TypedExecuteJoin[R] = creator(getMeta.fieldMap(field)
          .asInstanceOf[FieldMetaRefer].refer)
        getCascades += ((field, execute))
        execute
      case Some(pair) => pair._2.asInstanceOf[TypedExecuteJoin[R]]
    }
  }

  override def insert[R](fn: (T) => R): TypedExecuteJoin[R] = {
    cascade(fn, (meta) => new TypedInsertJoin[R](meta))
  }

  override def inserts[R](fn: T => Array[R]): TypedExecuteJoin[R] = {
    cascades(fn, (meta) => new TypedInsertJoin[R](meta))
  }

  override def update[R](fn: (T) => R): TypedExecuteJoin[R] = {
    cascade(fn, (meta) => new TypedUpdateJoin[R](meta))
  }

  override def updates[R](fn: T => Array[R]): TypedExecuteJoin[R] = {
    cascades(fn, (meta) => new TypedUpdateJoin[R](meta))
  }

  override def delete[R](fn: (T) => R): TypedExecuteJoin[R] = {
    cascade(fn, (meta) => new TypedDeleteJoin[R](meta))
  }

  override def deletes[R](fn: T => Array[R]): TypedExecuteJoin[R] = {
    cascades(fn, (meta) => new TypedDeleteJoin[R](meta))
  }

  override def fields(fns: (T => Object)*): TypedExecuteCascadeImpl[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    val invalid = fields.map(getMeta.fieldMap(_)).filter(!_.isNormalOrPkey).map(_.name).mkString(",")
    if (invalid.nonEmpty) {
      throw new RuntimeException(s"Not Normal Fields: [$invalid]")
    }
    this.fields(fields: _*)
    this
  }

  override def ignore(fns: (T => Object)*): TypedExecuteCascadeImpl[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    //    val invalid = fields.map(getMeta.fieldMap(_)).filter(_.isNormalOrPkey).map(_.name).mkString(",")
    //    if (invalid.nonEmpty) {
    //      throw new RuntimeException(s"Not Normal Fields: [$invalid]")
    //    }
    this.ignore(fields: _*)
    this
  }
}

class TypedInsertJoin[T](meta: EntityMeta) extends InsertJoin(meta)
  with TypedExecuteCascadeImpl[T] {
  override def getMeta: EntityMeta = meta

  override def getCascades: ArrayBuffer[(String, ExecuteJoin)] = this.cascades

}

class TypedUpdateJoin[T](meta: EntityMeta) extends UpdateJoin(meta)
  with TypedExecuteCascadeImpl[T] {
  override def getMeta: EntityMeta = meta

  override def getCascades: ArrayBuffer[(String, ExecuteJoin)] = this.cascades
}

class TypedDeleteJoin[T](meta: EntityMeta) extends DeleteJoin(meta)
  with TypedExecuteCascadeImpl[T] {
  override def getMeta: EntityMeta = meta

  override def getCascades: ArrayBuffer[(String, ExecuteJoin)] = this.cascades
}

class TypedExecuteRootImpl[T <: Object](obj: T, impl: TypedExecuteCascadeImpl[T]) extends ExecuteRootImpl(obj, impl)
  with TypedExecuteRoot[T] {
  override def insert[R](fn: (T) => R): TypedExecuteJoin[R] = impl.insert(fn)

  override def update[R](fn: (T) => R): TypedExecuteJoin[R] = impl.update(fn)

  override def delete[R](fn: (T) => R): TypedExecuteJoin[R] = impl.delete(fn)

  override def fields(fns: (T => Object)*): TypedExecuteJoin[T] = impl.fields(fns: _*)

  override def ignore(fns: (T => Object)*): TypedExecuteJoin[T] = impl.ignore(fns: _*)

  override def inserts[R](fn: T => Array[R]) = impl.inserts(fn)

  override def updates[R](fn: T => Array[R]) = impl.updates(fn)

  override def deletes[R](fn: T => Array[R]) = impl.deletes(fn)

  override def root(): T = obj
}

object ExecuteRootImpl {
  private def check(obj: Object): Unit = {
    if (!obj.isInstanceOf[Entity]) {
      throw new RuntimeException("Not Entity, Maybe Need Convert")
    }
  }

  def insert[T <: Object](obj: T): TypedExecuteRoot[T] = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new TypedInsertJoin[T](meta)
    new TypedExecuteRootImpl(obj, ex)
  }

  def update[T <: Object](obj: T): TypedExecuteRoot[T] = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new TypedUpdateJoin[T](meta)
    new TypedExecuteRootImpl(obj, ex)
  }

  def delete[T <: Object](obj: T): TypedExecuteRoot[T] = {
    check(obj)
    val meta = obj.asInstanceOf[Entity].$$core().meta
    val ex = new TypedDeleteJoin[T](meta)
    new TypedExecuteRootImpl(obj, ex)
  }
}
