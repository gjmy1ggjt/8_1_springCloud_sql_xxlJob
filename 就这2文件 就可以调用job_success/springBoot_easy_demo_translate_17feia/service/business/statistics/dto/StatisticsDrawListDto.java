package com.yangshan.eship.sales.business.statistics.dto;

import java.util.List;

public class StatisticsDrawListDto {
    /**
     * 二维图形横轴名称
     */
    private String name;
    /**
     * 双折线用
     */
    private Integer yAxisIndex;
    /**
     * 横轴对应的数据
     */
    private List<DrawItem> drawItemList;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<DrawItem> getDrawItemList() {
        return drawItemList;
    }

    public void setDrawItemList(List<DrawItem> drawItemList) {
        this.drawItemList = drawItemList;
    }

    public Integer getyAxisIndex() {
        return yAxisIndex;
    }

    public void setyAxisIndex(Integer yAxisIndex) {
        this.yAxisIndex = yAxisIndex;
    }
}
