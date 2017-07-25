package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import scala.Tuple2;
import test.model.*;
import yy.orm.Orm;
import yy.orm.Session.Session;
import yy.orm.db.Db;
import yy.orm.operate.traits.Query;
import yy.orm.operate.traits.core.*;

import java.util.ArrayList;

/**
 * Created by <yuemenglong@126.com> on 2017/7/10.
 */
public class SelectTest {
    private static Db db;

    @SuppressWarnings("Duplicates")
    @Before
    public void before() {
        ArrayList<String> clazzList = new ArrayList<>();
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
        Query<Long> query = Orm.select(ms.count()).from(ms);
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
        Query<Long> query = Orm.select(root.join("om").get("id").as(Long.class)).from(root)
                .limit(1).offset(5);
        Long c = session.first(query);
        Assert.assertEquals(c.intValue(), 6);
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
        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        session.execute(ex);

        SelectRoot<Obj> rs = Orm.root(Obj.class).asSelect();
        Selectable<OM> s1 = rs.join("om").as(OM.class);

        Query<Tuple2<Obj, OM>> query = Orm.select(rs, s1).from(rs);
        Tuple2<Obj, OM>[] res = (Tuple2<Obj, OM>[]) session.query(query);

        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0]._1().getName(), "name");
        Assert.assertEquals(res[0]._1().getId().longValue(), 1);
        Assert.assertEquals(res[1]._1().getId().longValue(), 1);
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
        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om").insert("mo");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 6);

        {
            SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
            SelectableJoin<MO> mo = root.join("om").join("mo", JoinType.LEFT()).as(MO.class);
            Query<Tuple2<Obj, MO>> query = Orm.select(root, mo).from(root);
            Tuple2<Obj, MO>[] res = (Tuple2<Obj, MO>[]) session.query(query);
            Assert.assertEquals(res.length, 2);
            Assert.assertEquals(res[0]._1().getPtr(), null);
            Assert.assertArrayEquals(res[0]._1().getOm(), new OM[0]);
            Assert.assertEquals(res[0]._2().getValue().intValue(), 100);
            Assert.assertEquals(res[1]._2(), null);
        }
        {
            SelectRoot<OM> root = Orm.root(OM.class).asSelect();
            root.select("mo").on(root.join("mo").get("value").eql(100));
            Query<OM> query = Orm.select(root).from(root);
            OM[] res = (OM[]) session.query(query);
            Assert.assertEquals(res.length, 2);
            Assert.assertEquals(res[0].getMo().getValue().longValue(), 100);
            Assert.assertEquals(res[1].getMo(), null);
        }
        {
            SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
            Join j1 = root.select("om").join("mo");
            Join j2 = root.join("om").join("mo");
            Assert.assertEquals(j1, j2);
        }
    }

    @Test
    public void testSelectOOWithNull() {
        Session session = db.openSession();
        session.execute(Orm.insert(Orm.convert(new Obj("name"))));

        SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
        root.select("ptr");
        root.select("oo");
        root.select("om");
        Query<Obj> query = Orm.select(root).from(root);
        Obj[] res = (Obj[]) session.query(query);
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getPtr(), null);
        Assert.assertEquals(res[0].getOo(), null);
        Assert.assertEquals(res[0].getOm().length, 0);
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
        ExecuteRoot ex = Orm.insert(obj);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om").insert("mo");
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 6);

        {
            SelectRoot<OM> root = Orm.root(OM.class).asSelect();
            Query<Long> query = Orm.select(root.count(root.get("objId"))).from(root);
            Long res = session.first(query);
            Assert.assertEquals(res.longValue(), 3);
        }
        {
            SelectRoot<OM> root = Orm.root(OM.class).asSelect();
            Query<Long> query = Orm.select(root.count(root.get("objId")).distinct()).from(root);
            Long res = session.first(query);
            Assert.assertEquals(res.longValue(), 1);
        }
        {
            SelectRoot<OM> root = Orm.root(OM.class).asSelect();
            Query<Long> query = Orm.select(root.count()).from(root);
            Long res = session.first(query);
            Assert.assertEquals(res.longValue(), 3);
        }
    }
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
