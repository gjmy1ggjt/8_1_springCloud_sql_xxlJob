package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
 * 航空公司统计
 *
 * @author Kee.Li
 * @date 2019/1/4 13:27
 */
@Service
public class DashboardAirlineStatisticsBusiness implements  DashboardDrawBusinessI{

    private final Logger logger = LoggerFactory.getLogger(DashboardAirlineStatisticsBusiness.class);

    private static final String ECHART_NAME = "航空货量统计图";

    @Autowired
    private RestTemplate simpleRestTemplate;

    @Value("${dashboard.statistics.url}")
    private String url;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {

        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.TIMESLOT);
        Map<String,Object> map  = DashboardUtil.initParams(vo,types,orgWarehouseService);

        String postUrl = url+"/viewOrderData/queryForAirlineOrderCalData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardAirlineStatisticsBusiness-entity:{}", entity);

        vo.setData(dataForJson(result));
        return vo;
    }

    private Map dataForJson(List<Map> result){
        Map data = Maps.newHashMap();

        List<String> yAxisData = Lists.newArrayList();
        List<Double> seriesData = Lists.newArrayList();
        for (Map map:result) {
            String airlineName = (String)map.get("airlineName");
            Double orderCount = (Double)map.get("orderCount");
            yAxisData.add(airlineName);
            seriesData.add(orderCount);
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

        Map gridMap = Maps.newHashMap();
        gridMap.put("left","3%");
        gridMap.put("right","4%");
        gridMap.put("bottom","3%");
        gridMap.put("containLabel",true);
        data.put("grid",gridMap);

        List<Map<String,Object>> yAxisList = Lists.newArrayList();
        Map<String,Object> yAxisMap = Maps.newHashMap();
        yAxisMap.put("type","category");
        yAxisMap.put("data",yAxisData);
        Map<String,Object> axisTickMap = Maps.newHashMap();
        axisTickMap.put("alignWithLabel",true);
        yAxisMap.put("axisTick",axisTickMap);
        yAxisList.add(yAxisMap);
        data.put("yAxis",yAxisList);

        List<Map<String,Object>> xAxisList = Lists.newArrayList();
        Map<String,Object> xAxisMap = Maps.newHashMap();
        xAxisMap.put("type","value");
        xAxisMap.put("name", "订单数(票)");
        xAxisList.add(xAxisMap);
        data.put("xAxis",xAxisList);

        List<Map<String,Object>> seriesList = Lists.newArrayList();
        Map<String,Object> seriesMap = Maps.newHashMap();
        seriesMap.put("name","票数");
        seriesMap.put("type","bar");
        seriesMap.put("barWidth","60%");
        seriesMap.put("data",seriesData);

        Map labelMap = new HashMap();
        Map normalMap = new HashMap();
        normalMap.put("show", true);
        normalMap.put("position", "right");
        labelMap.put("normal", normalMap);
        seriesMap.put("label", labelMap);
        seriesMap.put("stack", "总量");

        seriesList.add(seriesMap);
        data.put("series",seriesList);

        DashboardUtil.addToolBox(data);

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
