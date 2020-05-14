package com.yangshan.eship.sales.business;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.finance.entity.cust.Wallet;
import com.yangshan.eship.finance.entity.fina.FinanceOrderPackage;
import com.yangshan.eship.finance.service.cust.WalletServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderPackageServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderServiceI;
import com.yangshan.eship.order.service.orde.OrderSearchServiceI;
import com.yangshan.eship.sales.dto.AccountApplicationDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.AccountApplication;
import com.yangshan.eship.sales.entity.sale.DeliveryRequirementsType;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.sales.AccountApplicationServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AccountApplicationBusiness {

    @Autowired
    private UserServiceI userService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private AccountApplicationServiceI applicationService;

    @Autowired
    private WalletServiceI walletService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private OrderSearchServiceI orderSearchService;

    @Autowired
    private FinanceOrderPackageServiceI financeOrderPackageService;

    @Autowired
    private FinanceOrderServiceI financeOrderService;

    public DataGrid<AccountApplicationDto> getAll(AccountApplicationDto applicationDto) {
        DataGrid<AccountApplicationDto> dtoGrid = new DataGrid<AccountApplicationDto>();

        /*客户编号/客户公司简称*/
        //查询符合该条件的所有客户id
        Set<String> customerIds = new HashSet<>();
        List<User> users = new ArrayList<>();
        if (StringUtils.isNotBlank(applicationDto.getCustomerCode())) {
            users = userService.findByCustomer(SessionUtils.getOrganizationId(), applicationDto.getCustomerCode());
        }

        if (!users.isEmpty()) {
            for (User user: users) {
                customerIds.add(user.getId());
            }
        }

        applicationDto.setUserIds(new ArrayList<String>(customerIds));

        DataGrid<AccountApplicationDto> grid = applicationService.getAll(applicationDto);

        List<AccountApplicationDto> accountDtos = new ArrayList<AccountApplicationDto>();

        Set<String> userIds = new HashSet<>();

        Set<String> custIds = new HashSet<>();

        if (!grid.getRows().isEmpty()) {
            for (AccountApplicationDto dto : grid.getRows()) {
                AccountApplicationDto appliDto = new AccountApplicationDto();
                BeanUtils.copyProperties(dto, appliDto);
                userIds.add(dto.getCustomerId());
                userIds.add(dto.getSalesStaffId());

                custIds.add(dto.getCustomerId());

                accountDtos.add(appliDto);
            }

            Map<String, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

            //客户的可用额度
            Map<String, BigDecimal> map = walletService.findLineOfCreditUsedByCustomerIds(new ArrayList<>(custIds));

            List<Wallet> wallets = walletService.findByCustomerIds(new ArrayList<String>(userIds));

            Map<String, Wallet> walletMap = new HashMap<>();
            if (!wallets.isEmpty()) {
                for (Wallet wallet : wallets) {
                    walletMap.put(wallet.getCustomerId(), wallet);
                }
            }

            for (AccountApplicationDto accDto : accountDtos) {
                User user = userMap.get(accDto.getCustomerId());

                if (user == null) {
                    continue;
                }

                accDto.setCustomerName(user != null ? user.getNickName() : "");

                accDto.setCustomerCode(user != null ? user.getCustomerCode() : "");
                accDto.setSimpleCompanyName(user != null ? user.getSimpleCompanyName() : "");

                //获取用户的可用额度
                List<FinanceOrderPackage> unPayedPackageOrder = financeOrderPackageService.findUnPayedPackageOrder(user.getId());

                BigDecimal avialableCredict = BigDecimal.ZERO;

                //@Author: HuKai @Date: 2018-08-15 0015 上午 11:45
                //@Description: 如果有未付款的催款账单, 则可用额度为0; 否则就等于总额度-已用额度
                if (unPayedPackageOrder.isEmpty()) {
                    avialableCredict = walletMap.get(user.getId()).getLineOfCredit().subtract(map.get(user.getId()));
                }
                int compareResult = avialableCredict.compareTo(DataUtils.toBigDecimal(0f));
                accDto.setAvailableCredits(compareResult > 0 ? avialableCredict : DataUtils.toBigDecimal(0f));

                User saleUser = userMap.get(accDto.getSalesStaffId());
                accDto.setSaleStaffName(saleUser != null ? saleUser.getName() : "");

                //出货要求
                String shippedLimitDesc = getShippedLimitDesc(accDto.getDeliveryRequirementsType(), accDto.getRequirementsNumber());
                accDto.setDeliveryRequirementsDesc(shippedLimitDesc);

                //根据出货限制类型显示以出货量
                String shippedDesc = getShippedDescByType(accDto.getDeliveryRequirementsType(), accDto.getCustomerId(), accDto.getAccountStartTime(), accDto.getAccountEndTime());
                accDto.setShippedDesc(shippedDesc);

                if (accDto.getLineOfCredit() != null && accDto.getAvailableCredits() != null) {
                    accDto.setUsedCredits(accDto.getLineOfCredit().subtract(accDto.getAvailableCredits()));
                } else {
                    accDto.setUsedCredits(BigDecimal.ZERO);
                }
            }
        }

        dtoGrid.setTotal(grid.getTotal());
        dtoGrid.setRows(accountDtos);
        dtoGrid.setFlag(true);

        return dtoGrid;
    }

    /**
     * @Author: Kevin
     * @Date: 2019-10-10 11:37
     * @Description: 出货要求描述
     */
    private String getShippedLimitDesc(DeliveryRequirementsType deliveryRequirementsType, String requirementsNumber) {
        if (deliveryRequirementsType == null) {
            return "";
        }
        return requirementsNumber + deliveryRequirementsType.getUnit();
    }

    /**
     * @Author: Kevin
     * @Date: 2019-10-10 11:31
     * @Description: 计算该条账期申请对应的已出货量
     */
    private String getShippedDescByType(DeliveryRequirementsType deliveryRequirementsType, String customerId, String accountStartTime, String accountEndTime) {
        if (deliveryRequirementsType == null) {
            return "";
        }

        if (DeliveryRequirementsType.WEIGHT_LIMIT.name().equals(deliveryRequirementsType.name())) {
            //重量限制
            Float totalWeight = orderSearchService.calUserAccountTotalWeight(customerId, accountStartTime, accountEndTime);
            return totalWeight + deliveryRequirementsType.getUnit();
        } else if (DeliveryRequirementsType.AMOUNT_LIMIT.name().equals(deliveryRequirementsType.name())) {
            //金额限制
            Float totalPrice = financeOrderService.calUserAccountTotalPrice(customerId, accountStartTime, accountEndTime);
            return totalPrice + deliveryRequirementsType.getUnit();
        } else if (DeliveryRequirementsType.ORDER_COUNT_LIMIT.name().equals(deliveryRequirementsType.name())) {
            //票数限制
            Long totalNumber = orderSearchService.calUserAccountTotalNumber(customerId, accountStartTime, accountEndTime);
            return totalNumber + deliveryRequirementsType.getUnit();
        } else if (DeliveryRequirementsType.ORDER_PIECE_LIMIT.name().equals(deliveryRequirementsType.name())) {
            //件数限制
            Long totalBoxCount = orderSearchService.calUserAccountTotalBoxCount(customerId, accountStartTime, accountEndTime);
            return totalBoxCount + deliveryRequirementsType.getUnit();
        } else {
            return "";
        }
    }

    /**
     * 查询销售或客服下面的客户
     * type => 1:销售下面的客户   2:客服下面的客户
     * @author Hukai
     * @param customerId
     * @param type
     * @return
     */
    public DataGrid<CustomerAssign> getMyCustomers(String customerId, Integer type) {
        DataGrid<CustomerAssign> customerAssignDataGrid = new DataGrid<>();

        CustomerAssign customerAssign = new CustomerAssign();
        if (type == 1) {
            customerAssign.setSalesStaffId(customerId);
        } else if (type == 2) {
            customerAssign.setCustomServiceStaffId(customerId);
        }

        customerAssignDataGrid = customerAssignService.list(customerAssign);

        return this.dealMyCustomerResult(customerAssignDataGrid);
    }

    public DataGrid<String> commitAccountInfo(AccountApplicationDto applicationDto) {
        List<AccountApplication> applications = applicationService.findByUserId(applicationDto.getCustomerId());

        if (StringUtils.isBlank(applicationDto.getId()) && !applications.isEmpty()) {
            //1.新申请时候 判断是否含有新申请状态下的订单, 有则不让申请
            //2.判断是否和最新一条账期类型相同, 有则不让申请
            applicationService.judgeApplyAble(applications, applicationDto);
        }

        //@Author: HuKai @Date: 2018/1/17 16:58
        //@Description: 将新申请的账期信息自动分配给销售或财务
        //目前先全部分配给区域下的财务去处理
        //查询当前分公司下所有财务
        List<User> financeUsers = userRoleService.findWarehouseUserByRoleCode(SessionUtils.getWarehouseId(), User.UserRoleCode.finance.name());

        //随机分配给某个财务
        if (financeUsers.size() > 0) {
            Random random = new Random();
            int index= random.nextInt(financeUsers.size());

            applicationDto.setHandlePersonId(financeUsers.get(index).getId());
        }
        //如果没找到财务经理,找集团财务经理
        if(StringUtils.isEmpty(applicationDto.getHandlePersonId())) {
            financeUsers = userRoleService.findUserByRoleCode(User.UserRoleCode.bloc_finance_manager.name());
            if (financeUsers.size() > 0) {
                Random random = new Random();
                int index = random.nextInt(financeUsers.size());

                applicationDto.setHandlePersonId(financeUsers.get(index).getId());
            }
        }
        /*List<SalesStaffAssign> salesStaffAsginsDb = salesStaffAssignService.findBySalesStaffId(applicationDto.getSalesStaffId());
        if (!salesStaffAsginsDb.isEmpty()) {
            //根据销售经理查询区域id
            List<UserRole> saleManagers = userRoleService.findBySaleMangeId(salesStaffAsginsDb.get(0).getSalesManagerId());

            if (!saleManagers.isEmpty()) {
                //查询当前组织该区域下的财务
                String originId = saleManagers.get(0).getDestinationId();
                List<User> financeManagers = userRoleService.findFinanceManger(SessionUtils.getOrganizationId(), originId);

                if (!financeManagers.isEmpty()) {
                    applicationDto.setHandlePersonId(financeManagers.get(0).getId());
                }
            }
        }*/

        //@Author: Kevin 2019-08-14 16:58
        //@Descreption: 存入组织id和分公司id
        applicationDto.setOrganizationId(SessionUtils.getOrganizationId());
        applicationDto.setWarehouseId(SessionUtils.getWarehouseId());

        return applicationService.commitAccountInfo(applicationDto);
    }

    private DataGrid<CustomerAssign> dealMyCustomerResult(DataGrid<CustomerAssign> customerAssignDataGrid) {
        List<CustomerAssign> customerAssignList = customerAssignDataGrid.getRows();

        if (customerAssignList.isEmpty()) {
            return customerAssignDataGrid;
        }

        Set<String> userIds = new HashSet<>();
        for (CustomerAssign customerAssign : customerAssignList) {
            userIds.add(customerAssign.getCustomerId());
        }

        Map<String, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

        for (CustomerAssign customerAssign : customerAssignList) {
            customerAssign.setSimpleCompanyName(userMap.get(customerAssign.getCustomerId()) != null ? userMap.get(customerAssign.getCustomerId()).getSimpleCompanyName() : "");
        }

        customerAssignDataGrid.setRows(customerAssignList);

        return customerAssignDataGrid;
    }

    public DataGrid<String> deleteAccountInfo(String id) {
        return applicationService.deleteAccountInfo(id);
    }
}
