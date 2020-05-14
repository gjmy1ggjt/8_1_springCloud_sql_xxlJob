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

import java.util.*;

/**
 * tsd
 */
@Service
public class DashboardSalesAchievementBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardSalesAchievementBusiness.class);



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
        String postUrl = url + "/view/salesAchievement";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        DashboardUtil.sortData(result,"price");
        vo.setData(dataForJson(result));
        return vo;
    }

    public Map dataForJson(List<Map<String,Object>> result1){
        Map data = new HashMap();
        List<Map<String,Object>> result = new ArrayList<>();
        int i = 0;
        for (Map map : result1) {
            if(i<10) {
                Double lineOfCreditUsed = Double.parseDouble(map.get("price").toString());
                if (lineOfCreditUsed > 0 && map.get("name") != null) {
                    result.add(map);
                    i++;
                }
            }
        }

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "shadow");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);


        Map xAxisMap = new HashMap();
        xAxisMap.put("xAxisMap", "value");
        xAxisMap.put("name", "单位(元)");
        data.put("xAxis", xAxisMap);


        Map yAxisMap = new HashMap();
        yAxisMap.put("type", "category");
        yAxisMap.put("inverse", true);

        String[] yAxisData = new String[result.size()];
        int a = 0;
        for (Map map : result) {
            Double lineOfCreditUsed = Double.parseDouble(map.get("price").toString());
            yAxisData[a] = map.get("name").toString();
            a++;
        }
        yAxisMap.put("data", Arrays.asList(yAxisData));
        data.put("yAxis", yAxisMap);

        List<Map> series = new ArrayList<>();

            Map seriesMap = new HashMap();
            seriesMap.put("type", "bar");

            List<Double> seriesData = new ArrayList<>();

            for (Map map : result) {
                Double lineOfCreditUsed = Double.parseDouble(map.get("price").toString());
                seriesData.add(DashboardUtil.splitNumber(DataUtils.toBigDecimal(lineOfCreditUsed), 2));
            }

            seriesMap.put("data", seriesData);

            series.add(seriesMap);

        data.put("series", series);

        DashboardUtil.addToolBox(data);

        return data;
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
        return new String[]{StatisticsDrawType.SALESMODULE, "销售业绩排行榜TOP10"};
    }
}
