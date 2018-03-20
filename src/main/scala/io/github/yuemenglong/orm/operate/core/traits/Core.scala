package io.github.yuemenglong.orm.operate.core.traits

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.meta.EntityMeta
import io.github.yuemenglong.orm.operate.field.traits.Field

import scala.collection.mutable.ArrayBuffer
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.join.traits.Cond

/**
  * Created by <yuemenglong@126.com> on 2018/3/15.
  */
trait Params {
  def getParams: Array[Object]
}

trait Alias {
  def getAlias: String
}

trait Expr2 extends Params {
  def getSql: String
}

trait GetField {
  def get(field: String): Field
}

class JoinInner(val tableName: String, val joinName: String,
                val joinType: JoinType, val parent: Join,
                val leftColumn: String, val rightColumn: String) {
  def this(meta: EntityMeta) = {
    this(meta.table, Kit.lowerCaseFirst(meta.table), null, null, null, null)
    this.meta = meta
  }

  // Join
  var joins: List[Join] = List()
  var cond: Cond = _

  // Cascade
  var meta: EntityMeta = _

  //  // SelectFieldCascade
  //  var selects: ArrayBuffer[(String, SelectFieldCascade)] = _
  //  var fields: Array[FieldImpl] = _
  //  var ignores: Set[String] = _
}

trait Join extends Alias with Params { // 代表所属的Table
  private[orm] val inner: JoinInner

  def getTableName: String = inner.tableName

  def getParent: Join = inner.parent

  def getLeftColumn: String = inner.leftColumn

  def getRightColumn: String = inner.rightColumn

  def getJoinType: JoinType = inner.joinType

  def getJoinName: String = inner.joinName

  override def getAlias = getParent match {
    case null => s"${getJoinName}"
    case _ => s"${getParent.getAlias}_${getJoinName}"
  }

  override def getParams = {
    val self = inner.cond match {
      case null => Array()
      case c => c.getParams
    }
    self ++ inner.joins.flatMap(_.getParams)
  }

  def getTableSql: String = {
    val self = getParent match {
      case null => s"${getTableName} AS ${getAlias}"
      case _ =>
        val joinCond = s"${getParent.getAlias}.${getLeftColumn} = ${getAlias}.${getRightColumn}"
        val condSql = inner.cond match {
          case null => joinCond
          case _ => s"${joinCond} AND ${inner.cond.getSql}"
        }
        s"${getJoinType} JOIN ${getTableName} AS ${getAlias} ON ${condSql}"
    }
    (Array(self) ++ inner.joins.map(_.getTableSql)).mkString("\n")
  }

  def join(leftColumn: String, rightColumn: String, table: String, joinName: String, joinType: JoinType): Join = {
    inner.joins.find(_.getJoinName == joinName) match {
      case Some(j) => j
      case None =>
        val that = this
        val newJoin = new Join {
          override val inner = new JoinInner(table, joinName, joinType, that, leftColumn, rightColumn)
        }
        inner.joins ::= newJoin
        newJoin
    }
  }

  def on(c: Cond): Join = {
    inner.cond match {
      case null => inner.cond = c
      case cond => inner.cond = cond.and(c)
    }
    this
  }
}
