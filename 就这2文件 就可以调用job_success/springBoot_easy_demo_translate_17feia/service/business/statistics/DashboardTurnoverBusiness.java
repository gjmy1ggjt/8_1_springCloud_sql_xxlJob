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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tsd
 */
@Service
public class DashboardTurnoverBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardTurnoverBusiness.class);



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
        String postUrl = url+"/view/turnover";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        vo.setData(dataForJson(result));
        return vo;
    }

    public Map dataForJson(List<Map<String,Object>> result){
        List<String> xData = new ArrayList<>();
        List<Double> sData1 = new ArrayList<>();
        List<Double> sData2 = new ArrayList<>();
        for(Map<String,Object> m : result){
            Double mo = (Double)m.get("moth");
            Double cost = (Double) m.get("cost");
            Double profit = (Double) m.get("profit");
            xData.add(mo.intValue()+"月");
            sData1.add(DashboardUtil.splitNumber(DataUtils.toBigDecimal(cost),2));
            sData2.add(DashboardUtil.splitNumber(DataUtils.toBigDecimal(profit-cost),2));
        }
        Map map = new HashMap();
        Map legend = new HashMap();
        List<String> data = new ArrayList<>();
        data.add("成本");
        data.add("利润");
        legend.put("data",data);
        map.put("legend",legend);

        Map tooltip = new HashMap();
        tooltip.put("trigger","axis");
        Map axisPointer = new HashMap();
        axisPointer.put("type","shadow");
        tooltip.put("axisPointer",axisPointer);
        map.put("tooltip",tooltip);

        Map grid = new HashMap();
        grid.put("left","3%");
        grid.put("right","4%");
        grid.put("bottom","3%");
        grid.put("containLabel",true);
        map.put("grid",grid);

        List<Map> xAxis = new ArrayList<>();
        Map xAxi = new HashMap();
        xAxi.put("type","category");
        xAxi.put("data",xData);
        xAxis.add(xAxi);
        map.put("xAxis",xAxis);

        List<Map> yAxis = new ArrayList<>();
        Map yAxi = new HashMap();
        yAxi.put("type","value");
        yAxi.put("name", "单位(元)");
        yAxis.add(yAxi);
        map.put("yAxis",yAxis);

        List<Map> series = new ArrayList<>();
        Map serie = new HashMap();
        serie.put("name","成本");
        serie.put("type","bar");
        serie.put("stack","金额");
        serie.put("data",sData1);

        Map serie1 = new HashMap();
        serie1.put("name","利润");
        serie1.put("type","bar");
        serie1.put("stack","金额");
        serie1.put("data",sData2);
        series.add(serie);
        series.add(serie1);
        map.put("series",series);

        DashboardUtil.addToolBox(map);
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
        return new String[]{StatisticsDrawType.FINANCEMODULE, "营业额统计图"};
    }
}
