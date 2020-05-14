package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.order.dto.orde.OrderExportDto;
import com.yangshan.eship.order.entity.orde.OrderFromType;
import com.yangshan.eship.order.service.orde.OrderSearchServiceI;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController("AccountDetailsInSalesLibController")
@RequestMapping(Version.VERSION + "/accountDetailsBySales")
public class AccountDetailsController {
    @Autowired
    private OrderSearchServiceI orderSearchService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;
    /**
     * @Author: tsd
     * @Date: 2019/7/25 10:08
     * @Description: 对账明细（销售系统）
     */
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public DataGrid list(@RequestBody OrderExportDto orderExportDto) {
        DataGrid dataGrid = null;
        if(StringUtils.isNotEmpty(SessionUtils.getOrganizationId())) {
            orderExportDto.setOrganizationId("40288829605386c8016053ce73a30002");
        }else{
            orderExportDto.setOrganizationId(SessionUtils.getOrganizationId());
        }
        String userId = SessionUtils.getUserId();
        if(StringUtils.isEmpty(userId)){
            userId = "2c93ece063cffad40163d9fa06710014";
        }
        orderExportDto.setCurrentLoginUserId(userId);
        List<String> salesIds = new ArrayList<>();
        salesIds.add(userId);
        List<CustomerAssign>  customerAssignList = customerAssignService.findBySalesIds(salesIds);
        if (customerAssignList == null || customerAssignList.size() == 0) {
            dataGrid = new DataGrid();
           dataGrid.setFlag(true);
           dataGrid.setMsg("当前销售没有客户");
           return dataGrid;
        }
        List<CustomerAssign> newCustLit = new ArrayList<>();
        if(StringUtils.isNotEmpty(orderExportDto.getCustomerName())){
            for(CustomerAssign customerAssign : customerAssignList){
                if((StringUtils.isNotEmpty(customerAssign.getCustomerCode()) && customerAssign.getCustomerCode().contains(orderExportDto.getCustomerName()))
                        || (StringUtils.isNotEmpty(customerAssign.getSimpleCompanyName()) && customerAssign.getSimpleCompanyName().contains(orderExportDto.getCustomerName()))){
                    newCustLit.add(customerAssign);
                }
            }
            customerAssignList = newCustLit;
        }
        List<User> userList = customerAssignService.getUsers(customerAssignList);

        List<String> userIdList = userList.stream().map(User::getId).collect(Collectors.toList());
        if(StringUtils.isNotEmpty(orderExportDto.getCustomerName())){

        }
        orderExportDto.setCustomerIdList(userIdList);
        dataGrid = orderSearchService.getAccountDetails(orderExportDto);
        dataGrid.setFlag(true);

        return dataGrid;
    }

    /**
     * @Author: tsd
     * @Date: 2019/7/25 10:08
     * @Description: 对账明细统计（销售系统）
     */
    @RequestMapping(value = "/statistics", method = RequestMethod.POST)
    public DataGrid statistics(@RequestBody OrderExportDto orderExportDto) {
        DataGrid dataGrid = null;
        if(StringUtils.isNotEmpty(SessionUtils.getOrganizationId())) {
            orderExportDto.setOrganizationId("40288829605386c8016053ce73a30002");
        }else{
            orderExportDto.setOrganizationId(SessionUtils.getOrganizationId());
        }
        String userId = SessionUtils.getUserId();
        if(StringUtils.isEmpty(userId)){
            userId = "2c93ece063cffad40163d9fa06710014";
        }
        orderExportDto.setCurrentLoginUserId(userId);
        List<String> salesIds = new ArrayList<>();
        salesIds.add(userId);
        List<CustomerAssign>  customerAssignList = customerAssignService.findBySalesIds(salesIds);
        if (customerAssignList == null || customerAssignList.size() == 0) {
            dataGrid = new DataGrid();
            dataGrid.setFlag(true);
            dataGrid.setMsg("当前销售没有客户");
            return dataGrid;
        }
        List<CustomerAssign> newCustLit = new ArrayList<>();
        if(StringUtils.isNotEmpty(orderExportDto.getCustomerName())){
            for(CustomerAssign customerAssign : customerAssignList){
                if((StringUtils.isNotEmpty(customerAssign.getCustomerCode()) && customerAssign.getCustomerCode().contains(orderExportDto.getCustomerName()))
                        || (StringUtils.isNotEmpty(customerAssign.getSimpleCompanyName()) && customerAssign.getSimpleCompanyName().contains(orderExportDto.getCustomerName()))){
                    newCustLit.add(customerAssign);
                }
            }
            customerAssignList = newCustLit;
        }
        List<User> userList = customerAssignService.getUsers(customerAssignList);

        List<String> userIdList = userList.stream().map(User::getId).collect(Collectors.toList());
        if(StringUtils.isNotEmpty(orderExportDto.getCustomerName())){

        }
        orderExportDto.setCustomerIdList(userIdList);
        dataGrid = orderSearchService.statistics(orderExportDto);
        dataGrid.setFlag(true);

        return dataGrid;
    }

    /**
     * 获取业务类型
     *
     * @return
     */
    @RequestMapping(value = "/getOrderFromType", method = RequestMethod.GET)
    public Map<String, String> getAllStatus() {

        return OrderFromType.listStatus();
    }
}
