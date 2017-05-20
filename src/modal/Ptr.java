package modal;

import orm.java.anno.Entity;
import orm.java.anno.Id;

/**
 * Created by Administrator on 2017/5/19.
 */
@Entity
public class Ptr {
    @Id(auto = true)
    private Long id;

    private String value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
