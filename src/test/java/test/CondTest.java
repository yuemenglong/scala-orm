package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.traits.core.Cond;
import orm.operate.traits.core.ExecuteRoot;
import orm.operate.traits.core.SelectRoot;
import test.model.OM;
import test.model.Obj;
import test.model.Ptr;

import java.util.ArrayList;

/**
 * Created by <yuemenglong@126.com> on 2017/7/11.
 */
public class CondTest {
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
    public void testOr() {
        Session session = db.openSession();
        for (int i = 0; i < 10; i++) {
            Obj obj = new Obj();
            obj.setName("name" + i);
            obj.setPtr(new Ptr());
            obj.getPtr().setValue(i);
            obj.setOm(new OM[]{new OM(), new OM(), new OM()});
            for (OM om : obj.getOm()) {
                om.setValue(i * i);
            }
            obj = Orm.convert(obj);
            ExecuteRoot ex = Orm.insert(obj);
            ex.insert("om");
            session.execute(ex);
        }

        SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
        Cond cond = root.get("id").lt(2).or(root.get("id").gt(9))
                .and(root.select("om").get("id").gt(2));
        Obj[] objs = (Obj[]) session.query(Orm.select(root).from(root).where(cond));
        Assert.assertEquals(objs.length, 2);
        Assert.assertEquals(objs[0].getOm().length, 1);
        Assert.assertEquals(objs[1].getOm().length, 3);
    }

}
