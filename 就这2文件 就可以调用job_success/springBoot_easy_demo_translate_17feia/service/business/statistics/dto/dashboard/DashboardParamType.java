package com.yangshan.eship.sales.business.statistics.dto.dashboard;

import com.yangshan.eship.finance.dto.pay.OperateType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum DashboardParamType {
    COMPANY("分公司列表"), PRODUCT("产品列表"),COUNTRY("国家列表"),TIMESLOT("时间段查询"),TIMETYPE("时间类型查询"), CALTYPE("统计类型查询");

    private String desc;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private DashboardParamType(String desc) {
        this.desc = desc;
    }

    public List<String> list() {
        List<String> typeList = new ArrayList<>();
        for (DashboardParamType type : DashboardParamType.values()) {
            typeList.add(type.getDesc());
        }
        return typeList;
    }
    public static Map<String, String> mapList() {
        Map<String, String> map = new LinkedHashMap<String, String>();

        for (DashboardParamType type : DashboardParamType.values()) {
            map.put(type.name(), type.getDesc());
        }

        return map;
    }
}
