package io.github.yuemenglong.orm.test.model;

import io.github.yuemenglong.orm.lang.anno.Entity;
import io.github.yuemenglong.orm.lang.anno.Id;
import io.github.yuemenglong.orm.lang.anno.Pointer;

/**
 * Created by <yuemenglong@126.com> on 2017/5/19.
 */
@Entity(db = "test")
public class OM {
    @Id(auto = true)
    private Long id;

    private Integer value;

    @Pointer
    private MO mo;

    @Pointer
    private Obj obj;

    private Long subId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public MO getMo() {
        return mo;
    }

    public void setMo(MO mo) {
        this.mo = mo;
    }

    public Long getSubId() {
        return subId;
    }

    public void setSubId(Long subId) {
        this.subId = subId;
    }
}
