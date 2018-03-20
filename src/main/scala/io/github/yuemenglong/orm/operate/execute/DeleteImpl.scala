//package io.github.yuemenglong.orm.operate.execute
//
//import io.github.yuemenglong.orm.Session.Session
//import io.github.yuemenglong.orm.operate.execute.traits.ExecutableDelete
//import io.github.yuemenglong.orm.operate.join.CondHolder
//import io.github.yuemenglong.orm.operate.join.traits.{Cond, Cascade, Root}
//
///**
//  * Created by <yuemenglong@126.com> on 2017/7/16.
//  */
//
//class DeleteImpl(deletes: Cascade*) extends ExecutableDelete {
//  var cond: Cond = new CondHolder
//  var root: Root[_] = _
//
//  override def from(root: Root[_]): ExecutableDelete = {
//    this.root = root
//    this
//  }
//
//  override def where(cond: Cond): ExecutableDelete = {
//    this.cond = cond
//    this
//  }
//
//  override def execute(session: Session): Int = {
//    if (root == null) {
//      throw new RuntimeException("Must Spec A Delete Root")
//    }
//    val condSql = cond.getSql match {
//      case "" => "1 = 1"
//      case s => s
//    }
//    val targets = deletes.map(j => s"`${j.getAlias}`").mkString(", ")
//    val sql = s"DELETE $targets FROM\n${root.getTableSql}\nWHERE $condSql"
//    val params = root.getParams ++ cond.getParams
//    session.execute(sql, params)
//  }
//}
