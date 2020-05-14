package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tsd
 */
@Service
public class DashboardSalesDataStatisticsBusiness implements DashboardDrawBusinessI {
    private static final Logger logger = LoggerFactory.getLogger(DashboardSalesDataStatisticsBusiness.class);



    @Value("${dashboard.statistics.url}")
    private String url;


    @Autowired
    private RestTemplate simpleRestTemplate;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;
    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        //初始化参数
       /* List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.TIMESLOT);*/
        Map<String,Object> map  = new HashMap<>();//DashboardUtil.initParams(vo,types,orgWarehouseService);
        map.put(DashboardSeachKey.OID.name(), SessionUtils.getOrganizationId());
        String postUrl = url + "/salesData/list";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        result.get(0).put("customer",dataForJson((Double) result.get(0).get("customerCount"),(Integer)result.get(0).get("orderCustomer")));
        vo.setData(result.get(0));
        return vo;
    }
    public Map dataForJson(Double customerCount,Integer oerderCustomer){
        int noOderCustomer =0;
        if(customerCount != null) {
             noOderCustomer = customerCount.intValue() - oerderCustomer;
        }
        List<Map> dataList = new ArrayList<>();
        Map data = new HashMap();
        data.put("value", noOderCustomer);
        data.put("name","未下单客户 "+noOderCustomer);
        dataList.add(data);
        Map data1 = new HashMap();
        data1.put("value", oerderCustomer);
        data1.put("name","下单客户 "+oerderCustomer);
        dataList.add(data1);
        Map map = new HashMap();
        Map tooltip = new HashMap();
        tooltip.put("trigger","item");
        tooltip.put("formatter","{a} <br/>{b}  ({d}%)");
        map.put("tooltip",tooltip);
        Map series = new HashMap();
        series.put("type","pie");
        series.put("data",dataList);
        series.put("radius","60%");
        List<String> center = new ArrayList<>();
        center.add("50%");
        center.add("60%");
        series.put("center",center);
        Map itemStyle = new HashMap();
        Map emphasis = new HashMap();
        emphasis.put("shadowBlur",10);
        emphasis.put("shadowOffsetX",0);
        emphasis.put("shadowColor","(0, 0, 0, 0.5)");
        itemStyle.put("emphasis",emphasis);
        series.put("itemStyle",itemStyle);
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
        return new String[]{StatisticsDrawType.SALESMODULE, "销售数据总计"};
    }
}
