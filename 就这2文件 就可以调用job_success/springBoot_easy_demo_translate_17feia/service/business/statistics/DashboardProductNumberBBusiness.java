package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardSeachKey;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
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
@Service
public class DashboardProductNumberBBusiness implements DashboardDrawBusinessI  {
    private static final Logger logger = LoggerFactory.getLogger(DashboardProductNumberBBusiness.class);



    @Value("${dashboard.statistics.url}")
    private String url;


    @Autowired
    private RestTemplate simpleRestTemplate;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        //初始化参数
        Map<String,Object> map  = new HashMap<>();
        String postUrl = url + "/productNumber/listB";
        map.put(DashboardSeachKey.OID.name(),SessionUtils.getOrganizationId());
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        Map<String,List> mapData = drawDataCreate(result);
        vo.setData(DashboardUtil.dataForJson(mapData,"产品个数统计B","单位(个)"));
        return vo;
    }
    public Map<String,List>  drawDataCreate(List<Map<String,Object>> result) {
        DataGrid<OrgWarehouse> wares = orgWarehouseService.list(SessionUtils.getOrganizationId(),null);
        Map<String, List> dataMap = new HashMap<>();

        Map<String,String> nameMap = new HashMap<>();
        for(OrgWarehouse warehouse : wares.getRows()){
            nameMap.put(warehouse.getOriginNo(),warehouse.getName());
        }
        List<String> xNameArray = new ArrayList<>();
        List<List<Integer>> yNameArray = new ArrayList<>();
        List<Integer> yList = new ArrayList<>();
        for(Map<String,Object> m : result){
            Double count = (Double)m.get("count");
            String name = nameMap.get(m.get("originNo"));
            if(name != null) {
                xNameArray.add(name);
                yList.add(count.intValue());
            }

        }
        yNameArray.add(yList);
        dataMap.put("xNameArray",xNameArray);
        dataMap.put("yValueArray",yNameArray);
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
        return new String[]{StatisticsDrawType.SALESMODULE, "产品个数统计B"};
    }
}
