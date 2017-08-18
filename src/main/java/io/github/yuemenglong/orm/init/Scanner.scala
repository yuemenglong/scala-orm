package io.github.yuemenglong.orm.init

import java.io.File
import java.nio.file.Paths

import io.github.yuemenglong.orm.kit.Kit
import io.github.yuemenglong.orm.lang.anno._
import io.github.yuemenglong.orm.lang.types.Types
import io.github.yuemenglong.orm.meta._

/**
  * Created by Administrator on 2017/5/16.
  */
object Scanner {
  def scan(path: String): Unit = {
    val loader = Thread.currentThread().getContextClassLoader
    val filePath = path.replace(".", "/")
    val url = loader.getResource(filePath)
    require(url != null && url.getProtocol == "file")
    val fullPath = new File(url.getPath).getPath.replaceAll("\\\\", "/")
    val basePath = Paths.get(fullPath.replaceAll(s"$filePath$$", ""))
    val classes = scanFile(url.getPath).map(path => {
      // 全路径转为相对路径，将/变为.
      basePath.relativize(Paths.get(path)).toString.replaceAll("(\\\\)|(/)", ".").replaceAll("\\.class$", "")
    }).filter(path => {
      // 将带有$的去掉，主要是为了去掉scala的部分
      "[^$]*".r.pattern.matcher(path).matches()
    }).map(Class.forName)
    scan(classes)
  }

  def scan(paths: Array[String]): Unit = {
    scan(paths.map(Class.forName))
  }

  def scan(clazzs: Array[Class[_]]): Unit = {
    val metas = clazzs.filter(clazz => {
      // 不是entity的过滤掉
      val anno = clazz.getDeclaredAnnotation(classOf[Entity])
      anno != null
    }).map(clazz => {
      val entityMeta = new EntityMeta(clazz)
      OrmMeta.entityVec += entityMeta
      OrmMeta.entityMap += (entityMeta.entity -> entityMeta)
      entityMeta
    })
    metas.map(firstScan).map(secondScan).foreach(check)
    //    fixMeta()
  }

  def firstScan(entityMeta: EntityMeta): EntityMeta = {
    // 第一轮只遍历内建类型
    Kit.getDeclaredFields(entityMeta.clazz).filter(field => {
      field.getAnnotation(classOf[Pointer]) == null &&
        field.getAnnotation(classOf[OneToOne]) == null &&
        field.getAnnotation(classOf[OneToMany]) == null
    }).foreach(field => {
      val fieldMeta = field.getType match {
        case Types.IntegerClass => new FieldMetaInteger(field, entityMeta)
        case Types.LongClass => new FieldMetaLong(field, entityMeta)
        case Types.FloatClass => new FieldMetaFloat(field, entityMeta)
        case Types.DoubleClass => new FieldMetaDouble(field, entityMeta)
        case Types.BooleanClass => new FieldMetaBoolean(field, entityMeta)
        case Types.BigDecimalClass => new FieldMetaDecimal(field, entityMeta)
        case Types.StringClass =>
          val annoLongText = field.getAnnotation(classOf[LongText])
          if (annoLongText == null) new FieldMetaString(field, entityMeta)
          else new FieldMetaLongText(field, entityMeta)
        case Types.DateClass =>
          val annoDateTime = field.getAnnotation(classOf[DateTime])
          if (annoDateTime == null) new FieldMetaDate(field, entityMeta)
          else new FieldMetaDateTime(field, entityMeta)
        case _ => throw new RuntimeException(s"No Refer Annotation In Field: ${field.getName}")
      }
      entityMeta.fieldVec += fieldMeta
      entityMeta.fieldMap += (fieldMeta.name -> fieldMeta)
    })
    entityMeta
  }

