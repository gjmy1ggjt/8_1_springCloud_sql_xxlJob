package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Kevin
 * @Date: 2019-01-02 17:39
 * @Description:
 */
@Service
public class DashboardAirlineOrderWeightBusiness implements DashboardDrawBusinessI {
    private static final String ECHART_NAME = "航空货量统计图1";

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
        types.add(DashboardParamType.TIMESLOT);
        Map<String,Object> map  = DashboardUtil.initParams(vo, types, orgWarehouseService);

        String postUrl = url+"/viewOrderData/getAirlineOrderWeightData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardAirlineOrderWeightBusiness-entity:{}", entity);

        vo.setData(dataForJson(result));
        return vo;
    }

    private Map dataForJson(List<Map> result){
        Map data = new HashMap();

        List<String> xDataList = new ArrayList<>();
        List<Float> yDataList = new ArrayList<>();
        for (Map map : result) {
            xDataList.add(map.get("airline_name").toString());
            yDataList.add(Float.parseFloat(String.valueOf(map.get("total_count"))));
        }

//        Map titleMap = new HashMap();
//        titleMap.put("text", ECHART_NAME);
//        data.put("title", titleMap);

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "shadow");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);

        Map xAxisMap = new HashMap();
        xAxisMap.put("type", "value");
        xAxisMap.put("name", "订单数(票)");
        data.put("xAxis", xAxisMap);

        Map yAxisMap = new HashMap();
        yAxisMap.put("type", "category");
        yAxisMap.put("data", xDataList);
        yAxisMap.put("inverse", true);
        data.put("yAxis", yAxisMap);

        List<Map> series = new ArrayList<>();
        Map seriesMap = new HashMap();
        seriesMap.put("name", "订单数");
        seriesMap.put("type", "bar");
        //柱子最大宽度
        seriesMap.put("barMaxWidth", 100);
        seriesMap.put("barWidth", "50%");
        seriesMap.put("data", yDataList);
        Map labelMap = new HashMap();
        Map normalMap = new HashMap();
        normalMap.put("show", true);
        normalMap.put("position", "right");
        labelMap.put("normal", normalMap);
        seriesMap.put("label", labelMap);
        seriesMap.put("stack", "总量");
        series.add(seriesMap);
        data.put("series", series);

        DashboardUtil.addToolBox(data);

        logger.debug("=========> 航空货量统计图数据: {}", data.toString());

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
