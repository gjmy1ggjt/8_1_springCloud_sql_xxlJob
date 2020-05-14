package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.dto.account.CustomerDetailInfo;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.constants.ErrorCodeConstant;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.sales.business.CustomServiceAssignBusiness;
import com.yangshan.eship.sales.business.SaleBusiness;
import com.yangshan.eship.sales.dto.*;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.AccountApplication;
import com.yangshan.eship.sales.service.sales.AccountApplicationServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author HuKai
 * @date 2017年11月15日 下午2:41:06
 * 类说明 ==>销售Controller
 */
@RestController
@RequestMapping(Version.VERSION + "/salesAssign")
@Api(value = "SalesController", tags = "销售Controller")
public class SalesController {

    @Autowired
    private AccountApplicationServiceI accountApplicationService;

    @Autowired
    private SaleBusiness saleBusiness;

    @Autowired
    private CustomServiceAssignBusiness customServiceAssignBusiness;

    /**
     * 当前客服下面的所有客户
     *
     * @param customerAssign
     * @return
     */
    @RequestMapping(value = "/cusService", method = RequestMethod.POST)
    public DataGrid<CustomerAssignDto> getCustCustomers(@RequestBody CustomerAssign customerAssign) {
        if (customerAssign == null) {
            customerAssign = new CustomerAssign();
        }

        if (StringUtils.isBlank(customerAssign.getCustomServiceStaffId())) {
            customerAssign.setCustomServiceStaffId(SessionUtils.getUserId());
        }

        return saleBusiness.getMyCustomers(customerAssign);

    }

    /**
     * 当前仓库下面的所有客户
     * 既运营总监能够查看的分公司客户
     *
     * @param customerAssign
     * @return
     * @author LinYun
     * @date 2018年08月03日 上午10:41:06
     */
    @RequestMapping(value = "/cooCustomer", method = RequestMethod.GET)
    public DataGrid<CustomerAssignDto> getCooCustomer(CustomerAssign customerAssign) {
        customerAssign.setWarehouseId(SessionUtils.getWarehouseId());
        return saleBusiness.getMyCustomers(customerAssign);
    }

    /**
     * 当前销售下面的所有客户
     *
     * @param customerAssign
     * @return
     */
    @RequestMapping(value = "/sale", method = RequestMethod.GET)
    public DataGrid<CustomerAssignDto> getSaleCustomers(CustomerAssign customerAssign) {
        if (customerAssign == null) {
            customerAssign = new CustomerAssign();
        }

        if (StringUtils.isBlank(customerAssign.getSalesStaffId())) {
            customerAssign.setSalesStaffId(SessionUtils.getUserId());
        }

        return saleBusiness.getMyCustomers(customerAssign);

    }

    @RequestMapping(value = "/getAccountApplyInfo/{applyId}", method = RequestMethod.GET)
    public DataGrid<AccountApplicationDto> getAccountApplyInfo(@PathVariable String applyId) {
        DataGrid<AccountApplicationDto> grid = new DataGrid<AccountApplicationDto>();

        AccountApplication application = accountApplicationService.findOne(applyId);

        if (application == null) {
            throw new EshipException(ErrorCodeConstant.NO_RESULT);
        }

        AccountApplicationDto applicationDto = new AccountApplicationDto();

        BeanUtils.copyProperties(application, applicationDto);

        if (application.getAccountStartTime() != null) {
            applicationDto.setAccountStartTime(DateUtil.formatSecond(application.getAccountStartTime()));
        }

        if (application.getAccountEndTime() != null) {
            applicationDto.setAccountEndTime(DateUtil.formatSecond(application.getAccountEndTime()));
        }


        applicationDto.setAccountType(application.getAccountType() != null ? application.getAccountType().name() : "");
        applicationDto.setAccountStatus(application.getAccountStatus() != null ? application.getAccountStatus().name() : "");

        grid.setObj(applicationDto);

        return grid;
    }

