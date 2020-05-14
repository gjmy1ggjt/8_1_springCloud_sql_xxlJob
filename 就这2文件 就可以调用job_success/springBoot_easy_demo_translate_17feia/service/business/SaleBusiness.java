package com.yangshan.eship.sales.business;

import com.yangshan.eship.author.dto.account.CustomerDetailInfo;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.business.RoleCustomerInfoBusiness;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.common.utils.EshipBeanUtils;
import com.yangshan.eship.constants.ErrorCodeConstant;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.finance.entity.cust.Wallet;
import com.yangshan.eship.finance.entity.fina.FinanceCustomerCorrelation;
import com.yangshan.eship.finance.entity.fina.FinanceOrderPackage;
import com.yangshan.eship.finance.service.cust.WalletServiceI;
import com.yangshan.eship.finance.service.fina.FinanceCustomerCorrelationServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderItemServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderPackageServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderServiceI;
import com.yangshan.eship.order.entity.cust.CustomerInfo;
import com.yangshan.eship.order.service.orde.CustomerInfoServiceI;
import com.yangshan.eship.order.service.orde.OrderServiceI;
import com.yangshan.eship.sales.dto.*;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.AccountType;
import com.yangshan.eship.sales.entity.sale.Department;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.sales.AccountApplicationServiceI;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.sales.SalesServiceI;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import com.yangshan.eship.sales.service.serv.CustomServiceAssignService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.crypto.Data;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class SaleBusiness {

    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Value("${static.file.server}")
    private String staticFileServer;

    @Autowired
    private SalesServiceI salesService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private OrderServiceI orderService;

    @Autowired
    private WalletServiceI walletService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private CustomerInfoServiceI customerInfoService;

    @Autowired
    private AccountApplicationServiceI applicationService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private SalesStaffAssignServiceI salesStaffAssignService;

    @Autowired
    private FinanceOrderItemServiceI financeOrderItemService;

    @Autowired
    private FinanceOrderPackageServiceI financeOrderPackageService;

    @Autowired
    private FinanceOrderServiceI financeOrderService;

    @Autowired
    private FinanceCustomerCorrelationServiceI financeCustomerCorrelationService;

    @Autowired
    private RoleCustomerInfoBusiness roleCustomerInfoBusiness;

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private CustomServiceAssignService customServiceAssignService;

    public DataGrid<CustomerAssignDto> getMyCustomers(CustomerAssign customerAssign) {

//        设置管理客户权限
        customerAssign.setListCustomerId(roleCustomerInfoBusiness.findListCustomerId());

        DataGrid<CustomerAssignDto> customerAssignGrid = new DataGrid<>();

        //判断手机号/邮箱是否传过来
        if (StringUtils.isNotBlank(customerAssign.getUserPhone())) {
            List<User> userList = userService.getUserByWarehouseIdAndPhone(SessionUtils.getWarehouseId(), customerAssign.getUserPhone());

            Set<String> userIds = new HashSet<>();
            if (userList.isEmpty()) {
                return customerAssignGrid;
            } else {
                for (User user : userList) {
                    userIds.add(user.getId());
                }

                customerAssign.setCustomerIds(new ArrayList<>(userIds));
            }
        }

        if (StringUtils.isNotBlank(customerAssign.getUserEmail())) {
            List<User> userList = userService.getUserByWarehouseIdAndEmail(SessionUtils.getWarehouseId(), customerAssign.getUserEmail());

            Set<String> userIds = new HashSet<>();
            if (userList.isEmpty()) {
                return customerAssignGrid;
            } else {
                for (User user : userList) {
                    userIds.add(user.getId());
                }

                customerAssign.setCustomerIds(new ArrayList<>(userIds));
            }
        }

        customerAssignGrid = salesService.getMyCustomers(customerAssign);

        Set<String> userIds = new HashSet<>();

        Set<String> custIds = new HashSet<>();

        if (!customerAssignGrid.getRows().isEmpty()) {
            for (CustomerAssignDto assignDto : customerAssignGrid.getRows()) {
                if (StringUtils.isNotBlank(assignDto.getCustomerId())) {
                    userIds.add(assignDto.getCustomerId());
                    custIds.add(assignDto.getCustomerId());
                }

                if (StringUtils.isNotBlank(assignDto.getSalesStaffId())) {
                    userIds.add(assignDto.getSalesStaffId());
                }

                if (StringUtils.isNotBlank(assignDto.getCustomServiceStaffId())) {
                    userIds.add(assignDto.getCustomServiceStaffId());
                }
            }

            Map<String, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

            //客户的可用额度
            List<String> customerIds = new ArrayList<>(custIds);
            Map<String, BigDecimal> map = walletService.findLineOfCreditUsedByCustomerIds(customerIds);

            List<Wallet> wallets = walletService.findByCustomerIds(new ArrayList<String>(userIds));

            Map<String, Wallet> walletMap = new HashMap<>();
            if (!wallets.isEmpty()) {
                for (Wallet wallet : wallets) {
                    walletMap.put(wallet.getCustomerId(), wallet);
                }
            }

            //@Author: Kevin 2019-08-14 15:07
            //@Descreption: 获取关注客户的对应财务
            List<FinanceCustomerCorrelation> financeCustomerCorrelationList = financeCustomerCorrelationService.findByCustomerIdIn(customerIds);
            Map<String, String> correlationMap = new HashMap<>();
            if (!financeCustomerCorrelationList.isEmpty()) {
                for (FinanceCustomerCorrelation financeCustomerCorrelation : financeCustomerCorrelationList) {
                    String customerId = financeCustomerCorrelation.getCustomerId();
                    String financeName = financeCustomerCorrelation.getFinanceName();
                    if (correlationMap.containsKey(customerId) && correlationMap.get(customerId).indexOf(financeName) < 0) {
                        financeName = correlationMap.get(customerId) + "\n" + financeName;
                    }

                    correlationMap.put(customerId, financeName);
                }
            }

            for (CustomerAssignDto assignDto : customerAssignGrid.getRows()) {
                User user = userMap.get(assignDto.getCustomerId());
                if (user != null) {
                    //@Author:hyl @Date:2018/5/29 @Description:添加用户信息里面的联系人名
                    CustomerInfo customerInfo = customerInfoService.getByUserId(user.getId());
                    assignDto.setCustomerNo(user.getCustomerCode()); //用户编号
                    assignDto.setPhone(user.getPhone()); //联系电话
                    assignDto.setOrganizationId(user.getOrganizationId()); //客户分组
                    assignDto.setCustomerName(customerInfo.getContacts());

                    List<FinanceOrderPackage> unPayedPackageOrder = financeOrderPackageService.findUnPayedPackageOrder(user.getId());

                    BigDecimal avialableCredict = BigDecimal.ZERO;

                    //@Author: HuKai @Date: 2018-08-15 0015 上午 11:45
                    //@Description: 如果有未付款的催款账单, 则可用额度为0; 否则就等于总额度-已用额度
                    if (unPayedPackageOrder.isEmpty()) {
                        avialableCredict = walletMap.get(user.getId()).getLineOfCredit().subtract(map.get(user.getId()));
                    }
                    int compareResult = avialableCredict.compareTo(DataUtils.toBigDecimal(0f));
                    assignDto.setAvailableCredits(compareResult > 0 ? avialableCredict : DataUtils.toBigDecimal(0f));
                }

                User servUser = userMap.get(assignDto.getCustomServiceStaffId());
                if (servUser != null) {
                    assignDto.setCustomServiceStaffName(servUser.getName());
                }

                User saleUser = userMap.get(assignDto.getSalesStaffId());
                if (saleUser != null) {
                    assignDto.setSalesStaffName(saleUser.getName());
                }

                //获取用户的账期信息==> 钱包里的账期信息

                assignDto.setLineOfCredit(walletMap.get(assignDto.getCustomerId()) != null ? walletMap.get(assignDto.getCustomerId()).getLineOfCredit() : DataUtils.toBigDecimal(0f)); //信用额度
                //账期天数 = 账期结束日期 - 账期开始日期

                String paymentDaysStr = "";
                if (assignDto.getCustomerId() != null) {
                    Wallet wallet = walletMap.get(assignDto.getCustomerId());
                    EshipBeanUtils.copyExclude(wallet, assignDto);
                    assignDto.setPaymentDaysType(wallet.getPaymentDaysType().getDesc());
                    assignDto.setOvertimeDays(wallet.getOvertimeDays() != 0 ? (String.valueOf(wallet.getOvertimeDays())) : "0");
                    paymentDaysStr = wallet.getPaymentDaysType() == AccountType.NO_ACCOUNT ? "无账期" : String.valueOf(wallet.getPaymentDays());
                }

                assignDto.setPaymentDaysStr(paymentDaysStr); //当前账期天数
                assignDto.setQq(user != null ? user.getQq() : "");
                assignDto.setEmail(user != null ? user.getEmail() : "");
                assignDto.setCreatedDate(user != null ? user.getCreatedDate() : null); //注册时间
                assignDto.setNickName(user != null ? user.getNickName() : "");
                assignDto.setRemark(user != null ? user.getRemark() : "");

                if (correlationMap.containsKey(assignDto.getCustomerId())) {
                    assignDto.setFinanceNames(correlationMap.get(assignDto.getCustomerId()));
                }
            }
        }

        customerAssignGrid.setFlag(true);
        customerAssignGrid.setTotal(customerAssignGrid.getTotal());

        return customerAssignGrid;
    }

    /**
     * @Author: HuKai
     * @Date: 2018/2/2 14:59
     * @Description: 获取销售下的客户/单个客户 订单数量和订单金额数据, 用来绘制图表
     */
    public ChartDataDto getCustomerChartData(CustomerAssignDto customerAssignDto) {
        ChartDataDto chartDataDto = new ChartDataDto();

        List<String> userNames = new ArrayList<>();
        List<Integer> orderCount = new ArrayList<>();
        List<Float> orderPrice = new ArrayList<>();

        if (StringUtils.isNotBlank(customerAssignDto.getCustomerId())) {
            //查询单个客户的订单数量和订单金额信息
            List<Object[]> chartResult = financeOrderItemService.getSingleSalesChartData(customerAssignDto);

            if (!chartResult.isEmpty()) {
                for (Object[] obj : chartResult) {
                    userNames.add(obj[0].toString());
                    orderCount.add(Integer.parseInt(obj[1].toString()));
                    orderPrice.add(Float.parseFloat(obj[2].toString()));
                }
            }

        } else {
            //查询当前销售下所有客户的订单数量和订单金额
            //先拿到当前销售/客服下面的客户ids
            List<User> users = new ArrayList<>();

            if (StringUtils.isNotBlank(customerAssignDto.getSalesStaffId())) {
                users = customerAssignService.findBySalesStaffId(customerAssignDto.getSalesStaffId());
            }

            if (StringUtils.isNotBlank(customerAssignDto.getCustomServiceStaffId())) {
                users = customerAssignService.findByCustServiceStaffId(customerAssignDto.getCustomServiceStaffId());
            }

            if (users.size() == 0) {
                return null;
            }

            Set<String> userIds = new HashSet<>();
            for (User user : users) {
                userIds.add(user.getId());
            }

            List<String> userIdList = new ArrayList<>(userIds);

            Map<String, User> userMap = userService.mapByIds(userIdList);

            List<Object[]> chartResult = financeOrderItemService.getSalePriceChartData(userIdList, customerAssignDto);

            if (!chartResult.isEmpty()) {
                for (Object[] obj : chartResult) {
                    userNames.add(userMap.get(obj[0]).getCustomerCode());
                    orderCount.add(Integer.parseInt(obj[1].toString()));
                    orderPrice.add(Float.parseFloat(obj[2].toString()));
                }
            }

        }

        chartDataDto.setxAxisName(userNames);
        chartDataDto.setOrderCount(orderCount);
        chartDataDto.setPriceTotal(orderPrice);

        return chartDataDto;
    }

    public DataGrid updateCustAssignCode() {
        CustomerAssign customerAssign = new CustomerAssign();
        List<CustomerAssign> customerAssigns = customerAssignService.list(customerAssign).getRows();

        Set<String> userIds = new HashSet<>();

        for (CustomerAssign customerAssign1 : customerAssigns) {
            if (StringUtils.isNotBlank(customerAssign1.getCustomerId())) {
                userIds.add(customerAssign1.getCustomerId());
            }
        }

        Map<String, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

        int i = 0;

        for (CustomerAssign customerAssign1 : customerAssigns) {
            User user = userMap.get(customerAssign1.getCustomerId());
            customerAssign1.setCustomerCode(user.getCustomerCode());
            customerAssign1.setSimpleCompanyName(user.getSimpleCompanyName());

            customerAssignService.save(customerAssign1);

            i++;
        }

        return new DataGrid(true, "一共更新了 " + i + " 条数据");

    }

    public DataGrid<User> listSalesStaffList(String warehouseId) {
        List<User> saleStaffs = new ArrayList<>();


        if ("getStaffListFromOrganization".equals(warehouseId)) {
            saleStaffs = userService.findCustomerByOrganizationId(User.UserRoleCode.sale.name(), SessionUtils.getOrganizationId());
        } else {
            saleStaffs = userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.sale.name(), SessionUtils.getWarehouseId());
        }

        return new DataGrid<>(true, saleStaffs);
    }

    public DataGrid<StaffInfoStaticsDto> listSalesManagers(String warehouseId) {
        //获取分公司下所有销售经理
        List<User> salesManagers = userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.sale_manager.name(), warehouseId);
        Map<String, String> salesManagerMap = salesManagers.stream().collect(Collectors.toMap(User::getId, User::getName));

        List<String> salesManagerIds = salesManagers.stream().map(User::getId).collect(Collectors.toList());
        //获取销售经理下的销售数量
        List<SalesStaffAssign> salesManagerSalesStaffList = salesService.getSalesManagerSalesStatics(salesManagerIds);
        Map<String, Long> salesManagerSalesMap = salesManagerSalesStaffList.stream().collect(Collectors.toMap(SalesStaffAssign::getSalesManagerId, SalesStaffAssign::getSalesTotal));

        List<StaffInfoStaticsDto> staffInfoStaticsDtoList = new ArrayList<>();
        salesManagerMap.forEach((salesManagerId, salesManagerName) -> {
            StaffInfoStaticsDto staffInfoStaticsDto = new StaffInfoStaticsDto();
            staffInfoStaticsDto.setId(salesManagerId);
            staffInfoStaticsDto.setName(salesManagerName);
            staffInfoStaticsDto.setStaffCount(0);
            if (salesManagerSalesMap.containsKey(salesManagerId)) {
                staffInfoStaticsDto.setStaffCount(salesManagerSalesMap.get(salesManagerId).intValue());
            }

            staffInfoStaticsDtoList.add(staffInfoStaticsDto);
        });

        return new DataGrid<>(true, staffInfoStaticsDtoList);
    }

    public DataGrid<CustomServiceInfoDto> listSales(StaffSearchRequestDto staffSearchRequestDto) {
        //未分配的销售列表查询
        if (staffSearchRequestDto.getUnDistributeUser() != null && staffSearchRequestDto.getUnDistributeUser()) {
            staffSearchRequestDto.setRoleCode(User.UserRoleCode.sale.name());
            return listunDistributeSalesOrCustomService(staffSearchRequestDto);
        }

        List<CustomServiceInfoDto> customServiceInfoDtos = new ArrayList<>();

        DataGrid<SalesStaffAssign> salesStaffAssignDataGrid = salesService.listSales(staffSearchRequestDto);

        //处理数据
        if (!salesStaffAssignDataGrid.getRows().isEmpty()) {
            List<String> userIds = salesStaffAssignDataGrid.getRows().stream().map(SalesStaffAssign::getSalesStaffId).collect(Collectors.toList());
            Map<String, User> salesAssignMap = userService.mapByIds(userIds);

            salesStaffAssignDataGrid.getRows().stream().forEach(salesStaffAssign -> {
                CustomServiceInfoDto salesInfoDto = new CustomServiceInfoDto();
                salesInfoDto.setId(salesStaffAssign.getId());
                User salesUser = salesAssignMap.get(salesStaffAssign.getSalesStaffId());
                salesInfoDto.setCustomServiceManagerName(salesStaffAssign.getSalesManagerName());

                if (salesUser != null) {
                    salesInfoDto.setCreatedDate(salesUser.getCreatedDate());
                    salesInfoDto.setEmail(salesUser.getEmail());
                    salesInfoDto.setLoginId(salesUser.getLoginId());
                    salesInfoDto.setName(salesUser.getName());
                    salesInfoDto.setNickName(salesUser.getNickName());
                    salesInfoDto.setPhoneNo(salesUser.getPhone());
                    salesInfoDto.setQq(salesUser.getQq());
                    salesInfoDto.setWechat(salesUser.getWechat());
                    salesInfoDto.setSalesManagerName(salesInfoDto.getCustomServiceManagerName());
                }

                customServiceInfoDtos.add(salesInfoDto);
            });
        }

        return new DataGrid<>(true, customServiceInfoDtos);
    }

    public DataGrid distributionSales(String salesManagerId, List<String> salesStaffIds) {
        Set<String> userIdSet = new HashSet<>();
        userIdSet.add(salesManagerId);
        salesStaffIds.stream().forEach(salesStaffId -> {
            userIdSet.add(salesStaffId);
        });

        Map<String, User> staffMap = userService.mapByIds(new ArrayList<>(userIdSet));

        List<SalesStaffAssign> salesAssignList = new ArrayList<>();
        User salesAssignManagerStaff = staffMap.get(salesManagerId);
        salesStaffIds.stream().forEach(salesStaffAssignId -> {
            SalesStaffAssign salesStaffAssign = new SalesStaffAssign();
            if (!staffMap.containsKey(salesStaffAssignId)) {
                return;
            }

            User salesStaff = staffMap.get(salesStaffAssignId);
            salesStaffAssign.setSalesStaffId(salesStaff.getId());
            salesStaffAssign.setSalesStaffName(salesStaff.getName());
            salesStaffAssign.setSalesManagerId(salesAssignManagerStaff.getId());
            salesStaffAssign.setSalesManagerName(salesAssignManagerStaff.getName());
            salesStaffAssign.setWarehouseId(SessionUtils.getWarehouseId());

            salesAssignList.add(salesStaffAssign);
        });

        salesService.save(salesAssignList);

        //@Author: Hukai 2020-01-11 13:42
        //@Descreption: 同步customerAssign表
        customerAssignService.updateCustomerAssignSalesManagerId(salesStaffIds, salesManagerId);

        return new DataGrid(true, "销售分配成功!");
    }

    public DataGrid cancelDistributionSales(List<String> salesIds) {
        List<SalesStaffAssign> salesStaffAssignList = salesService.findByIdIn(salesIds);

        //直接删除
        salesService.delete(salesStaffAssignList);

        //同步customerAssign表中销售销售经理绑定关系
        customerAssignService.updateCustomerAssignSalesManagerId(salesIds, null);

        return new DataGrid(true, "销售取消分配成功!");
    }

    public DataGrid<StaffInfoStaticsDto> listSalesStaffs(String userId) {
        //当前销售经理下的销售
        List<String> salesStaffIds = new ArrayList<>();
        List<SalesStaffAssign> salesAssignList = salesService.findBySalesManagerId(userId);
        if (salesAssignList.isEmpty()) {
            return new DataGrid<>(true, new ArrayList<>());
        }

        List<String> salesIds = salesAssignList.stream().map(SalesStaffAssign::getSalesStaffId).collect(Collectors.toList());
        Map<String, User> salesUserMap = userService.mapByIds(salesIds);

        List<StaffInfoStaticsDto> staffInfoStaticsDtoList = new ArrayList<>();
        //销售下的客户数量
        List<CustomerAssign> customerAssignList = customerAssignService.getSalesCustomersStatics(salesIds);
        customerAssignList.stream().forEach(customerAssign -> {
            StaffInfoStaticsDto staffInfoStaticsDto = new StaffInfoStaticsDto();
            staffInfoStaticsDto.setId(customerAssign.getCustomServiceStaffId());
            staffInfoStaticsDto.setName(salesUserMap.get(customerAssign.getCustomServiceStaffId()).getName());
            staffInfoStaticsDto.setStaffCount(customerAssign.getUserTotal().intValue());

            staffInfoStaticsDtoList.add(staffInfoStaticsDto);
        });

        return new DataGrid<>(true, staffInfoStaticsDtoList);
    }

    public DataGrid distributionSalesCustomer(String salesStaffId, List<String> customerIds) {

        return salesService.distributionSalesCustomer(salesStaffId, customerIds);
    }

    public DataGrid cancelDistribution(List<String> customerIds) {
        return salesService.cancelDistribution(customerIds);
    }

    public DataGrid<SingleCustomerInfoDto> getCustomerInfo(String customerId) {
        SingleCustomerInfoDto singleCustomerInfoDto = new SingleCustomerInfoDto();

        User customer = userService.findOne(customerId);
        EshipBeanUtils.copyExclude(customer, singleCustomerInfoDto);
        singleCustomerInfoDto.setPhoneNo(customer.getPhone());

        CustomerInfo customerInfo = customerInfoService.getByUserId(customerId);
        EshipBeanUtils.copyExclude(customerInfo, singleCustomerInfoDto);

        CustomerAssign customerAssign = customerAssignService.findByCustomerId(customerId);
        EshipBeanUtils.copyExclude(customerAssign, singleCustomerInfoDto);

        singleCustomerInfoDto.setCustomerId(customerId);

        if (StringUtils.isNotBlank(singleCustomerInfoDto.getLicensePicUrl())) {
            singleCustomerInfoDto.setLicensePicSrc(staticFileServer + singleCustomerInfoDto.getLicensePicUrl());
        }

        return new DataGrid<>(true, singleCustomerInfoDto);
    }

    public DataGrid updateCustomerInfo(SingleCustomerInfoDto singleCustomerInfoDto) {
        if (StringUtils.isNotBlank(singleCustomerInfoDto.getPhoneNo())) {
            int phoneCount = userService.isExistByPhone(singleCustomerInfoDto.getCustomerId(), singleCustomerInfoDto.getPhoneNo(), singleCustomerInfoDto.getOrganizationId());
            if (phoneCount > 0) {
                throw new EshipException(ErrorCodeConstant.PHONE_EXIST);
            }
        }

        if (StringUtils.isNotBlank(singleCustomerInfoDto.getEmail())) {
            int emailCount = userService.isExistByEmail(singleCustomerInfoDto.getCustomerId(), singleCustomerInfoDto.getEmail(), singleCustomerInfoDto.getOrganizationId());
            if (emailCount > 0) {
                throw new EshipException(ErrorCodeConstant.EMAIL_EXIST);
            }
        }

        //执行异步任务
        ExecutorService es = new ScheduledThreadPoolExecutor(1);

        //修改User信息
        es.submit(new UpdateUserTask(singleCustomerInfoDto));

        //修改CustomerInfo信息
        es.submit(new UpdateCustomerInfoTask(singleCustomerInfoDto));

        //修改CustomerAssign信息
        es.submit(new UpdateCustomerAssignTask(singleCustomerInfoDto));

        return new DataGrid(true, null);
    }

    /**
     * @Author: Hukai
     * @Date: 2020-01-11 11:45
     * @Description: 查询分公司下未分配的销售或客服
     */
    public DataGrid<CustomServiceInfoDto> listunDistributeSalesOrCustomService(StaffSearchRequestDto staffSearchRequestDto) {
        DataGrid<CustomServiceInfoDto> dtoDataGrid = new DataGrid<>();

        //不区分销售经理/客服经理
        staffSearchRequestDto.setManagerId(null);

        List<String> distributedSalesStaffIdList = new ArrayList<>();

        if (User.UserRoleCode.sale.name().equals(staffSearchRequestDto.getRoleCode())) {
            //销售表的当前分公司下所有销售
            DataGrid<SalesStaffAssign> salesStaffAssignList = salesService.listSales(staffSearchRequestDto);
            distributedSalesStaffIdList = salesStaffAssignList.getRows().stream().map(SalesStaffAssign::getSalesStaffId).collect(Collectors.toList());
        } else {
            //客服表的当前分公司下所有客服
            DataGrid<CustomServiceAssign> customServiceAssignList = customServiceAssignService.listCustomServices(staffSearchRequestDto);
            distributedSalesStaffIdList = customServiceAssignList.getRows().stream().map(CustomServiceAssign::getCustomServiceStaffId).collect(Collectors.toList());
        }

        UnDistributeStaffsRequestDto distributeStaffsRequestDto = new UnDistributeStaffsRequestDto(staffSearchRequestDto.getName(), staffSearchRequestDto.getWarehouseId(), staffSearchRequestDto.getRoleCode(), distributedSalesStaffIdList, staffSearchRequestDto.getPagingDto());

        DataGrid<Department> unDistributeSaleGrid = departmentService.getUnDistributeStaffs(distributeStaffsRequestDto);

        dtoDataGrid.setFlag(unDistributeSaleGrid.isFlag());
        dtoDataGrid.setTotal(unDistributeSaleGrid.getTotal());
        if (!dtoDataGrid.isFlag() || dtoDataGrid.getTotal() == 0) {
            return new DataGrid<>(true, new ArrayList<>());
        }

        List<String> staffIdList = unDistributeSaleGrid.getRows().stream().map(Department::getUserId).collect(Collectors.toList());
        Map<String, User> staffMap = userService.mapByIds(staffIdList);

        List<CustomServiceInfoDto> customServiceInfoDtos = new ArrayList<>();
        unDistributeSaleGrid.getRows().forEach(department -> {
            if (!staffMap.containsKey(department.getUserId())) {
                return;
            }

            CustomServiceInfoDto customServiceInfoDto = new CustomServiceInfoDto();
            User staff = staffMap.get(department.getUserId());
            BeanUtils.copyProperties(staff, customServiceInfoDto);
            customServiceInfoDtos.add(customServiceInfoDto);
        });
        dtoDataGrid.setRows(customServiceInfoDtos);
        dtoDataGrid.setTotal(customServiceInfoDtos.size());

        return dtoDataGrid;
    }

    /*=========异步任务1: 修改User信息=============*/
    private class UpdateUserTask implements Callable<SingleCustomerInfoDto> {

        private SingleCustomerInfoDto singleCustomerInfoDto;

        public UpdateUserTask(SingleCustomerInfoDto singleCustomerInfoDto) {
            this.singleCustomerInfoDto = singleCustomerInfoDto;
        }

        @Override
        public SingleCustomerInfoDto call() throws Exception {
            User cUer = new User();
            BeanUtils.copyProperties(singleCustomerInfoDto, cUer);
            cUer.setId(singleCustomerInfoDto.getCustomerId());

            userService.updateUser(cUer);

            return singleCustomerInfoDto;
        }
    }

    /*=========异步任务2: 修改CustomerInfo信息=============*/
    private class UpdateCustomerInfoTask implements Callable<SingleCustomerInfoDto> {

        private SingleCustomerInfoDto singleCustomerInfoDto;

        public UpdateCustomerInfoTask(SingleCustomerInfoDto singleCustomerInfoDto) {
            this.singleCustomerInfoDto = singleCustomerInfoDto;
        }

        @Override
        public SingleCustomerInfoDto call() throws Exception {
            CustomerInfo customerInfo = customerInfoService.getByUserId(singleCustomerInfoDto.getCustomerId());

            EshipBeanUtils.copyExclude(singleCustomerInfoDto, customerInfo);

            customerInfoService.saveCustomerInfo(customerInfo);

            return singleCustomerInfoDto;
        }
    }

    /*=========异步任务2: 修改CustomerAssign信息=============*/
    private class UpdateCustomerAssignTask implements Callable<SingleCustomerInfoDto> {

        private SingleCustomerInfoDto singleCustomerInfoDto;

        public UpdateCustomerAssignTask(SingleCustomerInfoDto singleCustomerInfoDto) {
            this.singleCustomerInfoDto = singleCustomerInfoDto;
        }

        @Override
        public SingleCustomerInfoDto call() throws Exception {
            CustomerAssign customerAssign = customerAssignService.findByCustomerId(singleCustomerInfoDto.getCustomerId());

            EshipBeanUtils.copyExclude(singleCustomerInfoDto, customerAssign);

            customerAssignService.save(customerAssign);

            return singleCustomerInfoDto;
        }

    }


    /**
     * @Author yuchao
     * @Description     重新拉回以前的代码 并添加 Old 以示区别 免得前端重新调用接口  导致参数命名不同的改动
     * @Date Administrator 2020/1/9
     * @Param 
     */
    /*=========异步任务1: 修改User信息=============*/
    private class UpdateUserTaskOld implements Callable<CustomerDetailInfo> {

        private CustomerDetailInfo customerDetailInfo;

        public UpdateUserTaskOld(CustomerDetailInfo customerDetailInfo) {
            this.customerDetailInfo = customerDetailInfo;
        }

        @Override
        public CustomerDetailInfo call() throws Exception {
            User cUer = new User();
            BeanUtils.copyProperties(customerDetailInfo, cUer);
            cUer.setId(customerDetailInfo.getCustomerId());

            userService.updateUser(cUer);

            return customerDetailInfo;
        }
    }

    /*=========异步任务2: 修改CustomerInfo信息=============*/
    private class UpdateCustomerInfoTaskOld implements Callable<CustomerDetailInfo> {

        private CustomerDetailInfo customerDetailInfo;

        public UpdateCustomerInfoTaskOld(CustomerDetailInfo customerDetailInfo) {
            this.customerDetailInfo = customerDetailInfo;
        }

        @Override
        public CustomerDetailInfo call() throws Exception {
            CustomerInfo customerInfo = customerInfoService.getByUserId(customerDetailInfo.getId());

            EshipBeanUtils.copyExclude(customerDetailInfo, customerInfo);

            customerInfoService.saveCustomerInfo(customerInfo);

            return customerDetailInfo;
        }
    }

    /*=========异步任务2: 修改CustomerAssign信息=============*/
    private class UpdateCustomerAssignTaskOld implements Callable<CustomerDetailInfo> {

        private CustomerDetailInfo customerDetailInfo;

        public UpdateCustomerAssignTaskOld(CustomerDetailInfo customerDetailInfo) {
            this.customerDetailInfo = customerDetailInfo;
        }

        @Override
        public CustomerDetailInfo call() throws Exception {
            CustomerAssign customerAssign = customerAssignService.findByCustomerId(customerDetailInfo.getId());

            EshipBeanUtils.copyExclude(customerDetailInfo, customerAssign);

            customerAssignService.save(customerAssign);

            return customerDetailInfo;
        }

    }

    /**
     * @Author yuchao
     * @Description 客服销售系统-我的客户-客户信息
     * @Date Administrator 2020/1/9
     * @Param
     */
    public DataGrid<CustomerDetailInfo> getUserDetailInfo(String userId) {
        DataGrid<CustomerDetailInfo> dataGrid = new DataGrid<>();

        CustomerDetailInfo customerDetailInfo = new CustomerDetailInfo();

        User user = userService.findOne(userId);
        EshipBeanUtils.copyExclude(user, customerDetailInfo);

        CustomerInfo customerInfo = customerInfoService.getByUserId(userId);
        EshipBeanUtils.copyExclude(customerInfo, customerDetailInfo);

        CustomerAssign customerAssign = customerAssignService.findByCustomerId(userId);
        EshipBeanUtils.copyExclude(customerAssign, customerDetailInfo);

        customerDetailInfo.setId(userId);

        if (StringUtils.isNotBlank(customerDetailInfo.getLicensePicUrl())) {
            customerDetailInfo.setLicensePicSrc(staticFileServer + customerDetailInfo.getLicensePicUrl());
        }

        //客户支出总额
        float totalConsume = financeOrderService.getTotalConsume(userId);
        customerDetailInfo.setTotalConsume(totalConsume);

        dataGrid.setObj(customerDetailInfo);

        return dataGrid;
    }

    public DataGrid updateUser(CustomerDetailInfo customerDetailInfo) {
        if (StringUtils.isNotBlank(customerDetailInfo.getPhone())) {
            int phoneCount = userService.isExistByPhone(customerDetailInfo.getId(), customerDetailInfo.getPhone(), customerDetailInfo.getOrganizationId());
            if (phoneCount > 0) {
                throw new EshipException(ErrorCodeConstant.PHONE_EXIST);
            }
        }

        if (StringUtils.isNotBlank(customerDetailInfo.getEmail())) {
            int emailCount = userService.isExistByEmail(customerDetailInfo.getId(), customerDetailInfo.getEmail(), customerDetailInfo.getOrganizationId());
            if (emailCount > 0) {
                throw new EshipException(ErrorCodeConstant.EMAIL_EXIST);
            }
        }

        //执行异步任务
        ExecutorService es = Executors.newCachedThreadPool();

        //修改User信息
        es.submit(new UpdateUserTaskOld(customerDetailInfo));

        //修改CustomerInfo信息
        es.submit(new UpdateCustomerInfoTaskOld(customerDetailInfo));

        //修改CustomerAssign信息
        es.submit(new UpdateCustomerAssignTaskOld(customerDetailInfo));

        return new DataGrid(true, null);

    }
}
