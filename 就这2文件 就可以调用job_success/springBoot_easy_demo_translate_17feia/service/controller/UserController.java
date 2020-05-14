package com.yangshan.eship.sales.controller;

import com.google.common.collect.Lists;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.OrganizationServiceI;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.sales.business.CustomerAssignBusiness;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.cust.DataPermissions;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.cust.DataPermissionsServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController("UserControllerInSalesLib")
@RequestMapping(Version.VERSION + "/user")
@Api(value = "UserController", tags = "账号相关接口")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Autowired
    private UserServiceI userService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private DataPermissionsServiceI dataPermissionsService;

    @Autowired
    private OrganizationServiceI organizationService;

    @Autowired
    private CustomerAssignBusiness customerAssignBusiness;

    @RequestMapping(method = RequestMethod.GET)
    public DataGrid<User> list(String orgId, String roleCode) {

        orgId = SessionUtils.getOrganizationId();
        String warehouseId = SessionUtils.getWarehouseId();
        String userId = SessionUtils.getUserId();

        List<User> users = userService.listByRoleCode(orgId, warehouseId, roleCode, userId);

        DataGrid<User> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        dataGrid.setTotal(users == null ? 0 : users.size());
        dataGrid.setRows(users);

        return dataGrid;
    }

    /**
     * @Description: 获取客户列表
     * @param:
     * @return:
     * @Date: 11:40 2017/12/1
     */
    @RequestMapping(value = "/customers", method = RequestMethod.GET)
    @ApiOperation(value = "获取客户列表", notes = "by hyl", httpMethod = "GET")
    public DataGrid<User> findCustomerByOrganizationId() {
        List<User> users = userService.findCustomerByOrganizationId(SessionUtils.getOrganizationId());
        DataGrid<User> dataGrid = new DataGrid<>();
        dataGrid.setRows(users);
        return dataGrid;
    }


    /**
     * @Description: 获取列表
     * @param:
     * @return:
     * @Date: 11:40 2017/12/1
     */
    @RequestMapping(value = "/staffs", method = RequestMethod.GET)
    @ApiOperation(value = "所有员工列表", notes = "by hyl", httpMethod = "GET")
    public DataGrid<User> findStaffsByOrganizationId() {
        List<User> users = userService.findStaffsByOrganizationId(SessionUtils.getOrganizationId());
        DataGrid<User> dataGrid = new DataGrid<>();
        dataGrid.setRows(users);
        return dataGrid;
    }

    /**
     * 根据销售查询客户
     *
     * @param
     * @author Kee.Li
     * @date 2017/12/29 11:06
     */
    @RequestMapping(value = "/findBySaleStaff", method = RequestMethod.GET)
    @ApiOperation(value = "根据销售查询客户", notes = "by hyl", httpMethod = "GET")
    public DataGrid<User> findByOrganizationId() {

        CustomerAssign assign = new CustomerAssign();
        assign.setSalesStaffId(SessionUtils.getUserId());

        return customerAssignService.findByCustomerAssign(assign);
    }

    /**
     * 根据客服查询客户
     *
     * @param
     * @author linYun
     * @date 2017/12/29 14:06
     */
    @JsonFilter(type = User.class, include = "id,customerCode,nickName,simpleCompanyName")
    @RequestMapping(value = "/findByCustomerServiceStaffId", method = RequestMethod.GET)
    @ApiOperation(value = "根据客服查询客户", notes = "by hyl", httpMethod = "GET")
    public DataGrid<User> findByCustomerServiceStaffId(String customerServiceStaffId) {
//        String userId = SessionUtils.getCustomerId();
        CustomerAssign assign = new CustomerAssign();
        assign.setCustomServiceStaffId(customerServiceStaffId);

        return customerAssignService.findByCustomerAssign(assign);
    }

    /**
     * @author LinYun
     * @date 17:42 2018/4/3
     * @description 根据登录账号查询可查看的客服账号
     * roleCode:custom_service, sale, finance
     */
    @JsonFilter(type = User.class, include = "id,name")
    @RequestMapping(value = "/getCustomerServiceList/{roleCode}", method = RequestMethod.GET)
    @ApiOperation(value = "根据登录账号查询可查看的客服账号", notes = "by hyl", httpMethod = "GET")
    public DataGrid<User> getCustomerServiceList(@PathVariable("roleCode") String roleCode) {
        DataGrid<User> dataGrid = new DataGrid<>();
        String userId = SessionUtils.getUserId();
        List<String> userIdList = Lists.newArrayList();
        userIdList.add(userId);
        // 查询当前账号的客服数据权限
        List<DataPermissions> dataPermissionsList = dataPermissionsService.findAllByUserIdAndTargetRoleCode(userId, roleCode);

        if (dataPermissionsList != null && dataPermissionsList.size() > 0) {
            for (DataPermissions dataPermissions : dataPermissionsList) {
                if (!SessionUtils.getUserId().equals(dataPermissions.getTargetUserId())) {
                    userIdList.add(dataPermissions.getTargetUserId());
                }
            }
        }
        dataGrid.setCode(userId);
        dataGrid.setFlag(true);

        User currentUser = userService.findOne(userId);
        List<User> users = new ArrayList<>();
        users.add(currentUser);

        List<User> userList = userService.listByIds(userIdList);
        for (User user : userList) {
            if (!user.getId().equals(userId)) {
                users.add(user);
            }
        }

        dataGrid.setRows(users);
        return dataGrid;
    }

    /**
     * @author LinYun
     * @date 14:47 2018/4/4
     * @description 获取分公司名下所有客户
     */
    @JsonFilter(type = User.class, include = "id,customerCode,nickName,simpleCompanyName")
    @RequestMapping(value = "/getWarehouseCustomerAll", method = RequestMethod.GET)
    @ApiOperation(value = "获取分公司名下所有客户", notes = "by hyl", httpMethod = "GET")
    public DataGrid<User> getWarehouseCustomerAll() {
        DataGrid<User> dataGrid = new DataGrid<>();
        List<User> customerList = userRoleService.findWarehouseUserByRoleCode(SessionUtils.getWarehouseId(), User.UserRoleCode.customer.name());
        if (customerList != null && customerList.size() > 0) {
            dataGrid.setRows(customerList);
            dataGrid.setTotal(customerList.size());
            dataGrid.setFlag(true);
        } else {
            dataGrid.setFlag(false);
            dataGrid.setMsg("客户信息获取失败");
        }
        return dataGrid;
    }
}
