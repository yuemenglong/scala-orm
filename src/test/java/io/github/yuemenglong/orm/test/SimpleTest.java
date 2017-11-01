package io.github.yuemenglong.orm.test;

import io.github.yuemenglong.orm.test.model.*;
import io.github.yuemenglong.orm.Orm;
import io.github.yuemenglong.orm.Session.Session;
import io.github.yuemenglong.orm.db.Db;
import io.github.yuemenglong.orm.operate.traits.Query;
import io.github.yuemenglong.orm.operate.traits.core.ExecuteRoot;
import io.github.yuemenglong.orm.operate.traits.core.Root;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by <yuemenglong@126.com> on 2017/7/6.
 */
public class SimpleTest {

    private static Db db;

    @SuppressWarnings("Duplicates")
    @Before
    public void before() {
        Orm.init("io.github.yuemenglong.orm.test.model");
        db = openDb();
        db.rebuild();
    }

    @After
    public void after() {
        Orm.clear();
        db.shutdown();
    }

    private Db openDb() {
        return Orm.openDb("localhost", 3306, "root", "root", "test");
    }

    @Test
    public void testConvert() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        obj.setOm(new OM[]{new OM(), new OM()});
        obj = Orm.convert(obj);
        OM[] om = obj.getOm();

        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("om");
        session.execute(ex);

        Root<Obj> root = Orm.root(Obj.class);
        root.select("om");
        Obj res = session.first(Orm.select(root).from(root));
        om = res.getOm();
        session.close();
    }

    @Test
    public void testCURD() {
        String longText = "looooooooooooooooooooooooooooooooooooooooooooong";
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
        person.setDoubleValue(1.2);
        person.setPrice(new BigDecimal(123.45));
        person.setLongText(longText);

        ptr.setValue(10);
        oo.setValue(100);
        om.setValue(1000);
        om2.setValue(2000);

        // 忽略的数据
        person.setIgnValue(0);
//        person.setIgn(Orm.create(Ign.class));
        person.setIgn(new Ign());

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
        Root<Obj> root = Orm.root(Obj.class);
        root.select("ptr");
        root.select("oo");
        root.select("om");

        Query<Obj> query = Orm.select(root).from(root).where(root.get("id").in(new Integer[]{1, 2,}));
        Obj[] res = (Obj[]) session.query(query);
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getId().intValue(), 1);
        Assert.assertEquals(res[0].getLongText(), longText);
        Assert.assertEquals(res[0].getDoubleValue(), 1.2, 0.0000000001);
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

        Root<Obj> root = Orm.root(Obj.class);
        obj = session.first(Orm.select(root).from(root));
        Assert.assertEquals(obj.getName(), "name");
        Assert.assertEquals(obj.getAge().intValue(), 200);
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
        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 5);

        {
            Root<Obj> sr = Orm.root(Obj.class);
            Obj[] objs = (Obj[]) session.query(Orm.select(sr).from(sr));
            Assert.assertEquals(objs.length, 1);
            Root<Ptr> sr2 = Orm.root(Ptr.class);
            Ptr[] ptrs = (Ptr[]) session.query(Orm.select(sr2).from(sr2));
            Assert.assertEquals(ptrs.length, 1);
            Root<OO> sr3 = Orm.root(OO.class);
            OO[] oos = (OO[]) session.query(Orm.selectFrom(sr3));
            Assert.assertEquals(oos.length, 1);
            Root<OM> sr4 = Orm.root(OM.class);
            OM[] oms = (OM[]) session.query(Orm.selectFrom(sr4));
            Assert.assertEquals(oms.length, 2);
        }

        ExecuteRoot delete = Orm.delete(obj);
        delete.delete("ptr");
        delete.delete("oo");
        delete.delete("om");
        session.execute(delete);

        {
            Root<Obj> sr = Orm.root(Obj.class);
            Obj[] objs = (Obj[]) session.query(Orm.select(sr).from(sr));
            Assert.assertEquals(objs.length, 0);
            Root<Ptr> sr2 = Orm.root(Ptr.class);
            Ptr[] ptrs = (Ptr[]) session.query(Orm.select(sr2).from(sr2));
            Assert.assertEquals(ptrs.length, 0);
            Root<OO> sr3 = Orm.root(OO.class);
            OO[] oos = (OO[]) session.query(Orm.selectFrom(sr3));
            Assert.assertEquals(oos.length, 0);
            Root<OM> sr4 = Orm.root(OM.class);
            OM[] oms = (OM[]) session.query(Orm.selectFrom(sr4));
            Assert.assertEquals(oms.length, 0);
        }
        session.close();
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

        Root<OM> root = Orm.root(OM.class);
        Query<OM> query = Orm.select(root).from(root).desc(root.get("id")).limit(3).offset(2);

        OM[] oms = (OM[]) session.query(query);
        Assert.assertEquals(oms.length, 3);
        Assert.assertEquals(oms[0].getId().intValue(), 4);
        Assert.assertEquals(oms[1].getId().intValue(), 3);
        Assert.assertEquals(oms[2].getId().intValue(), 2);
        session.close();
    }
}