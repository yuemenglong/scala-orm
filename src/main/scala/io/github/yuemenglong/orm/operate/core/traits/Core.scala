package io.github.yuemenglong.orm.operate.core.traits

import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.{CondHolder, JoinCond, JoinType}
import io.github.yuemenglong.orm.operate.join.JoinType.JoinType
import io.github.yuemenglong.orm.operate.join.traits.Cond

/**
  * Created by <yuemenglong@126.com> on 2018/3/15.
  */
trait Params {
  def getParams: Array[Object]
}

trait Expr extends Params {
  def getSql: String
}

trait Alias {
  def getAlias: String
}

trait Join extends Params with Alias {
  var joins: List[Join] = List()
  var cond: Cond = new CondHolder

  def getTableString: String // 自己的table字符串(可以是子查询)

  def getJoinName: String // 用于命名AS后的表名

  def getParent: Join

  def getJoinType: JoinType

  def getLeftColumn: String

  def getRightColumn: String

  def getJoins: List[Join] = joins

  def getCond: Cond = cond

  override def getAlias: String = s"${getParent.getAlias}_${getJoinName}"

  def getTables: List[String] = {
    val self = getParent match {
      case null => s"`${getTableString}` AS `${getAlias}`"
      case _ => s"${getJoinType} JOIN `${getTableString}` AS `${getAlias}` ON ${getCond.getSql}"
    }
    self :: getJoins.flatMap(_.getTables)
  }

  def getTable: String = getTables.mkString("\n")

  def join(leftColumn: String, rightColumn: String, other: Join, joinName: String, joinType: JoinType): Join = {
    val that = this
    val newJoin = new Join {
      cond = new JoinCond(that.getAlias, leftColumn, other.getAlias, rightColumn)

      override def getJoinName = joinName

      override def getTableString = s"(${that.getTable} ${getJoinName} JOIN )"

      override def getLeftColumn = leftColumn

      override def getRightColumn = rightColumn

      override def getParent = that

      override def getJoinType = joinType

      override def getParams = cond.getParams ++ joins.flatMap(_.getParams).toArray[Object]
    }
    joins ::= newJoin
    newJoin
  }

  def join(left: String, right: String, table: Join, joinName: String): Join = join(left, right, table, joinName, JoinType.INNER)

  def leftJoin(left: String, right: String, table: Join, joinName: String): Join = join(left, right, table, joinName, JoinType.LEFT)

  def on(c: Cond): Join = {
    cond = cond.and(c)
    this
  }
}
