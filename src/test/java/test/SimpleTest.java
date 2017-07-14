package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.*;
import test.model.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Administrator on 2017/7/6.
 */
// TODO: update spec field

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
        Insert ex = new Insert(person);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(person.getId().longValue(), 1);
        Assert.assertEquals(ret, 5);// 一共写入5个对象

        // update
        person.setAge(20);
        Update update = new Update(person);
//        ex.where(Cond2.byEq("id", person.getId()));
        ret = session.execute(update);
        Assert.assertEquals(ret, 1);

        // select
        Root<Obj> root = new Root<>(Obj.class);
        root.select("ptr");
        root.select("oo");
        root.select("om");

        ArrayList<Integer> inList = new ArrayList<Integer>();
        inList.add(1);
        inList.add(2);
        root.where(root.get("id").in(new Integer[]{1, 2}));
        Obj[] res = (Obj[]) session.query(root);
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getId().intValue(), 1);
        Assert.assertEquals(res[0].getAge().intValue(), 20);
        Assert.assertEquals(res[0].getPtr().getValue().intValue(), 10);
        Assert.assertEquals(res[0].getOo().getValue().intValue(), 100);
        Assert.assertEquals(res[0].getOm()[0].getValue().intValue(), 1000);
        Assert.assertEquals(res[0].getOm()[1].getValue().intValue(), 2000);

        // delete
        Delete delete = new Delete(person);
        ret = session.execute(delete);
        Assert.assertEquals(ret, 1);

        // delete then select
        Obj obj = session.first(root);
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
        Insert ex = new Insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 5);

        {
            Root<Obj> sr = new Root<>(Obj.class);
            Obj[] objs = (Obj[]) session.query(sr);
            Assert.assertEquals(objs.length, 1);
            Root<Ptr> sr2 = new Root<>(Ptr.class);
            Ptr[] ptrs = (Ptr[]) session.query(sr2);
            Assert.assertEquals(ptrs.length, 1);
            Root<OO> sr3 = new Root<>(OO.class);
            OO[] oos = (OO[]) session.query(sr3);
            Assert.assertEquals(oos.length, 1);
            Root<OM> sr4 = new Root<>(OM.class);
            OM[] oms = (OM[]) session.query(sr4);
            Assert.assertEquals(oms.length, 2);
        }

        Delete delete = new Delete(obj);
        delete.delete("ptr");
        delete.delete("oo");
        delete.delete("om");
        session.execute(delete);

        {
            Root<Obj> sr = new Root<>(Obj.class);
            Obj[] objs = (Obj[]) session.query(sr);
            Assert.assertEquals(objs.length, 0);
            Root<Ptr> sr2 = new Root<>(Ptr.class);
            Ptr[] ptrs = (Ptr[]) session.query(sr2);
            Assert.assertEquals(objs.length, 0);
            Root<OO> sr3 = new Root<>(OO.class);
            OO[] oos = (OO[]) session.query(sr3);
            Assert.assertEquals(objs.length, 0);
            Root<OM> sr4 = new Root<>(OM.class);
            OM[] oms = (OM[]) session.query(sr4);
            Assert.assertEquals(objs.length, 0);
        }
    }

    @Test
    public void testOrderByLimitOffset() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("");
        obj.setOm(new OM[]{new OM(), new OM(), new OM(), new OM(), new OM(), new OM()});

        obj = Orm.convert(obj);
        Insert ex = new Insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 7);

        Root<OM> root = new Root<>(OM.class);
        root.desc(root.get("id")).limit(3).offset(2);

        OM[] oms = (OM[]) session.query(root);
        Assert.assertEquals(oms.length, 3);
        Assert.assertEquals(oms[0].getId().intValue(), 4);
        Assert.assertEquals(oms[1].getId().intValue(), 3);
        Assert.assertEquals(oms[2].getId().intValue(), 2);
    }

    @Test
    public void testMultiSelect() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("");
        obj.setOm(new OM[]{new OM(), new OM(), new OM(), new OM(), new OM(), new OM()});

        obj = Orm.convert(obj);
        Insert ex = new Insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 7);

        Root<OM> ms = new Root<>(OM.class);
        Count_<Long> count = ms.count(Long.class);
        Long c = session.first(count);
        Assert.assertEquals(c.intValue(), 6);
    }


}
