package com.yangshan.eship.sales.business.statistics.dto.dashboard;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public enum DashboardSeachKey {
    OID("组织id"),WAREHOUSEID("分公司id"),PRODUCTID("产品id"), PRODUCTNAME("产品名称") ,TIMETYPE("时间类型"),YEARS("对比年列表"),TIMESLOTSTART("时间段开始时间"),TIMESLOTEND("时间段结束时间"), CALTYPE("统计类型");

    private String desc;

    public String getDesc() {
        return desc;
    }

    private DashboardSeachKey(String desc) {
        this.desc = desc;
    }

    public List<String> list() {
        List<String> typeList = new ArrayList<>();
        for (DashboardSeachKey type : DashboardSeachKey.values()) {
            typeList.add(type.getDesc());
        }
        return typeList;
    }
    public static Map<String, String> mapList() {
        Map<String, String> map = new LinkedHashMap<>();

        for (DashboardSeachKey type : DashboardSeachKey.values()) {
            map.put(type.name(), type.getDesc());
        }

        return map;
    }
}
