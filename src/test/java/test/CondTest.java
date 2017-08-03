package test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import io.github.yuemenglong.orm.Orm;
import io.github.yuemenglong.orm.Session.Session;
import io.github.yuemenglong.orm.db.Db;
import io.github.yuemenglong.orm.operate.traits.Query;
import io.github.yuemenglong.orm.operate.traits.core.Cond;
import io.github.yuemenglong.orm.operate.traits.core.ExecuteRoot;
import io.github.yuemenglong.orm.operate.traits.core.SelectRoot;
import test.model.OM;
import test.model.Obj;
import test.model.Ptr;

/**
 * Created by <yuemenglong@126.com> on 2017/7/11.
 */
public class CondTest {
    private static Db db;

    @SuppressWarnings("Duplicates")
    @Before
    public void before() {
        Orm.init("test.model");
        db = openDb();
        db.rebuild();
    }

    @After
    public void after() {
        Orm.clear();
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

    @Test
    public void testNotNull() {
        Session session = db.openSession();
        Obj obj = new Obj();
        obj.setName("name");

        session.execute(Orm.insert(Orm.convert(obj)));

        obj = new Obj();
        obj.setName("name2");
        obj.setAge(10);

        session.execute(Orm.insert(Orm.convert(obj)));

        SelectRoot<Obj> root = Orm.root(Obj.class).asSelect();
        Query<Obj> query = Orm.select(root).from(root).where(root.get("age").isNull());
        Obj[] res = (Obj[]) session.query(query);
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getId().longValue(), 1);
        Assert.assertEquals(res[0].getAge(), null);

        query = Orm.select(root).from(root).where(root.get("age").notNull());
        res = (Obj[]) session.query(query);
        Assert.assertEquals(res.length, 1);
        Assert.assertEquals(res[0].getId().longValue(), 2);
        Assert.assertEquals(res[0].getAge().longValue(), 10);
    }
}
