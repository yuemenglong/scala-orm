package io.github.yuemenglong.orm.api.operate.sql.core

//noinspection ScalaRedundantCast
trait SelectStatement[S] extends SelectStmt with ExprLike[S] {

  def distinct(): S

  def select(cs: Array[ResultColumn]): S

  def from(ts: TableLike*): S

  def where(expr: ExprLike[_]): S

  def groupBy(es: ExprLike[_]*): S

  def having(e: ExprLike[_]): S

  def orderBy(e: ExprLike[_]*): S

  def limit(l: Integer): S

  def offset(o: Integer): S

  def union(stmt: SelectStatement[_]): S

  def unionAll(stmt: SelectStatement[_]): S
}

trait UpdateStatement extends SqlItem {
  val _table: TableLike
}

trait DeleteStatement extends SqlItem {
  val _targets: Array[TableLike]
}