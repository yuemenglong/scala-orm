package orm.operate

import orm.meta.EntityMeta

import scala.collection.mutable.ArrayBuffer

/**
  * Created by Administrator on 2017/7/7.
  */

class SelectItem

case class EntityItem(field: String, selector: MultiSelector) extends SelectItem

class MultiSelector(meta: EntityMeta, alias: String, parent: MultiSelector = null) {
  private var count = ArrayBuffer[String]()

  def count(field: String = "*"): MultiSelector = {
    if (field != "*" && !meta.fieldMap.contains(field)) {
      throw new RuntimeException()
    }
    count += field
    this
  }
}

