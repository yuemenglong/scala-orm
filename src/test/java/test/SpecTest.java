package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.github.yuemenglong.orm.Orm;
import io.github.yuemenglong.orm.Session.Session;
import io.github.yuemenglong.orm.db.Db;
import io.github.yuemenglong.orm.operate.traits.ExecutableInsert;
import io.github.yuemenglong.orm.operate.traits.core.*;
import test.model.OM;
import test.model.Obj;

import java.util.ArrayList;

/**
 * Created by <yuemenglong@126.com> on 2017/7/11.
 */
public class SpecTest {
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
    public void testUpdate() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("");
        obj.setOm(new OM[]{new OM(), new OM(), new OM(), new OM(), new OM(), new OM()});

        obj = Orm.convert(obj);
        ExecuteRoot er = Orm.insert(obj);
        er.insert("ptr");
        er.insert("oo");
        er.insert("om");
        int ret = session.execute(er);
        Assert.assertEquals(ret, 7);

        Root<OM> root = Orm.root(OM.class);
        Executable ex = Orm.update(root).set(root.get("objId").assign(2))
                .where(root.get("id").gt(4));
        ret = session.execute(ex);
        Assert.assertEquals(ret, 2);

        obj = new Obj();
        obj.setName("name2");

        session.execute(Orm.insert(Orm.convert(obj)));
        SelectRoot<Obj> objRoot = Orm.root(Obj.class).asSelect();
        objRoot.select("om");
        Obj[] res = (Obj[]) session.query(Orm.from(objRoot));
        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0].getOm().length, 4);
        Assert.assertEquals(res[1].getOm().length, 2);
    }

    @Test
    public void testDelete() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("");
        obj.setOm(new OM[]{new OM(), new OM(), new OM(), new OM(), new OM(), new OM()});

        Obj ent = Orm.convert(obj);
        ExecuteRoot er = Orm.insert(ent);
        er.insert("ptr");
        er.insert("oo");
        er.insert("om");
        int ret = session.execute(er);
        Assert.assertEquals(ret, 7);

        ent = Orm.convert(obj);
        er = Orm.insert(ent);
        er.insert("ptr");
        er.insert("oo");
        er.insert("om");
        ret = session.execute(er);
        Assert.assertEquals(ret, 7);

        Root<OM> root = Orm.root(OM.class);
        ret = session.execute(Orm.delete(root).where
                (root.join("obj").get("id").gt(1).or(root.get("id").lt(3))));
        Assert.assertEquals(ret, 8);
    }

    @Test
    public void testBatchInsert() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");
        Obj[] objs = (Obj[]) Orm.converts(new Obj[]{obj, obj, obj});
        ExecutableInsert<Obj> ex = Orm.insert(Obj.class).values(objs);
        int ret = session.execute(ex);
        Assert.assertEquals(ret, 3);
    }
}
