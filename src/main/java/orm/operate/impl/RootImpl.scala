package orm.operate.impl

import java.sql.ResultSet

import orm.entity.{EntityCore, EntityManager}
import orm.kit.Kit
import orm.lang.interfaces.Entity
import orm.meta.{EntityMeta, FieldMeta}
import orm.operate.traits.core._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Created by <yuemenglong@126.com> on 2017/7/15.
  */
class FieldImpl(val field: String, val meta: FieldMeta, val parent: JoinImpl) extends Field {
  override def getColumn: String = s"${parent.getAlias}.${meta.column}"

  override def getAlias: String = s"${parent.getAlias}$$${Kit.lodashCase(meta.name)}"

  override def getParent: Node = parent

  override def eql(value: Object): Cond = ???

  override def assign(value: Object): Assign = ???

  override def as[T](clazz: Class[T]): Selectable[T] = ???
}

class JoinImpl(val field: String, val meta: EntityMeta, val parent: JoinImpl) extends Join {
  private var cond: Cond = new CondRoot
  private[impl] val joins = new ArrayBuffer[JoinImpl]()
  private val fields = new ArrayBuffer[FieldImpl]()

  override def join(field: String): Join = {
    if (!meta.fieldMap.contains(field) || !meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Unknown Join Field $field")
    }
    joins.find(_.field == field) match {
      case Some(join) => join
      case None =>
        val refer = meta.fieldMap(field).refer
        val join = new JoinImpl(field, refer, this)
        joins += join
        join
    }
  }

  override def get(field: String): Field = {
    if (!meta.fieldMap.contains(field) || meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Unknown Join Field $field")
    }
    fields.find(_.field == field) match {
      case Some(f) => f
      case None =>
        val fieldMeta = meta.fieldMap(field)
        val f = new FieldImpl(field, fieldMeta, this)
        fields += f
        f
    }
  }

  override def getParent: Node = parent

  override def as[T](clazz: Class[T]): Selectable[T] = new SelectableJoinImpl[T](clazz, this)

  override def getAlias: String = {
    if (parent == null) {
      Kit.lowerCaseFirst(meta.entity)
    } else {
      s"${parent.getAlias}_${Kit.lowerCaseFirst(field)}"
    }
  }

  override def getTableWithJoinCond: String = {
    if (parent == null) {
      s"${meta.table} AS $getAlias"
    } else {
      val fieldMeta = parent.meta.fieldMap(field)
      val leftColumn = parent.meta.fieldMap(fieldMeta.left).column
      val rightColumn = meta.fieldMap(fieldMeta.right).column
      val leftTable = parent.meta.table
      val rightTable = meta.table
      s"LEFT JOIN $getAlias ON $leftTable.$leftColumn = $rightTable.$rightColumn"
    }
  }

  override def getParams: Array[Object] = cond.getParams ++ joins.flatMap(_.getParams).toArray[Object]

  override def on(c: Cond): Join = {
    cond = c
    this
  }
}

class SelectJoinImpl(val impl: JoinImpl) extends SelectJoin {
  protected[impl] var selects = new ArrayBuffer[SelectJoinImpl]()
  protected[impl] var fields: Array[FieldImpl] = impl.meta.managedFieldVec().filter(_.isNormalOrPkey).map(f => new FieldImpl(f.name, f, impl)).toArray

  def setSelectFields(arr: Array[String]): Unit = {
    fields = arr.map(impl.meta.fieldMap(_)).map(f => new FieldImpl(f.name, f, impl))
  }

  override def getAlias: String = impl.getAlias

  override def getTableWithJoinCond: String = impl.getTableWithJoinCond

  override def join(field: String): Join = impl.join(field)

  override def get(field: String): Field = impl.get(field)

  override def getParent: Node = impl.getParent

  override def as[R](clazz: Class[R]): Selectable[R] = throw new RuntimeException("Already Selectable")

  override def select(field: String): SelectJoin = {
    if (!impl.meta.fieldMap.contains(field) || !impl.meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Unknown Object Field $field")
    }
    selects.find(_.impl.field == field) match {
      case Some(s) => s
      case None =>
        val refer = impl.meta.fieldMap(field).refer
        val j = new JoinImpl(field, refer, impl)
        val s = new SelectJoinImpl(j)
        selects += s
        s
    }
  }

  protected def getFilterKey(core: EntityCore): String = {
    s"$getAlias@${core.getPkey.toString}"
  }

  protected def getOneManyFilterKey(field: String, core: EntityCore): String = {
    s"$getAlias@$field@${core.getPkey.toString}"
  }

