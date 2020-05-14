package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import com.yangshan.eship.sales.utils.DashboardUtil;
import com.yangshan.eship.sales.vo.DashboardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

/**
 * @Author: Kevin
 * @Date: 2018-12-28 14:55
 * @Description:
 */
@Service
public class DashboardWarehouseOrderWeightBusiness implements DashboardDrawBusinessI {
    private static final String ECHART_NAME = "仓库出货量统计图";

    private Logger logger = LoggerFactory.getLogger(DashboardUserAccountBusiness.class);

    @Autowired
    private RestTemplate simpleRestTemplate;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Value("${dashboard.statistics.url}")
    private String url;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.TIMETYPE);
        Map<String,Object> map  = DashboardUtil.initParams(vo, types, orgWarehouseService);

        String postUrl = url+"/orderWeightData/getWarehouseOrderWeightData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardWarehouseOrderWeightBusiness-entity:{}", entity);

        vo.setData(dataForJson(result, (String) map.get(DashboardParamType.TIMETYPE.name())));
        return vo;
    }

    private Map dataForJson(List<Map> result, String timeType){
        Map data = new HashMap();

        /*Map titleMap = new HashMap();
        titleMap.put("text", ECHART_NAME);
        data.put("title", titleMap);*/

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "shadow");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);

        //按照月份或季度展示
        if("QUARTER".equals(timeType) || "MONTH".equals(timeType)) {
            LinkedHashSet<String> yearDataSet = new LinkedHashSet<>();
            LinkedHashSet<String> xDataSet = new LinkedHashSet<>();

            Map<String, Float> dataMap = new HashMap<>();
            for (Map map : result) {
                int year = (int) map.get("year");
                int timeTypeValue = (int) map.get(timeType.toLowerCase());
                Double weightValue = (double) map.get("weight");
                Float weight = DashboardUtil.splitNumber(DataUtils.toBigDecimal(weightValue),3).floatValue();

                String yearValue = String.valueOf(year) + "年";
                String xDataValue = "QUARTER".equals(timeType) ? ("第" + timeTypeValue + "季度") : timeTypeValue + "月";
                yearDataSet.add(yearValue);
                xDataSet.add(xDataValue);

                dataMap.put(yearValue + "-" + xDataValue, weight);
            }

            List yearList = new ArrayList(yearDataSet);
            List xDataList = new ArrayList(xDataSet);

            List<String> colorList = DashboardUtil.getColors(yearList.size());
            data.put("color", colorList);

            Map legendDataMap = new HashMap();
            legendDataMap.put("data", yearList);
            data.put("legend", legendDataMap);

            List<Map> xAxisMapList = new ArrayList<>();
            Map xAisMap = new HashMap();
            xAisMap.put("type", "category");
            xAisMap.put("data", xDataList);
            xAxisMapList.add(xAisMap);
            data.put("xAxis", xAxisMapList);

            Map yAxisMap = new HashMap();
            yAxisMap.put("type", "value");
            yAxisMap.put("name", "重量(kg)");

            data.put("yAxis", yAxisMap);

            List<Map> series = new ArrayList<>();
            for (int i = 0; i < yearList.size(); i++) {
                Map singleYearData = new HashMap();

                singleYearData.put("name", yearList.get(i));
                singleYearData.put("type", "bar");
                //柱子最大宽度
                singleYearData.put("barMaxWidth", 100);
                singleYearData.put("barGap", 0);
                List<Float> weightDatas = new ArrayList();
                for (int j = 1; j <= xDataList.size(); j++) {
                    float weightValue = dataMap.get(yearList.get(i) + "-" + xDataList.get(j - 1));
                    weightDatas.add(DataUtils.toBigDecimal(weightValue).setScale(3, BigDecimal.ROUND_HALF_UP).floatValue());
                }
                singleYearData.put("data", weightDatas);

                series.add(singleYearData);
            }
            data.put("series", series);
        }

        //按照年展示
        if("YEAR".equals(timeType)) {
            LinkedHashSet<String> yearDataSet = new LinkedHashSet<>();

            Map<String, Float> dataMap = new HashMap<>();
            for (Map map : result) {
                int year = (int) map.get("year");
                Double weightValue = (double) map.get("weight");


                String yearValue = String.valueOf(year) + "年";
                yearDataSet.add(yearValue);

                dataMap.put(yearValue, DataUtils.toBigDecimal(weightValue).setScale(3, BigDecimal.ROUND_HALF_UP).floatValue());
            }

            List yearList = new ArrayList(yearDataSet);

            List<String> colorList = DashboardUtil.getColors(1);
            data.put("color", colorList);

            List<Map> xAxisMapList = new ArrayList<>();
            Map xAisMap = new HashMap();
            xAisMap.put("type", "category");
            xAisMap.put("data", yearList);
            xAxisMapList.add(xAisMap);
            data.put("xAxis", xAxisMapList);

            Map yAxisMap = new HashMap();
            yAxisMap.put("type", "value");
            yAxisMap.put("name", "重量(kg)");

            data.put("yAxis", yAxisMap);

            Map yearData = new HashMap();

            yearData.put("name", "货物总重");
            yearData.put("type", "bar");
            //柱子最大宽度
            yearData.put("barMaxWidth", 100);
            List<Float> weightDatas = new ArrayList();
            for (int i = 1; i <= yearList.size(); i++) {
                float weightValue = dataMap.get(yearList.get(i - 1));
                weightDatas.add(weightValue);
            }
            yearData.put("data", weightDatas);

            data.put("series", yearData);
        }

        DashboardUtil.addToolBox(data);

        logger.debug("=========> 仓库出货量统计图数据: {}", data.toString());

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
