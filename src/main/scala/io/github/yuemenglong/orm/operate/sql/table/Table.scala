package io.github.yuemenglong.orm.operate.sql.table

import java.sql.ResultSet

import io.github.yuemenglong.orm.api.operate.sql.core.ResultColumn
import io.github.yuemenglong.orm.impl.kit.Kit
import io.github.yuemenglong.orm.impl.entity.{Entity, EntityCore, EntityManager}
import io.github.yuemenglong.orm.impl.meta._
import io.github.yuemenglong.orm.operate.sql.table.JoinType.JoinType
import io.github.yuemenglong.orm.operate.query.Selectable
import io.github.yuemenglong.orm.operate.sql.core._
import io.github.yuemenglong.orm.operate.sql.field.{FieldExpr, FieldExprImpl, SelectableFieldExpr, SelectableFieldExprImpl}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object JoinType extends Enumeration {
  type JoinType = Value
  val INNER, LEFT, RIGHT, OUTER = Value
}

trait Table extends TableLike {

  def getMeta: EntityMeta

  def get(field: String): FieldExpr

  def join(field: String, joinType: JoinType): Table

  def join(field: String): Table = join(field, JoinType.INNER)

  def leftJoin(field: String): Table = join(field, JoinType.LEFT)

  def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): TypedResultTable[T]

  def joinAs[T](left: String, right: String, clazz: Class[T]): TypedResultTable[T] = this.joinAs(left, right, clazz, JoinType.INNER)

  def leftJoinAs[T](left: String, right: String, clazz: Class[T]): TypedResultTable[T] = this.joinAs(left, right, clazz, JoinType.LEFT)

  def as[T](clazz: Class[T]): TypedResultTableImpl[T]
}

trait TableImpl extends Table with TableLikeImpl {
  val meta: EntityMeta
  val joins: mutable.Map[String, (JoinType, TableImpl)] = mutable.Map()

  def getMeta: EntityMeta = meta

  def get(field: String): FieldExpr = {
    if (!getMeta.fieldMap.contains(field) || getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    val alias = s"${getAlias}$$${field}"
    val column = getColumn(getMeta.fieldMap(field).column, alias)
    new FieldExprImpl {
      override private[orm] val uid = column.uid
      override private[orm] val expr = column.expr
    }
  }

  def join(field: String, joinType: JoinType): TableImpl = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Field $field On ${getMeta.entity}")
    }
    if (getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer].refer.db != getMeta.db) {
      throw new RuntimeException(s"Field $field Not the Same DB")
    }
    joins.get(field) match {
      case Some((t, c)) =>
        if (t != joinType) throw new RuntimeException(s"JoinType Not Match, [${t}:${joinType}]")
        c
      case None =>
        val referMeta = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
        val tableName = referMeta.refer.table
        val alias = s"${getAlias}_${Kit.lowerCaseFirst(field)}"
        val leftColumn = getMeta.fieldMap(referMeta.left).column
        val rightColumn = referMeta.refer.fieldMap(referMeta.right).column
        val table = join(TableLikeUtil.create(tableName, alias), joinType.toString, leftColumn, rightColumn)
        val ret = new TableImpl {
          override val meta = referMeta.refer
          override private[orm] val _table = table._table
          override private[orm] val _joins = table._joins
          override private[orm] val _on = table.asInstanceOf[TableLikeImpl]._on
        }
        joins += (field -> (joinType, ret))
        ret
    }
  }

  def joinAs[T](left: String, right: String, clazz: Class[T], joinType: JoinType): TypedResultTable[T] = {
    if (!OrmMeta.entityMap.contains(clazz)) {
      throw new RuntimeException(s"$clazz Is Not Entity")
    }
    val referMeta = OrmMeta.entityMap(clazz)
    if (!getMeta.fieldMap.contains(left)) {
      throw new RuntimeException(s"Unknown Field $left On ${getMeta.entity}")
    }
    if (!referMeta.fieldMap.contains(right)) {
      throw new RuntimeException(s"Unknown Field $right On ${referMeta.entity}")
    }
    if (getMeta.db != referMeta.db) {
      throw new RuntimeException(s"$left And $right Not The Same DB")
    }
    val joinName = Kit.lowerCaseFirst(referMeta.entity)
    val tableName = referMeta.table
    val leftColumn = getMeta.fieldMap(left).column
    val rightColumn = referMeta.fieldMap(right).column
    val alias = s"${getAlias}__${joinName}"
    val table = join(TableLikeUtil.create(tableName, alias), joinType.toString, leftColumn, rightColumn)
    new TypedResultTableImpl[T] {
      override val meta = referMeta
      override private[orm] val _table = table._table
      override private[orm] val _joins = table._joins
      override private[orm] val _on = table.asInstanceOf[TableLikeImpl]._on
    }
  }

  def as[T](clazz: Class[T]): TypedResultTableImpl[T] = {
    if (!OrmMeta.entityMap.contains(clazz)) {
      throw new RuntimeException(s"$clazz Is Not Entity")
    }
    val that = this
    new TypedResultTableImpl[T] {
      override val meta = that.meta
      override private[orm] val _table = that._table
      override private[orm] val _joins = that._joins
      override private[orm] val _on = that._on
    }
  }
}

