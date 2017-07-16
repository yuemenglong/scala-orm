package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.traits.Update;
import orm.operate.traits.core.*;
import orm.operatebak.Count_;
import test.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Administrator on 2017/7/6.
 * //
 */
public class SimpleTest {

    private static Db db;

    @SuppressWarnings("Duplicates")
    @Before
    public void before() {
        ArrayList<String> clazzList = new ArrayList<String>();
        clazzList.add("test.model.Obj");
        clazzList.add("test.model.Sub");
        clazzList.add("test.model.Ptr");
        clazzList.add("test.model.OO");
        clazzList.add("test.model.OM");
        clazzList.add("test.model.MO");
        Orm.init(clazzList.toArray(new String[0]));
        db = openDb();
        db.rebuild();
    }

    private Db openDb() {
        return Orm.openDb("localhost", 3306, "root", "root", "test");
    }

    @Test
    public void testCURD() {
        Session session = db.openSession();

        // 初始化数据
        Obj person = new Obj();
        Ptr ptr = new Ptr();
        OO oo = new OO();
        OM om = new OM();
        OM om2 = new OM();

        person.setAge(10);
        person.setName("/TOM");
        person.setBirthday(new Date());
        person.setNowTime(new Date());
        person.setPrice(new BigDecimal(123.45));

        ptr.setValue(10);
        oo.setValue(100);
        om.setValue(1000);
        om2.setValue(2000);

        // 忽略的数据
        person.setIgnValue(0);
        person.setIgn(Orm.create(Ign.class));

        // 初始化关系
        person.setPtr(ptr);
        person.setOo(oo);
        person.setOm(new OM[]{om, om2});

        // insert
        person = Orm.convert(person);
        ExecuteRoot ex = Orm.insert(person);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(person.getId().longValue(), 1);
        Assert.assertEquals(ret, 5);// 一共写入5个对象

        // update
        person.setAge(20);
        ExecuteRoot update = Orm.update(person);
        ret = session.execute(update);
        Assert.assertEquals(ret, 1);

        // select
        SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
        root.select("ptr");
        root.select("oo");
        root.select("om");

        Query1<Obj> query = Orm.select(root).from(root).where(root.get("id").in(new Integer[]{1, 2,}));
        Obj[] res = (Obj[]) session.query(query);
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getId().intValue(), 1);
        Assert.assertEquals(res[0].getAge().intValue(), 20);
        Assert.assertEquals(res[0].getPtr().getValue().intValue(), 10);
        Assert.assertEquals(res[0].getOo().getValue().intValue(), 100);
        Assert.assertEquals(res[0].getOm()[0].getValue().intValue(), 1000);
        Assert.assertEquals(res[0].getOm()[1].getValue().intValue(), 2000);

        // delete
        ExecuteRoot delete = Orm.delete(person);
        ret = session.execute(delete);
        Assert.assertEquals(ret, 1);

        // delete then select
        Obj obj = session.first(Orm.select(root).from(root));
        Assert.assertEquals(obj, null);

        session.close();
    }

    @Test
    public void testUpdateSpec() {
        Session session = db.openSession();

        Obj obj = new Obj();
        obj.setName("name");
        obj.setAge(100);

        obj = Orm.convert(obj);
        ExecuteRoot ex = Orm.insert(obj);
        session.execute(ex);

        Long id = obj.getId();
        obj = Orm.empty(Obj.class);
        obj.setId(id);
        obj.setAge(200);

        ex = Orm.update(obj);
        session.execute(ex);

        SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
        obj = session.first(Orm.select(root).from(root));
        Assert.assertEquals(obj.getName(), "name");
        Assert.assertEquals(obj.getAge().intValue(), 200);
    }

    @Test
    public void testDeleteRefer() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("");
        obj.setPtr(new Ptr());
        obj.setOo(new OO());
        obj.setOm(new OM[]{new OM(), new OM()});

        obj = Orm.convert(obj);
        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 5);

        {
            SelectRoot<Obj> sr = Orm.root(Obj.class).asSelect();
            Obj[] objs = (Obj[]) session.query(Orm.select(sr).from(sr));
            Assert.assertEquals(objs.length, 1);
            SelectRoot<Ptr> sr2 = Orm.root(Ptr.class).asSelect();
            Ptr[] ptrs = (Ptr[]) session.query(Orm.select(sr2).from(sr2));
            Assert.assertEquals(ptrs.length, 1);
            SelectRoot<OO> sr3 = Orm.root(OO.class).asSelect();
            OO[] oos = (OO[]) session.query(Orm.from(sr3));
            Assert.assertEquals(oos.length, 1);
            SelectRoot<OM> sr4 = Orm.root(OM.class).asSelect();
            OM[] oms = (OM[]) session.query(Orm.from(sr4));
            Assert.assertEquals(oms.length, 2);
        }

        ExecuteRoot delete = Orm.delete(obj);
        delete.delete("ptr");
        delete.delete("oo");
        delete.delete("om");
        session.execute(delete);

        {
            SelectRoot<Obj> sr = Orm.root(Obj.class).asSelect();
            Obj[] objs = (Obj[]) session.query(Orm.select(sr).from(sr));
            Assert.assertEquals(objs.length, 0);
            SelectRoot<Ptr> sr2 = Orm.root(Ptr.class).asSelect();
            Ptr[] ptrs = (Ptr[]) session.query(Orm.select(sr2).from(sr2));
            Assert.assertEquals(ptrs.length, 0);
            SelectRoot<OO> sr3 = Orm.root(OO.class).asSelect();
            OO[] oos = (OO[]) session.query(Orm.from(sr3));
            Assert.assertEquals(oos.length, 0);
            SelectRoot<OM> sr4 = Orm.root(OM.class).asSelect();
            OM[] oms = (OM[]) session.query(Orm.from(sr4));
            Assert.assertEquals(oms.length, 0);
        }
    }

    @Test
    public void testOrderByLimitOffset() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("");
        obj.setOm(new OM[]{new OM(), new OM(), new OM(), new OM(), new OM(), new OM()});

        obj = Orm.convert(obj);
        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 7);

        SelectRoot<OM> root = Orm.root(OM.class).asSelect();
        Query1<OM> query = Orm.select(root).from(root).desc(root.get("id")).limit(3).offset(2);

        OM[] oms = (OM[]) session.query(query);
        Assert.assertEquals(oms.length, 3);
        Assert.assertEquals(oms[0].getId().intValue(), 4);
        Assert.assertEquals(oms[1].getId().intValue(), 3);
        Assert.assertEquals(oms[2].getId().intValue(), 2);
    }




}
