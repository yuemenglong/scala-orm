package io.github.yuemenglong.orm.test.model;

import io.github.yuemenglong.orm.lang.anno.Entity;
import io.github.yuemenglong.orm.lang.anno.Id;
import io.github.yuemenglong.orm.lang.anno.OneToOne;
import io.github.yuemenglong.orm.lang.anno.Pointer;

/**
 * Created by Administrator on 2017/5/19.
 */
@Entity
public class Ptr {
    @Id(auto = true)
    private Long id;

    private Integer value;

    @OneToOne
    private Obj obj;

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

    public Obj getObj() {
        return obj;
    }

    public void setObj(Obj obj) {
        this.obj = obj;
    }
}
