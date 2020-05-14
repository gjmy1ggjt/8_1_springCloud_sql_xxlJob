package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
import com.yangshan.eship.sales.entity.sale.AccountType;
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
 * @Date: 2018-12-29 11:33
 * @Description:
 */
@Service
public class DashboardAccountTypeUsersBusiness implements DashboardDrawBusinessI {
    private static final String ECHART_NAME = "账期类型统计图";

    private Logger logger = LoggerFactory.getLogger(DashboardAccountTypeUsersBusiness.class);

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

        String postUrl = url+"/userAccountData/getUserAccountTypeData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardAccountTypeUsersBusiness-entity:{}", entity);

        vo.setData(dataForJson(result));
        return vo;
    }

    private Map dataForJson(List<Map> result){
        Map data = new HashMap();

        //初始化
        LinkedHashMap<String, Integer> accountMap = new LinkedHashMap();
        for (AccountType accountType : AccountType.values()) {
            accountMap.put(accountType.name(), 0);
        }

        List<String> xDataList = new ArrayList<>();
        List<Integer> yDataList = new ArrayList<>();
        for (Map map : result) {
            if (!map.containsKey("payment_days_type")) {
                continue;
            }

            String accountType = map.get("payment_days_type").toString();
            Double userTotal = map.containsKey("user_total") ? (double) map.get("user_total") : 0d;
            if (accountMap.containsKey(accountType)) {
                accountMap.put(accountType, userTotal.intValue());
            }
        }

//        Map titleMap = new HashMap();
//        titleMap.put("text", ECHART_NAME);
//        data.put("title", titleMap);

        data.put("color", DashboardUtil.getColors(1));

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "shadow");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);

        accountMap.forEach((accountType, userTotal) -> {
            xDataList.add(AccountType.valueOf(accountType).getDesc());
            yDataList.add(userTotal);
        });

        List<Map> xAxisMapList = new ArrayList<>();
        Map xAxisMap = new HashMap();
        xAxisMap.put("type", "category");
        xAxisMap.put("data", xDataList);
        xAxisMapList.add(xAxisMap);

        data.put("xAxis", xAxisMapList);

        Map yAxisMap = new HashMap();
        yAxisMap.put("type", "value");
        yAxisMap.put("name", "客户数(个)");

        data.put("yAxis", yAxisMap);

        List<Map> series = new ArrayList<>();
        Map seriesMap = new HashMap();
        seriesMap.put("name", "客户数");
        seriesMap.put("type", "bar");
        seriesMap.put("barWidth", "50%");
        //柱子最大宽度
        seriesMap.put("barMaxWidth", 100);
        seriesMap.put("data", yDataList);

        Map labelMap = new HashMap();
        Map normalMap = new HashMap();
        normalMap.put("show", true);
        normalMap.put("position", "top");
        labelMap.put("normal", normalMap);
        seriesMap.put("label", labelMap);

        series.add(seriesMap);
        data.put("series", series);

        DashboardUtil.addToolBox(data);

        logger.debug("=========> 账期类型统计图数据: {}", data.toString());

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
