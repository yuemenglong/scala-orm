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
inTransaction,  beginTransaction,  clearTransaction,  isClosed,  close,  getConnection,  execute,  query,  first, record,  errorTrace,  batch

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
### execute(executor: Executable)
    session.execute() //execute program
### query[T](query: Queryable[T])
    val root = Orm.root(classOf[Obj])
    session.query(Orm.select(root).from(root))  //select all the data in the obj table
### first[T](q: Queryable[T])
    val root = Orm.root(classOf[Obj])
    session.first(Orm.select(root).from(root)) //Select the first of all the data in the obj table
    
# Types
String,  Integer,  Date,  Long,  Double,  Boolean, BigDecimal,  Float

Defining the data type of a certain data.

    var id:String=_
    var age:Integer=_
    var date:Date=_
    var orderId:Long=_
    var price:Double=_
    var isSponsor:Boolean=_
    var payPrice:BigDecimal=_
    var num:Foat=_
    
## Types Conversion
intToObject, longToObject, doubleToObject,  booleanToObject
    
# Orm
init,  reset,  openDb,  create,  empty,  convert,  converts,  setLogger,  insert,  update,  delete,  root,  cond,  select,  selectFrom,  inserts,  deleteFrom,  clear

### Orm.init(path:string)
    Orm.init("test.entity") //Initialize all entity data
    
### Orm.root[T](clazz: Class[T])
    val root = Orm.root(classOf[Obj])
    
### Orm.openDb(host: String, port: Int, user: String, pwd: String, db: String)
    Orm.openDb("localhost", 3306, "root", "root", "test")  //open the database
    
### Orm.openDb(host: String, port: Int, user: String, pwd: String, db: String,minConn: Int, maxConn: Int, partition: Int)
    Orm.openDb("localhost", 3306, "root", "root", "test",2,3,5)  
    //Open the database，the minConn is 2，the maxConn is 3，the partition is 5

### Orm.create\[T\](clazz:class[T])
    Orm.create(classOf[Obj])  //create an empty obj object with an empty value or create an empty array
    
### Orm.empty\[T\](clazz:class[T])
    Orm.empty(classOf[Obj]) //create an empty obj object  or  create an empty array
    
### Orm.convert[T](obj: T)
    val obj=new Obj()
    val convert=Orm.convert(obj) 
    
### Orm.setLogger(b:boolean)
    Orm.setLogger(true)  //Whether or not the logger system is used
    
### Orm.insert[T <: Object](obj: T)
    val obj = Orm.empty(classOf[Obj])
    obj.name = "test"
    session.execute(Orm.insert(obj))
    //Obj= {name:"test"}
    
### Orm.update[T <: Object](obj: T)
    val Obj={name:"aa",age:10}
    val root = Orm.root(classOf[Obj])
    val res = session.first(Orm.select(root).from(root))
    res.name = "test"
    session.execute(Orm.update(res))
    //{name:"test",age:10}
    
### Orm.delete[T <: Object](obj: T)
    val Obj=[{id:1,name:"a"},}{id:2,name:"b},{id:3,name:"c"}]
    val root = Orm.root(classOf[Obj])
    val ex = Orm.delete(root).from(root).where(root.get("id").eql(1))
    session.execute(ex)
    // [{id:2,name:"b"},{id:3,name:"c"}]
    
### Orm.select[T](s: Selectable[T])
    val Obj=[{id:1,name:"a"},{id:2,name:"b"},{id:3,name:"c"}]
    val root = session.query(Orm.select(root).from(root).where(root.get("id").in(Array(3, 4))))
    // [{id:3,name:"c"}]
    
### Orm.select[T1, T2](s1: Selectable[T1], s2: Selectable[T2])
### Orm.select[T1, T2, T3](s1: Selectable[T1], s2: Selectable[T2], s3: Selectable[T3])

### Orm.selectFrom[T](root: Root[T])
    val Obj=[{id:1,name:"a"},{id:2,name:"b"},{id:3,name:"c"}]
    val root = session.query(Orm.selectFrom(root).where(root.get("id").eq('1'))))
    // [{id:1,name:"a"}]
    
    

# Init
scan, trace, firstScan, secondScan, indexScan, genGetterSetter, checkPkey, scanFile

# Kit
lodashCase, lowerCaseFirst, upperCaseFirst, getDeclaredFields, getDeclaredMethods, newArray, getArrayType

# Logger
trace, debug, info, warn, error, setEnable

# Tool
getEmptyConstructorMap, exportTsClass, exportTsClass, attach, attachs, sattach, sattachs, attachx, attachsx, sattachx, sattachsx, updateById, selectById, deleteById

