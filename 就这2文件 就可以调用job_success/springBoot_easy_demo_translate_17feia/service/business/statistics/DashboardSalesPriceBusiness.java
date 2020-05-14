package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamsDto;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardSeachKey;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.Param;
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
public class DashboardSalesPriceBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardSalesPriceBusiness.class);


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
        types.add(DashboardParamType.TIMETYPE);
        Map<String,Object> map  = DashboardUtil.initParams(vo,types,orgWarehouseService);

        String postUrl = url + "/view/salesPrice";
        List<Map> result = restForStatistics(postUrl,map);
        Map<String,List> mapResult = drawDataCreate(result,map);
        vo.setData(DashboardUtil.dataForJson(mapResult,"销售额统计图","单位(元)"));
        return vo;
    }

    public List<Map> restForStatistics(String url, Map<String,Object> params) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(params, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(url, entity, List.class);
        logger.debug("DashboardSalesPriceBusiness-entity:{}",entity);
        logger.debug("DashboardSalesPriceBusiness-result:{}",result);
        return result;
    }

    public Map<String,List>  drawDataCreate(List<Map> result,Map<String,Object> param){
        Map<String,List> dataMap = new HashMap<>();
        String timeType = (String)param.get(DashboardSeachKey.TIMETYPE.name());
        List<Integer> years = (List<Integer>)param.get(DashboardSeachKey.YEARS.name());
        List topArray = new ArrayList();
        List names = new ArrayList();
        List<List> valuesList = new ArrayList<>();

        for (Map m : result) {
            if(m.get("year") !=null){
                m.put("year",new Double((double)m.get("year")).intValue()+"年");
            }
            if(m.get("month") !=null){
                m.put("month",new Double((double)m.get("month")).intValue()+"月");
            }
            if(m.get("quarter") !=null){
                m.put("quarter",new Double((double)m.get("quarter")).intValue()+"季度");
            }
        }

        //当选类型为年的时候，只显示一根柱子
        if("YEAR".equals(timeType)){
            List values = new ArrayList();
            if(years == null) {
                for (Map m : result) {
                    names.add(m.get("year"));
                    values.add(DataUtils.toBigDecimal(m.get("price")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                }
            }else{
                Map<String,Object> mapResult = new HashMap<>();
                for (Map m : result) {
                    mapResult.put(m.get("year")+"",m.get("price"));
                }
                for(Integer year : years){
                    names.add(year+"年");
                    values.add(DataUtils.toBigDecimal(mapResult.get(year+"年")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                }
            }
            valuesList.add(values);
            topArray = null;

        }

        //当选择季度时
        if("QUARTER".equals(timeType)){

            names.add("1季度");
            names.add("2季度");
            names.add("3季度");
            names.add("4季度");

            Map<String,Map<String,Object>> dataResult = new HashMap<>();
            for (Map m : result) {
                Map<String,Object> yearMap = dataResult.get(m.get("year"));
                if(yearMap == null){
                    yearMap = new HashMap<>();
                    dataResult.put(m.get("year")+"",yearMap);
                }
                yearMap.put(m.get("quarter")+"",m.get("price"));
            }

            if(years == null) {
                for (Map.Entry<String, Map<String,Object>> entry : dataResult.entrySet()) {
                    topArray.add(entry.getKey());
                    List values = new ArrayList();
                    for(int i = 1; i < 5 ;i++){
                        if(entry.getValue().get(i+"季度")==null){
                            values.add(0);
                        }else{
                            values.add(DataUtils.toBigDecimal(entry.getValue().get(i+"季度")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                        }
                    }
                    valuesList.add(values);
                }
            }else{
                for(Integer year : years){
                    topArray.add(year+"年");
                    List values = new ArrayList();
                    Map valueMap = dataResult.get(year+"年");
                    if(valueMap == null){
                        valueMap = new HashMap();
                    }
                    for(int i = 1; i < 5 ;i++){
                        if(valueMap.get(i+"季度")==null){
                            values.add(0);
                        }else{
                            values.add(DataUtils.toBigDecimal(valueMap.get(i+"季度")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                        }
                    }
                    valuesList.add(values);
                }
            }
        }

        //当选择月份时
        if("MONTH".equals(timeType)){
            names.add("1月");
            names.add("2月");
            names.add("3月");
            names.add("4月");
            names.add("5月");
            names.add("6月");
            names.add("7月");
            names.add("8月");
            names.add("9月");
            names.add("10月");
            names.add("11月");
            names.add("12月");
            Map<String,Map<String,Object>> dataResult = new HashMap<>();
            for (Map m : result) {
                Map<String,Object> yearMap = dataResult.get(m.get("year"));
                if(yearMap == null){
                    yearMap = new HashMap<>();
                    dataResult.put(m.get("year")+"",yearMap);
                }
                yearMap.put(m.get("month")+"",m.get("price"));
            }
            if(years == null) {
                for (Map.Entry<String, Map<String,Object>> entry : dataResult.entrySet()) {
                    topArray.add(entry.getKey());
                    List values = new ArrayList();
                    for(int i = 1; i < 13 ;i++){
                        if(entry.getValue().get(i+"月")==null){
                            values.add(0);
                        }else{
                            values.add(DataUtils.toBigDecimal(entry.getValue().get(i+"月")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                        }
                    }
                    valuesList.add(values);
                }
            }else{
                for(Integer year : years){
                    topArray.add(year+"年");
                    List values = new ArrayList();
                    Map valueMap = dataResult.get(year+"年");
                    if(valueMap == null){
                        valueMap = new HashMap();
                    }
                    for(int i = 1; i < 13 ;i++){
                        if(valueMap.get(i+"月")==null){
                            values.add(0);
                        }else{
                            values.add(DataUtils.toBigDecimal(valueMap.get(i+"月")).setScale(2, BigDecimal.ROUND_HALF_UP).floatValue());
                        }
                    }
                    valuesList.add(values);
                }
            }
        }

        dataMap.put("topArray",topArray);
        dataMap.put("xNameArray",names);
        dataMap.put("yValueArray",valuesList);
        return dataMap;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, "销售额统计图"};
    }
}
