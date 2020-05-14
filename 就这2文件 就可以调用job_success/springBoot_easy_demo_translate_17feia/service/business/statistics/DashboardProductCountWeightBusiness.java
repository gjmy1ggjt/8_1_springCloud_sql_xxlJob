package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.product.service.ProductServiceI;
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
 * tsd
 */
@Service
public class DashboardProductCountWeightBusiness implements DashboardDrawBusinessI {
    private static final Logger logger = LoggerFactory.getLogger(DashboardProductCountWeightBusiness.class);



    @Value("${dashboard.statistics.url}")
    private String url;


    @Autowired
    private RestTemplate simpleRestTemplate;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Autowired
    private ProductServiceI productService;
    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        //初始化参数
        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.PRODUCT);
        types.add(DashboardParamType.TIMESLOT);
        Map<String,Object> map  = DashboardUtil.initParams(vo,types,orgWarehouseService,productService);
        String postUrl = url + "/productCountWeight/list";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        DashboardUtil.sortData(result,"count");
        Map<String,List> mapData = drawDataCreate(result);
        vo.setData(DashboardUtil.dataForJsonTransverse(mapData,"产品票数和重量统计图","","单位(票/KG)"));
        return vo;
    }

    public Map<String,List>  drawDataCreate(List<Map<String,Object>> result) {
        Map<String,List> map = new HashMap<>();
        List<String> topArray = new ArrayList<>();
        topArray.add("票数");
        topArray.add("重量");
        List<String> xNameArray = new ArrayList<>();
        List<List<Double>> yValueArray = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        List<Double> wList = new ArrayList<>();
        for(Map<String,Object> m : result){
            if(m.get("name") != null && !"".equals(m.get("name"))) {
                String name = (String) m.get("name");
                Double count = (Double) m.get("count");
                Double weight = (Double) m.get("weight");
                if(count > 0 || weight > 0 && name != null) {
                    xNameArray.add(name);
                    yList.add(DashboardUtil.splitNumber(DataUtils.toBigDecimal(count), 2));
                    wList.add(DashboardUtil.splitNumber(DataUtils.toBigDecimal(weight), 3));
                }
            }

        }
        yValueArray.add(yList);
        yValueArray.add(wList);
        map.put("topArray",topArray);
        map.put("xNameArray",xNameArray);
        map.put("yValueArray",yValueArray);
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
        return new String[]{StatisticsDrawType.SALESMODULE, "产品票数和重量统计图"};
    }
}
