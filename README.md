
![测试](https://github.com/SimpleSmile412/scala-orm/edit/master/doc/one_to_one.png)

# Scala-Orm
scala-orm是一个用scala开发的轻量级的开源ORM框架，解决了Hibernate的一些问题

# QuickStart
## Installation
将下面内容加入到pom.xml文件中
```jsx
<!-- https://mvnrepository.com/artifact/io.github.yuemenglong/scala-orm -->
<dependency>
    <groupId>io.github.yuemenglong</groupId>
    <artifactId>scala-orm</artifactId>
    <version>1.3.0</version>
</dependency>
```
## 定义实体
```jsx
//职员表
@Entity(db = "dbtest")
class Stuff {
  @Id
  var id: String = _
  var departId: String = _
  var age: Integer = _
  @Column(length = 30)
  var name: String = _
  var sex: Integer = _
  @Pointer(left = "departId", right = "id")
  var department: Department = _
}

//领导表
@Entity(db = "dbtest")
class Manager {
  @Id
  var id: String = _
  var name: String = _
  var age: Integer = _
  var sex: Integer = _
  var phone: String = _

  @OneToOne(left = "id", right = "id")
  var department: Department = _
}

//部门表
@Entity(db = "dbtest")
class Department {
  @Id
  var id: String = _
  var name: String = _
  var numbers: Integer = _
  var computers: Integer = _

  @OneToMany(left = "id", right = "departId")
  var stuffs: Array[Stuff] = Array()

  @Pointer(left = "id", right = "id")
  var manager: Manager = _
}
```
## 实体间的关系
### OneToOne
一个领导管理一个部门，manager的id对应department的id
### OneToMany
一个部门对应多个职员，deparment的id对应stuff的departId
### Pointer
一个职员对应一个部门，stuff的departId对应department的id
一个部门对应一个领导，department的id对应manager的id

## 新增
###  insert(一次添加一条数据)
```jsx
//新增一个领导及下方一个部门
db.beginTransaction(session => {
  val manager = new Manager()
  manager.id = Math.random().toString
  manager.age = 40
  manager.sex = 1
  manager.name = "李红"
  manager.phone = "22222222"

  val department = new Department()
  department.id = manager.id
  department.name = "财务部门"
  department.numbers = 5
  department.computers = 5

  manager.department = department

  val ex = Orm.insert(Orm.convert(manager))
  ex.insert("department") //级联插入，还可写成 ex.insert(_.department) ，级联删除，级联更新，级联查询都和级联插入一样有两种写法
  session.execute(ex)
})
//结果：数据库中manager表格会增加一条数据 (40,1,李红,22222222)
//department表中增加一条数据 (财务部门，5，5)
```
###  inserts(一次添加多条数据)
```jsx
//新增多名职员
db.beginTransaction(session => {
  val ages = Array(20, 21, 22)
  val names = Array("小明", "小红", "小天")
  val sexs = Array(0, 1, 0)
  val stuffs = (0 to 2).map((item: Int) => {
    val stuff = new Stuff()
    stuff.id = Math.random().toString
    stuff.departId = "0.29005326502737394"
    stuff.age = ages(item)
    stuff.sex = sexs(item)
    stuff.name = names(item)
    stuff
  }).toArray

  session.execute(Orm.inserts(stuffs))
})
//数据库中stuff表中增加三条数据
(小明，20，0，0.29005326502737394)
(小红，21，1，0.29005326502737394)
(小天，22，0，0.29005326502737394)
```

## 删除
### delete，deleteFrom
```jsx
db.beginTransaction(session => {
  val root = Orm.root(classOf[Manager])
  //删除所有领导
  val ex = Orm.delete(root).from(root)//也可写成 val ex=Orm.deleteFrom(root)
  // 删除 id为 0.7628532707482609的领导
  // val ex = Orm.deleteFrom(root).where(root.get("id").eql("0.7628532707482609"))
  session.execute(ex)
})
```

## 更新
### update

```jsx
//将所有职员的名字改为 小奇
db.beginTransaction(session => {
  val root = Orm.root(classOf[Stuff])
  val ex = Orm.update(root).set(root.get("name").assign("小奇"))
  session.execute(ex)
})
```

```jsx
//更新id为0.7013507943626212的职员的(name:小奇，age：25)
db.beginTransaction(session => {
  val root = Orm.root(classOf[Stuff])
  val ex = Orm.update(root).set(
    root.get("name").assign("小奇"),
    root.get(_.age).assign(25)
  ).where(root.get("id").eql("0.7013507943626212"))
  session.execute(ex)
})
```

```jsx
//更新部门id为0.29005326502737394的(number:10,computers:20),同时更新该部门下的职员id为0.7013507943626212的(name:小明，age:20)
db.beginTransaction(session => {
  val department = OrmTool.selectById(classOf[Department], "0.29005326502737394", session)()
  department.numbers = 10
  department.computers = 20
  val stuffs = session.query(Orm.selectFrom(Orm.root(classOf[Stuff])))
  department.stuffs = stuffs.map((item: Stuff) => {
    if (item.id == "0.7013507943626212") {
      item.name = "小明"
      item.age = 20
    }
    item
  })
  val ex = Orm.update(department)
  ex.update(_.stuffs)
  session.execute(ex)
})
```

### update中set的不同情况举例

##### assign 等于
```jsx
//将所有部门的名字改为 财务部门
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.update(root).set(root.get("name").assign("财务部门"))
  session.execute(ex)
})
```

##### assignAdd 增加 ，assignSub 减少
```jsx
//将每个部门的人数加2
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.update(root).set(root.get("numbers").assignAdd(2))
  session.execute(ex)
})
```

```jsx
//将每个部门的人数在电脑的基础上加3，例如部门电脑数量是5，则部门人数为8
db.beginTransaction(session => {
 val root = Orm.root(classOf[Department])
 val ex = Orm.update(root).set(root.get("numbers").assignAdd(root.get("computers"), 3))
 session.execute(ex)
})
```

## 查询
### select selectFrom
```jsx
//查询出所有部门
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.select(root).from(root) //也可写成 Orm.selectFrom(root)
  val department = session.query(ex)
})
```

```jsx
//查询出所有部门的第一条
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.select(root).from(root)
  val department = session.first(ex)
})
```

```jsx
//查询出所有部门，及该部门下所有职员信息
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  root.select(_.stuffs) //级联查询该部门下的职员信息  也可写成 room.select("stuffs")
  val ex = Orm.select(root).from(root)
  val department = session.query(ex)
})
```


### where 条件语句不同情况解析（删除，更新，查询均可用）
 
##### eql 相等，neq不相等
```jsx
//查询出id等于0.29005326502737394的部门信息
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.selectFrom(root).where(root.get("id").eql("0.29005326502737394")) 
  //   in包含  nin不包含  isNull空  notNull非空   
  session.query(ex)
})
```

##### gt 大于，gte大于等于， lt小于， lte小于等于
```jsx
//查询人数大于10的部门信息
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.selectFrom(root).where(root.get("numbers").gt(10)) 
  session.query(ex)
})
```

##### like模糊查询
```jsx
//查询 姓名中间带“部”字的 部门信息
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.selectFrom(root).where(root.get("name").like("%部%")) 
  //IT%（模糊匹配以‘IT’开头）  %部门（模糊匹配以‘部门’结尾）  %部%（模糊匹配中间含‘部’字）
  session.query(ex)
})
```

##### in包含，nin不包含
```jsx
//查询 部门名称包含在以下数组中的 部门信息
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.selectFrom(root).where(root.get("name").in(Array("IT部门", "财务部门", "销售部门")))
  session.query(ex)
})
```

##### isNull 空，notNull非空
```jsx
//查询 name非空的 部门信息
db.beginTransaction(session => {
  val root = Orm.root(classOf[Department])
  val ex = Orm.selectFrom(root).where(root.get("name").notNull()) //若为空是isNull
  session.query(ex)
})
```


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
    
### Orm.deleteFrom(root: Root[\_])
    val Obj=[{id:1,name:"a"},}{id:2,name:"b},{id:3,name:"c"}]
    val root = Orm.root(classOf[Obj])
    val ex = Orm.deleteFrom(root).where(root.get("id").eql(1))
    session.execute(ex)
    // [{id:2,name:"b"},{id:3,name:"c"}]

    
### Orm.select[T](s: Selectable[T])
    val Obj=[{id:1,name:"a"},{id:2,name:"b"},{id:3,name:"c"}]
    val root = session.query(Orm.select(root).from(root).where(root.get("id").in(Array(3, 4))))
    // [{id:3,name:"c"}]
    
### Orm.selectFrom[T](root: Root[T])
    val Obj=[{id:1,name:"a"},{id:2,name:"b"},{id:3,name:"c"}]
    val root = session.query(Orm.selectFrom(root).where(root.get("id").eq('1'))))
    // [{id:1,name:"a"}]
    
### Orm.select[T1, T2](s1: Selectable[T1], s2: Selectable[T2])
### Orm.select[T1, T2, T3](s1: Selectable[T1], s2: Selectable[T2], s3: Selectable[T3])  
### Orm.insert[T](clazz: Class[T])
### Orm.inserts[T](arr: Array[T])
### Orm.update(root: Root[\_])
### Orm.delete(joins: Join*)
### Orm.clear(obj: Object, field: String)
### Orm.clear[T <: Object](obj: T)(fn: T => Any)
    
    

# Init
scan, trace, firstScan, secondScan, indexScan, genGetterSetter, checkPkey, scanFile

# Kit
lodashCase, lowerCaseFirst, upperCaseFirst, getDeclaredFields, getDeclaredMethods, newArray, getArrayType

# Logger
trace, debug, info, warn, error, setEnable

# Tool
getEmptyConstructorMap, exportTsClass, exportTsClass, attach, attachs, sattach, sattachs, attachx, attachsx, sattachx, sattachsx, updateById, selectById, deleteById

