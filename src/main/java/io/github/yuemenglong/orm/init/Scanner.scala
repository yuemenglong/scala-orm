package io.github.yuemenglong.orm.init

import java.io.File
import java.lang.reflect.Method
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
    metas.map(firstScan).map(checkPkey).map(secondScan)
      .map(genGetterSetter).foreach(trace)
    //    fixMeta()
  }

  def trace(meta: EntityMeta): Unit = {
    meta.fieldVec.foreach(field => {
      println(s"Entity: ${field.entity.entity}, Table: ${field.entity.table}, " +
        s"Field: ${field.name}, Column: ${field.column}, DbType: ${field.dbType}")
    })
  }

  def firstScan(entityMeta: EntityMeta): EntityMeta = {
    // 第一轮只遍历内建类型
    Kit.getDeclaredFields(entityMeta.clazz).filter(field => {
      field.getAnnotation(classOf[Pointer]) == null &&
        field.getAnnotation(classOf[OneToOne]) == null &&
        field.getAnnotation(classOf[OneToMany]) == null &&
        field.getAnnotation(classOf[Ignore]) == null
    }).foreach(field => {
      val fieldMeta = field.getType match {
        case Types.LongClass => new FieldMetaLong(field, entityMeta)
        case Types.FloatClass => new FieldMetaFloat(field, entityMeta)
        case Types.DoubleClass => new FieldMetaDouble(field, entityMeta)
        case Types.BooleanClass => new FieldMetaBoolean(field, entityMeta)
        case Types.BigDecimalClass => new FieldMetaDecimal(field, entityMeta)
        case Types.IntegerClass =>
          if (field.getAnnotation(classOf[TinyInt]) != null) {
            new FieldMetaTinyInt(field, entityMeta)
          } else if (field.getAnnotation(classOf[SmallInt]) != null) {
            new FieldMetaSmallInt(field, entityMeta)
          } else {
            new FieldMetaInteger(field, entityMeta)
          }
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
    // 第二轮只遍历引用类型, 并加上相关的外键（没有在对象中声明的那种）
    Kit.getDeclaredFields(entityMeta.clazz).filter(field => {
      (field.getAnnotation(classOf[Pointer]) != null ||
        field.getAnnotation(classOf[OneToOne]) != null ||
        field.getAnnotation(classOf[OneToMany]) != null) &&
        field.getAnnotation(classOf[Ignore]) == null
    }).foreach(field => {
      val ty = Kit.getArrayType(field.getType)
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

  def genGetterSetter(entityMeta: EntityMeta): EntityMeta = {
    val methodMap: Map[String, Method] = Kit.getDeclaredMethods(entityMeta.clazz).map(m => (m.getName, m))(collection.breakOut)
    entityMeta.fieldVec.foreach(fieldMeta => {
      val getterJ = s"get${Kit.upperCaseFirst(fieldMeta.name)}"
      val getterS = fieldMeta.name

      val getterMethod = if (methodMap.contains(getterJ)) methodMap(getterJ)
      else if (methodMap.contains(getterS)) methodMap(getterS)
      else null

      if (getterMethod != null && getterMethod.getParameterCount == 0
        && getterMethod.getReturnType == fieldMeta.clazz) {
        entityMeta.getterMap += (getterMethod -> fieldMeta)
      }

      val setterJ = s"set${Kit.upperCaseFirst(fieldMeta.name)}"
      val setterS = s"${fieldMeta.name}_$$eq"

      val setterMethod = if (methodMap.contains(setterJ)) methodMap(setterJ)
      else if (methodMap.contains(setterS)) methodMap(setterS)
      else null

      if (setterMethod != null && setterMethod.getParameterCount == 1
        && setterMethod.getParameterTypes()(0) == fieldMeta.clazz) {
        entityMeta.setterMap += (setterMethod -> fieldMeta)
      }
    })
    entityMeta
  }

  def checkPkey(meta: EntityMeta): EntityMeta = {
    // 检查pkey是否存在或出现多个
    meta.fieldVec.foreach(field => {
      if (field.isPkey && meta.pkey != null) {
        throw new RuntimeException(s"${meta.entity} Has Multi Pkey")
      } else if (field.isPkey) {
        meta.pkey = field
      }
    })
    if (meta.pkey == null) {
      throw new RuntimeException(s"${meta.entity} Has No Pkey")
    }
    meta
  }


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