    @RequestMapping(value = "/getCustomerChartData", method = RequestMethod.GET)
    public ChartDataDto getCustomerChartData(CustomerAssignDto customerAssignDto) {
        if (StringUtils.isBlank(customerAssignDto.getRoleCode())) {
            return new ChartDataDto();
        }

        if (User.UserRoleCode.sale.name().equals(customerAssignDto.getRoleCode())) {
            //销售下的客户统计数据
            customerAssignDto.setSalesStaffId(SessionUtils.getUserId());
        }

        if (User.UserRoleCode.custom_service.name().equals(customerAssignDto.getRoleCode())) {
            //客服下的客户统计数据
            customerAssignDto.setCustomServiceStaffId(customerAssignDto.getCustomServiceStaffId());
        }

        //customerAssignDto.setOrganizationId(SessionUtils.getOrganizationId());

        return saleBusiness.getCustomerChartData(customerAssignDto);
    }

    @RequestMapping(value = "/getCustomerInfo/{customerId}", method = RequestMethod.GET)
    @ApiOperation(value = "销售-完善客户资料-获取客户信息", notes = "by hukai")
    public DataGrid<SingleCustomerInfoDto> getCustomerInfo(@PathVariable("customerId") String customerId) {

        return saleBusiness.getCustomerInfo(customerId);
    }

    @RequestMapping(value = "/updateCustomerInfo", method = RequestMethod.POST)
    @ApiOperation(value = "销售-完善客户资料-修改客户信息", notes = "by hukai")
    public DataGrid updateCustomerInfo(@RequestBody SingleCustomerInfoDto singleCustomerInfoDto) {
        singleCustomerInfoDto.setOrganizationId(SessionUtils.getOrganizationId());

        return saleBusiness.updateCustomerInfo(singleCustomerInfoDto);
    }

    /**
     * @Author: HuKai
     * @Date: 2018/1/10 10:01
     * @Description: 获取客户详细信息
     */
    @RequestMapping(value = "/getUserDetailInfo/{userId}", method = RequestMethod.GET)
    @ApiOperation(value = "获取客户详细信息", notes = "by hukai, 账期信息直接在该条记录中有相关值")
    public DataGrid<CustomerDetailInfo> getUserDetailInfo(@PathVariable("userId") String userId) {

        return saleBusiness.getUserDetailInfo(userId);
    }

    /**
     * @Author: HuKai
     * @Date: 2018/1/10 10:02
     * @Description: 更新客户信息
     */
    @RequestMapping(value = "/updateUserInfo", method = RequestMethod.POST)
    public DataGrid updateUser(@RequestBody CustomerDetailInfo customerDetailInfo) {
        customerDetailInfo.setOrganizationId(SessionUtils.getOrganizationId());

        return saleBusiness.updateUser(customerDetailInfo);
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-17 0017 下午 5:12
     * @Description: 用于同步user表中的customerCode和customerAssign表中的customerCode
     */
    @RequestMapping(value = "/updateCustAssignCode", method = RequestMethod.GET)
    public DataGrid updateCustAssignCode() {

        return saleBusiness.updateCustAssignCode();
    }

    /**
     * @Author: Kevin
     * @Date: 2018-10-17 14:28
     * @Description: 获取分公司或总公司下所有销售
     * 如果要查询总公司下的所有销售, 则warehouseId传值为: getStaffListFromOrganization, 否则传: getStaffListFromWarehouse
     * 因为都是从session中拿warehouseId或orgId, 故前台传固定值用于区分就可以了
     */
    @RequestMapping(value = "/listSalesStaffList/{warehouseId}", method = RequestMethod.GET)
    @ApiOperation(value = "获取分公司或总公司下所有销售", notes = "by hukai; 如果要查询总公司下的所有销售, 则warehouseId传值为: " +
            "getStaffListFromOrganization, 否则传: getStaffListFromWarehouse因为都是从session中拿warehouseId或orgId, 故前台传固定值用于区分就可以了")
    @JsonFilter(type = User.class, include = "id, name")
    public DataGrid<User> listSalesStaffList(@PathVariable("warehouseId") String warehouseId) {

        return saleBusiness.listSalesStaffList(warehouseId);
    }

    @ApiOperation(value = "获取分公司下所有销售下拉数据", notes = "by Hukai")
    @JsonFilter(type = User.class, include = "id, name")
    @GetMapping(value = "/listWarehouseSalesStaffs")
    public DataGrid<User> listWarehouseSalesStaffs() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }

