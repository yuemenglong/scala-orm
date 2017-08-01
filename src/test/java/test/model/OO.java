package test.model;

import io.github.yuemenglong.orm.lang.anno.Entity;
import io.github.yuemenglong.orm.lang.anno.Id;

/**
 * Created by Administrator on 2017/5/19.
 */
@Entity
public class OO {
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
