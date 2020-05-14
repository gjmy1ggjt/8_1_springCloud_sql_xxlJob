package com.yangshan.eship.sales.business.statistics;

import com.yangshan.eship.finance.entity.cust.Wallet;
import com.yangshan.eship.sales.dto.CashoutApplicationResponseDto;
import com.yangshan.eship.sales.dto.CashoutApplicationSearchDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.entity.cashout.CashoutApplication;
import com.yangshan.eship.author.entity.cashout.CashoutState;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.author.service.cashout.CashoutApplicationServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.finance.dto.cust.WalletDto;
import com.yangshan.eship.finance.dto.pay.OperateType;
import com.yangshan.eship.finance.entity.cust.RechargeConsumeRecord;
import com.yangshan.eship.finance.service.cust.RechargeConsumeRecordServiceI;
import com.yangshan.eship.finance.service.cust.WalletServiceI;
import com.yangshan.eship.finance.service.fina.FinanceCustomerCorrelationServiceI;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Hukai
 * 2018-08-13 0013 上午 11:29
 */
@Service
public class CashoutApplicationBusiness {

    @Autowired
    private CashoutApplicationServiceI cashoutApplicationService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private WalletServiceI walletService;

    @Autowired
    private RechargeConsumeRecordServiceI rechargeConsumeRecordService;

    @Autowired
    private FinanceCustomerCorrelationServiceI financeCustomerCorrelationService;

    @Autowired
    private SalesStaffAssignServiceI salesStaffAssignService;

    public DataGrid<CashoutApplication> listByCustomer(CashoutApplication cashoutApplication) {
        String customerId = SessionUtils.getUserId();
        cashoutApplication.setOrganizationId(SessionUtils.getOrganizationId());
        cashoutApplication.setCustomerId(customerId);

        DataGrid<CashoutApplication> dataGrid = cashoutApplicationService.listAll(cashoutApplication);

        this.formatData(dataGrid);

        //@Author: Kevin 2019-05-13 14:16
        //@Descreption: 查询当前客户是否已经有待审核状态的记录
        Long count = cashoutApplicationService.countByCustomerIdAndCashoutState(customerId, CashoutState.APPLYED);
        dataGrid.setFlag(count.intValue() > 0);

        return dataGrid;
    }

    public DataGrid<CashoutApplication> listBySale(CashoutApplication cashoutApplication) {
        cashoutApplication.setOrganizationId(SessionUtils.getOrganizationId());
        List<User> customers = customerAssignService.findBySalesStaffId(SessionUtils.getUserId());

        if (customers.isEmpty()) {
            return new DataGrid<>(true, new ArrayList<>());
        }

        Set<String> userMap = new HashSet<>();
        for (User user : customers) {
            if (StringUtils.isNotBlank(user.getId())) {
                userMap.add(user.getId());
            }
        }

        cashoutApplication.setCustomerIds(new ArrayList<>(userMap));

        DataGrid<CashoutApplication> dataGrid = cashoutApplicationService.listAll(cashoutApplication);

        return this.formatData(dataGrid);
    }

    public DataGrid<CashoutApplication> listByFinance(CashoutApplication cashoutApplication) {
        DataGrid<CashoutApplication> dataGrid = new DataGrid<>();

        cashoutApplication.setOrganizationId(SessionUtils.getOrganizationId());

        //@Author: Kevin 2018-12-10 13:39
        //@Descreption: 查询关注的客户(财务)
        if (cashoutApplication.getCustomCorrelation() != null) {
            if (cashoutApplication.getCustomCorrelation()) {
                List<String> customerIds = financeCustomerCorrelationService.listAttentionUsers(SessionUtils.getUserId());

                if (customerIds.isEmpty()) {
                    return new DataGrid<>(true, new ArrayList<>());
                }

                cashoutApplication.setCustomerIds(customerIds);
            }
        }

        dataGrid = cashoutApplicationService.listAll(cashoutApplication);

        return this.formatData(dataGrid);
    }

