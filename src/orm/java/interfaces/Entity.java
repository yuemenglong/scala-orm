package orm.java.interfaces;

import orm.entity.EntityCore;

/**
 * Created by Administrator on 2017/5/18.
 */
public interface Entity {
    default EntityCore $$core() {
        return null;
    }
}
