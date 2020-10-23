package io.github.yuemenglong.orm.api.operate.sql.core

import io.github.yuemenglong.orm.api.operate.sql.field.Field

trait Expr extends SqlItem with ExprOps[Expr] {
  def as(alias: String): Field
}


trait ExprLike[S] {
  def toExpr: Expr

  def fromExpr(e: Expr): S
}

trait ExprOps[S]
  extends ExprLike[S]
    with ExprOpBool[S]
    with ExprOpMath[S]
    with ExprOpAssign[S]
    with ExprOpOrder[S]


trait ExprOpBool[S] extends ExprLike[S] {
  def eql(e: ExprLike[_]): S

  def eql[T](t: T): S

  def neq(e: ExprLike[_]): S

  def neq[T](t: T): S

  def gt(e: ExprLike[_]): S

  def gt[T](t: T): S

  def gte(e: ExprLike[_]): S

  def gte[T](t: T): S

  def lt(e: ExprLike[_]): S

  def lt[T](t: T): S

  def lte(e: ExprLike[_]): S

  def lte[T](t: T): S

  def between(l: ExprLike[_], r: ExprLike[_]): S

  def between[T](l: T, r: T): S

  def ===(e: ExprLike[_]): S

  def ===[T](t: T): S

  def !==(e: ExprLike[_]): S

  def !==[T](t: T): S

  def >(e: ExprLike[_]): S

  def >[T](t: T): S

  def >=(e: ExprLike[_]): S

  def >=[T](t: T): S

  def <(e: ExprLike[_]): S

  def <[T](t: T): S

  def <=(e: ExprLike[_]): S

  def <=[T](t: T): S

  def and(e: ExprLike[_]): S

  def or(e: ExprLike[_]): S

  def isNull: S

  def notNull: S

  def in(e: ExprLike[_]): S

  def in[T](arr: Array[T]): S

  def nin(e: Expr): S

  def nin[T](arr: Array[T]): S

  def like(s: String): S
}

trait ExprOpMath[S] extends ExprLike[S] {
  def add(e: ExprLike[_]): S

  def add[T](v: T): S

  def sub(e: ExprLike[_]): S

  def sub[T](v: T): S

  def +(e: ExprLike[_]): S

  def +[T](v: T): S

  def -(e: ExprLike[_]): S

  def -[T](v: T): S
}

trait ExprOpAssign[S] extends ExprLike[S] {
  def assign(e: ExprLike[_]): S

  def assign[T](v: T): S
}

trait ExprOpOrder[S] extends ExprLike[S] {
  def asc(): S

  def desc(): S
}