    private DataGrid<CashoutApplication> formatData(DataGrid<CashoutApplication> dataGrid) {
        if (dataGrid.getRows().isEmpty()) {
            return dataGrid;
        }

        Set<String> userIds = new HashSet<>();
        Set<String> allUserIds = new HashSet<>();
        Set<String> salesUserIds = new HashSet<>();
        Map<String, User> userMap = new HashMap<>();
        Map<String, User> salesMap = new HashMap<>();
        Map<String, Wallet> walletMap = new HashMap<>();

        for (CashoutApplication cashoutApplication : dataGrid.getRows()) {
            if (cashoutApplication.getWithdrawalType() != null) {
                cashoutApplication.setWithdrawalTypeStr(cashoutApplication.getWithdrawalType().getDesc());
            }

            if (cashoutApplication.getCashoutState() != null) {
                cashoutApplication.setCashoutStateStr(cashoutApplication.getCashoutState().getDesc());
            }

            userIds.add(cashoutApplication.getCustomerId());
            allUserIds.add(cashoutApplication.getCustomerId());

            if (StringUtils.isNotBlank(cashoutApplication.getHandlePersonId())) {
                userIds.add(cashoutApplication.getHandlePersonId());
            }

            if (StringUtils.isNotBlank(cashoutApplication.getSaleId())) {
                salesUserIds.add(cashoutApplication.getSaleId());
            }
        }

        if (!userIds.isEmpty()) {
            userMap = userService.mapByIds(new ArrayList<>(userIds));
            List<Wallet> wallets = walletService.findByCustomerIds(new ArrayList<>(userIds));
            if (wallets != null && !wallets.isEmpty()) {
                wallets.stream().forEach(wallet -> {
                    walletMap.put(wallet.getCustomerId(), wallet);
                });
            }
        }

        if (!salesUserIds.isEmpty()) {
            salesMap = userService.mapByIds(new ArrayList<>(salesUserIds));
        }

        for (CashoutApplication cashoutApplication : dataGrid.getRows()) {
            if (StringUtils.isNotBlank(cashoutApplication.getHandlePersonId())) {
                cashoutApplication.setHandlePersonName(userMap.get(cashoutApplication.getHandlePersonId()) != null ? userMap.get(cashoutApplication.getHandlePersonId()).getName() : "");
            }

            if (StringUtils.isNotBlank(cashoutApplication.getSaleId()) && salesMap.containsKey(cashoutApplication.getSaleId())) {
                cashoutApplication.setSalesName(salesMap.get(cashoutApplication.getSaleId()).getName());
            }

            User customer = userMap.get(cashoutApplication.getCustomerId());

            if (customer != null) {
                cashoutApplication.setCustomerCode(customer.getCustomerCode());
                cashoutApplication.setSimpleCompanyName(customer.getSimpleCompanyName());
            }

            if (walletMap.containsKey(cashoutApplication.getCustomerId())) {
                cashoutApplication.setCurrentAmount(walletMap.get(cashoutApplication.getCustomerId()).getCurrentAmount());
            }
        }

        return dataGrid;
    }

