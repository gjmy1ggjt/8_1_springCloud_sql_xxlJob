package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.yangshan.eship.author.dto.account.*;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.sales.business.DepartManagementBusiness;
import com.yangshan.eship.sales.entity.sale.Department;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: Kevin
 * @Date: 2019-07-26 9:53
 * @Description:
 */
@RestController
@RequestMapping(Version.VERSION + "/departManagement")
@Api(value = "DepartManagementController", tags = "组织架构管理-new")
public class DepartManagementController {

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private OrgWarehouseServiceI warehouseService;

    @Autowired
    private DepartManagementBusiness departManagementBusiness;

    @Autowired
    private RoleServiceI roleService;

    @Autowired
    private UserServiceI userService;

    @GetMapping
    @ApiOperation(value = "获取该组织的所有树节点", notes = "by 胡凯")
    public DataGrid<Department> listAllTree() {

        String organizationId = SessionUtils.getOrganizationId();

        //处理当前角色是运营总监，运营总监只看到自己分公司的数据
        String userId = SessionUtils.getUserId();

        Department department = departmentService.getDepartmentList(organizationId, userId);

        List<Department> departments = Lists.newArrayList();
        if (department != null) {
            departments.add(department);
        }

        DataGrid<Department> dataGrid = new DataGrid<>(true, departments);

        return dataGrid;
    }

    @PostMapping(value = "/users/list")
    @ApiOperation(value = "获取所有员工列表", notes = "by 胡凯")
    @JsonFilter(type = UserDto.class, include = "id, createdDate, loginId, name, nickName, qq, wechat, phone, email, roleName")
    public DataGrid<UserDto> userList(@RequestBody SearchUserDto searchUserDto) {
        DataGrid<UserDto> listResult = new DataGrid<UserDto>();

        UserDto userDto = new UserDto();
        userDto.setPagingDto(searchUserDto.getPagingDto());

        // 加入其它信息
        if (StringUtils.isNotBlank(searchUserDto.getRoleId())) {
            userDto.getOtherInfo().put("role", searchUserDto.getRoleId());
        }

        String organizationId = SessionUtils.getOrganizationId();

        userDto.getOtherInfo().put("organizationId", organizationId);

        userDto.setNickName(searchUserDto.getKeyword());
        userDto.setWarehouseId(SessionUtils.getWarehouseId());
        userDto.setId(SessionUtils.getUserId());

        listResult = userService.staffList(userDto);
        listResult.setFlag(true);

        return listResult;
    }

    @PostMapping(value = "/user/batchDelete")
    @ApiOperation(value = "批量删除员工", notes = "by 胡凯")
    public DataGrid batchDeleteUsers(@RequestBody List<String> userIds) {
        return departManagementBusiness.deleteUsers(userIds);
    }

    @PostMapping(value = "/users")
    @ApiOperation(value = "根据角色节点获取该角色的用户列表", notes = "by 胡凯")
    @JsonFilter(type = User.class, include = "id, createdDate, loginId, name, nickName, qq, wechat, phone, email, roleName, departmentId")
    public DataGrid<User> listRoleUsers(@RequestBody TreeRoleInfoDto treeRoleInfoDto) {

        return departManagementBusiness.listRoleUsers(treeRoleInfoDto);
    }

    @PostMapping(value = "/manageUsers")
    @ApiOperation(value = "根据角色节点获取该角色的员工", notes = "by 胡凯")
    @JsonFilter(type = User.class, include = "id, createdDate, loginId, name, nickName, qq, wechat, phone, email, roleName, departmentId")
    public DataGrid<User> manageUsers(@RequestBody TreeRoleInfoDto treeRoleInfoDto) {

        return departManagementBusiness.listManageUsers(treeRoleInfoDto);
    }

    @GetMapping(value = "/roles")
    @ApiOperation(value = "获取角色下拉框列表", notes = "by 胡凯")
    @JsonFilter(type = RoleDto.class, include = "id, name, code")
    public DataGrid<RoleDto> getRoleList() {
        RoleDto roleDto = new RoleDto();
        roleDto.setOrgId(SessionUtils.getOrganizationId());

        return roleService.roleList(roleDto);
    }

    @PostMapping
    @ApiOperation(value = "新增或修改部门信息", notes = "by 胡凯")
    public DataGrid addOrUpdate(@RequestBody DepartmentRequestDto departmentRequestDto) {
        return departmentService.addOrUpdate(departmentRequestDto);
    }

    @DeleteMapping(value = "/{departmentId}")
    @ApiOperation(value = "删除公司或部门", notes = "by 胡凯")
    public DataGrid delete(@PathVariable("departmentId") String departmentId) {
        Department department = departmentService.deleteDepartment(departmentId);
        return new DataGrid<>(true, department);
    }

    @PostMapping(value = "/user")
    @ApiOperation(value = "新增或修改员工", notes = "by 胡凯")
    public DataGrid saveUser(@RequestBody UserDto userDto) {
        return departManagementBusiness.saveUser(userDto);
    }

    @PostMapping(value = "/user/delete/{userId}")
    @ApiOperation(value = "删除员工", notes = "by 胡凯")
    public DataGrid deleteUsers(@PathVariable("userId") String userId) {
        return departManagementBusiness.deleteUser(userId);
    }

    @PostMapping(value = "/remove/{departmentId}")
    @ApiOperation(value = "从角色中批量移出员工", notes = "by 胡凯")
    public DataGrid removeUsers(@PathVariable("departmentId") String departmentId, @RequestBody TreeRoleInfoDto treeRoleInfoDto) {
        return departmentService.removeStaffFromRole(departmentId, treeRoleInfoDto.getUserIds(), treeRoleInfoDto);
    }

    @PostMapping(value = "/distributeStaff/{userIds}")
    @ApiOperation(value = "分配员工", notes = "by 胡凯")
    public DataGrid distributeStaff(@PathVariable("userIds") String userIds, @RequestBody TreeRoleInfoDto treeRoleInfoDto) {
        return departManagementBusiness.distributeStaff(userIds, treeRoleInfoDto);
    }
}
