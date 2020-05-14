package com.yangshan.eship.sales.business.statistics.dto;

import java.util.HashMap;
import java.util.Map;

public class DrayUnit {
    /**
     * 类型一般固定value
     */
    private String type="value";
    /**
     * 单位名称
     */
    private String name="";
    /**
     * 最小值
     */
    private Integer min=0;


    /**
     * axisLabel.put("formatter","{value}元");格式
     */
    private Map<String,String> axisLabel = new HashMap<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMin() {
        return min;
    }

    public void setMin(Integer min) {
        this.min = min;
    }


    public Map<String, String> getAxisLabel() {
        return axisLabel;
    }

    public void setAxisLabel(Map<String, String> axisLabel) {
        this.axisLabel = axisLabel;
    }
}