  def secondScan(entityMeta: EntityMeta): EntityMeta = {
    // 第二轮只遍历引用类型, 并加上相关的外键
    Kit.getDeclaredFields(entityMeta.clazz).filter(field => {
      field.getAnnotation(classOf[Pointer]) != null ||
        field.getAnnotation(classOf[OneToOne]) != null ||
        field.getAnnotation(classOf[OneToMany]) != null
    }).foreach(field => {
      val ty = Kit.getGenericType(field.getType)
      val refer = OrmMeta.entityMap(ty.getSimpleName)
      val annoPointer = field.getAnnotation(classOf[Pointer])
      val annoOneOne = field.getAnnotation(classOf[OneToOne])
      val annoOneMany = field.getAnnotation(classOf[OneToMany])
      val referMeta = (annoPointer, annoOneOne, annoOneMany) match {
        case (p, null, null) if p != null =>
          val referMeta = new FieldMetaPointer(field, entityMeta, refer)
          if (!entityMeta.fieldMap.contains(referMeta.left)) {
            val fkeyMeta = new FieldMetaFkey(referMeta.left, entityMeta, referMeta)
            entityMeta.fieldVec += fkeyMeta
            entityMeta.fieldMap += (fkeyMeta.name -> fkeyMeta)
          }
          referMeta
        case (null, oo, null) if oo != null =>
          val referMeta = new FieldMetaOneOne(field, entityMeta, refer)
          if (!refer.fieldMap.contains(referMeta.right)) {
            val fkeyMeta = new FieldMetaFkey(referMeta.right, refer, referMeta)
            refer.fieldVec += fkeyMeta
            refer.fieldMap += (fkeyMeta.name -> fkeyMeta)
          }
          referMeta
        case (null, null, om) if om != null =>
          val referMeta = new FieldMetaOneMany(field, entityMeta, refer)
          if (!refer.fieldMap.contains(referMeta.right)) {
            val fkeyMeta = new FieldMetaFkey(referMeta.right, refer, referMeta)
            refer.fieldVec += fkeyMeta
            refer.fieldMap += (fkeyMeta.name -> fkeyMeta)
          }
          referMeta
      }
      entityMeta.fieldVec += referMeta
      entityMeta.fieldMap += (referMeta.name -> referMeta)
    })
    entityMeta
  }

  def check(meta: EntityMeta): Unit = {
    meta.fieldVec.foreach(field => {
      if (field.isPkey && meta.pkey != null) {
        throw new RuntimeException(s"${meta.entity} Has Multi Pkey")
      }
      meta.pkey = field
    })
    if (meta.pkey == null) {
      throw new RuntimeException(s"${meta.entity} Has No Pkey")
    }
  }

