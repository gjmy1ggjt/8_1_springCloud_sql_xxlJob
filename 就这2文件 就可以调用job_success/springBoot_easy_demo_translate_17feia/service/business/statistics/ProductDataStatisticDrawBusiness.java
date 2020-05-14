package com.yangshan.eship.sales.business.statistics;

import com.yangshan.eship.common.utils.SetList;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.finance.dto.statistics.AxisDataType;
import com.yangshan.eship.finance.dto.statistics.StatisticsDataType;
import com.yangshan.eship.finance.entity.statistics.StatisticsProductData;
import com.yangshan.eship.finance.service.statistics.StatisticsProductDataServiceI;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import com.yangshan.eship.sales.business.statistics.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Hukai
 * 2018-06-08 17:29
 * 绘制产品-数量/金额线性统计图
 *
 */
@Service
public class ProductDataStatisticDrawBusiness implements OneStatisticsDrawBusinessI {
    private static Logger logger = LoggerFactory.getLogger(ProductDataStatisticDrawBusiness.class);

    @Autowired
    private StatisticsProductDataServiceI statisticsProductDataService;

    @Autowired
    private ProductServiceI productService;

    @Override
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawDto) {

        //查询参数设置下拉列表
        statisticsDrawDto.setParamTitle("数据统计类型");
        List<StatisticParamDto> statisticParamDtos = new ArrayList<>();

        Map<String, String> dataTypeList = AxisDataType.list();
        dataTypeList.forEach((name, desc) -> {
            StatisticParamDto statisticParamDto = new StatisticParamDto();
            statisticParamDto.setParamId(name);
            statisticParamDto.setParamName(desc);
            statisticParamDtos.add(statisticParamDto);
        });

        statisticsDrawDto.setParamsSelect(statisticParamDtos);
        //statisticsDrawDto.setContent("实线: 产品对应的订单数量,  虚线: 产品对应的订单金额");

        return getResultDataData(statisticsDrawDto);
    }

    private StatisticsDrawDto getResultDataData(StatisticsDrawDto statisticsDrawDto) {
        Date startDateTime = null;
        Date endDateTime = null;

        if (StringUtils.isNotBlank(statisticsDrawDto.getSearchStartDateTime()) && StringUtils.isNotBlank(statisticsDrawDto.getSearchEndDateTime())) {
            try {
                startDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(statisticsDrawDto.getSearchStartDateTime() + " 00:00:00");
                endDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(statisticsDrawDto.getSearchEndDateTime() + " 23:59:59");
            } catch (ParseException e) {
                logger.error("日期转换出错!");
                throw new EshipException("日期转换出错!");
            }
        }

        statisticsDrawDto.setDrawType(StatisticsDrawType.DRAWGSTACKTWOLINE);

        AxisDataType axisDataType = null;
        //区分按天统计还是按月统计(默认按天统计)
        //AxisDataType axisDataType = statisticsDrawDto.getAxisDataType() == null ? AxisDataType.DAILY : statisticsDrawDto.getAxisDataType();
        if (StringUtils.isBlank(statisticsDrawDto.getSearchParamId())) {
            axisDataType = AxisDataType.DAILY;
        } else {
            axisDataType = AxisDataType.valueOf(statisticsDrawDto.getSearchParamId());
        }
        statisticsDrawDto.setSearchParamId(axisDataType.name());

        if (statisticsDrawDto.getStatisticsDataType() == null) {
            //默认展示仓库签收状态的数据
            statisticsDrawDto.setStatisticsDataType(StatisticsDataType.SIGN_FOR_WAREHOUSE_DATA);
        }

        StatisticsDataType statisticsDataType = statisticsDrawDto.getStatisticsDataType();

        List<StatisticsDrawListDto> list = new ArrayList<>();

        List<StatisticsProductData> statisticsProductDataList = new ArrayList<>();
        if (AxisDataType.DAILY.equals(axisDataType)) {
            statisticsProductDataList = statisticsProductDataService.listDayDataByType(statisticsDataType, startDateTime, endDateTime);
        } else {
            statisticsProductDataList = statisticsProductDataService.listMonthDataByType(statisticsDataType, startDateTime, endDateTime);
        }

        //横轴数据
        SetList<String> dateValue = new SetList<>();
        Set<String> productValue = new HashSet<>();
        Map<String, StatisticsProductData> dataMap = new HashMap<>();

        for (StatisticsProductData statisticsProductData : statisticsProductDataList) {
            String dateStr = "";
            if (AxisDataType.DAILY.equals(axisDataType)) {
                dateStr = new SimpleDateFormat("yyyy-MM-dd").format(statisticsProductData.getStatisticDate());
            } else {
                dateStr = statisticsProductData.getShortStatisticDate();
            }
            dateValue.add(dateStr);

            productValue.add(statisticsProductData.getProductId());

            dataMap.put(statisticsProductData.getProductId() + "@" + dateStr, statisticsProductData);
        }

        logger.debug("dateValue size: " + dateValue.size());

        if (productValue.isEmpty()) {
            statisticsDrawDto.setDrawGroupAndStackLineItems(list);
            return statisticsDrawDto;
        }

        List<Product> productList = productService.listByProductIds(new ArrayList<>(productValue));
        Map<String, Product> productMap = new HashMap<>();
        for (Product product : productList) {
            productMap.put(product.getId(), product);
        }

        for (String productId : productValue) {
            //产品-数量
            StatisticsDrawListDto drawProductNumberList = new StatisticsDrawListDto();
            List<DrawItem> productNumberDrawItems = new ArrayList<>();
            for (String dateTime : dateValue) {
                StatisticsProductData statisticsProductData = dataMap.get(productId + "@" + dateTime);

                DrawItem numberDataItem = new DrawItem();
                String dateStr = "";
                long numOfCase = 0L;
                if (statisticsProductData != null) {
                    if (AxisDataType.DAILY.equals(axisDataType)) {
                        dateStr = new SimpleDateFormat("yyyy-MM-dd").format(statisticsProductData.getStatisticDate());
                    } else {
                        dateStr = statisticsProductData.getShortStatisticDate();
                    }

                    numOfCase = statisticsProductData.getNumberOfCases();

                    dateValue.add(dateStr);

                    numberDataItem.setName(dateStr);
                    numberDataItem.setValue(numOfCase);

                    productNumberDrawItems.add(numberDataItem);
                } else {
                    //没有的话要加一条空数据, 否则曲线出不来
                    //dateStr = dateTime;
                }


            }

            //找不到产品id对应的产品==>直接跳过
            if (productMap.get(productId) == null) {
                continue;
            }

            drawProductNumberList.setName(productMap.get(productId).getName() + " (" + productMap.get(productId).getOriginName() + ")");
            drawProductNumberList.setyAxisIndex(0);
            drawProductNumberList.setDrawItemList(productNumberDrawItems);
            list.add(drawProductNumberList);

            //产品-金额
            StatisticsDrawListDto drawProductPriceList = new StatisticsDrawListDto();

            List<DrawItem> productPriceDrawItems = new ArrayList<>();
            for (String dateTime : dateValue) {
                StatisticsProductData statisticsProductData = dataMap.get(productId + "@" + dateTime);

                DrawItem moneyDataItem = new DrawItem();
                String dateStr = "";
                if (statisticsProductData != null) {
                    if (AxisDataType.DAILY.equals(axisDataType)) {
                        dateStr = new SimpleDateFormat("yyyy-MM-dd").format(statisticsProductData.getStatisticDate());
                    } else {
                        dateStr = statisticsProductData.getShortStatisticDate();
                    }

                    BigDecimal amount = statisticsProductData.getAmount();

                    productPriceDrawItems.add(moneyDataItem);

                    dateValue.add(dateStr);

                    moneyDataItem.setName(dateTime);
                    moneyDataItem.setValue(amount);
                    productPriceDrawItems.add(moneyDataItem);
                } else {
                    //没有的话要加一条空数据, 否则曲线出不来
                    //dateStr = dateTime;
                }

            }

            drawProductPriceList.setName(productMap.get(productId).getName() + " (" + productMap.get(productId).getOriginName() + ")");
            drawProductPriceList.setyAxisIndex(1);
            drawProductPriceList.setDrawItemList(productPriceDrawItems);
            list.add(drawProductPriceList);
        }

        statisticsDrawDto.setDrawGroupAndStackLineItems(list);

        return statisticsDrawDto;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.PRODUCTMODULE, "产品交易量/额时间统计图"};
    }
}
