package modal;

import orm.lang.anno.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2017/5/17.
 */
@Entity
public class Person {
    @Id(auto = true)
    Long id = null;

    @Column(name = "age_")
    Integer age = null;

    @Column(precision = 5, scale = 2)
    BigDecimal price = null;

    String name = null;

    Date birthday = null;

    @DateTime
    Date nowTime = null;

    @Pointer
    Ptr ptr = null;

    @OneToOne
    OO oo = null;

    @OneToMany
    List<OM> om = null;

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

    public OO getOo() {
        return oo;
    }

    public void setOo(OO oo) {
        this.oo = oo;
    }

    public List<OM> getOm() {
        return om;
    }

    public void setOm(List<OM> om) {
        this.om = om;
    }

    public Date getBirthday() {
        return birthday;
    }

    public void setBirthday(Date birthday) {
        this.birthday = birthday;
    }

    public Date getNowTime() {
        return nowTime;
    }

    public void setNowTime(Date nowTime) {
        this.nowTime = nowTime;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
