package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson.JSONObject;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.finance.entity.fina.StaffCommission;
import com.yangshan.eship.finance.service.fina.StaffCommissionServiceI;
import jodd.util.StringUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author:
 * @Description:LiuWei
 * @Date: 上午 10:15 2018/4/17
 */
@RestController("StaffCommissionControllerInSalesLib")
@RequestMapping(Version.VERSION + "/commission")
public class StaffCommissionController {

    @Autowired
    StaffCommissionServiceI staffCommissionService;

    @Autowired
    UserRoleServiceI userRoleService;

    @Autowired
    RoleServiceI roleService;


    /**
     * @Author: LiuWei
     * @Description: 员工提成查询
     * @Param:
     * @Date: 下午 7:42 2018/4/17
     */
    @RequestMapping(value = "/staffCommissions", method = RequestMethod.POST)
    public DataGrid<StaffCommission> staffCommissions(@RequestBody String params) {
        JSONObject jsonObject = JSONObject.parseObject(params);
        StaffCommission staffCommission = jsonObject.toJavaObject(StaffCommission.class);
        staffCommission.setUserId(SessionUtils.getUserId());
        staffCommission.setOrganizationId(SessionUtils.getOrganizationId());
        DataGrid<StaffCommission> dataGrid = staffCommissionService.search(staffCommission);
        dataGrid.setFlag(true);
        return dataGrid;
    }


    /**
     * @Author: LiuWei
     * @Description: 操作，销售，销售经理角色
     * @Param:
     * @Date: 下午 7:42 2018/4/17
     */
    @RequestMapping(value = "/roleList", method = RequestMethod.GET)
    public DataGrid<Role> roleList() {
        DataGrid<Role> dataGrid = new DataGrid();
        List<String> roleCodes = new ArrayList<>();
        roleCodes.add(User.UserRoleCode.operator.name());
        roleCodes.add(User.UserRoleCode.sale.name());
        roleCodes.add(User.UserRoleCode.sale_manager.name());
        dataGrid.setRows(roleService.findByRoleCode(roleCodes, SessionUtils.getOrganizationId()));

        return dataGrid;
    }


    /**
     * @Author: LiuWei
     * @Description:
     * @Param:
     * @Date: 下午 7:42 2018/4/17
     */
    @RequestMapping(value = "/staffList", method = RequestMethod.GET)
    public DataGrid<User> staffList(String roleCode, String warehouseId) {
        DataGrid<User> dataGrid = new DataGrid();
        List<String> roleCodes = new ArrayList<>();
        if (StringUtils.isEmpty(roleCode)) {
            roleCodes.add(User.UserRoleCode.operator.name());
            roleCodes.add(User.UserRoleCode.sale.name());
            roleCodes.add(User.UserRoleCode.sale_manager.name());
        } else if (roleCode.equals(User.UserRoleCode.operator.name())) {
            roleCodes.add(User.UserRoleCode.operator.name());
        } else if (roleCode.equals(User.UserRoleCode.sale.name())) {
            roleCodes.add(User.UserRoleCode.sale.name());
        } else if (roleCode.equals(User.UserRoleCode.sale_manager.name())) {
            roleCodes.add(User.UserRoleCode.sale_manager.name());
        }
        warehouseId = StringUtil.isBlank(warehouseId) ? SessionUtils.getWarehouseId() : warehouseId;
        dataGrid.setRows(userRoleService.findCustomerByRoleCodeAndOrganizationId(roleCodes, SessionUtils.getOrganizationId(), warehouseId));
        dataGrid.setFlag(true);
        return dataGrid;
    }


}
