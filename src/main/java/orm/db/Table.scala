package orm.db

import orm.meta.{EntityMeta, OrmMeta}

/**
  * Created by <yuemenglong@126.com> on 2017/7/16.
  */
object Table {
  def getCreateSql(meta: EntityMeta): String = {
    val columns = meta.managedFieldVec().filter(field => field.isNormalOrPkey).map((field) => {
      field.getDbSql
    }).mkString(", ")
    val sql = s"CREATE TABLE IF NOT EXISTS `${meta.table}`($columns)"
    sql
  }

  def getCreateSql(clazz: Class[_]): String = {
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName)
    getCreateSql(entityMeta)
  }

  def getDropSql(clazz: Class[_]): String = {
    val entityMeta = OrmMeta.entityMap(clazz.getSimpleName)
    getDropSql(entityMeta)
  }

  def getDropSql(meta: EntityMeta): String = {
    val sql = s"DROP TABLE IF EXISTS `${meta.table}`"
    sql
  }
}

