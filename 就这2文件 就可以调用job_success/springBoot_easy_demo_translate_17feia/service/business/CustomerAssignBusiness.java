package com.yangshan.eship.sales.business;

import com.google.common.collect.Lists;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.business.CommonCustomerSearchBusiness;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.finance.entity.cust.Wallet;
import com.yangshan.eship.finance.service.cust.WalletServiceI;
import com.yangshan.eship.sales.dto.CustomerAssignDto;
import com.yangshan.eship.sales.dto.CustomerInfoRequestDto;
import com.yangshan.eship.sales.dto.CustomerInfoResponseDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class CustomerAssignBusiness {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAssignBusiness.class);

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private WalletServiceI walletService;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${rest.exportData.customerSearchUrl}")
    private String customerSearchUrl;

    @Value("${rest.exportData.orderSearchUrl}")
    private String orderSearchUrl;

    @Autowired
    private CommonCustomerSearchBusiness commonCustomerSearchBusiness;

    public DataGrid<CustomerInfoResponseDto> listCustomers(CustomerInfoRequestDto customerInfoRequestDto) {
        customerInfoRequestDto.setCustomerSearchUrl(customerSearchUrl);

        return commonCustomerSearchBusiness.searchCustomerData(customerInfoRequestDto);
    }

    public DataGrid<CustomerAssignDto> list(CustomerAssign customerAssign) {

        DataGrid<CustomerAssignDto> dataGrid = new DataGrid<>();

        //查询所有的客户分配信息
        DataGrid<CustomerAssign> customerAssignDataGrid = customerAssignService.list(customerAssign);
        dataGrid.setFlag(customerAssignDataGrid.isFlag());
        dataGrid.setTotal(customerAssignDataGrid.getTotal());
        if (customerAssignDataGrid.getTotal() < 1) {
            return dataGrid;
        }

        //查询用户信息和钱包信息
        Map<String, User> userMap = new HashMap<>();
        Map<String, Wallet> walletMap = new HashMap<>();
        queryUserAndWallet(customerAssignDataGrid, userMap, walletMap);

        //设置用户信息和钱包信息到DTO中
        setUserAndWalletInfo(dataGrid, customerAssignDataGrid, userMap, walletMap);

        return dataGrid;
    }

    /**
     * 设置用户信息和钱包信息到DTO中
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/10/31 17:04
     */
    private void setUserAndWalletInfo(DataGrid<CustomerAssignDto> dataGrid, DataGrid<CustomerAssign> customerAssignDataGrid, Map<String, User> userMap, Map<String, Wallet> walletMap) {
        for (CustomerAssign ca : customerAssignDataGrid.getRows()) {
            CustomerAssignDto dto = new CustomerAssignDto();
            BeanUtils.copyProperties(ca, dto);
            //用户及分配基本信息
            User customer = userMap.get(ca.getCustomerId());
            if (customer == null) {
                logger.error("## 客户id=" + ca.getCustomerId() + "没有查询到用户信息");
            }else {
                dto.setCustomerName(customer.getNickName());
                dto.setCustomerNo(customer.getCustomerCode());
                dto.setPhone(customer.getPhone());
                dto.setCreatedDate(customer.getCreatedDate());
                dto.setEmail(customer.getEmail());
                dto.setWechat(customer.getWechat());
                dto.setQq(customer.getQq());
                dto.setSimpleCompanyName(customer.getSimpleCompanyName());
            }
            //客服经理
            String customServiceManagerName = "";
            if(StringUtils.isNotBlank(ca.getCustomServiceManagerId()) && userMap.get(ca.getCustomServiceManagerId()) != null){
                customServiceManagerName =userMap.get(ca.getCustomServiceManagerId()).getName();
            }
            dto.setCustomServiceManagerName(customServiceManagerName);

            //客服
            String customServiceStaffName = "";
            if(StringUtils.isNotBlank(ca.getCustomServiceStaffId()) && userMap.get(ca.getCustomServiceStaffId()) != null){
                customServiceStaffName =userMap.get(ca.getCustomServiceStaffId()).getName();
            }
            dto.setCustomServiceStaffName(customServiceStaffName);

            //销售经理
            String salesManagerName = "";
            if(StringUtils.isNotBlank(ca.getSalesManagerId()) && userMap.get(ca.getSalesManagerId()) != null){
                salesManagerName =userMap.get(ca.getSalesManagerId()).getName();
            }
            dto.setSalesManagerName(salesManagerName);

            //销售
            String salesStaffName = "";
            if(StringUtils.isNotBlank(ca.getSalesStaffId()) && userMap.get(ca.getSalesStaffId()) != null){
                salesStaffName =userMap.get(ca.getSalesStaffId()).getName();
            }
            dto.setSalesStaffName(salesStaffName);

            //账期信息
            Wallet wallet = walletMap.get(ca.getCustomerId());
            if (wallet == null) {
                logger.error("## 客户id=" + ca.getCustomerId() + "没有添加钱包信息");
            }else {
                dto.setPaymentDays(wallet.getPaymentDays());
                dto.setPaymentDaysStartTime(wallet.getPaymentDaysStartTime());
                dto.setPaymentDaysType(wallet.getPaymentDaysType().getDesc());
                dto.setOvertimeDays(String.valueOf(wallet.getOvertimeDays()));
                dto.setLineOfCredit(wallet.getLineOfCredit());
                dto.setLineOfCreditUsed(wallet.getLineOfCreditUsed());
                dto.setTakeDeliveryEndtime(wallet.getTakeDeliveryEndtime());
            }
            dataGrid.getRows().add(dto);
        }
    }

    /**
     * 查询用户信息和钱包信息
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/10/31 17:03
     */
    private void queryUserAndWallet(DataGrid<CustomerAssign> customerAssignDataGrid, Map<String, User> userMap, Map<String, Wallet> walletMap) {
        //获取所有的用户id并查询详情
        Set<String> userIds = new HashSet<>();
        Set<String> customerIds = new HashSet<>();
        for (CustomerAssign ca : customerAssignDataGrid.getRows()) {
            customerIds.add(ca.getCustomerId());
            userIds.add(ca.getCustomerId());
            if(StringUtils.isNotBlank(ca.getCustomServiceManagerId())) {
                userIds.add(ca.getCustomServiceManagerId());
            }
            if(StringUtils.isNotBlank(ca.getCustomServiceStaffId())) {
                userIds.add(ca.getCustomServiceStaffId());
            }
            if(StringUtils.isNotBlank(ca.getSalesManagerId())) {
                userIds.add(ca.getSalesManagerId());
            }
            if(StringUtils.isNotBlank(ca.getSalesStaffId())) {
                userIds.add(ca.getSalesStaffId());
            }
        }

        List<User> users = userService.listByIds(new ArrayList<>(userIds));

        if (users != null) {
            for (User user : users) {
                userMap.put(user.getId(), user);
            }
        }

        //获取用户信用额度和账期
        List<Wallet> wallets = walletService.listByCustomerIds(new ArrayList<>(customerIds));

        if (wallets != null) {
            for (Wallet wallet : wallets) {
                walletMap.put(wallet.getCustomerId(), wallet);
            }
        }
    }

    public int assign(String selectedRowIds,String salesManagerId,String customServiceManagerId,String customServiceStaffId,String salesStaffId){

        String [] ids = selectedRowIds.trim().split(",");

        return customerAssignService.assign(Lists.newArrayList(ids),customServiceManagerId,salesManagerId,customServiceStaffId,salesStaffId);
    }

    public DataGrid<User> listWarehouseUser(String orgId) {

        List<User> users = userService.findByOrganizationId(orgId);

        return new DataGrid<>(true, users);
    }


}
