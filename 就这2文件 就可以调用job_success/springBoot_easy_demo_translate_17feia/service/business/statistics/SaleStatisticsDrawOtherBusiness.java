package com.yangshan.eship.sales.business.statistics;

import com.yangshan.eship.order.entity.cust.CustomerInfo;
import com.yangshan.eship.order.service.orde.CustomerInfoServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.finance.dto.fina.SalePerformanceStatisticDto;
import com.yangshan.eship.finance.dto.fina.SalePerformanceStatisticType;
import com.yangshan.eship.finance.service.fina.FinanceOrderItemServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderServiceI;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsOtherDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SaleStatisticsDrawOtherBusiness implements OneStatisticsDrawBusinessI {


    @Autowired
    private FinanceOrderItemServiceI financeOrderItemService;

    @Autowired
    private FinanceOrderServiceI financeOrderService;

    @Autowired
    private SalesStaffAssignServiceI salesStaffAssignService;

    @Autowired
    private RoleServiceI roleService;

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private CustomerInfoServiceI customerInfoService;

    @Autowired
    private SalePerformanceStatisticDrawBusiness spf;

    @Override
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        //职员id
        String staffId = SessionUtils.getUserId();
        //组织id
        String organizationId = SessionUtils.getOrganizationId();
        //分公司id
        String warehouseId = SessionUtils.getWarehouseId();

        //--------------------------默认查询当月---------------------------//
        //--------------------------默认查询当月---------------------------//
        //获取当月的第一天
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        //将小时至0
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        //将分钟至0
        calendar.set(Calendar.MINUTE, 0);
        //将秒至0
        calendar.set(Calendar.SECOND,0);
        //将毫秒至0
        calendar.set(Calendar.MILLISECOND, 0);
        //获得当前月第一天
        Date startDate = calendar.getTime();


        // 获取当月的最后一天
        //将当前月加1；
        calendar.add(Calendar.MONTH, 1);
        //在当前月的下一月基础上减去1毫秒
        calendar.add(Calendar.MILLISECOND, -1);
        //获得当前月最后一天
        Date endDate = calendar.getTime();

        //-------------------------------------------------------------------//

        if (StringUtils.isNoneBlank(statisticsDrawsDto.getSearchStartDateTime())) {
            startDate = format.parse(statisticsDrawsDto.getSearchStartDateTime());
        }
        if (StringUtils.isNoneBlank(statisticsDrawsDto.getSearchEndDateTime())) {
            endDate = format.parse(statisticsDrawsDto.getSearchEndDateTime());
        }

        List<Role> allRolesByStaffId = roleService.findAllRolesByStaffId(staffId);
        List<String> saleIds = departmentService.findByUserRoleCode(allRolesByStaffId, staffId, organizationId, warehouseId);
        if(saleIds==null || saleIds.size() ==0){
            saleIds = new ArrayList<>();
            saleIds.add(staffId);
        }
        //所有客户
        List<CustomerAssign> customers = customerAssignService.findBySalesIds(saleIds);
        //新客户
        List<String> custIds = new ArrayList<>();

        for(CustomerAssign customerAssign:customers){
            custIds.add(customerAssign.getCustomerId());
        }
        //@Author: hyl @Date: 2020-02-29 17:18 @Description: UserExtensionService 迁移到 CustomerInfoService
        List<CustomerInfo> thenCustomers = customerInfoService.findByUserIds(custIds);
        int thenCount =0;
        if(thenCustomers == null){
            thenCustomers = new ArrayList<>();
        }else{
            for(CustomerInfo user:thenCustomers) {
                if (startDate.getTime() < user.getFirstOrderTime().getTime()
                        && user.getFirstOrderTime().getTime() < endDate.getTime()) {
                    thenCount++;
                }
            }
        }
        //下单客户
        int orderCustomerCount = spf.getPlaceAnOrderCustomerCount(customers);

        //获取销售月销售目标
        List<SalesStaffAssign> salesStaffAssignList = spf.salesStaffAssignList(saleIds);
        //获取销售统计数据
        Map<String, Object> stringObjectMap =  spf.getSalesPerformanceStatistic(statisticsDrawsDto,saleIds);
        //完成
        Map<String, BigDecimal> completeMap = (Map<String, BigDecimal>) stringObjectMap.get(SalePerformanceStatisticType.COMPLETE.name());
        //收款
        Map<String, BigDecimal> receiptMap = (Map<String, BigDecimal>) stringObjectMap.get(SalePerformanceStatisticType.RECEIPT.name());
        //待收款
        Map<String, BigDecimal> receivableMap = (Map<String, BigDecimal>) stringObjectMap.get(SalePerformanceStatisticType.RECEIVABLE.name());

        List<SalePerformanceStatisticDto> salePerformanceStatisticDtoList = new ArrayList<>();

        BigDecimal monthlyGoalsValues = DataUtils.toBigDecimal("0.00");
        BigDecimal completeValues = DataUtils.toBigDecimal("0.00");
        BigDecimal receiptValues = DataUtils.toBigDecimal("0.00");
        BigDecimal receivableValues = DataUtils.toBigDecimal("0.00");
        for(SalesStaffAssign salesStaffAssign:salesStaffAssignList){
            //BigDecimal monthlyGoalsValue= salesStaffAssign.getMonthlyGoals() == null?DataUtils.toBigDecimal("0.00"):salesStaffAssign.getMonthlyGoals();
            BigDecimal completeValue = completeMap.getOrDefault(salesStaffAssign.getSalesStaffId(), DataUtils.toBigDecimal("0.00"));
            BigDecimal receiptValue = receiptMap.getOrDefault(salesStaffAssign.getSalesStaffId(), DataUtils.toBigDecimal("0.00"));
            BigDecimal receivableValue = receivableMap.getOrDefault(salesStaffAssign.getSalesStaffId(), DataUtils.toBigDecimal("0.00"));

            //monthlyGoalsValues = monthlyGoalsValues.add(monthlyGoalsValue);
            completeValues = completeValues.add(completeValue);
            receiptValues =receiptValues.add(receiptValue);
            receivableValues = receivableValues.add(receivableValue);
        }

        String receiableStr = receivableValues.toString();
        String[] names = new String[]{"总客户数量","总下单客户","当月目标","当月完成","当月收款","总待收款","当月新客户数"};
        String[] values = new String[]{String.valueOf(customers.size()),orderCustomerCount+"",
                monthlyGoalsValues.toPlainString(),completeValues.toPlainString(),receiptValues.toPlainString(),receiableStr,String.valueOf(thenCount)};
        String[] units = new String[]{"位","位","元","元","元","元","位"};
        List<StatisticsOtherDto> otherDtoList = new ArrayList<>();
        for(int i = 0; i< names.length; i++){
            StatisticsOtherDto statisticsOtherDto = new StatisticsOtherDto();
            statisticsOtherDto.setId(i);
            statisticsOtherDto.setName(names[i]);
            statisticsOtherDto.setUnit(units[i]);
            statisticsOtherDto.setValue(values[i]);
            otherDtoList.add(statisticsOtherDto);
        }
        statisticsDrawsDto.setOtherDtoList(otherDtoList);
        statisticsDrawsDto.setDrawType(StatisticsDrawType.DRAWGOTHER);
        return statisticsDrawsDto;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE,"销售总计"};
    }
}
