package com.yangshan.eship.sales.controller;

import com.google.common.collect.Lists;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.order.service.orde.CustomerInfoServiceI;
import com.yangshan.eship.sales.business.CustomerAssignBusiness;
import com.yangshan.eship.sales.dto.*;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.vo.CustomerAssignVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户分配
 *
 * @author: Kee.Li
 * @date: 2017/10/31 15:47
 */
@RestController
@RequestMapping(Version.VERSION + "/customerAssign")
@Api(value = "CustomerAssignController", tags = "客户CustomerAssign")
public class CustomerAssignController {

    @Autowired
    private CustomerAssignBusiness customerAssignBusiness;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private CustomerInfoServiceI customerInfoService;

    /**
     * 客户分配查询
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/11/2 13:34
     */
    @RequestMapping(method = RequestMethod.GET)
    @JsonFilter(type = SalesStaffAssignDto.class, exclude = "version,createdBy,lastUpdatedBy,lastUpdatedDate")
    public DataGrid<CustomerAssignDto> list(CustomerAssign customerAssign) {

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        customerAssign.setOrganizationId(organizationId);

        boolean isCoo = userRoleService.checkUserRole(SessionUtils.getUserId(), User.UserRoleCode.coo);
        if(isCoo){
            //如果是运营总监，则查询分公司的客户
            customerAssign.setWarehouseId(SessionUtils.getWarehouseId());
        }

        return customerAssignBusiness.list(customerAssign);
    }


    /**
     * 查询客服经理的客户
     * @param
     * @author Kee.Li
     * @date 2017/12/29 15:34
     */
    @RequestMapping(value = "/csmCustomers" ,method = RequestMethod.GET)
    @JsonFilter(type = SalesStaffAssignDto.class, exclude = "version,createdBy,lastUpdatedBy,lastUpdatedDate")
    public DataGrid<CustomerAssignDto> listForCustomServiceManager(CustomerAssign customerAssign) {

        List<User> userList = getUsersByPhoneOrEmail(customerAssign);
        //如果传入的手机或邮箱不为空，但是查询结果是空的，则直接返回
        if((StringUtils.isNotBlank(customerAssign.getUserPhone()) || StringUtils.isNotBlank(customerAssign.getUserEmail())) && (userList == null || userList.isEmpty())){
            return new DataGrid<>();
        }

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        customerAssign.setOrganizationId(organizationId);
        customerAssign.setCustomServiceManagerId(SessionUtils.getUserId());

        return customerAssignBusiness.list(customerAssign);
    }

    /**
     * 查询销售经理的客户
     * @param
     * @author Kee.Li
     * @date 2017/12/29 15:34
     */
    @RequestMapping(value = "/smCustomers",method = RequestMethod.GET)
    @JsonFilter(type = SalesStaffAssignDto.class, exclude = "version,createdBy,lastUpdatedBy,lastUpdatedDate")
    public DataGrid<CustomerAssignDto> listForSalesManager(CustomerAssign customerAssign) {

        List<User> userList = getUsersByPhoneOrEmail(customerAssign);
        //如果传入的手机或邮箱不为空，但是查询结果是空的，则直接返回
        if((StringUtils.isNotBlank(customerAssign.getUserPhone()) || StringUtils.isNotBlank(customerAssign.getUserEmail())) && (userList == null || userList.isEmpty())){
            return new DataGrid<>();
        }

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        customerAssign.setOrganizationId(organizationId);
        customerAssign.setSalesManagerId(SessionUtils.getUserId());

        return customerAssignBusiness.list(customerAssign);
    }

    private List<User> getUsersByPhoneOrEmail(CustomerAssign customerAssign) {
        List<User> userList = Lists.newArrayList();
        //判断手机号/邮箱是否传过来
        if (StringUtils.isNotBlank(customerAssign.getUserPhone())) {
            userList = userService.getUserByWarehouseIdAndPhone(SessionUtils.getWarehouseId(), customerAssign.getUserPhone());

            Set<String> userIds = new HashSet<>();
            if (userList != null && !userList.isEmpty()) {
                for (User user : userList) {
                    userIds.add(user.getId());
                }

                customerAssign.setCustomerIds(new ArrayList<>(userIds));
            }
        }

        if (StringUtils.isNotBlank(customerAssign.getUserEmail())) {
             userList = userService.getUserByWarehouseIdAndEmail(SessionUtils.getWarehouseId(), customerAssign.getUserEmail());

            Set<String> userIds = new HashSet<>();
            if (userList != null && !userList.isEmpty()) {
                for (User user : userList) {
                    userIds.add(user.getId());
                }

                customerAssign.setCustomerIds(new ArrayList<>(userIds));
            }
        }
        return userList;
    }

