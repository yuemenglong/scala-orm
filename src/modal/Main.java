package modal;

import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.entity.EntityManager;
import orm.operate.Cond;
import orm.operate.Executor;
import orm.operate.Selector;

import java.util.List;

/**
 * Created by Administrator on 2017/5/20.
 */
public class Main {
    public static void main(String args[]) {
        Orm.init("");
        Db db = Orm.openDb("localhost", 3306, "root", "root", "test");
        db.rebuild();
        Session session = db.openSession();

        Person person = EntityManager.create(Person.class);
        Ptr ptr = EntityManager.create(Ptr.class);
        OO oo = EntityManager.create(OO.class);
        OM om = EntityManager.create(OM.class);
        OM om2 = EntityManager.create(OM.class);


        person.setAge(10);
        ptr.setValue(10);
        oo.setValue(100);
        om.setValue(1000);
        om2.setValue(2000);

        person.setPtr(ptr);
        person.setOo(oo);
        List oms = person.getOm();
        oms.add(om);
        oms.add(om2);
        person.setOm(oms);

        Executor ex = Executor.createInsert(person);

        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        System.out.println(ret);

        Selector selector = Selector.from(Person.class);
        selector.select("ptr");
        selector.select("oo");
        selector.select("om");

        selector.where(Cond.byEq("id", 1));
        String sql = selector.getSql();
        System.out.println(sql);
        List list = session.query(selector);
        for (Object obj : list) {
            System.out.println(obj);
        }
        session.close();
    }
}
