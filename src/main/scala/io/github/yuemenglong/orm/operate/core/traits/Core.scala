package io.github.yuemenglong.orm.operate.core.traits

import io.github.yuemenglong.orm.lang.types.Types.String
import io.github.yuemenglong.orm.operate.field.traits.Field
import io.github.yuemenglong.orm.operate.join.{CondHolder, JoinType}
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

//trait Join extends Params with Alias {
//
//  def getTableName: String
//
//  def getJoinName: String
//
//  def getParent: Join
//
//  def getJoins: Array[Join]
//
//  def getJoinType: JoinType
//
//  def getLeftColumn: String
//
//  def getRightColumn: String
//
//  def getCond: Cond
//
//  def getTable: String = {
//    getParent match {
//      case null => s"`${getTableName}` AS `${getAlias}`"
//      case _ => s"${getJoinType} JOIN `${getTableName}` AS `${getAlias}` ON ${getCond.getSql}"
//    }
//  }
//
//  def get(field: String): Field
//
//  def join(left: String, right: String, table: Join, joinType: JoinType): this.type
//
//  def join(left: String, right: String, table: Join): this.type = join(left, right, table, JoinType.INNER)
//
//  def leftJoin(left: String, right: String, table: Join): this.type = join(left, right, table, JoinType.LEFT)
//
//  def on(cond: Cond): this.type
//}
