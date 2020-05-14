package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.finance.dto.cust.WalletDto;
import com.yangshan.eship.finance.entity.cust.Wallet;
import com.yangshan.eship.finance.service.cust.WalletServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderPackageServiceI;
import com.yangshan.eship.sales.business.AccountApplicationBusiness;
import com.yangshan.eship.sales.dto.*;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.AccountApplication;
import com.yangshan.eship.sales.entity.sale.AccountStatus;
import com.yangshan.eship.sales.entity.sale.AccountType;
import com.yangshan.eship.sales.entity.sale.DeliveryRequirementsType;
import com.yangshan.eship.sales.service.sales.AccountApplicationServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping(Version.VERSION + "/accountApplication")
@Api(value = "AccountApplicationController", tags = "账期申请")
public class AccountApplicationController {

    private static Logger logger = LoggerFactory.getLogger(AccountApplicationController.class);

    @Autowired
    private AccountApplicationBusiness applicationBusiness;

    @Autowired
    private AccountApplicationServiceI accountApplicationService;

    @Autowired
    private FinanceOrderPackageServiceI financeOrderPackageService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private WalletServiceI walletService;

    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation(value = "查询当前登录的销售下所有客户的账期信息", notes = "by 胡凯")
    public DataGrid<AccountApplicationDto> list(@RequestBody AccountApplicationRequestDto accountApplicationRequestDto) {
        //查询当前登录的销售下所有客户的账期信息
        AccountApplicationDto applicationDto = new AccountApplicationDto();
        String currentUserId = SessionUtils.getUserId();
        applicationDto.setSalesStaffId(currentUserId);
        BeanUtils.copyProperties(accountApplicationRequestDto, applicationDto);

        if (accountApplicationRequestDto.getAccountStatus() != null) {
            applicationDto.setAccountStatus(accountApplicationRequestDto.getAccountStatus().name());
        }        applicationDto.setOrganizationId(SessionUtils.getOrganizationId());
        return applicationBusiness.getAll(applicationDto);
    }

    @RequestMapping(value = "/listAll", method = RequestMethod.POST)
    @ApiOperation(value = "财务账期审批列表", notes = "by 胡凯")
    public DataGrid<AccountApplicationDto> listAll(@RequestBody AccountApplicationRequestDto accountApplicationRequestDto) {
        AccountApplicationDto applicationDto = new AccountApplicationDto();
        BeanUtils.copyProperties(accountApplicationRequestDto, applicationDto);
        if (accountApplicationRequestDto.getAccountStatus() != null) {
            applicationDto.setAccountStatus(accountApplicationRequestDto.getAccountStatus().name());
        }
        applicationDto.setOrganizationId(SessionUtils.getOrganizationId());

        if ("GET_WAREHOUSE_FROM_SESSION".equals(applicationDto.getWarehouseId())) {
            applicationDto.setWarehouseId(SessionUtils.getWarehouseId());
        }

        return applicationBusiness.getAll(applicationDto);
    }

    @RequestMapping(value = "/mapAllType", method = RequestMethod.POST)
    @ApiOperation(value = "获取所有的出货限制类型", notes = "by 胡凯")
    public DataGrid mapAllType() {
        return new DataGrid(true, DeliveryRequirementsType.mapAllType());
    }

    @RequestMapping(value = "/getMyPaymentList", method = RequestMethod.POST)
    @ApiOperation(value = "获取当前登录客户的账期列表", notes = "by 胡凯")
    public DataGrid<AccountApplicationDto> getMyPaymentList(@RequestBody AccountApplicationDto applicationDto) {
        //查询当前登录的销售下所有客户的账期信息
        String currentUserId = SessionUtils.getUserId();

        if (StringUtils.isBlank(currentUserId)) {
            return new DataGrid<>(true, new ArrayList<>());
        }

        applicationDto.setCustomerId(currentUserId);
        applicationDto.setOrganizationId(SessionUtils.getOrganizationId());
        return applicationBusiness.getAll(applicationDto);
    }

