package com.yangshan.eship.sales.business.statistics.dto;

import com.yangshan.eship.finance.dto.statistics.AxisDataType;
import com.yangshan.eship.finance.dto.statistics.StatisticsDataType;

import java.util.List;

public class StatisticsDrawDto {

    //-------------------- 传入参数--------------------start
    private String searchParamId;
    private String searchStartDateTime;
    private String searchEndDateTime;
    private String drawBusinessName;

    //@Author: HuKai @Date: 2018-06-13 13:24 @Description: 额外参数
    private StatisticsDataType statisticsDataType; //要查看的数据类型
    private AxisDataType axisDataType; //图表横轴的数据类型
    private String content; //额外提示信息
    private String modul;//请求模块

    //-------------------- 传入参数--------------------end

    //-------------------- 返回数据结构--------------------start
    /**
     * 查询参数目前支持下拉列表
     */
    private String paramTitle;
    private List<StatisticParamDto> paramsSelect;

    /**
     * 图表类型（支持4种）
     */
    private String drawType;
    /**
     * 是否显示详情
     */
    private Boolean showDetail;
    /**
     * 访问详情路径
     */
    private String detailUrl;
    /**
     * 饼图返回数据与单柱状图返回数据
     */
    private List<DrawItem> drawPieAndCylinderItems;

    /**
     * 多柱状图返回数据与线行图返回数据
     */
    private List<StatisticsDrawListDto> drawGroupAndStackLineItems;
    /**
     * 其它总计类返回数据
     */
    private List<StatisticsOtherDto> otherDtoList;
    /**
     * 单位
     */
    private List<DrayUnit> drayUnits;


    public String getSearchParamId() {
        return searchParamId;
    }

    public void setSearchParamId(String searchParamId) {
        this.searchParamId = searchParamId;
    }

    public String getSearchStartDateTime() {
        return searchStartDateTime;
    }

    public void setSearchStartDateTime(String searchStartDateTime) {
        this.searchStartDateTime = searchStartDateTime;
    }

    public String getSearchEndDateTime() {
        return searchEndDateTime;
    }

    public void setSearchEndDateTime(String searchEndDateTime) {
        this.searchEndDateTime = searchEndDateTime;
    }

    public String getDrawBusinessName() {
        return drawBusinessName;
    }

    public void setDrawBusinessName(String drawBusinessName) {
        this.drawBusinessName = drawBusinessName;
    }

    public List<StatisticParamDto> getParamsSelect() {
        return paramsSelect;
    }

    public void setParamsSelect(List<StatisticParamDto> paramsSelect) {
        this.paramsSelect = paramsSelect;
    }

    public String getDrawType() {
        return drawType;
    }

    public void setDrawType(String drawType) {
        this.drawType = drawType;
    }

    public String getParamTitle() {
        return paramTitle;
    }

    public void setParamTitle(String paramTitle) {
        this.paramTitle = paramTitle;
    }

    public List<DrawItem> getDrawPieAndCylinderItems() {
        return drawPieAndCylinderItems;
    }

    public void setDrawPieAndCylinderItems(List<DrawItem> drawPieAndCylinderItems) {
        this.drawPieAndCylinderItems = drawPieAndCylinderItems;
    }

    public List<StatisticsDrawListDto> getDrawGroupAndStackLineItems() {
        return drawGroupAndStackLineItems;
    }

    public void setDrawGroupAndStackLineItems(List<StatisticsDrawListDto> drawGroupAndStackLineItems) {
        this.drawGroupAndStackLineItems = drawGroupAndStackLineItems;
    }

    public List<DrayUnit> getDrayUnits() {
        return drayUnits;
    }

    public void setDrayUnits(List<DrayUnit> drayUnits) {
        this.drayUnits = drayUnits;
    }


    public StatisticsDataType getStatisticsDataType() {
        return statisticsDataType;
    }

    public void setStatisticsDataType(StatisticsDataType statisticsDataType) {
        this.statisticsDataType = statisticsDataType;
    }

    public AxisDataType getAxisDataType() {
        return axisDataType;
    }

    public void setAxisDataType(AxisDataType axisDataType) {
        this.axisDataType = axisDataType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModul() {
        return modul;
    }

    public void setModul(String modul) {
        this.modul = modul;
    }

    public Boolean getShowDetail() {
        return showDetail;
    }

    public void setShowDetail(Boolean showDetail) {
        this.showDetail = showDetail;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl;
    }

    public List<StatisticsOtherDto> getOtherDtoList() {
        return otherDtoList;
    }

    public void setOtherDtoList(List<StatisticsOtherDto> otherDtoList) {
        this.otherDtoList = otherDtoList;
    }
}
