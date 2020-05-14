package com.yangshan.eship.sales.business.statistics;

import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
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
 * tsd
 */
@Service
public class DashboardProductNumberABusiness implements DashboardDrawBusinessI {
    private static final Logger logger = LoggerFactory.getLogger(DashboardProductNumberABusiness.class);



    @Value("${dashboard.statistics.url}")
    private String url;


    @Autowired
    private RestTemplate simpleRestTemplate;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        //初始化参数
        Map<String,Object> map  = new HashMap<>();
        String postUrl = url + "/productNumber/listA";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        vo.setData(dataForJson(result));
        return vo;
    }
    public Map dataForJson(List<Map<String,Object>> result){
        Double productNumber = (Double)result.get(0).get("productNumber");
        Integer orderCount = (Integer)result.get(0).get("orderNumber");
        int noOdercount = productNumber.intValue() - orderCount;

        List<Map> dataList = new ArrayList<>();
        Map data = new HashMap();
        data.put("value", noOdercount);
        data.put("name","未活跃产品 "+noOdercount);
        dataList.add(data);
        Map data1 = new HashMap();
        data1.put("value", orderCount);
        data1.put("name","今日活跃产品 "+orderCount);
        dataList.add(data1);
        Map map = new HashMap();

        Map tooltip = new HashMap();
        tooltip.put("trigger","item");
        tooltip.put("formatter","{a} <br/>{b}  ({d}%)");
        map.put("tooltip",tooltip);



        Map series = new HashMap();
        series.put("type","pie");
        series.put("data",dataList);
        series.put("name","产品情况");
        series.put("radius",new String[]{"30%", "40%"});
        series.put("avoidLabelOverlap",false);

        map.put("series",series);

        DashboardUtil.addToolBox(data);

        return map;
    }
    public List<Map<String,Object>> restForStatistics(String url, Map<String,Object> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(params, headers);
        //result 返回的json数据
        List<Map<String,Object>> result = simpleRestTemplate.postForObject(url, entity, List.class);
        logger.debug("DashboardTurnoverBusiness-entity:{}",entity);
        logger.debug("DashboardTurnoverBusiness-result:{}",result);
        return result;
    }
    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, "产品个数统计A"};
    }
}
