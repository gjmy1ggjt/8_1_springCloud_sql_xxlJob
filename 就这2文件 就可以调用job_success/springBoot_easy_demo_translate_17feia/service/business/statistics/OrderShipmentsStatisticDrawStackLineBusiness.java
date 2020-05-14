package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.order.dto.statistics.StatisticsOrderShipmentsDto;
import com.yangshan.eship.order.dto.statistics.StatisticsOrderShipmentsType;
import com.yangshan.eship.order.entity.statistics.StatisticsOrderShipments;
import com.yangshan.eship.order.service.orde.StatisticsOrderShipmentsServiceI;
import com.yangshan.eship.sales.business.statistics.dto.*;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author: LinYun
 * @Description: 订单出货量统计
 * @date: 9:40 2018/6/11
 * Modified By:
 */
@Service
public class OrderShipmentsStatisticDrawStackLineBusiness implements OneStatisticsDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(OrderShipmentsStatisticDrawStackLineBusiness.class);

    @Autowired
    private StatisticsOrderShipmentsServiceI statisticsOrderShipmentsService;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat simpleDateFormatForDay = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) {
        // 直接设置画图方式
        statisticsDrawsDto.setDrawType(StatisticsDrawType.DRAWGSTACKLINE);
        List<StatisticsDrawListDto> list = Lists.newArrayList();
        //region 查询统计数据
        //默认查询一个月的时间
        Date minDate = DateUtil.getBeforeDayStartTime(DateUtil.getPlusDays(new Date(), -30));
        Date maxDate = DateUtil.getBeforeDayEndTime(new Date());
        try {
            if (StringUtils.isNoneEmpty(statisticsDrawsDto.getSearchStartDateTime())) {
                minDate = simpleDateFormat.parse(statisticsDrawsDto.getSearchStartDateTime() + " 00:00:00");
            }
            if (StringUtils.isNoneEmpty(statisticsDrawsDto.getSearchEndDateTime())) {
                maxDate = simpleDateFormat.parse(statisticsDrawsDto.getSearchEndDateTime() + " 23:59:59");
            }
        } catch (ParseException e) {
            logger.error(e.getMessage(), e);
        }
        //默认仓库收货
        StatisticsOrderShipmentsType statisticsOrderShipmentsType = StatisticsOrderShipmentsType.SHIPMENTS_SIGN_FOR_WAREHOUSE;
        if (StringUtils.isEmpty(statisticsDrawsDto.getSearchParamId())) {
            // 为空是首次请求，添加收货类型到下拉框
            statisticsDrawsDto.setParamTitle("收货类型");
            List<StatisticParamDto> paramDtoList = Lists.newArrayList();
            for (String s : StatisticsOrderShipmentsType.list().keySet()) {
                StatisticParamDto statisticParamDto = new StatisticParamDto();
                statisticParamDto.setParamId(s);
                statisticParamDto.setParamName(StatisticsOrderShipmentsType.list().get(s));
                paramDtoList.add(statisticParamDto);
            }
            statisticsDrawsDto.setSearchParamId(statisticsOrderShipmentsType.name());
            statisticsDrawsDto.setParamsSelect(paramDtoList);
        } else {
            statisticsOrderShipmentsType = EnumUtils.getEnum(StatisticsOrderShipmentsType.class, statisticsDrawsDto.getSearchParamId());
        }

        //封装查询条件，查询统计数据
        StatisticsOrderShipmentsDto shipmentsDto = new StatisticsOrderShipmentsDto();
        shipmentsDto.setMaxDate(maxDate);
        shipmentsDto.setMinDate(minDate);
        shipmentsDto.setWarehouseId(SessionUtils.getWarehouseId());
        shipmentsDto.setOrganizationId(SessionUtils.getOrganizationId());
        shipmentsDto.setStatisticsOrderShipmentsType(statisticsOrderShipmentsType);

        List<StatisticsOrderShipments> statisticsOrderShipmentsList = statisticsOrderShipmentsService.getStatisticsOrderShipments(shipmentsDto);

        //endregion

        //region 封装统计数据
        List<DrawItem> numberOfCasesList = Lists.newArrayList();
        List<DrawItem> weightList = Lists.newArrayList();

        for (StatisticsOrderShipments orderShipments : statisticsOrderShipmentsList) {
            //票数统计数据
            DrawItem dNumber = new DrawItem();
            dNumber.setName(simpleDateFormatForDay.format(orderShipments.getStatisticDate()));
            dNumber.setValue(orderShipments.getNumberOfCases());
            numberOfCasesList.add(dNumber);
            //重量统计数据
            DrawItem dWeight = new DrawItem();
            dWeight.setName(simpleDateFormatForDay.format(orderShipments.getStatisticDate()));
            dWeight.setValue(orderShipments.getWeight());
            weightList.add(dWeight);
        }

        StatisticsDrawListDto drawList = new StatisticsDrawListDto();
        drawList.setName("票数");
        drawList.setDrawItemList(numberOfCasesList);
        drawList.setyAxisIndex(0);
        list.add(drawList);

        StatisticsDrawListDto drawWeightList = new StatisticsDrawListDto();
        drawWeightList.setName("重量");
        drawWeightList.setDrawItemList(weightList);
        drawWeightList.setyAxisIndex(1);
        list.add(drawWeightList);

        List<DrayUnit> drayUnitList = Lists.newArrayList();
        DrayUnit day = new DrayUnit();
        day.setName("票");
        day.setMin(0);
        day.getAxisLabel().put("formatter", "{value}票");
        drayUnitList.add(day);
        DrayUnit day1 = new DrayUnit();
        day1.setName("kg");
        day1.setMin(0);
        day1.getAxisLabel().put("formatter", "{value}kg");
        drayUnitList.add(day1);

        statisticsDrawsDto.setDrayUnits(drayUnitList);
        statisticsDrawsDto.setDrawGroupAndStackLineItems(list);
//        statisticsDrawsDto.setDrawType(StatisticsDrawType.DRAWGSTACKLINE);
        //endregion
        return statisticsDrawsDto;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, "出货信息"};

    }
}