trait ResultTable extends Table {
  def select(field: String): ResultTable

  def fields(fields: String*): ResultTable

  def ignore(fields: String*): ResultTable

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity
}

trait ResultTableImpl extends ResultTable with TableImpl {
  private[orm] val _selects = new ArrayBuffer[(String, ResultTableImpl)]()
  private[orm] val _fields = ArrayBuffer[String]()
  private[orm] val _ignores = mutable.Set[String]()

  def select(field: String): ResultTableImpl = {
    if (!getMeta.fieldMap.contains(field) || !getMeta.fieldMap(field).isRefer) {
      throw new RuntimeException(s"Unknown Object Field $field")
    }
    _selects.find(_._1 == field) match {
      case Some(p) => p._2
      case None =>
        val j = leftJoin(field)
        val ret = new ResultTableImpl {
          override val meta = j.getMeta
          override private[orm] val _table = j._table
          override private[orm] val _joins = j._joins
          override private[orm] val _on = j.asInstanceOf[TableLikeImpl]._on
        }
        _selects += ((field, ret))
        ret
    }
  }

  def fields(fields: String*): ResultTable = {
    _fields.clear()
    _fields += this.getMeta.pkey.name
    fields.foreach(f => {
      if (!this.getMeta.fieldMap.contains(f)) {
        throw new RuntimeException(s"Invalid Field $f In ${this.getMeta.entity}")
      }
      if (!this.getMeta.fieldMap(f).isNormal) {
        throw new RuntimeException(s"Not Normal Field $f In ${this.getMeta.entity}")
      }
      _fields += f
    })
    this
  }

  def ignore(fields: String*): ResultTable = {
    _ignores.clear()
    fields.foreach(f => {
      if (!getMeta.fieldMap.contains(f)) {
        throw new RuntimeException(s"Not Exists Field, $f")
      }
      val fieldMeta = getMeta.fieldMap(f)
      if (!fieldMeta.isNormal) {
        throw new RuntimeException(s"Only Normal Field Can Ignore, $f")
      }
      _ignores += f
    })
    this
  }

  private[orm] def validFields(): Array[String] = {
    val fs: Array[String] = _fields.isEmpty match {
      case true => getMeta.fieldVec.filter(_.isNormalOrPkey).map(_.name).toArray
      case false => _fields.toArray
    }
    fs.filter(!_ignores.contains(_))
  }

  private[orm] def getFilterKey(core: EntityCore): String = {
    s"$getAlias@${core.getPkey.toString}"
  }

  def pickSelf(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val map: mutable.Map[String, Object] = validFields().map(f => {
      val field = get(f)
      val fieldMeta = getMeta.fieldMap(f)
      val alias = field.getAlias
      // val value = resultSet.getObject(alias)
      // 适配sqlite
      val value = Kit.getObjectFromResultSet(resultSet, alias, fieldMeta.clazz).asInstanceOf[Object]
      (f, value)
    })(collection.breakOut)
    val core = new EntityCore(getMeta, map)
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
}

trait TypedTable[T] extends Table {

  def apply[R](fn: T => R): SelectableFieldExpr[R] = get(fn)

  def get[R](fn: (T => R)): SelectableFieldExpr[R]

  def join[R](fn: (T => R), joinType: JoinType): TypedTable[R]

  def join[R](fn: (T => R)): TypedTable[R] = join(fn, JoinType.INNER)

  def leftJoin[R](fn: (T => R)): TypedTable[R] = join(fn, JoinType.LEFT)

  def joinAs[R](fn: (T => R), joinType: JoinType): TypedResultTable[R]

  def joinAs[R](fn: (T => R)): TypedResultTable[R] = joinAs(fn, JoinType.INNER)

