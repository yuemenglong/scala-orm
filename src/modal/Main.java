package modal;

import orm.Orm;
import orm.Session.Session;
import orm.db.Db;
import orm.entity.EntityManager;
import orm.operate.Cond;
import orm.operate.Executor;
import orm.operate.Selector;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2017/5/20.
 */
public class Main {
    public static void main(String args[]) {
        ArrayList clazzList = new ArrayList<String>();
        clazzList.add("modal.Person");
        clazzList.add("modal.Male");
        clazzList.add("modal.Ptr");
        clazzList.add("modal.OO");
        clazzList.add("modal.OM");
        clazzList.add("modal.Ign");
        Orm.init(clazzList);

        Db db = Orm.openDb("localhost", 3306, "root", "root", "test");
        db.rebuild();
        Session session = db.openSession();

        Person person = new Person();
        Ptr ptr = new Ptr();
        OO oo = new OO();
        OM om = new OM();
        OM om2 = new OM();

        person.setAge(10);
        person.setName("/TOM");
        person.setBirthday(new Date());
        person.setNowTime(new Date());
        person.setPrice(new BigDecimal(123.45));


        person.setIgnValue(0);
        person.setIgn(Orm.create(Ign.class));

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

        person = Orm.convert(person);

        String json = Orm.stringify(person);
        System.out.println(json);
        person = Orm.parse(Person.class, json);
        System.out.println(person);

        Executor ex = Executor.createInsert(person);

        ex.insert("ptr");
        ex.insert("oo");
        ex.insert("om");
        int ret = session.execute(ex);
        System.out.println(ret);

        person.setAge(20);
        ex = Executor.createUpdate(person);
        ex.where(Cond.byEq("id", person.getId()));
        session.execute(ex);

        Selector selector = Selector.from(Person.class);
        selector.select("ptr");
        selector.select("oo");
        selector.select("om");

        ArrayList<Integer> inList = new ArrayList<>();
        inList.add(1);
        inList.add(2);
        selector.where(Cond.byIn("id", inList));
        String sql = selector.getSql();
        Collection coll = session.query(selector);
        for (Object obj : coll) {
            System.out.println(obj);
        }

        ex = Executor.createDelete(person);
        ret = session.execute(ex);
        System.out.println(ret);

        coll = session.query(selector);
        System.out.println(coll.size());

        session.close();
    }
}