    @RequestMapping(value = "/listWarehouseUsersPayments", method = RequestMethod.POST)
    @ApiOperation(value = "运营总监/销售经理查看客户账期列表", notes = "by 胡凯")
    public DataGrid<AccountApplicationDto> listWarehouseUsersPayments(@RequestBody AccountApplicationSearchDto accountApplicationSearchDto) {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录");
        }

        AccountApplicationDto accountApplicationDto = new AccountApplicationDto();
        accountApplicationDto.setWarehouseId(warehouseId);
        accountApplicationDto.setOrganizationId(SessionUtils.getOrganizationId());

        //只看已经申请通过的
        accountApplicationDto.setAccountStatus(AccountStatus.PASS_REVIEW.name());

        if (accountApplicationSearchDto.getAccountType() != null) {
            accountApplicationDto.setAccountType(accountApplicationSearchDto.getAccountType().name());
        }

        if (StringUtils.isNotBlank(accountApplicationSearchDto.getCustomerId())) {
            accountApplicationDto.setCustomerId(accountApplicationSearchDto.getCustomerId());
        }

        if (StringUtils.isNotBlank(accountApplicationSearchDto.getSalesStaffId())) {
            accountApplicationDto.setSalesStaffId(accountApplicationSearchDto.getSalesStaffId());
        }

        if (StringUtils.isNotBlank(accountApplicationSearchDto.getStartCreatedDate())) {
            accountApplicationDto.setStartCreatedDate(accountApplicationSearchDto.getStartCreatedDate());
        }

        if (StringUtils.isNotBlank(accountApplicationSearchDto.getEndCreatedDate())) {
            accountApplicationDto.setEndCreatedDate(accountApplicationSearchDto.getEndCreatedDate());
        }

        accountApplicationDto.setPagingDto(accountApplicationSearchDto.getPagingDto());

