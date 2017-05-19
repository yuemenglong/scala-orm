package orm.init

import java.io.File
import java.lang.reflect.Field
import java.nio.file.Paths

import orm.java.anno.Entity
import orm.meta.{EntityMeta, FieldMeta, OrmMeta}

/**
  * Created by Administrator on 2017/5/16.
  */
object Scanner {
  def scan(path: String): Unit = {
    var loader = Thread.currentThread().getContextClassLoader()
    var filePath = path.replace(".", "/")
    var url = loader.getResource(filePath)
    require(url != null && url.getProtocol() == "file")
    var fullPath = new File(url.getPath()).getPath()
    var basePath = Paths.get(fullPath.replaceAll(filePath + "$", ""))
    var clazzVec = scanFile(url.getPath()).map(path => {
      // 全路径转为相对路径，将/变为.
      basePath.relativize(Paths.get(path)).toString().replaceAll("(\\\\)|(/)", ".").replaceAll("\\.class$", "")
    }).filter(path => {
      // 将带有$的去掉，主要是为了去掉scala的部分
      "[^$]*".r.pattern.matcher(path).matches()
    }).map(path => {
      // 不是entity的过滤掉
      var clazz = Class.forName(path)
      var anno = clazz.getAnnotation[Entity](classOf[Entity])
      if (anno != null) {
        clazz
      } else {
        null
      }
    }).filter(item => {
      item != null
    })
    for (clazz <- clazzVec) {
      analyzeClass(clazz)
    }
  }

  def analyzeClass(clazz: Class[_]): Unit = {
    var entityMeta = new EntityMeta(clazz)
    OrmMeta.entityVec :+= entityMeta
    OrmMeta.entityMap += (entityMeta.entity -> entityMeta)

    for (field <- clazz.getDeclaredFields()) {
      analyzeField(entityMeta, field)
    }
  }

  def analyzeField(entityMeta: EntityMeta, field: Field): Unit = {
    var fieldMeta = new FieldMeta(field)

    if(fieldMeta.pkey){
      entityMeta.pkey = fieldMeta
    }
    entityMeta.fieldVec :+= fieldMeta
    entityMeta.fieldMap += (fieldMeta.name-> fieldMeta)
  }

  def scanFile(path: String): Array[String] = {
    var file = new File(path)
    if (file.isFile()) {
      return Array(path)
    }
    var list = file.listFiles();
    if (list == null) {
      return Array()
    }
    return list.flatMap(f => scanFile(f.getPath()))
  }

  def main(args: Array[String]): Unit = {
    println("hello")
    scan("")
  }
}
