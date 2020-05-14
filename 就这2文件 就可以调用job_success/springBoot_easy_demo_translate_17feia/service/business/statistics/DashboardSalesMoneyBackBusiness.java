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
public class DashboardSalesMoneyBackBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardSalesMoneyBackBusiness.class);



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

        String postUrl = url + "/moneyBack/salesMoneyBack";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        Map<String,List> mapResult = drawDataCreate(result);
        vo.setData(DashboardUtil.dataForJsonTransverse(mapResult,"销售回款统计图","回款额","单位(元)"));
        return vo;
    }

    public Map<String,List>  drawDataCreate(List<Map<String,Object>> result) {
        List<Map> pay = new ArrayList<>();
        List<Map> notPay = new ArrayList<>();
        if(result.get(0).get("pay") != null) {
             pay = (List<Map>) result.get(0).get("pay");
        }
        if(result.get(0).get("notPay") != null) {
             notPay = (List<Map>) result.get(0).get("notPay");
        }
        Map<String,Float[]> paySet = new HashMap<>();
        for(Map p : pay){
            String name = p.get("name")+"";
            float price = DataUtils.toBigDecimal(p.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            paySet.put(name,new Float[]{price,0f});

        }

        for(Map n : notPay){
            String name = n.get("name")+"";
            float price = DataUtils.toBigDecimal(n.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            Float[] array = paySet.get(name);
            if( array != null){
                array[1] = price;
            }else{
                paySet.put(name,new Float[]{0f,price});
            }
        }

        List<Map<String,Object>> dataResult = new ArrayList<>();

        for (Map.Entry<String, Float[]> entry : paySet.entrySet()) {
            Map<String,Object> data = new HashMap<>();
            data.put("name",entry.getKey());
            data.put("price",entry.getValue()[0]);
            data.put("price1",entry.getValue()[1]);
            dataResult.add(data);
        }

        Map<String, List> dataMap = new HashMap<>();
        List<String> topArray = new ArrayList<>();
        List<Float> prices1 = new ArrayList<>();
        List<Float> prices2 = new ArrayList<>();
        topArray.add("已收款");
        topArray.add("待收款");
        List<String> names = new ArrayList<>();
        for(Map map : dataResult){
            String name = map.get("name")+"";
            float price = DataUtils.toBigDecimal(map.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            float price1 = DataUtils.toBigDecimal(map.get("price1")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            names.add(name);
            prices1.add(price);
            prices2.add(price1);
        }
        List<List<Float>> valuesList = new ArrayList<>();
        valuesList.add(prices1);
        valuesList.add(prices2);
        dataMap.put("topArray",topArray);
        dataMap.put("xNameArray",names);
        dataMap.put("yValueArray",valuesList);
        return dataMap;
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
        return new String[]{StatisticsDrawType.SALESMODULE, "销售回款统计图"};
    }
}
