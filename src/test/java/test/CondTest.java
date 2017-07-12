package test;

import org.junit.Before;
import org.junit.Test;
import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.operate.Cond;
import orm.operate.Executor;
import orm.operate.Root;
import orm.operate.Selector;
import test.model.OM;
import test.model.Obj;
import test.model.Ptr;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Administrator on 2017/7/11.
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
    public void testCond() {
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

        }
    }

}
