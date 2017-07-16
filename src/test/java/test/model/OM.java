package test.model;

import orm.lang.anno.Entity;
import orm.lang.anno.Id;
import orm.lang.anno.OneToMany;
import orm.lang.anno.Pointer;

/**
 * Created by <yuemenglong@126.com> on 2017/5/19.
 */
@Entity
public class OM {
    @Id(auto = true)
    private Long id;

    private Integer value;

    @Pointer
    private MO mo;

    @Pointer
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

    public MO getMo() {
        return mo;
    }

    public void setMo(MO mo) {
        this.mo = mo;
    }

}
