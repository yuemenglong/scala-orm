# Scala-Orm

# Tables of Contents
* [Database](#database)
* [Session](#session)
* [Types](#types)
* [Orm](#orm)
* [Init](#init)
* [Kit](#kit)
* [Logger](#logger)
* [Tool](#tool)
  
  
[Learn how to use Scala-Orm in your own project](#types).

# Database
Database is called db for short

beginTransaction[T], rebuild,  drop, create, check, shutdown, openSession, openConnection, execute, entities

### beginTransaction[T](fn: (Session) => T)
    db.beginTransaction(Session=>{ })  //start the following
### rebuild()
    db.rebuild() //Rebuild the data table，This operation is equal to “ drop and  create ”
### drop() 
    db.drop() //Delete the current table, if it already exists
### create()
    db.create() //create new table
### check()
    db.check() //Check the difference between the entity and the database
### shutdown()
    db.shutdown() //After the thread is finished, close the thread
### openSession()
    val session=openSession()
### openConnection[T](fn: (Connection) => T)
    val oConnection=openConnection()

# Session
inTransaction,  beginTransaction,  clearTransaction,  isClosed,  close,  getConnection,  execute,  query,  firstrecord,  errorTrace,  batch

### inTransaction()
    Session.inTransaction() //judge whether it is a transaction，return the Boolean value
### beginTransaction()
    val tx=Session.beginTransaction() //Create a new transaction
### clearTransaction()
    Session.clearTransaction() // clear the transaction
### isClosed
    session.isClosed //judge whether it is closed，return the Boolean value
### close()
    session.close() //close all unclosed connections
### getConnection
    session.getConnection //access to connections
    session.getConnection.setAutoCommit(false) //open a transaction
### execute
    session.execute() //execute program
### query
    session.query()  
    session.query(Orm.select(root).from(root))




# Types
Integer,  Long,  Float,  Double,  Boolean,  String,  Date,  BigDecimal

Defining the data type of a certain data.

    var id:String=_
    var age:Integer=_
    var date:Date=_

## Types Conversion
intToObject, longToObject, doubleToObject,  booleanToObject
    
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

