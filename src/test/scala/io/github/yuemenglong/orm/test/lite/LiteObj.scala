package io.github.yuemenglong.orm.test.lite

import io.github.yuemenglong.orm.lang.anno.{Column, Entity, Id, TinyInt}
import io.github.yuemenglong.orm.lang.types.Types.{BigDecimal, Double, Integer, Long, String}

/**
  * Created by <yuemenglong@126.com> on 2018/9/9.
  */
@Entity
class LiteObj{
  @Id(auto = true)
  var id: Integer = _

  @Column(name = "age_")
  var age: Long = _

  @TinyInt
  var tinyAge: Integer = _

  var doubleValue: Double = _

  @Column(precision = 5, scale = 2)
  var price: BigDecimal = _

  var name: String = _
}
