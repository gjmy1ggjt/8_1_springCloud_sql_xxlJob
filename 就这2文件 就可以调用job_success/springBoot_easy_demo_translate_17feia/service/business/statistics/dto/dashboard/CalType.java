package com.yangshan.eship.sales.business.statistics.dto.dashboard;

/**
 * @Author: Kevin
 * @Date: 2018-12-29 17:12
 * @Description:
 */
public enum CalType {
    ORDERCOUNT("订单数量"), ORDERWEIGHT("货物总重");

    private String desc;

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private CalType(String desc) {
        this.desc = desc;
    }

}