    /**
     * 查询客服的客户
     * @param
     * @author Kee.Li
     * @date 2017/12/29 15:34
     */
    @RequestMapping(value = "/cssCustomers",method = RequestMethod.GET)
    @JsonFilter(type = SalesStaffAssignDto.class, exclude = "version,createdBy,lastUpdatedBy,lastUpdatedDate")
    public DataGrid<CustomerAssignDto> listForCustomServiceStaff(CustomerAssign customerAssign) {

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        customerAssign.setOrganizationId(organizationId);
        customerAssign.setCustomServiceStaffId(SessionUtils.getUserId());

        return customerAssignBusiness.list(customerAssign);
    }

    /**
     * 查询销售的客户
     * @param
     * @author Kee.Li
     * @date 2017/12/29 15:34
     */
    @RequestMapping(value = "/ssCustomers",method = RequestMethod.GET)
    @JsonFilter(type = SalesStaffAssignDto.class, exclude = "version,createdBy,lastUpdatedBy,lastUpdatedDate")
    public DataGrid<CustomerAssignDto> listForSalesStaff(CustomerAssign customerAssign) {

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        customerAssign.setOrganizationId(organizationId);
        customerAssign.setSalesStaffId(SessionUtils.getUserId());

        return customerAssignBusiness.list(customerAssign);
    }

    /**
     * 分配客户
     *
     * @return
     * @author: Kee.Li
     * @date: 2017/11/2 13:34
     */
    @RequestMapping(method = RequestMethod.PUT)
    public DataGrid assign(@RequestBody CustomerAssignVo customerAssignVo) {

        String selectedRowIds = customerAssignVo.getSelectedRowIds();
        String salesManagerId = customerAssignVo.getSalesManagerId();
        String salesStaffId = customerAssignVo.getSalesStaffId();
        String customServiceManagerId = customerAssignVo.getCustomServiceManagerId();
        String customServiceStaffId = customerAssignVo.getCustomServiceStaffId();

        DataGrid dataGrid = new DataGrid();
        dataGrid.setTotal(0);
        dataGrid.setFlag(true);

        if (StringUtils.isBlank(selectedRowIds)) {
            return dataGrid;
        }
        int count = customerAssignBusiness.assign(selectedRowIds, salesManagerId, customServiceManagerId, customServiceStaffId, salesStaffId);

        dataGrid.setTotal(count);

        return dataGrid;
    }

    /**
     * 客户分配销售
     *
     * @return
     * @author: Kee.Li
     * @date: 2017/11/2 13:34
     */
    @RequestMapping(value = "/assignSales",method = RequestMethod.PUT)
    public DataGrid assignSales(@RequestBody CustomerAssignVo customerAssignVo) {

        //客户id
        String selectedRowIds = customerAssignVo.getSelectedRowIds();
        //销售id
        String salesStaffId = customerAssignVo.getSalesStaffId();

        DataGrid dataGrid = new DataGrid();
        dataGrid.setTotal(0);
        dataGrid.setFlag(true);

        if (StringUtils.isBlank(selectedRowIds)) {
            return dataGrid;
        }
        int count = customerAssignService.assignSales(salesStaffId,selectedRowIds);

        dataGrid.setTotal(count);

        return dataGrid;
    }

    /**
     * @author Hukai
     * 获取当前组织下所有客户
     * 订单查询条件==>可选客户下拉列表
     * @return
     */
    @RequestMapping(value = "/listAllCusAssign", method = RequestMethod.GET)
    @JsonFilter(type = CustomerAssignDto.class, include = "id, customerCode, simpleCompanyName")
    public DataGrid<CustomerAssignDto> listAll(CustomerAssign customerAssign) {
        customerAssign.setOrganizationId(SessionUtils.getOrganizationId());
        customerAssign.setPagingDto(null);  //不分页
        return customerAssignBusiness.list(customerAssign);
    }

