# scala-orm

# Tables of Contents
* [Types](#types)
* [Session](#session)
* [Database(简称Db)](#database)
* [Orm](#orm)
  * [Init](#init)

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

# Database(简称Db)
driver,  url,  config,  pool,  openConnection,  openConnection[T],  shutdown,  entities,  check,  rebuild,  drop,  create,  openSession,    execute,  beginTransaction[T]

### beginTransaction[T]
    Db.beginTransaction(Session=>{ })
### openConnection
    val urlConnection = new URL(url).openConnection()
    
# Orm
init,  reset,  openDb,  create,  empty,  convert,  converts,  setLogger,  insert,  update,  delete,  root,  cond,  select,  selectFrom,  inserts,  deleteFrom,  clear

### Orm.init(path:string)
    Orm.init("test.entity")//初始化所有的entity数据
### Orm.openDb(host: String, port: Int, user: String, pwd: String, db: String)
    Orm.openDb()




