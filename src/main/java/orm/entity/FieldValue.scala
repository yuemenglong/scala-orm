package orm.entity

import scala.collection.mutable.ArrayBuffer

object FieldValueType {
  val VALUE: Int = 1
  val ENTITY: Int = 2
  val VEC: Int = 3
}

class FieldValue(val tp: Int, value: Object, entity: Option[Object], vec: ArrayBuffer[Object]) {

  def asValue(): Object = {
    require(this.tp == FieldValueType.VALUE)
    this.value
  }

  def asEntity(): Option[Object] = {
    require(this.tp == FieldValueType.ENTITY)
    this.entity
  }

  def asVec(): ArrayBuffer[Object] = {
    require(this.tp == FieldValueType.VEC)
    this.vec
  }
}

object FieldValue {
  def newValus(value: Object): FieldValue = {
    new FieldValue(FieldValueType.VALUE, value, null, null)
  }

  def newEntity(entity: Option[Object]): FieldValue = {
    new FieldValue(FieldValueType.VALUE, null, entity, null)
  }

  def newVec(vec: ArrayBuffer[Object]): FieldValue = {
    new FieldValue(FieldValueType.VALUE, null, null, vec)
  }
}




