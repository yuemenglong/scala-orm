package io.github.yuemenglong.orm.test

import io.github.yuemenglong.orm.api.db.Db
import io.github.yuemenglong.orm.impl.Orm
import io.github.yuemenglong.orm.test.entity.Obj

/**
 * Created by <yuemenglong@126.com> on 2018/3/14.
 */
object PerformanceTest {
  def openDb(): Db = Orm.open(Orm.mysql("localhost", 3306, "root", "root", "test"))

  def main(args: Array[String]): Unit = {
    Orm.init("io.github.yuemenglong.orm.test.entity")
    val db = openDb()
    db.rebuild()
    db.beginTransaction(session => {
      val obj = new Obj
      obj.name = "name"
      session.execute(Orm.insert(obj))

      val start = System.currentTimeMillis()
      val root = Orm.root(classOf[Obj])
      //    root.select(_.ptr)
      //    root.select(_.oo)
      //    root.selects(_.om).select(_.mo)
      val query = Orm.selectFrom(root).where(root.get(_.id).===(1).and(root.get(_.age) >= 10))
      val res = (1 to 100000).map(_ => {
        query.toString
        //        session.query(query)
      })
      val end = System.currentTimeMillis()
      println(end - start, res.length)
    })
  }
}
