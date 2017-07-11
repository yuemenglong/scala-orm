package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.*;
import scala.Tuple2;
import test.model.*;

import java.util.ArrayList;

/**
 * Created by Administrator on 2017/7/10.
 */
public class SelectTest {
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
    public void testCount() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        obj.setPtr(new Ptr());
        obj.setOo(new OO());
        obj.setOm(new OM[]{new OM(), new OM()});
        obj = Orm.convert(obj);
        Executor ex = Executor.createInsert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        session.execute(ex);

        RootSelector<OM> rs = Selector.createSelect(OM.class);
        AggreSelector<Long> count = rs.count(Long.class);
        Long[] res = (Long[]) Selector.query(count, db.openConnection());
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].longValue(), 2);
    }

    @Test
    public void testSingleTarget() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        obj.setPtr(new Ptr());
        obj.setOo(new OO());
        obj.setOm(new OM[]{new OM(), new OM()});
        obj = Orm.convert(obj);
        Executor ex = Executor.createInsert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        session.execute(ex);

        RootSelector<OM> rs = Selector.createSelect(OM.class);
        OM[] res = (OM[]) Selector.query(rs, db.openConnection());
        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0].getId().longValue(), 1);
        Assert.assertEquals(res[1].getId().longValue(), 2);
    }

    @Test
    public void testMultiTarget() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        obj.setPtr(new Ptr());
        obj.setOo(new OO());
        obj.setOm(new OM[]{new OM(), new OM()});
        obj = Orm.convert(obj);
        Executor ex = Executor.createInsert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        session.execute(ex);

        RootSelector<Obj> rs = Selector.createSelect(Obj.class);
        EntitySelector<OM> s1 = rs.join("om", OM.class);
        Object[][] res = Selector.query(new TargetSelector[]{rs, s1}, db.openConnection());
        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(((Obj) (res[0][0])).getName(), "name");
        Assert.assertEquals(((Obj) (res[1][0])).getName(), "name");
        Assert.assertEquals(((Obj) (res[0][0])).getId().longValue(), 1);
        Assert.assertEquals(((Obj) (res[1][0])).getId().longValue(), 1);
        Assert.assertEquals(((OM) (res[0][1])).getId().longValue(), 1);
        Assert.assertEquals(((OM) (res[1][1])).getId().longValue(), 2);
    }

    @Test
    public void testMultiTarget2() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        obj.setPtr(new Ptr());
        obj.setOo(new OO());
        obj.setOm(new OM[]{new OM(), new OM()});
        obj = Orm.convert(obj);
        Executor ex = Executor.createInsert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        session.execute(ex);

        RootSelector<Obj> rs = Selector.createSelect(Obj.class);
        EntitySelector<OM> s1 = rs.join("om", OM.class);
        Tuple2<Obj, OM>[] res = Selector.query(rs, s1, db.openConnection());
        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0]._1().getId().longValue(), 1);
        Assert.assertEquals(res[1]._1().getId().longValue(), 1);
        Assert.assertEquals(res[0]._1().getName(), "name");
        Assert.assertEquals(res[1]._1().getName(), "name");
        Assert.assertEquals(res[0]._2().getId().longValue(), 1);
        Assert.assertEquals(res[1]._2().getId().longValue(), 2);
    }

    @Test
    public void testJoin() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        obj.setPtr(new Ptr());
        obj.setOo(new OO());
        obj.setOm(new OM[]{new OM(), new OM()});
        obj.getOm()[0].setMo(new MO());
        obj.getOm()[0].getMo().setValue(100);
        obj = Orm.convert(obj);
        Executor ex = Executor.createInsert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om").insert("mo");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 6);

        RootSelector<Obj> root = Selector.createSelect(Obj.class);
        EntitySelector<MO> mo = root.join("om").join("mo", MO.class);
        Tuple2<Obj, MO>[] res = session.query(root, mo);
        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0]._1().getPtr(), null);
        Assert.assertArrayEquals(res[0]._1().getOm(), null);
        Assert.assertEquals(res[0]._2().getValue().intValue(), 100);
        Assert.assertEquals(res[1]._2(), null);
    }

    @Test
    public void testDistinctCount() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        obj.setPtr(new Ptr());
        obj.setOo(new OO());
        obj.setOm(new OM[]{new OM(), new OM(), new OM()});
        obj = Orm.convert(obj);
        Executor ex = Executor.createInsert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om").insert("mo");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 6);

        {
            RootSelector<OM> root = Selector.createSelect(OM.class);
            AggreSelector<Long> count = root.count("objId", Long.class);
            Long res = session.first(count);
            Assert.assertEquals(res.longValue(), 1);
        }
        {
            RootSelector<OM> root = Selector.createSelect(OM.class);
            AggreSelector<Long> count = root.count(Long.class);
            Long res = session.first(count);
            Assert.assertEquals(res.longValue(), 3);
        }
    }

    @Test
    public void testGet() {
        Session session = db.openSession();
        for (int i = 0; i < 2; i++) {
            Obj obj = new Obj();
            obj.setName("name" + i);
            obj = Orm.convert(obj);
            Executor ex = Executor.createInsert(obj);
            int ret = session.execute(ex);
            Assert.assertEquals(ret, 1);
        }

        RootSelector<Obj> root = Selector.createSelect(Obj.class);
        FieldSelector<Long> id = root.get("id", Long.class);
        FieldSelector<String> name = root.get("name", String.class);

        FieldSelectorImpl a = root.get("id");
        FieldSelector<Long> b = root.get("id", Long.class);
        Assert.assertEquals(a, b);

        Tuple2<Long, String>[] res = session.query(id, name);
        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0]._1().longValue(), 1);
        Assert.assertEquals(res[1]._1().longValue(), 2);
        Assert.assertEquals(res[0]._2(), "name0");
        Assert.assertEquals(res[1]._2(), "name1");

    }
}