    public DataGrid addOrUpdate(CashoutApplication cashoutApplication) {
        DataGrid<CashoutApplication> dataGrid = new DataGrid<>();

        cashoutApplication.setOrganizationId(SessionUtils.getOrganizationId());
        String customerId = SessionUtils.getUserId();

        //判断是否超过可与余额
        WalletDto walletDto = walletService.findByCustomerId(customerId);

        if (cashoutApplication.getAmount() != null) {
            //说明是客户申请
            if (cashoutApplication.getAmount() > walletDto.getCurrentAmount().doubleValue()) {
                throw new EshipException("提现金额不能超过账户余额(" + walletDto.getCurrentAmount() + " 元)");
            }
        }

        //@Author: Kevin 2019-05-13 15:10
        //@Descreption: 先将已经申请的待审核的记录改为:已撤销
        cashoutApplicationService.updateStatusByCustomer(CashoutState.APPLYED, CashoutState.CANCELED, customerId);

        if (StringUtils.isNotBlank(cashoutApplication.getId())) {
            CashoutApplication cashoutApplicationDb = cashoutApplicationService.findById(cashoutApplication.getId());

            if (cashoutApplicationDb == null) {
                throw new EshipException("该条提现申请不存在!");
            }

            dataGrid.setMsg("修改成功!");

            if (cashoutApplication.getAmount() != null) {
                cashoutApplicationDb.setAmount(cashoutApplication.getAmount());
            }

            cashoutApplicationDb.setCashoutState(CashoutState.APPLYED);

            if (StringUtils.isNotBlank(cashoutApplication.getCustomerDesc())) {
                cashoutApplicationDb.setCustomerDesc(cashoutApplication.getCustomerDesc());
                dataGrid.setMsg("操作成功, 请等待审核!");

                //@Author: Kevin 2019-10-11 16:11
                //@Descreption: 状态改为处理中
                //cashoutApplicationDb.setCashoutState(CashoutState.IN_PROCESSING);
            }

            if (StringUtils.isNotBlank(cashoutApplication.getSaleDesc())) {
                cashoutApplicationDb.setSaleDesc(cashoutApplication.getSaleDesc());
                cashoutApplicationDb.setSaleId(SessionUtils.getUserId());
                dataGrid.setMsg("操作成功, 请等待审核!");

                //@Author: Kevin 2019-10-11 16:11
                //@Descreption: 状态改为处理中
                cashoutApplicationDb.setCashoutState(CashoutState.IN_PROCESSING);
            }

            cashoutApplicationService.save(cashoutApplicationDb);

            dataGrid.setFlag(true);

        } else {
            //查询当前分公司下所有财务
            //List<User> financeUsers = userRoleService.findWarehouseUserByRoleCode(SessionUtils.getWarehouseId(), User.UserRoleCode.finance.name());

            //@Author: Kevin 2019-05-23 10:11
            //@Descreption: 最新修改: 不分配了, 财务都可以处理

            //随机分配给某个财务
            /*if (financeUsers.size() > 0) {
                Random random = new Random();
                int index= random.nextInt(financeUsers.size());

                cashoutApplication.setHandlePersonId(financeUsers.get(index).getId());
            }*/

            //@Author: Hukai 2019-12-26 16:59
            //@Descreption: 判断客户是否有正在处理的提现申请, 如果有则不能重复提交
            Long count = cashoutApplicationService.countByCustomerIdAndCashoutState(customerId, CashoutState.APPLYED);
            if (count.intValue() > 0) {
                throw new EshipRedirectException("当前客户有正在处理的提现申请, 不允许继续提交新申请!");
            }

            cashoutApplication.setCustomerId(SessionUtils.getUserId());
            cashoutApplication.setCashoutState(CashoutState.APPLYED);
            cashoutApplication.setWarehouseId(SessionUtils.getWarehouseId());
            cashoutApplication.setOrganizationId(SessionUtils.getOrganizationId());

            cashoutApplicationService.save(cashoutApplication);

            dataGrid.setFlag(true);
            dataGrid.setMsg("添加成功, 请等待审核!");
        }

        return dataGrid;
    }

    public DataGrid findById(String id) {
        CashoutApplication cashoutApplicationDb = cashoutApplicationService.findById(id);

        return new DataGrid(true, cashoutApplicationDb);
    }

    public void updateCertificate(String cashoutApplicationId, String savePath) {
        CashoutApplication cashoutApplicationDb = cashoutApplicationService.findById(cashoutApplicationId);

        cashoutApplicationDb.setCertificateUrl(savePath);

        cashoutApplicationService.save(cashoutApplicationDb);
    }

