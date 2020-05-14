package com.yangshan.eship.sales.business.statistics;


import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;

import java.text.ParseException;
import java.util.Map;

/**
 * @Author: tsding
 * @Description:画统计图接口
 * @Date: 10:15 2018/06/01
 * Modified By:
 */
public interface OneStatisticsDrawBusinessI {

    StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) throws ParseException;

    //返回元素0：模块名， 1：统计图标识title
    String[] getDrawBusinessName();
}
