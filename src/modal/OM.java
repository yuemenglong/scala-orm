package modal;

import orm.java.anno.Entity;
import orm.java.anno.Id;

/**
 * Created by Administrator on 2017/5/19.
 */
@Entity
public class OM {
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