  def leftJoinAs[R](fn: (T => R)): TypedResultTable[R] = joinAs(fn, JoinType.LEFT)

  def joinsAs[R](fn: (T => Array[R]), joinType: JoinType): TypedResultTable[R]

  def joinsAs[R](fn: (T => Array[R])): TypedResultTable[R] = joinsAs(fn, JoinType.INNER)

  def leftJoinsAs[R](fn: (T => Array[R])): TypedTable[R] = joinsAs(fn, JoinType.LEFT)

  def joins[R](fn: (T => Array[R]), joinType: JoinType): TypedTable[R]

  def joins[R](fn: (T => Array[R])): TypedTable[R] = joins(fn, JoinType.INNER)

  def joinArray[R](fn: (T => Array[R])): TypedTable[R] = joins(fn)

  def leftJoins[R](fn: (T => Array[R])): TypedTable[R] = joins(fn, JoinType.LEFT)

  def leftJoinArray[R](fn: (T => Array[R])): TypedTable[R] = leftJoins(fn)

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedTable[R]

  def joinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedTable[R] = this.joinAs(clazz, JoinType.INNER)(leftFn, rightFn)

  def leftJoinAs[R](clazz: Class[R])(leftFn: T => Object, rightFn: R => Object): TypedTable[R] = this.joinAs(clazz, JoinType.LEFT)(leftFn, rightFn)
}

trait TypedTableImpl[T] extends TypedTable[T] with TableImpl {
  private def typedCascade[R](field: String, joinType: JoinType): TypedTable[R] = {
    val j: Table = this.join(field, joinType)
    new TypedTableImpl[R] {
      override val meta = j.getMeta
      override private[orm] val _table = j._table
      override private[orm] val _joins = j._joins
      override private[orm] val _on = j.asInstanceOf[TableLikeImpl]._on
    }
  }

  def get[R](fn: (T => R)): SelectableFieldExpr[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = get(marker.toString)
    new SelectableFieldExprImpl[R] {
      override val clazz = getMeta.fieldMap(marker.toString).clazz.asInstanceOf[Class[R]]
      override private[orm] val expr = field.expr
      override private[orm] val uid = field.uid
    }
  }

  def join[R](fn: (T => R), joinType: JoinType): TypedTable[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedCascade(field, joinType)
  }

  def joinAs[R](fn: (T => R), joinType: JoinType): TypedResultTable[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    val referMeta = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
    val left = referMeta.left
    val right = referMeta.right
    joinAs(left, right, referMeta.refer.clazz.asInstanceOf[Class[R]], joinType)
  }

  def joinsAs[R](fn: (T => Array[R]), joinType: JoinType): TypedResultTable[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    val referMeta = getMeta.fieldMap(field).asInstanceOf[FieldMetaRefer]
    val left = referMeta.left
    val right = referMeta.right
    joinAs(left, right, referMeta.refer.clazz.asInstanceOf[Class[R]], joinType)
  }

  def joins[R](fn: (T => Array[R]), joinType: JoinType): TypedTable[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedCascade(field, joinType)
  }

