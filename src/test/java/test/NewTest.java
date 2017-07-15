package test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.traits.core.ExecutableRoot;
import test.model.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by <yuemenglong@126.com> on 2017/7/6.
 */
public class NewTest {

    private static Db db;

    @SuppressWarnings("Duplicates")
    @Before
    public void before() {
        Orm.init("");
        db = openDb();
        db.rebuild();
    }

    private Db openDb() {
        return Orm.openDb("localhost", 3306, "root", "root", "test");
    }

    @Test
    public void testExecutor() {
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
        ExecutableRoot ex = Orm.insert(person);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        Assert.assertEquals(person.getId().longValue(), 1);
        Assert.assertEquals(ret, 5);// 一共写入5个对象

        // update
        person.setAge(20);
        ExecutableRoot update = Orm.update(person);
//        ex.where(Cond2.byEq("id", person.getId()));
        ret = session.execute(update);
        Assert.assertEquals(ret, 1);

        // select
//        Root<Obj> root = new Root<>(Obj.class);
//        root.select("ptr");
//        root.select("oo");
//        root.select("om");
//
//        ArrayList<Integer> inList = new ArrayList<Integer>();
//        inList.add(1);
//        inList.add(2);
//        root.where(root.get("id").in(new Integer[]{1, 2}));
//        Obj[] res = (Obj[]) session.query(root);
//        Assert.assertEquals(res.length, 1);
//        Assert.assertEquals(res[0].getId().intValue(), 1);
//        Assert.assertEquals(res[0].getAge().intValue(), 20);
//        Assert.assertEquals(res[0].getPtr().getValue().intValue(), 10);
//        Assert.assertEquals(res[0].getOo().getValue().intValue(), 100);
//        Assert.assertEquals(res[0].getOm()[0].getValue().intValue(), 1000);
//        Assert.assertEquals(res[0].getOm()[1].getValue().intValue(), 2000);

        // delete
        ExecutableRoot delete = Orm.delete(person);
        ret = session.execute(delete);
        Assert.assertEquals(ret, 1);

        // delete then select
//        Obj obj = session.first(root);
//        Assert.assertEquals(obj, null);

        session.close();
    }

}
