# Scala-Orm

# Tables of Contents
* [Types](#types)
* [Session](#session)
* [Database](#database)
* [Orm](#orm)
* [Init](#init)
* [Kit](#kit)
* [Logger](#logger)
* [Tool](#tool)
  
  
## [Learn how to use Scala-Orm in your own project](#types).
  
# Types
Integer,  Long,  Float,  Double,  Boolean,  String,  Date,  BigDecimal

Defining the data type of a certain data.

    var id:String=_
    var age:Integer=_
    var date:Date=_

## Types Conversion
intToObject, longToObject, doubleToObject,  booleanToObject

# Session
inTransaction,  beginTransaction,  clearTransaction,  isClosed,  close,  getConnection,  execute,  query,  firstrecord,  errorTrace,  batch

# Database
Db for short
driver,  url,  config,  pool,  openConnection,  openConnection[T],  shutdown,  entities,  check,  rebuild,  drop,  create,  openSession,    execute,  beginTransaction[T]

### beginTransaction[T]
    Db.beginTransaction(Session=>{ })
### openConnection
    val urlConnection = new URL(url).openConnection()
    
# Orm
init,  reset,  openDb,  create,  empty,  convert,  converts,  setLogger,  insert,  update,  delete,  root,  cond,  select,  selectFrom,  inserts,  deleteFrom,  clear

### Orm.init(path:string)
    Orm.init("test.entity")//Initialize all entity data
    
### Orm.openDb(host: String, port: Int, user: String, pwd: String, db: String)
    Orm.openDb("localhost", 3306, "root", "root", "test")
    
### Orm.openDb(host: String, port: Int, user: String, pwd: String, db: String,minConn: Int, maxConn: Int, partition: Int)
    Orm.openDb("localhost", 3306, "root", "root", "test",2,3,5)

### Orm.create\[T\](clazz:class[T])
    Orm.create(classOf[Obj])
    
### Orm.empty\[T\](clazz:class[T])
    Orm.empty(classOf[Obj])

# Init
scan, trace, firstScan, secondScan, indexScan, genGetterSetter, checkPkey, scanFile

# Kit
lodashCase, lowerCaseFirst, upperCaseFirst, getDeclaredFields, getDeclaredMethods, newArray, getArrayType

# Logger
trace, debug, info, warn, error, setEnable

# Tool
getEmptyConstructorMap, exportTsClass, exportTsClass, attach, attachs, sattach, sattachs, attachx, attachsx, sattachx, sattachsx, updateById, selectById, deleteById

