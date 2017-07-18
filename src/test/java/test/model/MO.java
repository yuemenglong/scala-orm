package test.model;

import yy.orm.lang.anno.Entity;
import yy.orm.lang.anno.Id;

/**
 * Created by Administrator on 2017/7/10.
 */
@Entity
public class MO {
    @Id(auto = true)
    private Long id;

    private Integer value;

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
}
