package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
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

/**
 * tsd
 * 总公司概览总计
 */
@Service
public class DashboardHeadquartersBusiness implements DashboardDrawBusinessI {
    private static final Logger logger = LoggerFactory.getLogger(DashboardHeadquartersBusiness.class);



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
        /* types.add(DashboardParamType.TIMESLOT);*/
        Map<String,Object> map  = new HashMap<>();
        map.put(DashboardSeachKey.OID.name(),SessionUtils.getOrganizationId());
        String postUrl = url + "/headquarters/list";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        DataGrid<OrgWarehouse> wares = orgWarehouseService.list(SessionUtils.getOrganizationId(), null);
        result.get(0).put("warehouses",wares.getRows().size());
        vo.setData(result.get(0));
        return vo;
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
        return new String[]{StatisticsDrawType.SALESMODULE, "总公司概览总计"};
    }

}
