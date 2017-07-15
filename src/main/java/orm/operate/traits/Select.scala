package orm.operate.traits

import java.sql.{Connection, ResultSet}

import orm.operate.traits.core.{Field, Join}


/**
  * Created by yml on 2017/7/14.
  */
trait AsSelectable {
  def as[T](clazz: Class[T]): Selectable[T]
}

trait Selectable[T] {
  def pick(rs: ResultSet): T

  def getColumnWithAs: String
}

trait SelectView[T] extends Selectable[T] with Join {
  def getFromExpr: String
}

trait Select {
  def limit(l: Long)

  def offset(l: Long)

  def asc(field: Field)

  def desc(field: Field)
}

