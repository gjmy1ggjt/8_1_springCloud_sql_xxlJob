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
public class DashboardCustomerDebtBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardCustomerDebtBusiness.class);



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
        String postUrl = url + "/customerDebt/list";
        List<Map<String,Object>> result = restForStatistics(postUrl,map);
        DashboardUtil.sortData(result,"price");
        vo.setData(dataForJson(result));
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

    public Map dataForJson(List<Map<String,Object>> result){
        List<String> categorys = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        int index = 0;
        for(Map item : result){
            if(index < 10) {
                Object name = item.get("name");
                if(name != null) {
                    categorys.add("" + item.get("name"));
                }else{
                    categorys.add("" + item.get("code"));
                }
                values.add(DataUtils.toBigDecimal(Float.parseFloat(item.get("price")+"")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                index++;
            }
        }
        Map data = new HashMap();
        List<String> colors = new ArrayList<>();
        colors.add("#3398DB");
        data.put("color",colors);


        Map tooltip = new HashMap();
        tooltip.put("trigger","axis");
        Map axisPointer = new HashMap();
        axisPointer.put("type","shadow");
        tooltip.put("axisPointer",axisPointer);
        data.put("tooltip",tooltip);

        Map grid = new HashMap();
        grid.put("left","3%");
        grid.put("right","4%");
        grid.put("bottom","3%");
        grid.put("containLabel",true);
        data.put("grid",grid);

        List<Map> xAxis = new ArrayList<>();
        Map xAxi = new HashMap();
        xAxi.put("type","category");
        xAxi.put("data",categorys);
        Map axisTick = new HashMap();
        axisTick.put("alignWithLabel",true);
        xAxi.put("axisTick",axisTick);

        //X轴文字旋转-45度
        Map axisLabelMap = new HashMap();
        axisLabelMap.put("interval", 0);
        axisLabelMap.put("rotate", -45);
        xAxi.put("axisLabel", axisLabelMap);

        xAxis.add(xAxi);
        data.put("xAxis",xAxis);

        List<Map> yAxis = new ArrayList<>();
        Map yAxi = new HashMap();
        yAxi.put("type","value");
        yAxi.put("name","单位(元)");

        yAxis.add(yAxi);
        data.put("yAxis",yAxis);

        List<Map> series = new ArrayList<>();
        Map serie = new HashMap();
        serie.put("type","bar");
        serie.put("barWidth","60%");
        serie.put("data",values);
        Map labelMap = new HashMap();
        Map normalMap = new HashMap();
        normalMap.put("show", true);
        normalMap.put("position", "right");
        labelMap.put("normal", normalMap);
        serie.put("label", labelMap);
        series.add(serie);
        data.put("series",series);

        DashboardUtil.addToolBox(data);
        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, "客户欠款排行榜TOP10"};
    }
}