        return saleBusiness.listSalesStaffList(warehouseId);
    }

    @ApiOperation(value = "运营总监-销售调配-销售经理列表", notes = "by Hukai")
    @PostMapping(value = "/listSalesManagers")
    public DataGrid<StaffInfoStaticsDto> listCustomServiceManagers() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }

        return saleBusiness.listSalesManagers(warehouseId);
    }

    @ApiOperation(value = "运营总监-销售调配-销售列表", notes = "by Hukai")
    @PostMapping(value = "/listSales")
    public DataGrid<CustomServiceInfoDto> listSales(@RequestBody StaffSearchRequestDto staffSearchRequestDto) {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }
        staffSearchRequestDto.setWarehouseId(warehouseId);

        return saleBusiness.listSales(staffSearchRequestDto);
    }

    @ApiOperation(value = "分配销售给销售经理", notes = "by Hukai")
    @PostMapping(value = "/distributionSales/{salesManagerId}")
    public DataGrid distributionSales(@PathVariable("salesManagerId") String salesManagerId, @RequestBody List<String> salesStaffIds) {
        return saleBusiness.distributionSales(salesManagerId, salesStaffIds);
    }

    @ApiOperation(value = "销售销售经理取消分配", notes = "by Hukai")
    @PostMapping(value = "/cancelDistributionSales")
    public DataGrid cancelDistributionSales(@RequestBody List<String> salesIds) {
        return saleBusiness.cancelDistributionSales(salesIds);
    }

    /***********************销售经理分配客户给销售**************************/
    @ApiOperation(value = "销售经理-客户分配-销售列表", notes = "by Hukai")
    @PostMapping(value = "/listSaleStaffs")
    public DataGrid<StaffInfoStaticsDto> listSaleStaffs() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }

        return saleBusiness.listSalesStaffs(SessionUtils.getUserId());
    }

    @ApiOperation(value = "查询客户列表", notes = "by Hukai")
    @PostMapping(value = "/listCustomers")
    public DataGrid<CustomerInfoResponseDto> listCustomerAssign(@RequestBody CustomerInfoRequestDto customerInfoRequestDto) {
        String orgId = SessionUtils.getOrganizationId();
        if (StringUtils.isBlank(orgId)) {
            throw new EshipRedirectException("用户未登录!");
        }

        customerInfoRequestDto.setOrganizationId(orgId);
        customerInfoRequestDto.setWarehouseId(SessionUtils.getWarehouseId());

        //销售经理只能看自己下面销售的客户
        if (User.UserRoleCode.sale_manager.name().equals(SessionUtils.getCurrentRoleCode())) {
            customerInfoRequestDto.setSalesManagerId(SessionUtils.getUserId());
        }
        return customServiceAssignBusiness.listSalesCustomerAssign(customerInfoRequestDto);
    }

    @ApiOperation(value = "给客户分配销售", notes = "by Hukai")
    @PostMapping(value = "/distributionSalesCustomer/{salesStaffId}")
    public DataGrid distributionSalesCustomer(@PathVariable String salesStaffId, @RequestBody List<String> customerIds) {

        return saleBusiness.distributionSalesCustomer(salesStaffId, customerIds);
    }

    @ApiOperation(value = "销售客户取消分配", notes = "by Hukai")
    @PostMapping(value = "/cancelDistribution")
    public DataGrid cancelDistribution(@RequestBody List<String> customerIds) {

        return saleBusiness.cancelDistribution(customerIds);
    }

}
 