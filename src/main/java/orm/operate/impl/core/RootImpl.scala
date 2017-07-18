package orm.operate.impl.core

import java.sql.ResultSet

import orm.entity.{EntityCore, EntityManager}
import orm.kit.Kit
import orm.lang.interfaces.Entity
import orm.meta.{EntityMeta, FieldMeta}
import orm.operate.impl._
import orm.operate.traits.core.JoinType.JoinType
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

  override def as[T](clazz: Class[T]): Selectable[T] = new SelectableFieldImpl[T](clazz, this)

  override def eql(v: Object): Cond = EqFV(this, v)

  override def eql(f: Field): Cond = EqFF(this, f)

  override def neq(v: Object): Cond = NeFV(this, v)

  override def neq(f: Field): Cond = NeFF(this, f)

  override def gt(v: Object): Cond = GtFV(this, v)

  override def gt(f: Field): Cond = GtFF(this, f)

  override def gte(v: Object): Cond = GteFV(this, v)

  override def gte(f: Field): Cond = GteFF(this, f)

  override def lt(v: Object): Cond = LtFV(this, v)

  override def lt(f: Field): Cond = LteFF(this, f)

  override def lte(v: Object): Cond = LteFV(this, v)

  override def lte(f: Field): Cond = LteFF(this, f)

  override def in(a: Array[Object]): Cond = InFA(this, a)

  override def assign(f: Field): Assign = AssignFF(this, f)

  override def assign(v: Object): Assign = AssignFV(this, v)
}

class SelectableFieldImpl[T](clazz: Class[T], val impl: FieldImpl) extends Field with Selectable[T] {
  override def getColumn: String = impl.getColumn

  override def getAlias: String = impl.getAlias

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = resultSet.getObject(getAlias, clazz)

  override def getColumnWithAs: String = s"$getColumn AS $getAlias"

  override def getType: Class[T] = clazz

  override def getKey(value: Object): String = value.toString

  override def getParent: Node = impl.getParent

  override def assign(v: Object): Assign = impl.assign(v)

  override def assign(f: Field): Assign = impl.assign(f)

  override def eql(v: Object): Cond = impl.eql(v)

  override def eql(f: Field): Cond = impl.eql(f)

  override def neq(v: Object): Cond = impl.neq(v)

  override def neq(f: Field): Cond = impl.neq(f)

  override def gt(v: Object): Cond = impl.gt(v)

  override def gt(f: Field): Cond = impl.gt(f)

  override def gte(v: Object): Cond = impl.gte(v)

  override def gte(f: Field): Cond = impl.gte(f)

  override def lt(v: Object): Cond = impl.lt(v)

  override def lt(f: Field): Cond = impl.lt(f)

  override def lte(v: Object): Cond = impl.lte(v)

  override def lte(f: Field): Cond = impl.lte(f)

  override def in(a: Array[Object]): Cond = impl.in(a)

  override def as[T](clazz: Class[T]): Selectable[T] = ???
}

class JoinImpl(val field: String, val meta: EntityMeta, val parent: Join, val joinType: JoinType) extends Join {
  private var cond: Cond = new CondRoot
  private[impl] val joins = new ArrayBuffer[JoinImpl]()
  private val fields = new ArrayBuffer[FieldImpl]()

  override def getMeta: EntityMeta = meta

  override def join(field: String, joinType: JoinType): Join = {
    if (!meta.fieldMap.contains(field) || !meta.fieldMap(field).isObject) {
      throw new RuntimeException(s"Unknown Join Field $field")
    }
    joins.find(_.field == field) match {
      case Some(join) => join
      case None =>
        val refer = meta.fieldMap(field).refer
        val join = new JoinImpl(field, refer, this, joinType)
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
      val fieldMeta = parent.getMeta.fieldMap(field)
      val leftColumn = parent.getMeta.fieldMap(fieldMeta.left).column
      val rightColumn = meta.fieldMap(fieldMeta.right).column
      val leftTable = parent.getAlias
      val rightTable = getAlias
      val joinCondSql = cond.getSql match {
        case "" => ""
        case s => s" AND $s"
      }
      s"${joinType.toString} JOIN ${meta.table} AS $getAlias ON $leftTable.$leftColumn = $rightTable.$rightColumn$joinCondSql"
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

  override def join(field: String, joinType: JoinType): Join = impl.join(field, joinType)

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
        val j = impl.join(field).asInstanceOf[JoinImpl]
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

  protected def pickResult(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
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
      val b = select.pickResult(resultSet, filterMap)
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
          filterMap += (key -> b)
      }
    }
  }