        return applicationBusiness.getAll(accountApplicationDto);
    }

    /**
     * @Author: Kevin
     * @Date: 2018-10-17 11:13
     * @Description: 提交账期申请(提交/修改)
     */
    @RequestMapping(value = "/commitAccountInfo", method = RequestMethod.POST)
    @ApiOperation(value = "修改或提交账期申请", notes = "by 胡凯")
    public DataGrid<String> commitAccountInfo(@RequestBody AccountApplicationSaveDto accountApplicationSaveDto) {
        //获取当前登录用户id
        AccountApplicationDto applicationDto = new AccountApplicationDto();
        BeanUtils.copyProperties(accountApplicationSaveDto, applicationDto);
        applicationDto.setSalesStaffId(SessionUtils.getUserId());

        return applicationBusiness.commitAccountInfo(applicationDto);
    }

    /**
     * 列出我的所有客户
     * @return
     */
    @RequestMapping(value = "/getMyCustomers", method = RequestMethod.GET)
    public DataGrid<CustomerAssign> getMyCustomer() {
        String currentUserId = SessionUtils.getUserId();
        return applicationBusiness.getMyCustomers(currentUserId,1);
    }

    /**
     * 列出所有账期类型
     * @return
     */
    @RequestMapping(value = "/getAccountTypes", method = RequestMethod.GET)
    @ApiOperation(value = "获取所有账期类型", notes = "by 胡凯")
    public DataGrid<Map<String, String>> getAccountTypes() {
        return new DataGrid<>(true, AccountType.listAccountType());
    }

    /**
     * 列出所有账期状态
     * @return
     */
    @RequestMapping(value = "/getAccountStatus", method = RequestMethod.GET)
    @ApiOperation(value = "获取所有账期状态", notes = "by 胡凯")
    public DataGrid<Map<String, String>> getAccountStatus() {
        DataGrid<Map<String, String>> grid = new DataGrid<>();

        grid.setObj(AccountStatus.listAccountStatus());

        return grid;
    }

    /**
     * 删除账期信息
     * @param id
     * @return
     */
    @RequestMapping(value = "/deleteAccountInfo/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "删除账期信息", notes = "by 胡凯")
    public DataGrid<String> deleteAccountInfo(@PathVariable("id") String id) {
        return applicationBusiness.deleteAccountInfo(id);
    }

    /**
     * @Author: HuKai
     * @Date: 2018/1/18 17:26
     * @Description: 驳回账期申请
     */
    @RequestMapping(value = "/turnDownApply", method = RequestMethod.POST)
    @ApiOperation(value = "驳回账期申请", notes = "by Hukai")
    public DataGrid turnDownApply(@RequestBody CancelRequestDto cancelRequestDto) {
        AccountApplicationDto applicationDto = new AccountApplicationDto();
        applicationDto.setId(cancelRequestDto.getId());
        applicationDto.setTurnDownReason(cancelRequestDto.getReason());

        return accountApplicationService.turnDownApply(applicationDto);
    }

    /**
     * @Author: HuKai
     * @Date: 2018/1/18 17:54
     * @Description: 审核通过
     */
    @RequestMapping(value = "/applySuccess/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "账期审核通过", notes = "by Hukai")
    public DataGrid applySuccess(@PathVariable("id") String id) {
        AccountApplication application = accountApplicationService.findOne(id);
        String customerId = application.getCustomerId();

        AccountStatus accountStatus = application.getAccountStatus();

        //修改账期状态
        application = accountApplicationService.applySuccess(id, AccountStatus.PASS_REVIEW);

        //获取客户钱包信息
        WalletDto walletDto = walletService.findByCustomerId(customerId);

        //是否是无账期
        if (AccountType.NO_ACCOUNT.equals(application.getAccountType())) {
            walletDto.setLineOfCredit(DataUtils.toBigDecimal(0f));
            walletDto.setOvertimeDays(0);
            walletDto.setTakeDeliveryEndtime(null);
            walletDto.setPaymentDays(0);
            walletDto.setLineOfCreditUsed(DataUtils.toBigDecimal(0f));
            walletDto.setPaymentDaysType(AccountType.NO_ACCOUNT);

            Wallet wallet = new Wallet();
            BeanUtils.copyProperties(walletDto, wallet);

            walletService.saveWallet(wallet);
        } else {
            //修改该用户钱包信息
            boolean result = walletService.updateWalletAccount(application);

            if (!result) {
                //账期状态还原回去
                accountApplicationService.applySuccess(id, accountStatus);
            }
        }

        return new DataGrid();
    }

    /**
     * @Author: HuKai
     * @Date: 2018/1/31 14:31
     * @Description: 计算一个日期之后的iDays天的时间
     */
    public static Date getLateDate(String sDate, int iDays) {
        Date sLateDate = null;
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            // add方法中的第二个参数n中，正数表示该日期后n天，负数表示该日期的前n天
            calendar.add(Calendar.DATE, iDays);

            sLateDate = calendar.getTime();
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
        }
        return sLateDate;
    }
    /**
     * @Author: tsd
     * @Date: 2018/1/18 17:54
     * @Description: 账期个数
     */
    @RequestMapping(value = "/applyCount", method = RequestMethod.GET)
    public DataGrid applyCount() {
        DataGrid dataGrid = new DataGrid();
        AccountApplicationDto applicationDto = new AccountApplicationDto();
        applicationDto.setOrganizationId(SessionUtils.getOrganizationId());
        applicationDto.setWarehouseId(SessionUtils.getWarehouseId());
        applicationDto.setAccountStatus(AccountStatus.APPLYED.name());
        DataGrid dataGrid1 = accountApplicationService.getAll(applicationDto);
        dataGrid.setFlag(true);
        if(dataGrid1.getRows() != null) {
            dataGrid.setObj(dataGrid1.getRows().size());
        }else{
            dataGrid.setObj(0);
        }
        return dataGrid;
    }

}
