package test;

import org.junit.Test;
import orm.Orm;
import orm.operate.Cond;
import orm.operate.Root;
import orm.operate.Selector;
import test.model.Obj;

import java.util.Arrays;

/**
 * Created by Administrator on 2017/7/11.
 */
public class CondTest {
    @Test
    public void test() {
        Orm.init("");
        Root<Obj> root = Selector.createSelect(Obj.class);
        Cond c = root.get("id").eql(1).and(root.get("name").eql("name"));
        c = c.and(root.get("id").eql(root.get("id")));
        System.out.println(c.toSql());
        System.out.println(Arrays.toString(c.toParam()));
    }
}
