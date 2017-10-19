package io.github.yuemenglong.orm.test;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import io.github.yuemenglong.orm.test.model._ScalaObj;
import io.github.yuemenglong.orm.Orm;

/**
 * Created by <yuemenglong@126.com> on 2017/7/20.
 */
public class ScalaTest {
    @Test
    public void testScala() {
        Orm.init("io.github.yuemenglong.orm.test.model");
        _ScalaObj obj = Orm.create(_ScalaObj.class);
        obj.id_$eq(1L);
        Assert.assertEquals(obj.id().longValue(), 1);
    }

    @After
    public void after() {
        Orm.clear();
    }
}
