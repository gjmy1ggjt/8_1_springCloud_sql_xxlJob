package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.sales.business.SaleBusiness;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.sales.business.CustomServiceAssignBusiness;
import com.yangshan.eship.sales.dto.*;
import com.yangshan.eship.sales.service.serv.CustomServiceAssignServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: Kevin
 * @Date: 2019-11-20 13:46
 * @Description:
 */
@RestController
@RequestMapping(Version.VERSION + "/customServiceCustomer")
@Api(value = "CustomServiceCustomerController", tags = "客服与客户管理")
public class CustomServiceCustomerController {
    @Autowired
    private CustomServiceAssignServiceI customServiceAssignService;

    @Autowired
    private CustomServiceAssignBusiness customServiceAssignBusiness;

    @Autowired
    private SaleBusiness saleBusiness;

    @ApiOperation(value = "查询客服列表", notes = "by Hukai")
    @PostMapping(value = "/listCustomService")
    public DataGrid<CustomServiceSearchResponseDto> listCustomService(@RequestBody CustomServiceSearchRequestDto customServiceSearchRequestDto) {
        String orgId = SessionUtils.getOrganizationId();
        if (StringUtils.isBlank(orgId)) {
            throw new EshipRedirectException("用户未登录!");
        }

        customServiceSearchRequestDto.setOrganizationId(orgId);
        customServiceSearchRequestDto.setWarehouseId(SessionUtils.getWarehouseId());
        return customServiceAssignService.listCustomService(customServiceSearchRequestDto);
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
        return customServiceAssignBusiness.listSalesCustomerAssign(customerInfoRequestDto);
    }

    @ApiOperation(value = "所属客服下拉数据", notes = "by Hukai")
    @GetMapping(value = "/listCustomServiceData")
    public DataGrid<CustomServiceSearchResponseDto> listCustomServiceData() {
        String orgId = SessionUtils.getOrganizationId();
        if (StringUtils.isBlank(orgId)) {
            throw new EshipRedirectException("用户未登录!");
        }

        CustomServiceSearchRequestDto customServiceSearchRequestDto = new CustomServiceSearchRequestDto();
        customServiceSearchRequestDto.setOrganizationId(orgId);
        customServiceSearchRequestDto.setWarehouseId(SessionUtils.getWarehouseId());
        return customServiceAssignService.listCustomService(customServiceSearchRequestDto);
    }

    @ApiOperation(value = "所属销售下拉数据", notes = "by Hukai")
    @GetMapping(value = "/listSalesData")
    public DataGrid<CustomServiceSalseResponseDto> listSalesData() {
        String orgId = SessionUtils.getOrganizationId();
        if (StringUtils.isBlank(orgId)) {
            throw new EshipRedirectException("用户未登录!");
        }

        return customServiceAssignBusiness.listSalesData();
    }

    @ApiOperation(value = "给客户分配客服", notes = "by Hukai")
    @PostMapping(value = "/distributionCustomService/{customServiceStaffId}")
    public DataGrid distributionCustomService(@PathVariable String customServiceStaffId, @RequestBody List<String> customerIds) {

        return customServiceAssignService.distributionCustomService(customServiceStaffId, customerIds);
    }

    @ApiOperation(value = "客服客户取消分配", notes = "by Hukai")
    @PostMapping(value = "/cancelDistribution")
    public DataGrid cancelDistribution(@RequestBody List<String> customerIds) {

        return customServiceAssignService.cancelDistribution(customerIds);
    }

    @ApiOperation(value = "运营总监-客服调配-客服经理列表", notes = "by Hukai")
    @PostMapping(value = "/listCustomServiceManagers")
    public DataGrid<StaffInfoStaticsDto> listCustomServiceManagers() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }

        return customServiceAssignBusiness.listCustomServiceManagers(warehouseId);
    }

    @ApiOperation(value = "客服列表", notes = "by Hukai")
    @PostMapping(value = "/listCustomServices")
    public DataGrid<CustomServiceInfoDto> listCustomServices(@RequestBody StaffSearchRequestDto staffSearchRequestDto) {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }
        staffSearchRequestDto.setWarehouseId(warehouseId);

        if (staffSearchRequestDto.getUnDistributeUser() != null && staffSearchRequestDto.getUnDistributeUser()) {
            staffSearchRequestDto.setRoleCode(User.UserRoleCode.custom_service.name());
            return saleBusiness.listunDistributeSalesOrCustomService(staffSearchRequestDto);
        }

        return customServiceAssignBusiness.listCustomServices(staffSearchRequestDto);
    }

    @ApiOperation(value = "分配客服给客服经理", notes = "by Hukai")
    @PostMapping(value = "/distributionCustomServices/{customManagerId}")
    public DataGrid distributionCustomServices(@PathVariable("customManagerId") String customManagerId, @RequestBody List<String> customServiceIds) {
        return customServiceAssignBusiness.distributionCustomServices(customManagerId, customServiceIds);
    }

    @ApiOperation(value = "客服客服经理取消分配", notes = "by Hukai")
    @PostMapping(value = "/cancelDistributionCustomServices")
    public DataGrid cancelDistributionCustomServices(@RequestBody List<String> customServiceIds) {
        return customServiceAssignBusiness.cancelDistributionCustomServices(customServiceIds);
    }

}
