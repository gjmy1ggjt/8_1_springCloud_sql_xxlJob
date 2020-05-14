package com.yangshan.eship.sales.business.statistics.dto;

/**
 * 首页图表其它总计类返回VO
 */
public class StatisticsOtherDto {
    private Integer id;
    private String name;
    private String value;
    private String unit;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