  protected def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val a = pickSelf(resultSet, filterMap)
    if (a == null) {
      return null
    }
    pickRefer(a, resultSet, filterMap)
    a
  }

  protected def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val map: Map[String, Object] = fields.map(field => {
      val alias = field.getAlias
      val value = resultSet.getObject(alias)
      (field.meta.name, value)
    })(collection.breakOut)
    val core = new EntityCore(impl.meta, map)
    if (core.getPkey == null) {
      return null
    }
    val key = getFilterKey(core)
    if (filterMap.contains(key)) {
      return filterMap(key)
    }
    val a = EntityManager.wrap(core)
    filterMap += (key -> a)
    a
  }

  protected def pickRefer(a: Object, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]) {
    val aCore = EntityManager.core(a)
    selects.foreach { select =>
      val field = select.impl.field
      val fieldMeta = impl.meta.fieldMap(field)
      val b = select.pick(resultSet, filterMap)
      (b, fieldMeta.isOneMany) match {
        case (null, false) => aCore.fieldMap += (field -> null)
        case (null, true) => aCore.fieldMap += (field -> new ArrayBuffer[Entity]())
        case (_, false) => aCore.fieldMap += (field -> b)
        case (_, true) =>
          val key = getOneManyFilterKey(field, b.$$core())
          if (filterMap.contains(key)) {
            // 该对象已经被加入过一对多数组了
          } else if (!aCore.fieldMap.contains(field)) {
            aCore.fieldMap += (field -> new ArrayBuffer[Entity]())
            aCore.fieldMap(field).asInstanceOf[ArrayBuffer[Entity]] += b
          } else {
            aCore.fieldMap(field).asInstanceOf[ArrayBuffer[Entity]] += b
          }
      }
    }
  }

  override def on(c: Cond): Join = impl.on(c)

  override def getParams: Array[Object] = impl.getParams
}

class SelectableJoinImpl[T](val clazz: Class[T], impl: JoinImpl) extends SelectJoinImpl(impl) with Selectable[T] {
  if (clazz != impl.meta.clazz) {
    throw new RuntimeException("Class Not Match")
  }

  override def pick(rs: ResultSet): T = {
    val filterMap = mutable.Map[String, Entity]()
    pick(rs, filterMap).asInstanceOf[T]
  }

  override def getColumnWithAs: String = {
    def go(select: SelectJoinImpl): String = {
      val selfColumn = select.fields.map(field => s"${field.getColumn} AS ${field.getAlias}")
      // 1. 属于自己的字段 2. 级联的部分
      (selfColumn ++ select.selects.flatMap(go)).mkString(",\n")
    }

    go(this)
  }

  override def getType: Class[T] = clazz

  override def getKey(value: Object): String = value.asInstanceOf[Entity].$$core().getPkey.toString
}

class RootImpl[T](clazz: Class[T], meta: EntityMeta) extends Root[T] {
  if (clazz != meta.clazz) {
    throw new RuntimeException("Class Not Match")
  }
  protected val impl = new JoinImpl(null, meta, null)

  override def getFromExpr: String = {
    def go(join: JoinImpl): Array[String] = {
      Array(join.getTableWithJoinCond) ++ join.joins.flatMap(go)
    }

    go(impl).mkString("\n")
  }

  override def getAlias: String = impl.getAlias

  override def getTableWithJoinCond: String = impl.getTableWithJoinCond

  override def join(field: String): Join = impl.join(field)

  override def get(field: String): Field = impl.get(field)

  override def getParent: Node = null

  override def asSelect(): SelectRoot[T] = {
    val selectableJoinImpl = new SelectableJoinImpl[T](clazz, impl)
    new SelectRootImpl[T](clazz, meta, selectableJoinImpl)
  }

  override def as[R](clazz: Class[R]): Selectable[R] = throw new RuntimeException("Use asSelect Instead When Call As On Root")

  override def on(c: Cond): Join = throw new RuntimeException("Root Not Need On Cond")

  override def getParams: Array[Object] = impl.getParams
}

class SelectRootImpl[T](clazz: Class[T], meta: EntityMeta, impl: SelectableJoinImpl[T])
  extends RootImpl[T](clazz, meta) with SelectRoot[T] {

  override def select(field: String): SelectJoin = impl.select(field)

  override def pick(rs: ResultSet): T = impl.pick(rs)

  override def getColumnWithAs: String = impl.getColumnWithAs

  override def getType: Class[T] = impl.getType

  override def getKey(value: Object): String = impl.getKey(value)
}

