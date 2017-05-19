package orm.execute

import java.sql.{Connection, Statement}
import orm.entity.EntityManager

class Execute {

}

object Execute {
  def insert(entity: Object, conn: Connection): Int = {
    val core = EntityManager.core(entity)
    val validFields = core.meta.fieldVec.filter(field => {
      field.isNormal() && core.fieldMap.contains(field.name)
    })
    val columns = validFields.map(field => {
      s"`${field.column}`"
    }).mkString(", ")
    val values = validFields.map(_ => {
      "?"
    }).mkString(", ")
    val sql = s"INSERT INTO ${core.meta.table}(${columns}) values(${values})"
    val stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
    validFields.zipWithIndex.foreach {
      case (field, i) => stmt.setObject(i + 1, core.get(field.name))
    }
    println(sql)
    val affected = stmt.executeUpdate()
    if (!core.meta.pkey.auto) {
      return affected
    }
    val rs = stmt.getGeneratedKeys()
    if (rs.next()) {
      var id = rs.getObject(1)
      core.fieldMap += (core.meta.pkey.name -> id)
    }
    affected
  }
}