    public DataGrid applyPassed(CashoutApplication cashoutApplication) {
        DataGrid<CashoutApplication> dataGrid = new DataGrid<>();
        CashoutApplication cashoutApplicationDb = cashoutApplicationService.findById(cashoutApplication.getId());
        if(cashoutApplication.getAmount() > 0){
            cashoutApplicationDb.setAmount(cashoutApplication.getAmount());
            //cashoutApplicationService.save(cashoutApplicationDb);
        }
        cashoutApplicationDb.setCertificateUrl(cashoutApplication.getCertificateUrl());
        cashoutApplicationDb.setFinanceDesc(cashoutApplication.getFinanceDesc());
        cashoutApplicationDb.setCashoutState(CashoutState.APPLY_SUCCESS);
        cashoutApplicationDb.setHandlePersonId(SessionUtils.getUserId());
        cashoutApplicationDb.setHandleTime(new Date());

        //1.更新提现记录数据
        cashoutApplicationService.save(cashoutApplicationDb);

        //2.将用户钱包里面的月扣掉相应金额
        walletService.updateCurrentAmount(cashoutApplicationDb.getAmount(), cashoutApplicationDb.getCustomerId());

        WalletDto walletDto = walletService.findByCustomerId(cashoutApplicationDb.getCustomerId());

        //3.生成一条充值消费记录
        RechargeConsumeRecord rechargeConsumeRecord = new RechargeConsumeRecord();

        rechargeConsumeRecord.setCurrentAmount(walletDto.getCurrentAmount());
        rechargeConsumeRecord.setOperateAmount(DataUtils.toBigDecimal(cashoutApplicationDb.getAmount()));
        rechargeConsumeRecord.setCurrentCoupon(walletDto.getCurrentCoupon());
        rechargeConsumeRecord.setOperateCouponAmount(0);

        rechargeConsumeRecord.setCustomerId(walletDto.getCustomerId());
        User user = userService.findOne(cashoutApplicationDb.getCustomerId());
        rechargeConsumeRecord.setCustomerCode(user.getCustomerCode());

        String description = "成功提现" + cashoutApplicationDb.getAmount() + "元";
        rechargeConsumeRecord.setDescription(description);
        rechargeConsumeRecord.setSubSystemBizId(null);

        rechargeConsumeRecord.setOperateTime(new Date());
        rechargeConsumeRecord.setOperateType(OperateType.CONSUME);
        rechargeConsumeRecord.setOperateUserId(SessionUtils.getUserId());
        rechargeConsumeRecord.setOperateUsername(SessionUtils.getCustomer().getName());
        rechargeConsumeRecord.setOrderPrice(DataUtils.toBigDecimal(cashoutApplicationDb.getAmount()));
        rechargeConsumeRecord.setPreOperationAmount(walletDto.getCurrentAmount());
        rechargeConsumeRecord.setWarehouseId(SessionUtils.getWarehouseId());
        rechargeConsumeRecord.setOrganizationId(SessionUtils.getOrganizationId());
        rechargeConsumeRecord.setDestinationId(SessionUtils.getDestinationId());
        rechargeConsumeRecord.setPayType(null);
        rechargeConsumeRecord.setCreatedDate(new Date());

        rechargeConsumeRecordService.save(rechargeConsumeRecord);

        dataGrid.setFlag(true);
        dataGrid.setMsg("操作成功!");

        return dataGrid;
    }

    public DataGrid applyReject(CashoutApplication cashoutApplication) {
        DataGrid<CashoutApplication> dataGrid = new DataGrid<>();
        CashoutApplication cashoutApplicationDb = cashoutApplicationService.findById(cashoutApplication.getId());

        cashoutApplicationDb.setFinanceDesc(cashoutApplication.getFinanceDesc());
        cashoutApplicationDb.setCashoutState(CashoutState.APPLY_REJECT);
        cashoutApplicationDb.setHandlePersonId(SessionUtils.getUserId());
        cashoutApplicationDb.setHandleTime(new Date());

        cashoutApplicationService.save(cashoutApplicationDb);

        dataGrid.setFlag(true);
        dataGrid.setMsg("操作成功!");

        return dataGrid;
    }

    public WalletDto getCustomerBalanceConsume(String customerId){
        return walletService.findByCustomerId(customerId);
    }

    public DataGrid applyCancel(String applyId) {
        DataGrid<CashoutApplication> dataGrid = new DataGrid<>();

        CashoutApplication cashoutApplicationDb = cashoutApplicationService.findById(applyId);

        if (cashoutApplicationDb == null) {
            throw new EshipRedirectException("提现申请信息不存在!");
        }

        if (!CashoutState.APPLYED.name().equals(cashoutApplicationDb.getCashoutState().name())) {
            throw new EshipRedirectException("新申请状态的才能进行撤销操作!");
        }

        cashoutApplicationDb.setCashoutState(CashoutState.CANCELED);

        cashoutApplicationService.save(cashoutApplicationDb);

        dataGrid.setFlag(true);
        dataGrid.setMsg("操作成功!");

        return dataGrid;
    }