  //  def analyzeClass(clazz: Class[_], ignore: Boolean = false): EntityMeta = {
  //    val ignoreStr = if (ignore) {
  //      "Ignore "
  //    } else {
  //      ""
  //    }
  //    println(s"[Scanner] Find ${ignoreStr}Entity: [${clazz.getName}]")
  //    var entityMeta = new EntityMeta(clazz, ignore)
  //    OrmMeta.entityVec += entityMeta
  //    OrmMeta.entityMap += (entityMeta.entity -> entityMeta)
  //
  //    Kit.getDeclaredFields(clazz).foreach(field => analyzeField(entityMeta, field))
  //
  //    val methodMap: Map[String, Method] = Kit.getDeclaredMethods(clazz).map(m => (m.getName, m))(collection.breakOut)
  //    entityMeta.fieldVec.foreach(fieldMeta => {
  //      val getter = s"get${Kit.upperCaseFirst(fieldMeta.name)}"
  //
  //      val getterMethod = if (methodMap.contains(getter)) methodMap(getter)
  //      else if (methodMap.contains(fieldMeta.name)) methodMap(fieldMeta.name)
  //      else null
  //
  //      if (getterMethod != null && getterMethod.getParameterCount == 0
  //        && getterMethod.getReturnType == fieldMeta.clazz) {
  //        entityMeta.getterMap += (getterMethod -> fieldMeta)
  //      }
  //
  //      val setterJ = s"set${Kit.upperCaseFirst(fieldMeta.name)}"
  //      val setterS = s"${fieldMeta.name}_$$eq"
  //
  //      val setterMethod = if (methodMap.contains(setterJ)) methodMap(setterJ)
  //      else if (methodMap.contains(setterS)) methodMap(setterS)
  //      else null
  //
  //      if (setterMethod != null && setterMethod.getParameterCount == 1
  //        && setterMethod.getParameterTypes()(0) == fieldMeta.clazz) {
  //        entityMeta.setterMap += (setterMethod -> fieldMeta)
  //      }
  //    })
  //    entityMeta
  //  }
  //
  //  def analyzeField(entityMeta: EntityMeta, field: Field): Unit = {
  //    var fieldMeta = FieldMeta.createFieldMeta(entityMeta, field)
  //
  //    if (fieldMeta.pkey && entityMeta.pkey2 != null) throw new RuntimeException("Already Has Pkey")
  //    if (fieldMeta.pkey) entityMeta.pkey2 = fieldMeta
  //
  //    entityMeta.fieldVec += fieldMeta
  //    entityMeta.fieldMap += (fieldMeta.name -> fieldMeta)
  //  }
  //
  //  def fixMeta(): Unit = {
  //    OrmMeta.entityVec.foreach(entity => {
  //      // 检查是否配置主键
  //      if (!entity.ignore && entity.pkey2 == null) {
  //        throw new RuntimeException(s"[${entity.entity}] Has No Pkey")
  //      }
  //      // 未标注ignore的字段对应的对象都必须显式标注为entity,也就是已经在orm中
  //      entity.managedFieldVec().filter(!_.isNormalOrPkey).foreach(field => {
  //        if (!OrmMeta.entityMap.contains(field.typeName)) {
  //          throw new RuntimeException(s"[${field.typeName}] Is Not Entity")
  //        }
  //      })
  //    })
  //    var entityVec = OrmMeta.entityVec.clone()
  //    var pos = 0
  //    while (pos < entityVec.size) {
  //      // 标注ignore的字段对应的对象如果没有加入entity，都要加进去管理起来, 因为算法都是递归调用的
  //      val entity = entityVec(pos)
  //      entity.fieldVec.filter(field => {
  //        field.ignore && field.isObject
  //      }).foreach(fieldMeta => {
  //        // 放在foreach而不是filter里防止一个实体被多次scan
  //        if (!OrmMeta.entityMap.contains(fieldMeta.typeName)) {
  //          val clazz = fieldMeta.field.getType
  //          val entityMeta = analyzeClass(clazz, ignore = true)
  //          entityVec += entityMeta // 需要加入队尾再次循环
  //        }
  //      })
  //      pos += 1
  //    }
  //
  //    Logger.info("[Scanner] Start To Fix Refer Field / Column")
  //    OrmMeta.entityVec.foreach(entity => {
  //      // 补关系字段，ignore的不用补
  //      entity.managedFieldVec().foreach(field => {
  //        if (!field.isNormalOrPkey) {
  //          //补左边
  //          if (!entity.fieldMap.contains(field.left)) {
  //            val idx = entity.fieldVec.indexOf(field)
  //            val refer = FieldMeta.createReferMeta(entity, field.left)
  //            entity.fieldVec.insert(idx, refer)
  //            entity.fieldMap += (field.left -> refer)
  //          }
  //          //补右边
  //          val referEntityMeta = OrmMeta.entityMap(field.typeName)
  //          if (!referEntityMeta.fieldMap.contains(field.right)) {
  //            val refer = FieldMeta.createReferMeta(referEntityMeta, field.right)
  //            referEntityMeta.fieldVec += refer
  //            referEntityMeta.fieldMap += (field.right -> refer)
  //          }
  //        }
  //      })
  //    })
  //    // 统一注入refer,这里ignore的也要注入
  //    OrmMeta.entityVec.foreach(entity => {
  //      entity.fieldVec.foreach(field => {
  //        if (field.isObject) {
  //          field.refer = OrmMeta.entityMap(field.typeName)
  //        }
  //      })
  //    })
  //  }

  def scanFile(path: String): Array[String] = {
    val file = new File(path)
    if (file.isFile) {
      return Array(path)
    }
    val list = file.listFiles()
    if (list == null) {
      return Array()
    }
    list.flatMap(f => scanFile(f.getPath))
  }
}
