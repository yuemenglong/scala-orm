package io.github.yuemenglong.orm.test.entity

import io.github.yuemenglong.orm.api.anno.{Column, Entity, Enum, ExportTS, Id, Ignore, Index, LongText, OneToMany, OneToOne, Pointer, SmallInt, Text, TinyInt}
import io.github.yuemenglong.orm.api.types.Types._

/**
 * Created by <yuemenglong@126.com> on 2018/1/31.
 */
@Entity(db = "orm_test")
class Obj {
  @Id(auto = true)
  var id: Long = _

  @Column(name = "age_")
  @ExportTS(value = "undefined")
  var age: Integer = _

  @TinyInt
  var tinyIntValue: Integer = _

  @SmallInt
  var smallIntValue: Integer = _

  var longValue: Long = _

  var doubleValue: Double = _

  @Column(precision = 5, scale = 2)
  var price: BigDecimal = _

  @Column(length = 128, nullable = false)
  @Index
  @ExportTS(value = "'name'")
  var name: String = _

  @Text
  var text: String = _

  @LongText
  var longText: String = _

  var birthday: Date = _

  var datetimeValue: DateTime = _

  @Enum(Array("succ", "fail"))
  var status: String = _

  @Column(defaultValue = "10")
  var dftValue: Integer = _

  @Ignore
  var ignValue: Integer = _

  @Ignore
  @ExportTS(value = "")
  var ign: Ign = _

  @Ignore
  @ExportTS(value = "[ {}, {} ]")
  var igns: Array[Ign] = Array()

  @Pointer
  @ExportTS(value = "new Ptr()")
  var ptr: Ptr = _
  var ptrId: Long = _

  @OneToOne
  var oo: OO = _

  @OneToMany
  var om: Array[OM] = Array()
}

@Entity(db = "orm_test")
class Ptr {
  @Id(auto = true)
  var id: Long = _

  var value: Integer = _

  @OneToOne
  var obj: Obj = _
}

@Entity(db = "orm_test")
class OO {
  @Id(auto = true)
  var id: Long = _

  var value: Integer = _

  var objId: Long = _
}

@Entity(db = "orm_test")
class OM {
  @Id(auto = true)
  var id: Long = _

  var value: Integer = _

  @Pointer
  var mo: MO = _
  var moId: Long = _

  @Pointer
  var obj: Obj = _

  var objId: Long = _

  @Pointer
  var sub: Sub = _

  var subId: Long = _
}

@Entity(db = "orm_test")
class MO {
  @Id(auto = true)
  var id: Long = _

  var value: Integer = _
}

class Ign {
  var id: Integer = _
}

@Entity(db = "orm_test2")
class Sub extends Obj {}

@Entity(db = "orm_test")
class _ScalaObj {
  @Id
  var id: Long = _
  var isTrue: Boolean = _
  var name: String = _
}