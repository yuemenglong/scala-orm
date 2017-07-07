package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.Cond;
import orm.operate.Executor;
import orm.operate.Selector;
import test.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Administrator on 2017/7/6.
 */
public class SimpleTest {

    private static Db db;

    @Before
    public void before() {
        ArrayList<String> clazzList = new ArrayList<String>();
        clazzList.add("test.model.Obj");
        clazzList.add("test.model.Sub");
        clazzList.add("test.model.Ptr");
        clazzList.add("test.model.OO");
        clazzList.add("test.model.OM");
        clazzList.add("test.model.Ign");
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
        Executor ex = Executor.createInsert(person);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(person.getId().longValue(), 1);
        Assert.assertEquals(ret, 5);// 一共写入5个对象

        // update
        person.setAge(20);
        ex = Executor.createUpdate(person);
        ex.where(Cond.byEq("id", person.getId()));
        ret = session.execute(ex);
        Assert.assertEquals(ret, 1);

        // select
        Selector<Obj> selector = Selector.from(Obj.class);
        selector.select("ptr");
        selector.select("oo");
        selector.select("om");

        ArrayList<Integer> inList = new ArrayList<Integer>();
        inList.add(1);
        inList.add(2);
        selector.where(Cond.byIn("id", inList.toArray(new Integer[0])));
        Obj[] res = (Obj[]) session.query(selector);
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getId().intValue(), 1);
        Assert.assertEquals(res[0].getAge().intValue(), 20);
        Assert.assertEquals(res[0].getPtr().getValue().intValue(), 10);
        Assert.assertEquals(res[0].getOo().getValue().intValue(), 100);
        Assert.assertEquals(res[0].getOm()[0].getValue().intValue(), 1000);
        Assert.assertEquals(res[0].getOm()[1].getValue().intValue(), 2000);

        // delete
        ex = Executor.createDelete(person);
        ret = session.execute(ex);
        Assert.assertEquals(ret, 1);

        // delete then select
        Obj obj = session.first(selector);
        Assert.assertEquals(obj, null);

        session.close();
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
        Executor ex = Executor.createInsert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 5);

        {
            Selector<Obj> sr = Selector.createSelect(Obj.class);
            Obj[] objs = (Obj[]) session.query(sr);
            Assert.assertEquals(objs.length, 1);
            Selector<Ptr> sr2 = Selector.createSelect(Ptr.class);
            Ptr[] ptrs = (Ptr[]) session.query(sr2);
            Assert.assertEquals(ptrs.length, 1);
            Selector<OO> sr3 = Selector.createSelect(OO.class);
            OO[] oos = (OO[]) session.query(sr3);
            Assert.assertEquals(oos.length, 1);
            Selector<OM> sr4 = Selector.createSelect(OM.class);
            OM[] oms = (OM[]) session.query(sr4);
            Assert.assertEquals(oms.length, 2);
        }

        ex = Executor.createDelete(obj);
        ex.delete("ptr");
        ex.delete("oo");
        ex.delete("om");
        session.execute(ex);

        {
            Selector<Obj> sr = Selector.createSelect(Obj.class);
            Obj[] objs = (Obj[]) session.query(sr);
            Assert.assertEquals(objs.length, 0);
            Selector<Ptr> sr2 = Selector.createSelect(Ptr.class);
            Ptr[] ptrs = (Ptr[]) session.query(sr2);
            Assert.assertEquals(objs.length, 0);
            Selector<OO> sr3 = Selector.createSelect(OO.class);
            OO[] oos = (OO[]) session.query(sr3);
            Assert.assertEquals(objs.length, 0);
            Selector<OM> sr4 = Selector.createSelect(OM.class);
            OM[] oms = (OM[]) session.query(sr4);
            Assert.assertEquals(objs.length, 0);
        }
    }
}
