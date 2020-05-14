package com.yangshan.eship.sales.business.statistics.dto.dashboard;


import java.util.List;

public class DashboardParamsDto {
    private String title;//标题
    private DashboardParamType paramType;//查询控件类型
    private List<Param> selects;//返回查询数据列表
    private String seachValue;//选中查询项
    private String timeSlotStart;//开始时间
    private String timeSlotEnd;//结束时间
    private List<Integer> years;//对比年
    private String timeType;//对比刻度类型
    private String productName;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public DashboardParamType getParamType() {
        return paramType;
    }

    public void setParamType(DashboardParamType paramType) {
        this.paramType = paramType;
    }

    public List<Param> getSelects() {
        return selects;
    }

    public void setSelects(List<Param> selects) {
        this.selects = selects;
    }

    public String getSeachValue() {
        return seachValue;
    }

    public void setSeachValue(String seachValue) {
        this.seachValue = seachValue;
    }

    public String getTimeSlotStart() {
        return timeSlotStart;
    }

    public void setTimeSlotStart(String timeSlotStart) {
        this.timeSlotStart = timeSlotStart;
    }

    public String getTimeSlotEnd() {
        return timeSlotEnd;
    }

    public void setTimeSlotEnd(String timeSlotEnd) {
        this.timeSlotEnd = timeSlotEnd;
    }

    public List<Integer> getYears() {
        return years;
    }

    public void setYears(List<Integer> years) {
        this.years = years;
    }

    public String getTimeType() {
        return timeType;
    }

    public void setTimeType(String timeType) {
        this.timeType = timeType;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }
}