  def joinAs[R](clazz: Class[R], joinType: JoinType)(leftFn: T => Object, rightFn: R => Object): TypedTable[R] = {
    if (!OrmMeta.entityMap.contains(clazz)) {
      throw new RuntimeException(s"$clazz Is Not Entity")
    }
    val referMeta = OrmMeta.entityMap(clazz)
    val lm = EntityManager.createMarker[T](getMeta)
    val rm = EntityManager.createMarker[R](referMeta)
    leftFn(lm)
    rightFn(rm)
    val left = lm.toString
    val right = rm.toString
    val j = this.joinAs(left, right, clazz)
    new TypedResultTableImpl[R] {
      override val meta = j.getMeta
      override private[orm] val _table = j._table
      override private[orm] val _joins = j._joins
      override private[orm] val _on = j.asInstanceOf[TableLikeImpl]._on
    }
  }
}

trait TypedResultTable[T] extends TypedTable[T]
  with ResultTable with Selectable[T] {

  def select[R](fn: T => R): TypedResultTable[R]

  def selects[R](fn: T => Array[R]): TypedResultTable[R]

  def selectArray[R](fn: T => Array[R]): TypedResultTable[R] = selects(fn)

  def fields(fns: (T => Object)*): TypedResultTable[T]

  def ignore(fns: (T => Object)*): TypedResultTable[T]
}

trait TypedResultTableImpl[T] extends TypedResultTable[T]
  with TypedTableImpl[T] with ResultTableImpl {

  private def typedSelect[R](field: String): TypedResultTable[R] = {
    val j = this.select(field)
    val ret = new TypedResultTableImpl[R] {
      override val meta = j.meta
      override private[orm] val _table = j._table
      override private[orm] val _joins = j._joins
      override private[orm] val _selects = j._selects
      override private[orm] val _fields = j._fields
      override private[orm] val _ignores = j._ignores
      override private[orm] val _on = j._on
    }
    ret
  }

  override def getColumns: Array[ResultColumn] = {
    val ab = new ArrayBuffer[ResultColumn]()

    def go(cascade: ResultTableImpl): Unit = {
      val self = cascade.validFields().map(cascade.get)
      ab ++= self
      cascade._selects.foreach(s => go(s._2))
    }

    go(this)
    ab.toArray
  }

  def select[R](fn: T => R): TypedResultTable[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedSelect[R](field)
  }

  def selects[R](fn: T => Array[R]): TypedResultTable[R] = {
    val marker = EntityManager.createMarker[T](getMeta)
    fn(marker)
    val field = marker.toString
    typedSelect[R](field)
  }

  //  def selectArray[R](fn: T => Array[R]): TypedResultTable[R] = selects(fn)

  def fields(fns: (T => Object)*): TypedResultTable[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    this.fields(fields: _*)
    this
  }

  def ignore(fns: (T => Object)*): TypedResultTable[T] = {
    val fields = fns.map(fn => {
      val marker = EntityManager.createMarker[T](getMeta)
      fn(marker)
      marker.toString
    })
    this.ignore(fields: _*)
    this
  }

  private def pickSelfAndRefer(select: ResultTableImpl, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): Entity = {
    val a = select.pickSelf(resultSet, filterMap)
    if (a == null) {
      return null
    }
    pickRefer(select, a, resultSet, filterMap)
    a
  }

  private def getOneManyFilterKey(field: String, core: EntityCore): String = {
    s"$getAlias@$field@${core.getPkey.toString}"
  }

  private def pickRefer(selfSelect: ResultTableImpl, a: Object, resultSet: ResultSet, filterMap: mutable.Map[String, Entity]) {
    val aCore = EntityManager.core(a)
    selfSelect._selects.foreach { case (field, select) =>
      val fieldMeta = selfSelect.meta.fieldMap(field)
      val b = pickSelfAndRefer(select, resultSet, filterMap)
      fieldMeta match {
        case _: FieldMetaPointer => aCore.fieldMap += (field -> b)
        case _: FieldMetaOneOne => aCore.fieldMap += (field -> b)
        case f: FieldMetaOneMany =>
          val hasArr = aCore.fieldMap.contains(field)
          if (!hasArr) {
            aCore.fieldMap += (field -> Kit.newArray(f.refer.clazz))
          }
          if (b != null) {
            val key = getOneManyFilterKey(field, b.$$core())
            if (!filterMap.contains(key)) {
              // 该对象还未被加入过一对多数组
              val arr = aCore.fieldMap(field).asInstanceOf[Array[_]].map(_.asInstanceOf[Entity])
              val brr = Kit.newArray(f.refer.clazz, arr ++ Array(b): _*)
              aCore.fieldMap += (field -> brr)
            }
            filterMap += (key -> b)
          }
      }
    }
  }

  override def pick(resultSet: ResultSet, filterMap: mutable.Map[String, Entity]): T = {
    pickSelfAndRefer(this, resultSet, filterMap).asInstanceOf[T]
  }

  override def getKey(value: Object): String = {
    value match {
      case null => ""
      case _ => value.asInstanceOf[Entity].$$core().getPkey.toString
    }
  }

  override def getType: Class[T] = getMeta.clazz.asInstanceOf[Class[T]]
}

trait SubQuery extends TableLike {
  def get(alias: String): FieldExpr

  def join(t: TableLike, joinType: JoinType): TableLike

  def join(t: TableLike): TableLike = join(t, JoinType.INNER)
}

trait SubQueryImpl extends SubQuery with TableLikeImpl {
  def get(alias: String): FieldExpr = {
    val that = this
    new FieldExprImpl {
      override private[orm] val uid = alias
      override private[orm] val expr = ExprUtil.column(that.getAlias, alias)
    }
  }

  def join(t: TableLike, joinType: JoinType): TableLike = super.join(t, joinType.toString)
}
