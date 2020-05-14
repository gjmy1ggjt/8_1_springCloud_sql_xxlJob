package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.CalType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardSeachKey;
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
 * @Date: 2018-12-29 16:05
 * @Description:
 */
@Service
public class DashboardLogisticsOrderDataBusiness implements DashboardDrawBusinessI {
    private static final String ECHART_NAME = "末端派送商货量统计图";

    private Logger logger = LoggerFactory.getLogger(DashboardDestinationOrderWeightBusiness.class);

    @Autowired
    private RestTemplate simpleRestTemplate;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Autowired
    private RegionServiceI regionService;

    @Value("${dashboard.statistics.url}")
    private String url;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.TIMESLOT);
        types.add(DashboardParamType.CALTYPE);
        Map<String,Object> map  = DashboardUtil.initParams(vo, types, orgWarehouseService);

        String postUrl = url + "/viewOrderData/getLogisticsOrderCalData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardDestinationOrderWeightBusiness-entity:{}", entity);

        vo.setData(dataForJson(result, map.containsKey(DashboardSeachKey.CALTYPE.name()) ? map.get(DashboardSeachKey.CALTYPE.name()).toString() : CalType.ORDERCOUNT.name()));
        return vo;
    }

    private Map dataForJson(List<Map> result, String calType) {
        Map data = new HashMap();

        LinkedHashSet<String> legendDataSet = new LinkedHashSet<>();
        LinkedHashSet<String> yDataSet = new LinkedHashSet<>();
        Map<String, Double> resultValueMap = new HashMap<>();
        for (Map map : result) {
            String name = map.get("logistics_name").toString();
            String logisticsName = map.get("logistics_logistics_name").toString();
            Double calData = (Double) map.get("cal_data");

            legendDataSet.add(name);
            yDataSet.add(logisticsName);
            resultValueMap.put(name + "-" + logisticsName, DashboardUtil.splitNumber(DataUtils.toBigDecimal(calData),3));
        }

        List<String> legendData = new ArrayList(legendDataSet);
        List<String> yDataList = new ArrayList(yDataSet);

//        Map titleMap = new HashMap();
//        titleMap.put("text", ECHART_NAME);
//        data.put("title", titleMap);

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "shadow");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);

        Map legendMap = new HashMap();
        legendMap.put("data", legendData);
        legendMap.put("type", "scroll");
        legendMap.put("orient", "vertical");
        legendMap.put("right", 0);
        legendMap.put("top", 50);
        legendMap.put("bottom", 20);
        data.put("legend", legendMap);

        Map xAxisMap = new HashMap();
        xAxisMap.put("type", "value");
        xAxisMap.put("name", CalType.ORDERCOUNT.name().equals(calType) ? "订单数(票)" : "货物总重(kg)");
        data.put("xAxis", xAxisMap);

        Map yAxisMap = new HashMap();
        yAxisMap.put("type", "category");
        yAxisMap.put("data", yDataList);
        data.put("yAxis", yAxisMap);

        List<Map> series = new ArrayList<>();

        /*Map labelMap = new HashMap();
        Map normalMap = new HashMap();
        normalMap.put("show", true);
        normalMap.put("position", "insideRight");
        labelMap.put("normal", normalMap);*/

        for (String legend : legendData) {
            Map seriesMap = new HashMap();
            seriesMap.put("name", legend);
            seriesMap.put("type", "bar");
            seriesMap.put("stack", "总量");
            //seriesMap.put("label", labelMap);
            //柱子最大宽度
            seriesMap.put("barMaxWidth", 100);

            List seriesData = new ArrayList<>();
            for (String yData : yDataList) {
                String mapKey = legend + "-" + yData;
                if (resultValueMap.containsKey(mapKey)) {
                    if (CalType.ORDERCOUNT.name().equals(calType)) {
                        seriesData.add(resultValueMap.get(mapKey).intValue());
                    } else {
                        seriesData.add(resultValueMap.get(mapKey).floatValue());
                    }
                } else {
                    if (CalType.ORDERCOUNT.name().equals(calType)) {
                        seriesData.add(0);
                    } else {
                        seriesData.add(0f);
                    }
                }

            }
            seriesMap.put("data", seriesData);
            series.add(seriesMap);
        }
        data.put("series", series);

        DashboardUtil.addToolBox(data);

        logger.debug("=========> 末端派送商货量统计图数据: {}", data.toString());

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
