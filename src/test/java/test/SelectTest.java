package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.traits.Query1;
import orm.operate.traits.core.ExecuteRoot;
import orm.operate.traits.core.SelectRoot;
import test.model.OM;
import test.model.Obj;

import java.util.ArrayList;

/**
 * Created by <yuemenglong@126.com> on 2017/7/10.
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
        obj.setName("");
        obj.setOm(new OM[]{new OM(), new OM(), new OM(), new OM(), new OM(), new OM()});

        obj = Orm.convert(obj);
        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 7);

        SelectRoot<OM> ms = Orm.root(OM.class).asSelect();
        Query1<Long> query = Orm.select(ms.count()).from(ms);
        Long c = session.first(query);
        Assert.assertEquals(c.intValue(), 6);
    }

    @Test
    public void testField() {
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

        SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
        Query1<Long> query = Orm.select(root.join("om").get("id").as(Long.class)).from(root)
                .limit(1).offset(5);
        Long c = session.first(query);
        Assert.assertEquals(c.intValue(), 6);
    }
//
//    @Test
//    public void testMultiTarget() {
//        Session session = db.openSession();
//        Obj obj = new Obj();
//        obj.setName("name");
//        obj.setPtr(new Ptr());
//        obj.setOo(new OO());
//        obj.setOm(new OM[]{new OM(), new OM()});
//        obj = Orm.convert(obj);
//        Insert ex = Orm.insert(obj);
//        ex.insert("ptr");
//        ex.insert("oo");
//        ex.insert("om");
//        session.execute(ex);
//
//        Root<Obj> rs = Orm.root(Obj.class);
//        JoinT<OM> s1 = rs.join("om", OM.class);
//        Object[][] res = Selector.query(new Target[]{rs, s1}, db.openConnection());
//        Assert.assertEquals(res.length, 2);
//        Assert.assertEquals(((Obj) (res[0][0])).getName(), "name");
//        Assert.assertEquals(((Obj) (res[1][0])).getName(), "name");
//        Assert.assertEquals(((Obj) (res[0][0])).getId().longValue(), 1);
//        Assert.assertEquals(((Obj) (res[1][0])).getId().longValue(), 1);
//        Assert.assertEquals(((OM) (res[0][1])).getId().longValue(), 1);
//        Assert.assertEquals(((OM) (res[1][1])).getId().longValue(), 2);
//    }
//
//    @Test
//    public void testMultiTarget2() {
//        Session session = db.openSession();
//        Obj obj = new Obj();
//        obj.setName("name");
//        obj.setPtr(new Ptr());
//        obj.setOo(new OO());
//        obj.setOm(new OM[]{new OM(), new OM()});
//        obj = Orm.convert(obj);
//        Insert ex = Orm.insert(obj);
//        ex.insert("ptr");
//        ex.insert("oo");
//        ex.insert("om");
//        session.execute(ex);
//
//        Root<Obj> rs = Orm.root(Obj.class);
//        JoinT<OM> s1 = rs.join("om", OM.class);
//        Tuple2<Obj, OM>[] res = Selector.query(rs, s1, db.openConnection());
//        Assert.assertEquals(res.length, 2);
//        Assert.assertEquals(res[0]._1().getId().longValue(), 1);
//        Assert.assertEquals(res[1]._1().getId().longValue(), 1);
//        Assert.assertEquals(res[0]._1().getName(), "name");
//        Assert.assertEquals(res[1]._1().getName(), "name");
//        Assert.assertEquals(res[0]._2().getId().longValue(), 1);
//        Assert.assertEquals(res[1]._2().getId().longValue(), 2);
//    }
//
//    @Test
//    public void testJoin() {
//        Session session = db.openSession();
//        Obj obj = new Obj();
//        obj.setName("name");
//        obj.setPtr(new Ptr());
//        obj.setOo(new OO());
//        obj.setOm(new OM[]{new OM(), new OM()});
//        obj.getOm()[0].setMo(new MO());
//        obj.getOm()[0].getMo().setValue(100);
//        obj = Orm.convert(obj);
//        Insert ex = Orm.insert(obj);
//        ex.insert("ptr");
//        ex.insert("oo");
//        ex.insert("om").insert("mo");
//        int ret = session.execute(ex);
//        Assert.assertEquals(ret, 6);
//
//        Root<Obj> root = Orm.root(Obj.class);
//        JoinT<MO> mo = root.join("om").join("mo", MO.class);
//        Tuple2<Obj, MO>[] res = session.query(root, mo);
//        Assert.assertEquals(res.length, 2);
//        Assert.assertEquals(res[0]._1().getPtr(), null);
//        Assert.assertArrayEquals(res[0]._1().getOm(), null);
//        Assert.assertEquals(res[0]._2().getValue().intValue(), 100);
//        Assert.assertEquals(res[1]._2(), null);
//    }
//
//    @Test
//    public void testDistinctCount() {
//        Session session = db.openSession();
//        Obj obj = new Obj();
//        obj.setName("name");
//        obj.setPtr(new Ptr());
//        obj.setOo(new OO());
//        obj.setOm(new OM[]{new OM(), new OM(), new OM()});
//        obj = Orm.convert(obj);
//        Insert ex = Orm.insert(obj);
//        ex.insert("ptr");
//        ex.insert("oo");
//        ex.insert("om").insert("mo");
//        int ret = session.execute(ex);
//        Assert.assertEquals(ret, 6);
//
//        {
//            Root<OM> root = Orm.root(OM.class);
//            Count<Long> count = root.count(root.get("objId"), Long.class);
//            Long res = session.first(count);
//            Assert.assertEquals(res.longValue(), 1);
//        }
//        {
//            Root<OM> root = Orm.root(OM.class);
//            Count_<Long> count = root.count(Long.class);
//            Long res = session.first(count);
//            Assert.assertEquals(res.longValue(), 3);
//        }
//    }
//
//    @Test
//    public void testGetTarget() {
//        Session session = db.openSession();
//        for (int i = 0; i < 2; i++) {
//            Obj obj = new Obj();
//            obj.setName("name" + i);
//            obj = Orm.convert(obj);
//            Insert ex = Orm.insert(obj);
//            int ret = session.execute(ex);
//            Assert.assertEquals(ret, 1);
//        }
//
//        Root<Obj> root = Orm.root(Obj.class);
//        FieldT<Long> id = root.get("id", Long.class);
//        FieldT<String> name = root.get("name", String.class);
//
//        Field a = root.get("id");
//        FieldT<Long> b = root.get("id", Long.class);
//        Assert.assertEquals(a, b);
//
//        Tuple2<Long, String>[] res = session.query(id, name);
//        Assert.assertEquals(res.length, 2);
//        Assert.assertEquals(res[0]._1().longValue(), 1);
//        Assert.assertEquals(res[1]._1().longValue(), 2);
//        Assert.assertEquals(res[0]._2(), "name0");
//        Assert.assertEquals(res[1]._2(), "name1");
//    }
}
