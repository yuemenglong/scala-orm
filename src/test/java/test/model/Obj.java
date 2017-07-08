package test.model;

import orm.lang.anno.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2017/5/17.
 */
@Entity
public class Obj {
    @Id(auto = true)
    Long id = null;

    @Column(name = "age_")
    Integer age = null;

    @Column(precision = 5, scale = 2)
    BigDecimal price = null;

    @Column(length = 128, nullable = false)
    String name = null;

    Date birthday = null;

    Boolean married = false;

    @DateTime
    Date nowTime = null;

    @Ignore
    Integer ignValue = null;

    @Ignore
    Ign ign = null;

    @Ignore
    List<Ign> igns = new ArrayList<Ign>();

    @Pointer
    Ptr ptr = null;

    @OneToOne
    OO oo = null;

    @OneToMany
    List<OM> om = new ArrayList<OM>();

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

    public Boolean getMarried() {
        return married;
    }

    public void setMarried(Boolean married) {
        this.married = married;
    }

    public Integer getIgnValue() {
        return ignValue;
    }

    public void setIgnValue(Integer ignValue) {
        this.ignValue = ignValue;
    }

    public Ign getIgn() {
        return ign;
    }

    public void setIgn(Ign ign) {
        this.ign = ign;
    }

    public List<Ign> getIgns() {
        return igns;
    }

    public void setIgns(List<Ign> igns) {
        this.igns = igns;
    }
}
