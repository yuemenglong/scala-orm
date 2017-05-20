package modal;

import orm.Orm;
import orm.db.Db;
import orm.entity.EntityManager;
import orm.execute.Executor;

/**
 * Created by Administrator on 2017/5/20.
 */
public class Main {
    public static void main(String args[]) {
        Orm.init("");
        Db db = Orm.openDb("localhost", 3306, "root", "root", "test");
        db.rebuild();

        Person person = EntityManager.create(Person.class);
        Ptr ptr = EntityManager.create(Ptr.class);
        OO oo = EntityManager.create(OO.class);
        OM om = EntityManager.create(OM.class);
        OM om2 = EntityManager.create(OM.class);
        Executor ex = Executor.createInsert(Person.class);

        person.setAge(1);
        ptr.setValue(10);
        oo.setValue(100);
        om.setValue(1000);
        om2.setValue(2000);
        person.setPtr(ptr);
        person.setOo(oo);
        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = ex.execute(person, db.getConn());
        System.out.println(ret);
    }
}
