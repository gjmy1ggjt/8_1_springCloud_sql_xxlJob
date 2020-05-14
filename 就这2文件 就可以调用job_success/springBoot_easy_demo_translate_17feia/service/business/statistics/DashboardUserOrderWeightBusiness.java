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

import java.util.*;

/**
 * @Author: Kevin
 * @Date: 2018-12-27 17:56
 * @Description:
 */
@Service
public class DashboardUserOrderWeightBusiness implements DashboardDrawBusinessI {
    private static final String ECHART_NAME = "客户货量排行榜TOP10";

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

        String postUrl = url+"/orderWeightData/getUserOrderWeightData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardUserOrderWeightBusiness-entity:{}", entity);

        vo.setData(dataForJson(result));
        return vo;
    }

    private Map dataForJson(List<Map> result){
        Map data = new HashMap();

        List<String> xDataList = new ArrayList<>();
        List<Float> yDataList = new ArrayList<>();
        for (Map map : result) {
            xDataList.add(map.get("xData").toString());
            yDataList.add(Float.parseFloat(String.valueOf(map.get("yData"))));
        }

        /*Map titleMap = new HashMap();
        titleMap.put("text", ECHART_NAME);
        data.put("title", titleMap);*/

        data.put("color", DashboardUtil.getColors(1));

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "shadow");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);

        //获取x轴最大文字个数, 控制与底部间的距离
        int maxLength = 0;
        for (String text : xDataList) {
            if (text.length() >= maxLength) {
                maxLength = text.length();
            }
        }
        Map gridMap = new HashMap();
        gridMap.put("y2", maxLength * 10);
        data.put("grid", gridMap);

        List<Map> xAxisMapList = new ArrayList<>();
        Map xAxisMap = new HashMap();
        xAxisMap.put("type", "category");
        xAxisMap.put("data", xDataList);
        //X轴文字旋转-45度
        Map axisLabelMap = new HashMap();
        axisLabelMap.put("interval", 0);
        axisLabelMap.put("rotate", -45);
        xAxisMap.put("axisLabel", axisLabelMap);
        xAxisMapList.add(xAxisMap);
        data.put("xAxis", xAxisMapList);

        Map yAxisMap = new HashMap();
        yAxisMap.put("type", "value");
        yAxisMap.put("name", "重量(kg)");

        data.put("yAxis", yAxisMap);

        List<Map> series = new ArrayList<>();
        Map seriesMap = new HashMap();
        seriesMap.put("name", "货物总重");
        seriesMap.put("type", "bar");
        seriesMap.put("barWidth", "50%");
        //柱子最大宽度
        seriesMap.put("barMaxWidth", 100);
        seriesMap.put("data", yDataList);

        Map labelMap = new HashMap();
        Map normalMap = new HashMap();
        normalMap.put("show", true);
        normalMap.put("position", "top");
        labelMap.put("normal", normalMap);
        seriesMap.put("label", labelMap);
        
        series.add(seriesMap);
        data.put("series", series);

        DashboardUtil.addToolBox(data);


        logger.debug("=========> 客户货量排行榜TOP10数据: {}", data.toString());

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
