package orm.init

import java.io.File
import java.lang.reflect.Field
import java.nio.file.Paths

import orm.kit.Kit
import orm.lang.anno.Entity
import orm.meta.{EntityMeta, FieldMeta, FieldMetaTypeKind, OrmMeta}

/**
  * Created by Administrator on 2017/5/16.
  */
object Scanner {
  def scan(path: String): Unit = {
    val loader = Thread.currentThread().getContextClassLoader()
    val filePath = path.replace(".", "/")
    val url = loader.getResource(filePath)
    require(url != null && url.getProtocol() == "file")
    val fullPath = new File(url.getPath()).getPath().replaceAll("\\\\", "/")
    val basePath = Paths.get(fullPath.replaceAll(s"${filePath}$$", ""))
    scanFile(url.getPath()).map(path => {
      // 全路径转为相对路径，将/变为.
      basePath.relativize(Paths.get(path)).toString().replaceAll("(\\\\)|(/)", ".").replaceAll("\\.class$", "")
    }).filter(path => {
      // 将带有$的去掉，主要是为了去掉scala的部分
      "[^$]*".r.pattern.matcher(path).matches()
    }).map(path => {
      // 不是entity的过滤掉
      val clazz = Class.forName(path)
      val anno = clazz.getAnnotation[Entity](classOf[Entity])
      if (anno != null) {
        clazz
      } else {
        null
      }
    }).filter(item => {
      item != null
    }).foreach(analyzeClass(_))
    fixMeta()
  }

  def analyzeClass(clazz: Class[_], ignore: Boolean = false): Unit = {
    val ignoreStr = ignore match {
      case true => "Ignore "
      case false => ""
    }
    println(s"[Scanner] Find ${ignoreStr}Entity: [${clazz.getName()}]")
    var entityMeta = new EntityMeta(clazz, ignore)
    OrmMeta.entityVec += entityMeta
    OrmMeta.entityMap += (entityMeta.entity -> entityMeta)

    clazz.getDeclaredFields().foreach(field => analyzeField(entityMeta, field))
  }

  def analyzeField(entityMeta: EntityMeta, field: Field): Unit = {
    var fieldMeta = FieldMeta.createFieldMeta(entityMeta, field)

    if (fieldMeta.pkey) {
      entityMeta.pkey = fieldMeta
    }
    entityMeta.fieldVec += fieldMeta
    entityMeta.fieldMap += (fieldMeta.name -> fieldMeta)
  }

  def fixMeta(): Unit = {
    OrmMeta.entityVec.clone().foreach(entity => {
      // 未标注ignore的字段对应的对象都必须显式标注为entity,也就是已经在orm中
      entity.managedFieldVec().filter(!_.isNormalOrPkey()).foreach(field => {
        if (!OrmMeta.entityMap.contains(field.typeName)) {
          throw new RuntimeException(s"[${field.typeName}] Is Not Entity")
        }
      })
      // 标注ignore的字段对应的对象如果没有加入entity，也加进去
      entity.fieldVec.filter(field => {
        field.ignore && field.isObject() && !OrmMeta.entityMap.contains(field.typeName)
      }).foreach(fieldMeta => {
        val clazz = Kit.getGenericType(fieldMeta.field)
        analyzeClass(clazz, true)
      })

    })
    OrmMeta.entityVec.foreach(entity => {
      // 补关系字段，ignore的不用补
      entity.managedFieldVec().foreach(field => {
        if (!field.isNormalOrPkey()) {
          //补左边
          if (!entity.fieldMap.contains(field.left)) {
            val idx = entity.fieldVec.indexOf(field)
            val refer = FieldMeta.createReferMeta(entity, field.left)
            entity.fieldVec.insert(idx, refer)
            entity.fieldMap += (field.left -> refer)
          }
          //补右边
          val referEntityMeta = OrmMeta.entityMap(field.typeName)
          if (!referEntityMeta.fieldMap.contains(field.right)) {
            val refer = FieldMeta.createReferMeta(entity, field.right)
            referEntityMeta.fieldVec += refer
            referEntityMeta.fieldMap += (field.right -> refer)
          }
        }
      })
    })
    // 统一注入refer,这里ignore的也要注入
    OrmMeta.entityVec.foreach(entity => {
      entity.fieldVec.foreach(field => {
        if (field.isObject()) {
          field.refer = OrmMeta.entityMap(field.typeName)
        }
      })
    })
  }

  def scanFile(path: String): Array[String] = {
    val file = new File(path)
    if (file.isFile()) {
      return Array(path)
    }
    val list = file.listFiles();
    if (list == null) {
      return Array()
    }
    return list.flatMap(f => scanFile(f.getPath()))
  }
}
