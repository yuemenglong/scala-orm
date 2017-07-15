package orm.operate.traits.core

import java.sql.{Connection, ResultSet}


/**
  * Created by yml on 2017/7/14.
  */
trait Queryable {
  def query(conn: Connection): Array[Array[Object]]
}

trait AsSelectable {
  def as[T](clazz: Class[T]): Selectable[T]
}

trait Selectable[T] {
  def pick(rs: ResultSet): T

  def getColumnWithAs: String
}

trait SelectJoin extends Join {
  def select(field: String): SelectJoin
}

trait SelectRoot[T] extends Selectable[T] with Root with SelectJoin {

}

trait Select {
  def limit(l: Long)

  def offset(l: Long)

  def asc(field: Field)

  def desc(field: Field)
}

