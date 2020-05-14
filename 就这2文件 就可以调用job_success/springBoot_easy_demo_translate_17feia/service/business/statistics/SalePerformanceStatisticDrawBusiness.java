package com.yangshan.eship.sales.business.statistics;

import com.google.common.collect.Lists;
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
import com.yangshan.eship.order.service.orde.OrderServiceI;
import com.yangshan.eship.sales.business.statistics.dto.DrawItem;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawListDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author: hyl
 * @Date: 2018/6/11 16:35
 * @Description:
 */
@Service
public class SalePerformanceStatisticDrawBusiness implements OneStatisticsDrawBusinessI {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private FinanceOrderItemServiceI financeOrderItemService;

    @Autowired
    private FinanceOrderServiceI financeOrderService;

    @Autowired
    private SalesStaffAssignServiceI salesStaffAssignService;

    @Autowired
    private OrderServiceI orderService;

    @Autowired
    private RoleServiceI roleService;

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private CustomerInfoServiceI customerInfoService;

    @Override
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) throws ParseException {

        //获取销售ids
        List<String> saleIds = getSaleIds();
        //获取销售月销售目标
        List<SalesStaffAssign> salesStaffAssignList = salesStaffAssignList(saleIds);
        //获取销售统计数据
        Map<String, Object> stringObjectMap =  getSalesPerformanceStatistic(statisticsDrawsDto,saleIds);

        //完成
        Map<String, BigDecimal> completeMap = (Map<String, BigDecimal>) stringObjectMap.get(SalePerformanceStatisticType.COMPLETE.name());
        //收款
        Map<String, BigDecimal> receiptMap = (Map<String, BigDecimal>) stringObjectMap.get(SalePerformanceStatisticType.RECEIPT.name());
        //待收款
        Map<String, BigDecimal> receivableMap = (Map<String, BigDecimal>) stringObjectMap.get(SalePerformanceStatisticType.RECEIVABLE.name());

        List<SalePerformanceStatisticDto> salePerformanceStatisticDtoList = new ArrayList<>();
        salesStaffAssignList.forEach(salesStaffAssign -> {

            //BigDecimal monthlyGoalsValue = salesStaffAssign.getMonthlyGoals();

            SalePerformanceStatisticDto salePerformanceStatisticDto = new SalePerformanceStatisticDto();
            salePerformanceStatisticDto.setStaffId(salesStaffAssign.getSalesStaffId());
            //salePerformanceStatisticDto.setMonthlyGoals(monthlyGoalsValue);
            salePerformanceStatisticDto.setName(salesStaffAssign.getSalesStaffName());

            BigDecimal completeValue = completeMap.getOrDefault(salesStaffAssign.getSalesStaffId(), DataUtils.toBigDecimal("0.00"));
            BigDecimal receiptValue = receiptMap.getOrDefault(salesStaffAssign.getSalesStaffId(), DataUtils.toBigDecimal("0.00"));
            BigDecimal receivableValue = receivableMap.getOrDefault(salesStaffAssign.getSalesStaffId(), DataUtils.toBigDecimal("0.00"));

            salePerformanceStatisticDto.setComplete(completeValue);
            salePerformanceStatisticDto.setReceipt(receiptValue);
            salePerformanceStatisticDto.setReceivable(receivableValue);
            salePerformanceStatisticDtoList.add(salePerformanceStatisticDto);
        });

        List<DrawItem> monthlyItemList = new ArrayList<>();
        List<DrawItem> completeItemList = new ArrayList<>();
        List<DrawItem> receiptItemList = new ArrayList<>();
        List<DrawItem> receivableItemList = new ArrayList<>();

        salePerformanceStatisticDtoList.forEach(salePerformanceStatisticDto -> {

            //------------------------------------ 当月目标-----------------------------------------------------//
            DrawItem monthlyGoalsItem = new DrawItem();
            monthlyGoalsItem.setName(salePerformanceStatisticDto.getName());
            monthlyGoalsItem.setValue(salePerformanceStatisticDto.getMonthlyGoals());
            monthlyItemList.add(monthlyGoalsItem);
            //---------------------------------------------------------------------------------------------------//

            //------------------------------------ 完成 -----------------------------------------------------//
            DrawItem completeItem = new DrawItem();
            completeItem.setName(salePerformanceStatisticDto.getName());
            completeItem.setValue(salePerformanceStatisticDto.getComplete());
            completeItemList.add(completeItem);
            //---------------------------------------------------------------------------------------------------//

            //------------------------------------ 收款 -----------------------------------------------------//
            DrawItem receiptItem = new DrawItem();
            receiptItem.setName(salePerformanceStatisticDto.getName());
            receiptItem.setValue(salePerformanceStatisticDto.getReceipt());
            receiptItemList.add(receiptItem);
            //---------------------------------------------------------------------------------------------------//

            //------------------------------------ 待收款 -----------------------------------------------------//
            DrawItem receivableItem = new DrawItem();
            receivableItem.setName(salePerformanceStatisticDto.getName());
            receivableItem.setValue(salePerformanceStatisticDto.getReceivable());
            receivableItemList.add(receivableItem);
            //---------------------------------------------------------------------------------------------------//

        });

        StatisticsDrawDto statisticsDrawDto = new StatisticsDrawDto();
        List<StatisticsDrawListDto> statisticsDrawListDtoList = new ArrayList<>();

        StatisticsDrawListDto monthlyGoalStatisticsDrawListDto = new StatisticsDrawListDto();
        monthlyGoalStatisticsDrawListDto.setName("当月目标");
        monthlyGoalStatisticsDrawListDto.setDrawItemList(monthlyItemList);
        statisticsDrawListDtoList.add(monthlyGoalStatisticsDrawListDto);

        StatisticsDrawListDto completeStatisticsDrawListDto = new StatisticsDrawListDto();
        completeStatisticsDrawListDto.setName(SalePerformanceStatisticType.COMPLETE.getDesc());
        completeStatisticsDrawListDto.setDrawItemList(completeItemList);
        statisticsDrawListDtoList.add(completeStatisticsDrawListDto);

        StatisticsDrawListDto receiptStatisticsDrawListDto = new StatisticsDrawListDto();
        receiptStatisticsDrawListDto.setName(SalePerformanceStatisticType.RECEIPT.getDesc());
        receiptStatisticsDrawListDto.setDrawItemList(receiptItemList);
        statisticsDrawListDtoList.add(receiptStatisticsDrawListDto);

        StatisticsDrawListDto receivableStatisticsDrawListDto = new StatisticsDrawListDto();
        receivableStatisticsDrawListDto.setName(SalePerformanceStatisticType.RECEIVABLE.getDesc());
        receivableStatisticsDrawListDto.setDrawItemList(receivableItemList);
        statisticsDrawListDtoList.add(receivableStatisticsDrawListDto);

        statisticsDrawDto.setDrawGroupAndStackLineItems(statisticsDrawListDtoList);
        statisticsDrawDto.setDrawType(StatisticsDrawType.DRAWGROUP);


        return statisticsDrawDto;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.SALESMODULE, "销售业绩统计"};
    }

    public static void main(String[] args) {

    }

    /**
     * @Author: hyl
     * @Date: 2018/6/21 16:42
     * @Description:获取销售统计数据
     */
    public Map<String, Object> getSalesPerformanceStatistic(StatisticsDrawDto statisticsDrawsDto,List<String> saleIds ) throws ParseException {

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
//            endDate = format.parse(statisticsDrawsDto.getSearchEndDateTime());
        }

        //获取销售金额
        Map<String, Object> stringObjectMap =
                financeOrderService.getSalesPerformanceStatistic(saleIds, startDate, endDate);

        return stringObjectMap;
    }

    /**
     * @Author: hyl
     * @Date: 2018/6/21 16:38
     * @Description:获取销售ids
     */
    public List<String> getSaleIds() {
        //职员id
        String staffId = SessionUtils.getUserId();
        //组织id
        String organizationId = SessionUtils.getOrganizationId();
        //分公司id
        String warehouseId = SessionUtils.getWarehouseId();

        List<Role> allRolesByStaffId = roleService.findAllRolesByStaffId(staffId);
        List<String> saleIds =  departmentService.findByUserRoleCode(allRolesByStaffId, staffId, organizationId, warehouseId);
        if(saleIds.isEmpty()){
            return Lists.newArrayList(staffId);
        }
        return saleIds;
    }

    /**
     * @Author: hyl
     * @Date: 2018/6/21 16:41
     * @Description:获取销售月销售目标
     */
    public List<SalesStaffAssign> salesStaffAssignList(List<String> saleIds) {
        return salesStaffAssignService.findBySalesStaffIdIn(saleIds);
    }

    /**
     * @Author: hyl
     * @Date: 2018/6/21 16:57
     * @Description:下单用户数量
     */
    public int getPlaceAnOrderCustomerCount(List<CustomerAssign> customers){
        List<String> userIds = new ArrayList<>();
        customers.forEach(customerAssign -> {
            userIds.add(customerAssign.getCustomerId());
        });
        //@Author: hyl @Date: 2020-02-29 17:18 @Description: UserExtensionService 迁移到 CustomerInfoService
      return customerInfoService.findByUserIdInAndFirstOrderTimeIsNotNull(userIds);
    }
}
