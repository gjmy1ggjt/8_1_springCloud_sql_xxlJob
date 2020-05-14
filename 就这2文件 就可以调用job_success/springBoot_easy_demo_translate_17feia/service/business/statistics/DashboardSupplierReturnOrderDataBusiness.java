package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.service.syst.RegionServiceI;
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
 * @Author: Kevin
 * @Date: 2019-01-02 14:15
 * @Description:
 */
@Service
public class DashboardSupplierReturnOrderDataBusiness implements DashboardDrawBusinessI {
    private static final String ECHART_NAME = "海外合作商仓库退货统计图";

    private Logger logger = LoggerFactory.getLogger(DashboardSupplierReturnOrderDataBusiness.class);

    @Autowired
    private RestTemplate simpleRestTemplate;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Autowired
    private RegionServiceI regionService;

    @Value("${dashboard.statistics.url}")
    private String url;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.TIMESLOT);
        Map<String,Object> map  = DashboardUtil.initParams(vo, types, orgWarehouseService);

        String postUrl = url + "/viewOrderData/getSupplierReturnOrderCalData";
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(map, headers);
        //result 返回的json数据
        List<Map> result = simpleRestTemplate.postForObject(postUrl, entity, List.class);
        logger.debug("DashboardSupplierReturnOrderDataBusiness-entity:{}", entity);

        vo.setData(dataForJson(result));
        return vo;
    }

    private Map dataForJson(List<Map> result) {
        Map data = new HashMap();

        List<String> xDataList = new ArrayList<>();
        List<Integer> orderCountList = new ArrayList<>();
        List<Float> orderWeightList = new ArrayList<>();
        for (Map map : result) {
            String xData = map.get("logistics_logistics_name").toString();
            xDataList.add(xData);
            Double orderCount = (Double) map.get("total_count");
            Double orderWeight = (Double) map.get("total_weight");
            orderCountList.add(orderCount.intValue());
            orderWeightList.add(orderWeight.floatValue());
        }

//        Map titleMap = new HashMap();
//        titleMap.put("text", ECHART_NAME);
//        data.put("title", titleMap);

        List<String> colorList = DashboardUtil.getColors(2);
        data.put("color", colorList);

        //获取x轴最大文字个数, 控制与底部间的距离
        int maxLength = 0;
        for (String text : xDataList) {
            if (text.length() >= maxLength) {
                maxLength = text.length();
            }
        }
        Map gridMap = new HashMap();
        gridMap.put("y2", maxLength * 10);
        data.put("grid", gridMap);

        Map tooltipMap = new HashMap();
        tooltipMap.put("trigger", "axis");
        Map axisPointerMap = new HashMap();
        axisPointerMap.put("type", "cross");
        tooltipMap.put("axisPointer", axisPointerMap);
        data.put("tooltip", tooltipMap);

        Map legendMap = new HashMap();
        String[] legendArr = new String[]{"订单数", "货物总重"};
        List<String> legendList = Arrays.asList(legendArr);

        String[] stateArr = new String[]{"票", "kg"};
        List<String> stateList = Arrays.asList(stateArr);

        legendMap.put("data", legendList);
        data.put("legend", legendMap);

        Map xAxisMap = new HashMap();
        xAxisMap.put("data", xDataList);

        //X轴文字旋转-45度
        Map axisLabelMap = new HashMap();
        axisLabelMap.put("interval", 0);
        axisLabelMap.put("rotate", -45);
        xAxisMap.put("axisLabel", axisLabelMap);
        data.put("xAxis", xAxisMap);

        List<Map> yAxisMapList = new ArrayList<>();
        List<Map> seriesMapList = new ArrayList<>();
        for (int i = 0; i < legendList.size(); i++) {
            Map singleYAxisMap = new HashMap();
            singleYAxisMap.put("type", "value");
            singleYAxisMap.put("name", legendList.get(i));
            singleYAxisMap.put("min", 0);
            singleYAxisMap.put("position", i == 0 ? "left" : "right");
            Map axisLineMap = new HashMap();
            Map lineStyleMap = new HashMap();
            lineStyleMap.put("color", colorList.get(i));
            axisLineMap.put("lineStyle", lineStyleMap);
            singleYAxisMap.put("axisLine", axisLineMap);
            Map formatterMap = new HashMap();
            formatterMap.put("formatter", "{value}" + " " + stateList.get(i));
            singleYAxisMap.put("axisLabel", formatterMap);
            yAxisMapList.add(singleYAxisMap);

            Map seriesMap = new HashMap();
            seriesMap.put("name", legendList.get(i));
            seriesMap.put("type", i == 0 ? "bar" : "line");
            seriesMap.put("yAxisIndex", i);
            seriesMap.put("data", i == 0 ? orderCountList : orderWeightList);
            //柱子最大宽度
            seriesMap.put("barMaxWidth", 100);
            seriesMapList.add(seriesMap);
        }
        data.put("yAxis", yAxisMapList);
        data.put("series", seriesMapList);

        DashboardUtil.addToolBox(data);

        logger.debug("=========> 海外合作商仓库退货统计图数据: {}", data.toString());

        return data;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, ECHART_NAME};
    }
}