  override def on(c: Cond): Join = impl.on(c)

  override def getParams: Array[Object] = impl.getParams

  override def getMeta: EntityMeta = impl.getMeta
}

class SelectableJoinImpl[T](val clazz: Class[T], impl: JoinImpl) extends SelectJoinImpl(impl) with Selectable[T] {
  if (clazz != impl.meta.clazz) {
    throw new RuntimeException("Class Not Match")
  }

  override def getColumnWithAs: String = {
    def go(select: SelectJoinImpl): Array[String] = {
      val selfColumn = select.fields.map(field => s"${field.getColumn} AS ${field.getAlias}")
      // 1. 属于自己的字段 2. 级联的部分
      selfColumn ++ select.selects.flatMap(go)
    }

    go(this).mkString(",\n")
  }

  override def getType: Class[T] = clazz

  override def getKey(value: Object): String = value.asInstanceOf[Entity].$$core().getPkey.toString

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = pickResult(resultSet, filterMap).asInstanceOf[T]
}

class RootImpl[T](clazz: Class[T], meta: EntityMeta) extends Root[T] {
  if (clazz != meta.clazz) {
    throw new RuntimeException("Class Not Match")
  }
  private[impl] val impl = new JoinImpl(null, meta, null, null)

  override def getFromExpr: String = {
    def go(join: JoinImpl): Array[String] = {
      Array(join.getTableWithJoinCond) ++ join.joins.flatMap(go)
    }

    go(impl).mkString("\n")
  }

  override def getAlias: String = impl.getAlias

  override def getTableWithJoinCond: String = {
    def go(join: JoinImpl): Array[String] = {
      val self = Array(join.getTableWithJoinCond)
      val joins = join.joins.flatMap(go).toArray[String]
      self ++ joins
    }

    go(impl).mkString("\n")
  }

  override def join(field: String, joinType: JoinType): Join = impl.join(field, joinType)

  override def get(field: String): Field = impl.get(field)

  override def getParent: Node = null

  override def getRoot: Node = impl

  override def asSelect(): SelectRoot[T] = new SelectRootImpl[T](clazz, meta, this)

  override def as[R](clazz: Class[R]): Selectable[R] = throw new RuntimeException("Use asSelect Instead When Call As On Root")

  override def on(c: Cond): Join = throw new RuntimeException("Root Not Need On Cond")

  override def getParams: Array[Object] = impl.getParams

  override def getMeta: EntityMeta = meta
}

class SelectRootImpl[T](clazz: Class[T], meta: EntityMeta,
                        rootImpl: RootImpl[T])
  extends Root[T] with SelectRoot[T] {

  private[impl] val selectImpl = new SelectableJoinImpl[T](clazz, rootImpl.impl)

  override def select(field: String): SelectJoin = selectImpl.select(field)

  override def getColumnWithAs: String = selectImpl.getColumnWithAs

  override def getType: Class[T] = selectImpl.getType

  override def getKey(value: Object): String = selectImpl.getKey(value)

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = selectImpl.pick(resultSet, filterMap)

  // A---Selectable---A   V---Root---V

  override def getFromExpr: String = rootImpl.getFromExpr

  override def asSelect(): SelectRoot[T] = throw new RuntimeException("Already Select Root")

  override def getAlias: String = rootImpl.getAlias

  override def getTableWithJoinCond: String = rootImpl.getTableWithJoinCond

  override def join(field: String, joinType: JoinType): Join = rootImpl.join(field, joinType)

  override def get(field: String): Field = rootImpl.get(field)

  override def on(c: Cond): Join = rootImpl.on(c)

  override def getParent: Node = rootImpl.getParent

  override def getRoot: Node = rootImpl.getRoot

  override def as[R](clazz: Class[R]): Selectable[R] = throw new RuntimeException("ALready Selectable")

  override def getParams: Array[Object] = rootImpl.getParams

  override def getMeta: EntityMeta = meta

  override def count(): Selectable[java.lang.Long] = new Count_(this)
}

