package io.github.yuemenglong.orm.test.model;

import io.github.yuemenglong.orm.lang.anno.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by <yuemenglong@126.com> on 2017/5/17.
 */
@Entity
public class Obj {

    public Obj() {
    }

    public Obj(String name) {
        this.name = name;
    }

    @Id(auto = true)
    Long id = null;

    @Column(name = "age_")
    Integer age = null;

    @TinyInt
    Integer tinyAge = null;

    Double doubleValue = null;

    @Column(precision = 5, scale = 2)
    BigDecimal price = null;

    @Column(length = 128, nullable = false)
    String name = null;

    Date birthday = null;

    Boolean married = false;

    @LongText
    String longText = null;

    @DateTime
    Date nowTime = null;

    @Ignore
    Integer ignValue = null;

    @Ignore
    Ign ign = null;

    @Ignore
    Ign[] igns = new Ign[0];

    @Pointer
    Ptr ptr = null;

    @OneToOne
    OO oo = null;

    @OneToMany
    OM[] om = new OM[0];

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

    public Ign[] getIgns() {
        return igns;
    }

    public void setIgns(Ign[] igns) {
        this.igns = igns;
    }

    public OM[] getOm() {
        return om;
    }

    public void setOm(OM[] om) {
        this.om = om;
    }

    public String getLongText() {
        return longText;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public void setLongText(String longText) {
        this.longText = longText;
    }
}
