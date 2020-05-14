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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tsd
 */
@Service
public class DashboardSalesCommissionBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardSalesCommissionBusiness.class);


    @Value("${dashboard.statistics.url}")
    private String url;


    @Autowired
    private RestTemplate simpleRestTemplate;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        //初始化参数
        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.TIMESLOT);
        Map<String,Object> map  = DashboardUtil.initParams(vo,types,orgWarehouseService);

        String postUrl = url + "/commission/salesCommission";
        List<Map> result = restForStatistics(postUrl,map);
        Map<String,List> mapResult = drawDataCreate(result,map);
        vo.setData(DashboardUtil.dataForJsonTransverse(mapResult,"销售概况一览表","销售额","单位(元)"));
        return vo;
    }
    public Map<String,List>  drawDataCreate(List<Map> result,Map<String,Object> param) {
        List<String> names = new ArrayList<>();
        List<List<Float>> valuesList = new ArrayList<>();
        List<String> topArray = new ArrayList<>();
        topArray.add("销售提成");
        topArray.add("实际销售额");
        topArray.add("销售目标");
        Map resultMap = result.get(0);
        List<Map> salesCommission = new ArrayList<>();
        List<Map> salesManagerCommission = new ArrayList<>();
        List<Map> saleCostPrice = new ArrayList<>();
        List<Map> forMonthlyGoals = new ArrayList<>();
        if(resultMap.get("salesCommission") != null) {
            salesCommission = (List<Map>)resultMap.get("salesCommission");
        }
        if(resultMap.get("salesManagerCommission") != null) {
            salesManagerCommission = (List<Map>)resultMap.get("salesManagerCommission");
        }
        if(resultMap.get("saleCostPrice") != null) {
            saleCostPrice = (List<Map>)resultMap.get("saleCostPrice");
        }
        if(resultMap.get("forMonthlyGoals") != null) {
            forMonthlyGoals = (List<Map>)resultMap.get("forMonthlyGoals");
        }
        Map<String,Float> salesCommissionMap = new HashMap<>();
        //销售提成
        for(Map map : salesCommission){
            String name = map.get("name")+"";
            float price = DataUtils.toBigDecimal(map.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            salesCommissionMap.put(name,price);
        }
        //加上销售经理提成
        for(Map map : salesManagerCommission){
            String name = map.get("name")+"";
            float price = DataUtils.toBigDecimal(map.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            if(salesCommissionMap.get(name) != null){
                price = DataUtils.toBigDecimal(price).add(DataUtils.toBigDecimal(salesCommissionMap.get(name))).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            }
            salesCommissionMap.put(name,price);
        }
        //销售目标
        Map<String,Float> forMonthlyGoalsMap = new HashMap<>();
        for(Map map : forMonthlyGoals){
            String name = map.get("name")+"";
            float price = DataUtils.toBigDecimal(map.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            forMonthlyGoalsMap.put(name,price);
        }
        List<Float> data1 = new ArrayList<>();
        List<Float> data2 = new ArrayList<>();
        List<Float> data3 = new ArrayList<>();
        for(Map map : saleCostPrice){
            if(map.get("name") != null) {
                String name = map.get("name") + "";
                names.add(name);
                if (salesCommissionMap.get(name) == null) {
                    data1.add(0f);
                } else {
                    data1.add(salesCommissionMap.get(name));
                }
                data2.add(DataUtils.toBigDecimal(map.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                if (forMonthlyGoalsMap.get(name) == null) {
                    data3.add(0f);
                } else {
                    data3.add(forMonthlyGoalsMap.get(name));
                }
            }
        }
        valuesList.add(data1);
        valuesList.add(data2);
        valuesList.add(data3);
        Map<String, List> dataMap = new HashMap<>();

        dataMap.put("topArray",topArray);
        dataMap.put("xNameArray",names);
        dataMap.put("yValueArray",valuesList);
        return dataMap;
    }

    public List<Map> restForStatistics(String url, Map<String,Object> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(params, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(url, entity, List.class);
        logger.debug("DashboardSalesPriceBusiness-entity:{}",entity);
        logger.debug("DashboardSalesPriceBusiness-result:{}",result);
        return result;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, "销售概况一览表"};
    }


}
