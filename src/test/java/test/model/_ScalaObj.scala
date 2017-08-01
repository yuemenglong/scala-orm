package test.model

import java.lang.Long
import java.lang.Boolean

import io.github.yuemenglong.orm.lang.anno.{Entity, Id}

/**
  * Created by <yuemenglong@126.com> on 2017/7/20.
  */
@Entity
class _ScalaObj {
  @Id
  var id: Long = _
  var isTrue: Boolean = _
  var name: String = _
}
