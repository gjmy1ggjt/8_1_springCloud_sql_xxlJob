package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.finance.service.fina.FinanceOrderItemStaticsServiceI;
import com.yangshan.eship.sales.business.statistics.dto.DrawItem;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * product模块的 产品金额信息统计(饼图)
 * @author:  TuShiDing
 * @date: 2018/06/08 11:12:00
 */
@Service
public class ProductStatisticDrawPieBusiness implements OneStatisticsDrawBusinessI {
    @Autowired
    private FinanceOrderItemStaticsServiceI financeOrderItemStaticsServiceI;
    @Autowired
    RoleServiceI roleService;

    @Autowired
    CustomerAssignServiceI customerAssignService;
    @Override
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) {
        String staffId = SessionUtils.getUserId();
        String orgId = SessionUtils.getOrganizationId();
        String warehouseId = SessionUtils.getWarehouseId();
        List<Role> allRolesByStaffId = roleService.findAllRolesByStaffId(staffId);
        List<String> customerIds = customerAssignService.findByUserRoleCode(allRolesByStaffId, staffId, orgId, warehouseId);
        StatisticsDrawDto statisticsDrawDto = new StatisticsDrawDto();
        statisticsDrawDto.setDrawType(StatisticsDrawType.DRAWPIE);
        List<Map<String,Object>> resultList=financeOrderItemStaticsServiceI.findProductInfoChartData(customerIds,statisticsDrawsDto.getSearchStartDateTime(),statisticsDrawsDto.getSearchEndDateTime());
        List<DrawItem> drawItems = new ArrayList<>();
        for(Map<String,Object> map:resultList){
            DrawItem item = new DrawItem();
            item.setName(String.valueOf(map.get("name")));
            item.setValue(map.get("value"));
            drawItems.add(item);
        }
        statisticsDrawDto.setDrawPieAndCylinderItems(drawItems);

        return statisticsDrawDto;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String []{StatisticsDrawType.PRODUCTMODULE,"产品-各个产品金额对比"};
    }
}