    public DataGrid listByCoo(CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        CashoutApplication cashoutApplication = new CashoutApplication();

        //只看审核通过的
        cashoutApplication.setProcessedFlag(true);

        cashoutApplication = constructSearchParam(cashoutApplication, cashoutApplicationSearchDto);

        DataGrid dataGrid = cashoutApplicationService.listAll(cashoutApplication);

        return this.formatResponseData(dataGrid);
    }

    private CashoutApplication constructSearchParam(CashoutApplication cashoutApplication, CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        cashoutApplication.setPagingDto(cashoutApplicationSearchDto.getPagingDto());

        if (cashoutApplicationSearchDto.getRoleCode() != null) {
            cashoutApplication.setRoleCode(cashoutApplicationSearchDto.getRoleCode());
        }

        if (StringUtils.isNotBlank(cashoutApplicationSearchDto.getWarehouseId())) {
            cashoutApplication.setWarehouseId(cashoutApplicationSearchDto.getWarehouseId());
        }

        if (StringUtils.isNotBlank(cashoutApplicationSearchDto.getSalesStaffId())) {
            cashoutApplication.setSaleId(cashoutApplicationSearchDto.getSalesStaffId());
        }

        if (cashoutApplicationSearchDto.getCashoutType() != null) {
            cashoutApplication.setWithdrawalType(cashoutApplicationSearchDto.getCashoutType());
        }

        if (cashoutApplicationSearchDto.getCustomerId() != null) {
            cashoutApplication.setCustomerId(cashoutApplicationSearchDto.getCustomerId());
        }

        if (StringUtils.isNotBlank(cashoutApplicationSearchDto.getStartTime()) && StringUtils.isNotBlank(cashoutApplicationSearchDto.getEndTime())) {
            cashoutApplication.setStartTime(cashoutApplicationSearchDto.getStartTime());
            cashoutApplication.setEndTime(cashoutApplicationSearchDto.getEndTime());
        }

        if (cashoutApplicationSearchDto.getCashoutState() != null) {
            cashoutApplication.setCashoutState(cashoutApplicationSearchDto.getCashoutState());
        }

        return cashoutApplication;
    }

    public DataGrid listBySalesManager(CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        CashoutApplication cashoutApplication = new CashoutApplication();

        //只看审核通过的
        cashoutApplication.setProcessedFlag(true);

        //只查看自己销售下所有客户的
        List<String> salesManagerIds = new ArrayList<>();
        salesManagerIds.add(SessionUtils.getUserId());
        List<SalesStaffAssign> salesStaffAssignList = salesStaffAssignService.findInSaleManagerIds(salesManagerIds);
        if (salesStaffAssignList.isEmpty()) {
            return new DataGrid(true, new ArrayList());
        }

        List<String> salesStaffIds = salesStaffAssignList.stream().map(SalesStaffAssign::getSalesStaffId).collect(Collectors.toList());
        cashoutApplication.setSaleIdList(salesStaffIds);

        cashoutApplication = constructSearchParam(cashoutApplication, cashoutApplicationSearchDto);

        DataGrid dataGrid = cashoutApplicationService.listAll(cashoutApplication);

        return this.formatResponseData(dataGrid);
    }

    public DataGrid<CashoutApplicationResponseDto> listBySales(CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        CashoutApplication cashoutApplication = new CashoutApplication();

        //区分待处理已处理
        cashoutApplication.setProcessedFlag(cashoutApplicationSearchDto.getProcessedFlag());

        //只查看自己名下所有客户的
        List<String> salesStaffIds = new ArrayList<>();
        salesStaffIds.add(SessionUtils.getUserId());
        List<CustomerAssign> customerAssignList = customerAssignService.findBySalesIds(salesStaffIds);
        if (customerAssignList.isEmpty()) {
            return new DataGrid(true, new ArrayList());
        }

        List<String> customerIds = customerAssignList.stream().map(CustomerAssign::getCustomerId).collect(Collectors.toList());
        cashoutApplication.setCustomerIds(customerIds);

        cashoutApplication = constructSearchParam(cashoutApplication, cashoutApplicationSearchDto);

        DataGrid<CashoutApplication> dataGrid = cashoutApplicationService.listAll(cashoutApplication);

        return this.formatResponseData(dataGrid);
    }

