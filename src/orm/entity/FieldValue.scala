package orm.entity

object FieldValueType {
  val VALUE: Int = 1
  val ENTITY: Int = 2
  val VEC: Int = 3
}

class FieldValue(val tp: Int, value: Object, entity: Option[Object], vec: Array[Object]) {

  def asValue(): Object = {
    require(this.tp == FieldValueType.VALUE)
    return this.value
  }

  def asEntity(): Option[Object] = {
    require(this.tp == FieldValueType.ENTITY)
    return this.entity
  }

  def asVec(): Array[Object] = {
    require(this.tp == FieldValueType.VEC)
    return this.vec
  }
}

object FieldValue {
  def newValus(value: Object): FieldValue = {
    new FieldValue(FieldValueType.VALUE, value, null, null)
  }

  def newEntity(entity: Option[Object]): FieldValue = {
    new FieldValue(FieldValueType.VALUE, null, entity, null)
  }

  def newVec(vec: Array[Object]): FieldValue = {
    new FieldValue(FieldValueType.VALUE, null, null, vec)
  }
}




