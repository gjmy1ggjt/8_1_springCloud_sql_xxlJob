package com.yangshan.eship.sales.business.statistics.dto;

/**
 * @author: Yifan
 * @Description:
 * @date: 2019/7/17
 * Modified By:
 */
public enum StatisticsDrawEnum {

    product("产品"), order("订单"), sales("销售"), user("用户"), problem("问题件"), finance("财务");

    private String desc;

    StatisticsDrawEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