    /**
    * @Author: HuKai
    * @Date: 2018/4/3 16:34
    * @Description: 查询当前组织下所有客户
    */
    @RequestMapping(value = "/listWarehouseUsers", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, customerCode, simpleCompanyName")
    public DataGrid<User> listWarehouseCusAssign() {

        return customerAssignBusiness.listWarehouseUser(SessionUtils.getOrganizationId());
    }

    /**
     * @Author: Hukai
     * @Date: 2019-12-11 17:29
     * @Description: 运营总监：客户列表
     */
    @ApiOperation(value = "运营总监-客户列表", notes = "by Hukai")
    @RequestMapping(value = "/listCustomers", method = RequestMethod.POST)
    public DataGrid<CustomerInfoResponseDto> listCustomers(@RequestBody CustomerInfoRequestDto customerInfoRequestDto) {
        String organizationId = SessionUtils.getOrganizationId();
        if (StringUtils.isBlank(organizationId)) {
            throw new EshipRedirectException("请先登录");
        }

        customerInfoRequestDto.setOrganizationId(organizationId);
        customerInfoRequestDto.setWarehouseId(SessionUtils.getWarehouseId());

        return customerAssignBusiness.listCustomers(customerInfoRequestDto);
    }

    @ApiOperation(value = "所有销售下拉框数据", notes = "by Hukai")
    @RequestMapping(value = "/listAllSales", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, name")
    public DataGrid<User> listAllSales() {
        String orgId = SessionUtils.getOrganizationId();
        if (StringUtils.isBlank(orgId)) {
            throw new EshipRedirectException("请先登录");
        }

        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            //总公司下的
            return new DataGrid<>(true, userService.findCustomerByCodeAndOrganizationId(User.UserRoleCode.sale.name(), orgId));
        }

        return new DataGrid<>(true, userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.sale.name(), warehouseId));
    }

    @ApiOperation(value = "所有销售经理下拉框数据", notes = "by Hukai")
    @RequestMapping(value = "/listAllSalesManagers", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, name")
    public DataGrid<User> listAllSalesManagers() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录");
        }

        return new DataGrid<>(true, userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.sale_manager.name(), warehouseId));
    }

    @ApiOperation(value = "所有客服下拉框数据", notes = "by Hukai")
    @RequestMapping(value = "/listAllCustomServices", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, name")
    public DataGrid<User> listAllCustomServices() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录");
        }

        return new DataGrid<>(true, userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.custom_service.name(), warehouseId));
    }

    @ApiOperation(value = "所有客服经理下拉框数据", notes = "by Hukai")
    @RequestMapping(value = "/listAllCustomServiceManagers", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, name")
    public DataGrid<User> listAllCustomServiceManagers() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录");
        }

        return new DataGrid<>(true, userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.custom_service_manager.name(), warehouseId));
    }

    @ApiOperation(value = "所有财务下拉框数据", notes = "by Hukai")
    @RequestMapping(value = "/listAllFinance", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, name")
    public DataGrid<User> listAllFinance() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录");
        }

        return new DataGrid<>(true, userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.finance.name(), warehouseId));
    }

    @ApiOperation(value = "所有客户下拉框数据", notes = "by Hukai")
    @RequestMapping(value = "/listAllCustomer", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, customerCode, simpleCompanyName")
    public DataGrid<User> listAllCustomer() {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录");
        }

        return new DataGrid<>(true, userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.customer.name(), warehouseId));
    }

    @ApiOperation(value = "设置客户打印派送标签的权限")
    @RequestMapping(value = "/setPrintDeliveryLabelPermission", method = RequestMethod.POST)
    public DataGrid setPrintDeliveryLabelPermission(@RequestBody PrintDeliveryLabelDto printDeliveryLabelDto) {
        //@Author: hyl @Date: 2020-02-29 17:18 @Description: UserExtensionService 迁移到 CustomerInfoService
        int result = customerInfoService.updatePrintDeliveryLabelPermission(printDeliveryLabelDto.getCustomerId(), printDeliveryLabelDto.getPrintLabelPermission());
        return new DataGrid(result > 0, null);
    }

    @ApiOperation(value = "销售-客户列表", notes = "by Hukai")
    @RequestMapping(value = "/listSalesCustomers", method = RequestMethod.POST)
    public DataGrid<CustomerInfoResponseDto> listSalesCustomers(@RequestBody CustomerInfoRequestDto customerInfoRequestDto) {
        String organizationId = SessionUtils.getOrganizationId();
        if (StringUtils.isBlank(organizationId)) {
            throw new EshipRedirectException("请先登录");
        }

        customerInfoRequestDto.setOrganizationId(organizationId);
        customerInfoRequestDto.setWarehouseId(SessionUtils.getWarehouseId());
        customerInfoRequestDto.setSalesStaffId(SessionUtils.getUserId());

        return customerAssignBusiness.listCustomers(customerInfoRequestDto);
    }
}