    private DataGrid<CashoutApplicationResponseDto> formatResponseData(DataGrid<CashoutApplication> dataGrid) {
        List<CashoutApplicationResponseDto> cashoutApplicationResponseDtos = new ArrayList<>();

        if (dataGrid.isFlag()) {
            List<CashoutApplication> cashoutApplicationList = dataGrid.getRows();

            if (cashoutApplicationList.isEmpty()) {
                return new DataGrid<>(true, new ArrayList<>());
            }

            Set<String> userIds = new HashSet<>();
            cashoutApplicationList.stream().forEach(cashoutApplication -> {
                if (StringUtils.isNotBlank(cashoutApplication.getCustomerId())) {
                    userIds.add(cashoutApplication.getCustomerId());
                }

                if (StringUtils.isNotBlank(cashoutApplication.getSaleId())) {
                    userIds.add(cashoutApplication.getSaleId());
                }

                if (StringUtils.isNotBlank(cashoutApplication.getSaleId())) {
                    userIds.add(cashoutApplication.getSaleId());
                }

                if (StringUtils.isNotBlank(cashoutApplication.getHandlePersonId())) {
                    userIds.add(cashoutApplication.getHandlePersonId());
                }
            });

            Map<String, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

            cashoutApplicationList.stream().forEach(cashoutApplication -> {
                CashoutApplicationResponseDto cashoutApplicationResponseDto = new CashoutApplicationResponseDto();

                cashoutApplicationResponseDto.setId(cashoutApplication.getId());
                cashoutApplicationResponseDto.setCreatedDate(cashoutApplication.getCreatedDate());

                if (userMap.containsKey(cashoutApplication.getCustomerId())) {
                    User customer = userMap.get(cashoutApplication.getCustomerId());
                    cashoutApplicationResponseDto.setSimpleCompanyName(customer.getSimpleCompanyName());
                    cashoutApplicationResponseDto.setCustomerCode(customer.getCustomerCode());
                }

                if (userMap.containsKey(cashoutApplication.getSaleId())) {
                    User sales = userMap.get(cashoutApplication.getSaleId());
                    cashoutApplicationResponseDto.setSalesName(sales.getName());
                }

                cashoutApplicationResponseDto.setAmount(cashoutApplication.getAmount());
                if (cashoutApplication.getWithdrawalType() != null) {
                    cashoutApplicationResponseDto.setWithdrawalType(cashoutApplication.getWithdrawalType());
                    cashoutApplicationResponseDto.setWithdrawalTypeDesc(cashoutApplication.getWithdrawalType().getDesc());
                }

                cashoutApplicationResponseDto.setCashoutAccount(cashoutApplication.getCashoutAccount());
                cashoutApplicationResponseDto.setCashoutCustomerName(cashoutApplication.getCashoutCustomerName());
                cashoutApplicationResponseDto.setCustomerDesc(cashoutApplication.getCustomerDesc());
                cashoutApplicationResponseDto.setSaleDesc(cashoutApplication.getSaleDesc());
                cashoutApplicationResponseDto.setFinanceDesc(cashoutApplication.getFinanceDesc());
                cashoutApplicationResponseDto.setCashoutState(cashoutApplication.getCashoutState());
                cashoutApplicationResponseDto.setCashoutStateDesc(cashoutApplication.getCashoutState().getDesc());

                if (StringUtils.isNotBlank(cashoutApplication.getHandlePersonId()) && userMap.containsKey(cashoutApplication.getHandlePersonId())) {
                    cashoutApplicationResponseDto.setHandlePersonName(userMap.get(cashoutApplication.getHandlePersonId()).getName());
                }

                cashoutApplicationResponseDto.setHandleTime(cashoutApplication.getHandleTime());

                cashoutApplicationResponseDtos.add(cashoutApplicationResponseDto);
            });
        }

        return new DataGrid<>(true, cashoutApplicationResponseDtos);
    }
}
