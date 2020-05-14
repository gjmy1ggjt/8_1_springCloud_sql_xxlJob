package com.yangshan.eship.sales.business.statistics;

import com.google.common.collect.Lists;
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
 * @Date: 2018-12-26 14:37
 * @Description:
 */
@Service
public class DashboardUserAccountBusiness implements DashboardDrawBusinessI {
    private static final String ECHART_NAME = "客户账期额度排行榜TOP10";

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
        Map<String,Object> map  = DashboardUtil.initParams(vo,types,orgWarehouseService);

        String postUrl = url+"/userAccountData/getUserAccountData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardUserAccountBusiness-entity:{}", entity);

        vo.setData(dataForJson(result));
        return vo;
    }

    private Map dataForJson(List<Map> result){
        Map data = new HashMap();

//        Map titleMap = new HashMap();
//        titleMap.put("text", ECHART_NAME);
//        //titleMap.put("subtext", "客户账期额度排行榜TOP10");
//        data.put("title", titleMap);

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "shadow");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);

        Map legendMap = new HashMap();
        String[] legendData = new String[]{"已用额度", "剩余额度"};
        legendMap.put("data", Lists.newArrayList(legendData));
        data.put("legend", legendMap);

        List<String> colors = DashboardUtil.getColors(legendData.length);
        data.put("color", colors);

        Map xAxisMap = new HashMap();
        xAxisMap.put("type", "value");
        xAxisMap.put("name", "额度(元)");
        data.put("xAxis", xAxisMap);


        Map yAxisMap = new HashMap();
        yAxisMap.put("type", "category");
        yAxisMap.put("inverse", true);

        String[] yAxisData = new String[result.size()];
        int i = 0;
        for (Map map : result) {
            yAxisData[i] = (map.containsKey("simpleCompanyName") && map.get("simpleCompanyName") != null) ? map.get("simpleCompanyName").toString() : map.get("customerCode").toString();
            i++;
        }
        yAxisMap.put("data", Lists.newArrayList(yAxisData));
        data.put("yAxis", yAxisMap);

        List<Map> series = new ArrayList<>();
        for (String legend : legendData) {
            Map seriesMap = new HashMap();
            seriesMap.put("name", legend);
            seriesMap.put("type", "bar");
            //柱子最大宽度
            seriesMap.put("barMaxWidth", 100);
            seriesMap.put("stack", "总额度");

            List<Double> seriesData = new ArrayList<>();

            for (Map map : result) {
                if ("已用额度".equals(legend)) {
                    Double lineOfCreditUsed = Double.parseDouble(map.get("lineOfCreditUsed").toString());
                    seriesData.add(lineOfCreditUsed);
                } else {
                    Double remainingAmount = Double.parseDouble(map.get("remainingAmount").toString());
                    seriesData.add(remainingAmount);
                }
            }

            seriesMap.put("data", seriesData);

            series.add(seriesMap);
        }
        data.put("series", series);

        DashboardUtil.addToolBox(data);

        logger.debug("=========> 客户账期额度排行榜TOP10数据: {}", data.toString());

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
