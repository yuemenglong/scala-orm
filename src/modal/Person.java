package modal;

import orm.java.anno.Entity;
import orm.java.anno.Id;
import orm.java.anno.Pointer;

/**
 * Created by Administrator on 2017/5/17.
 */
@Entity
public class Person {
    @Id(auto = true)
    Long id = null;

    Integer age = null;

    String name = null;

    @Pointer
    Ptr ptr = null;

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ptr getPtr() {
        return ptr;
    }

    public void setPtr(Ptr ptr) {
        this.ptr = ptr;
    }
}
