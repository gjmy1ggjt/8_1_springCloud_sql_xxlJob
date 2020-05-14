package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import com.yangshan.eship.sales.utils.DashboardUtil;
import com.yangshan.eship.sales.vo.DashboardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: hyl
 * @Date: 2018/12/28 9:31
 * @Description:
 */
@Service
public class DashboardUsersBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardUsersBusiness.class);

    @Value("${dashboard.statistics.url}")
    private String url;
    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.TIMETYPE);

        Map<String, Object> map = DashboardUtil.initParams(vo, types, orgWarehouseService);
        Map jsonMap = DashboardUtil.getDrawDashboardMap(map, url + "/view/registerAndPlaceOrderUsersData");
        vo.setData(jsonMap);
        return vo;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.USERMODULE, "注册下单用户走势图"};
    }
}
