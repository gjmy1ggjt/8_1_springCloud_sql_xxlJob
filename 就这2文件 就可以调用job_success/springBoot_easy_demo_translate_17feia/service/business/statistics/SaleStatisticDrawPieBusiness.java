package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.finance.entity.fina.CustomerConsumption;
import com.yangshan.eship.finance.service.fina.FinanceOrderItemServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderServiceI;
import com.yangshan.eship.sales.business.statistics.dto.DrawItem;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Author:
 * @Description:LiuWei
 * @Date: 下午 2:15 2018/6/9
 */

@Service
public class SaleStatisticDrawPieBusiness implements OneStatisticsDrawBusinessI {



    @Autowired
    FinanceOrderItemServiceI financeOrderItemService;

    @Autowired
    RoleServiceI roleService;

    @Autowired
    CustomerAssignServiceI customerAssignService;


    @Override
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) throws ParseException {

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String staffId = SessionUtils.getUserId();
        String orgId = SessionUtils.getOrganizationId();
        String warehouseId = SessionUtils.getWarehouseId();
        List<Role> allRolesByStaffId = roleService.findAllRolesByStaffId(staffId);
        List<String> customerIds = customerAssignService.findByUserRoleCode(allRolesByStaffId, staffId, orgId, warehouseId);

        Date startDate=null;
        Date endDate=null;

        if(StringUtils.isNoneBlank(statisticsDrawsDto.getSearchStartDateTime())){
            startDate  = format.parse(statisticsDrawsDto.getSearchStartDateTime());
        }
        if(StringUtils.isNoneBlank(statisticsDrawsDto.getSearchEndDateTime())){
             endDate = format.parse(statisticsDrawsDto.getSearchEndDateTime());
        }
        customerIds.add("0");
        List<CustomerConsumption> customerConsumptions = financeOrderItemService.searchCustomerConsumption(customerIds, startDate, endDate);


        StatisticsDrawDto statisticsDrawDto = new StatisticsDrawDto();
        List<DrawItem> drawItems = new ArrayList<>();
        for(CustomerConsumption customerConsumption:customerConsumptions){
            DrawItem item = new DrawItem();
            item.setName(customerConsumption.getCustomerCode());
            item.setValue(customerConsumption.getPrice());

            drawItems.add(item);
        }
        if(drawItems.size() ==0){
            DrawItem item = new DrawItem();
            item.setName("无消费");
            item.setValue(0);
            drawItems.add(item);
        }
        statisticsDrawDto.setDrawPieAndCylinderItems(drawItems);
        statisticsDrawDto.setDrawType(StatisticsDrawType.DRAWPIE);
        return statisticsDrawDto;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String []{StatisticsDrawType.SALESMODULE,"客户消费额度"};
    }
}
