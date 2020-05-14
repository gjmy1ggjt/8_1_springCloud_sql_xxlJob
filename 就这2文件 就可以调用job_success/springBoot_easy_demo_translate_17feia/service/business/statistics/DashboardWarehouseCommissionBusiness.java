package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tsd
 */
@Service
public class DashboardWarehouseCommissionBusiness implements DashboardDrawBusinessI{
    private static final Logger logger = LoggerFactory.getLogger(DashboardWarehouseCommissionBusiness.class);



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
        types.add(DashboardParamType.TIMESLOT);
        Map<String,Object> map  = DashboardUtil.initParams(vo,types,orgWarehouseService);

        String postUrl = url + "/warehouse/warehouseCommission";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        Map<String,List> mapResult = drawDataCreate(result);
        DashboardUtil.sortData(result,"price");
        vo.setData(DashboardUtil.dataForJson(mapResult,"销售回款统计图","单位(元)"));
        return vo;
    }
    public Map<String,List>  drawDataCreate(List<Map<String,Object>> result) {
        DataGrid<OrgWarehouse> wares = orgWarehouseService.list(SessionUtils.getOrganizationId(),null);
        Map<String, List> dataMap = new HashMap<>();
        List<String> names = new ArrayList<>();
        Map<String,String> nameMap = new HashMap<>();
        for(OrgWarehouse warehouse : wares.getRows()){
            nameMap.put(warehouse.getId(),warehouse.getName());
        }
        List<Float> values = new ArrayList<>();
        for(Map<String,Object> map : result){
            String warehouseId = map.get("warehouseId")+"";
            float price = DataUtils.toBigDecimal(map.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
            if(nameMap.get(warehouseId) != null && price > 0) {
                names.add(nameMap.get(warehouseId));
                values.add(price);
            }

        }
        List<List<Float>> valuesList = new ArrayList<>();
        valuesList.add(values);
        dataMap.put("topArray",null);
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
        return new String[]{StatisticsDrawType.SALESMODULE, "分公司业绩排行榜"};
    }
}
