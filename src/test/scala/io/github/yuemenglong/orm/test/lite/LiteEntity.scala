package io.github.yuemenglong.orm.test.lite

import java.util.Date

import io.github.yuemenglong.orm.lang.anno._
import io.github.yuemenglong.orm.lang.types.Types._

/**
  * Created by <yuemenglong@126.com> on 2018/1/31.
  */
@Entity(db = "test.db")
class Obj {
  @Id(auto = true)
  var id: Integer = _

  @Column(name = "age_")
  var age: Integer = _

  @TinyInt
  var tinyAge: Integer = _

  var boolValue: Boolean = _

  var doubleValue: Double = _

  @Column(precision = 5, scale = 2)
  var price: BigDecimal = _

  @Column(length = 128, nullable = false)
  @Index
  var name: String = _

  @Text
  var text: String = _

  @LongText
  var longText: String = _

  var birthday: Date = _

  @DateTime
  var nowTime: Date = _

  @Check(in = Array("succ", "fail"))
  var status: String = _

  @Column(defaultValue = "10")
  var dftValue: Integer = _

  @Ignore
  var ignValue: Integer = _

  @Ignore
  var ign: Ign = _

  @Ignore
  var igns: Array[Ign] = Array()

  @Pointer
  @ExportTS(init = false)
  var ptr: Ptr = _
  var ptrId: Integer = _

  @OneToOne
  var oo: OO = _

  @OneToMany
  var om: Array[OM] = Array()
}

@Entity(db = "test.db")
class Ptr {
  @Id(auto = true)
  var id: Integer = _

  var value: Integer = _

  @OneToOne
  var obj: Obj = _
}

@Entity(db = "test.db")
class OO {
  @Id(auto = true)
  var id: Integer = _

  var value: Integer = _

  var objId: Integer = _
}

@Entity(db = "test.db")
class OM {
  @Id(auto = true)
  var id: Integer = _

  var value: Integer = _

  @Pointer
  var mo: MO = _
  var moId: Integer = _

  @Pointer
  var obj: Obj = _

  var objId: Integer = _

  var subId: Integer = _
}

@Entity(db = "test.db")
class MO {
  @Id(auto = true)
  var id: Integer = _

  var value: Integer = _
}

class Ign {
  var id: Integer = _
}
