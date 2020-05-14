package com.yangshan.eship.sales.business;

import com.google.common.collect.Lists;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.dto.account.TreeRoleInfoDto;
import com.yangshan.eship.author.dto.account.UserDto;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.Department;
import com.yangshan.eship.sales.entity.sale.DepartmentNodeType;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Kevin
 * @Date: 2019-07-27 14:37
 * @Description:
 */
@Service
public class DepartManagementBusiness {
    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private SalesStaffAssignServiceI salesStaffAssignService;

    public DataGrid<User> listRoleUsers(TreeRoleInfoDto treeRoleInfoDto) {
        Map<String, String> departUserIdMap = getStaffIdList(treeRoleInfoDto);

        Set<String> userIds = new HashSet<>();
        departUserIdMap.forEach((userId, departUserId) -> {
            userIds.add(userId);
        });

        List<String> staffIdList = new ArrayList<>(userIds);
        List<User> userList = new ArrayList<>();

        if (!staffIdList.isEmpty()) {
            userList = userService.getRoleStaffListByIds(staffIdList);
            for (User user : userList) {
                user.setRoleName(treeRoleInfoDto.getRoleName());
                user.setDepartmentId(departUserIdMap.get(user.getId()));
            }
        }

        return new DataGrid<>(true, userList);
    }

    public DataGrid saveUser(UserDto userDto) {
        String organizationId = SessionUtils.getOrganizationId();

        userDto.getOtherInfo().put("organizationId", organizationId);
        userDto.getOtherInfo().put("loginUserId",SessionUtils.getUserId());
        userDto.getOtherInfo().put("warehouseId",SessionUtils.getWarehouseId());
        userDto.setCreatedDate(new Date());
        userService.staffEdit(userDto);

        //修改组织关系中的姓名
        departmentService.updateDepartmentByUser(userDto);

        return new DataGrid(true, "员工信息" + (StringUtils.isBlank(userDto.getId()) ? "添加" : "修改") + "成功!");
    }

    public DataGrid distributeStaff(String userIds, TreeRoleInfoDto treeRoleInfoDto) {
        if (StringUtils.isBlank(userIds)) {
            throw new EshipRedirectException("还未勾选要分配的员工!");
        }

        if (treeRoleInfoDto == null) {
            throw new EshipRedirectException("还未选中要分配的角色!");
        }

        String[] userIdArr = StringUtils.split(userIds, ",");
        List<String> userIdList = Lists.newArrayList(userIdArr);

        //管理角色只允许分匹配一个员工
        if (treeRoleInfoDto.isManagerRoleUser() && userIdList.size() > 1) {
            throw new EshipRedirectException("管理角色只允许分匹配一个员工!");
        }

        //@Author: Hukai 2020-01-19 13:50
        //@Descreption: 如果同级非管理角色下没有员工, 需要先分配员工到非管理角色中(先只针对销售-->)
        //将员工A指定为销售经理，这时A只有销售经理Role， 不能发展客户；
        //只有将A(销售经理)节点下添加一个销售A, 自己分配给自己，A就有销售经理和销售两个Role, 才能发展客户；
        if (User.UserRoleCode.sale_manager.name().equals(treeRoleInfoDto.getRoleCode())) {
            String salesManagerId = treeRoleInfoDto.getUserIds();
            //销售经理名下的销售数量
            List<String> salesManagerIdList = Lists.newArrayList(StringUtils.split(salesManagerId, ','));
            List<SalesStaffAssign> salesList = salesStaffAssignService.findInSaleManagerIds(salesManagerIdList);
            if (salesList.isEmpty()) {
                throw new EshipRedirectException("该销售经理下暂无销售, 若要分配该员工到销售经理, 请先将其分到销售里面!");
            }
        }


        Map<String, User> userMap = userService.mapByIds(userIdList);

        //获取parentId对应的管理角色code和普通角色code
        Department parentDepart = departmentService.getManagerAndMemberRoleCode(treeRoleInfoDto.getParentId());

        if (treeRoleInfoDto.isManagerRoleUser()) {
            String userId = userIdList.get(0);

            //查看管理角色是否已经有一个人了
            Long mangerUserCount = departmentService.countByParentIdAndManagerRoleAndManagerRoleUser(treeRoleInfoDto.getParentId(), treeRoleInfoDto.getRoleCode(), true);
            if (mangerUserCount.intValue() > 0) {
                throw new EshipRedirectException("管理角色只允许分匹配一个员工!");
            }

            //直接添加一个管理角色
            Department department = new Department();
            department.setParentIdUrl(treeRoleInfoDto.getParentIdUrl());
            department.setParentId(treeRoleInfoDto.getParentId());
            department.setNodeType(DepartmentNodeType.STAFF_NODE);
            department.setOrganizationId(treeRoleInfoDto.getOrganizationId());
            department.setWarehouseId(treeRoleInfoDto.getWarehouseId());
            department.setManagerRole(treeRoleInfoDto.getRoleCode());
            department.setManagerRoleUser(true);
            department.setName(userMap.get(userId).getName());
            department.setUserId(userId);
            department.setMemberRole(parentDepart.getMemberRole());
            department.setInitData(false);

            Department departmentData = departmentService.save(department);

            //修改user_role表中的数据
            userRoleService.addUserRoleData(userId, treeRoleInfoDto);

            //将当前部门下的普通角色parentId更新为这个管理角色的id
            departmentService.updateParentIdAndParentIdUrl(treeRoleInfoDto.getParentIdUrl(), false, DepartmentNodeType.STAFF_NODE, departmentData.getId(), treeRoleInfoDto.getParentIdUrl() + "_" + departmentData.getId());
        } else {
            //先获取当前角色下普通角色员工列表, 避免重复添加
            Map<String, Object> existStaffMap = departmentService.findUserIdByRoleCode(userIdList, treeRoleInfoDto.getRoleCode());

            //添加多个普通角色
            for (String userId : userIdList) {
                //向Department表中加数据
                if (!existStaffMap.containsKey(userId)) {
                    this.saveMemberRoleUser(treeRoleInfoDto, userMap, parentDepart, userId);
                }

                //向userRole表中加数据
                userRoleService.addUserRoleData(userId, treeRoleInfoDto);
            }
        }

        return new DataGrid(true, "员工分配成功!");
    }

    //在角色节点下分配普通员工
    private void saveMemberRoleUser(TreeRoleInfoDto treeRoleInfoDto, Map<String, User> userMap, Department parentDepart, String userId) {
        Department department = new Department();
        department.setParentIdUrl(treeRoleInfoDto.getParentIdUrl());
        department.setParentId(treeRoleInfoDto.getParentId());
        department.setNodeType(DepartmentNodeType.STAFF_NODE);
        department.setOrganizationId(treeRoleInfoDto.getOrganizationId());
        department.setWarehouseId(treeRoleInfoDto.getWarehouseId());
        department.setManagerRole(parentDepart.getManagerRole());
        department.setManagerRoleUser(false);
        department.setName(userMap.get(userId).getName());
        department.setUserId(userId);
        department.setMemberRole(treeRoleInfoDto.getRoleCode());
        department.setInitData(false);

        departmentService.save(department);
    }

    public DataGrid deleteUser(String userId) {
        //初始化数据不允许删除
        List<Department> departments = departmentService.findByUserId(userId);
        for (Department department : departments) {
            if (department.isInitData()) {
                throw new EshipRedirectException("初始化数据不允许删除!");
            }
        }

        //判断组织架构树中是否存在用户
        List<Department> departmentList = departmentService.findByUserIdAndNodeTypeAndManagerRoleUser(userId, DepartmentNodeType.STAFF_NODE, true);
        if (!departmentList.isEmpty()) {
            List<String> parentIds = new ArrayList<>();
            for (Department department : departmentList) {
                parentIds.add(department.getId());
            }

            int total = departmentService.countByParentIdInAndNodeTypeAndManagerRoleUser(parentIds, DepartmentNodeType.STAFF_NODE, false);
            if (total > 0) {
                throw new EshipRedirectException("该员工所在部门下有普通角色员工数据, 不允许删除!");
            }
        }

        //员工下面有客户不允许删除
        List<String> userIds = new ArrayList<>();
        userIds.add(userId);
        List<CustomerAssign> customerAssignList = customerAssignService.findBySalesStaffIdInOrSalesManagerIdInOrCustomServiceStaffIdInOrCustomServiceManagerIdIn(userIds);
        if (!customerAssignList.isEmpty()) {
            throw new EshipRedirectException("该员工下有客户数据, 不允许删除!");
        }

        //删除动作
        userService.delete(userId);
        departmentService.deleteDepartmentByUser(userId);

        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        dataGrid.setMsg("删除成功!");

        return dataGrid;
    }

    public DataGrid<User> listManageUsers(TreeRoleInfoDto treeRoleInfoDto) {
        DataGrid<User> dataGrid = new DataGrid<>();
        Map<String, String> departUserIdMap = getStaffIdList(treeRoleInfoDto);

        Set<String> userIds = new HashSet<>();
        departUserIdMap.forEach((userId, departUserId) -> {
            userIds.add(userId);
        });

        List<User> users = userService.listByIds(new ArrayList<>(userIds));

        dataGrid.setFlag(true);
        dataGrid.setRows(users);

        return dataGrid;
    }

    private Map<String, String> getStaffIdList(TreeRoleInfoDto treeRoleInfoDto) {
        Map<String, String> departUserIdMap = new HashMap<>();

        //获取该角色节点下的所有userId
        List<Department> departmentUserList = departmentService.getRoleUserIdList(treeRoleInfoDto);

        if (departmentUserList.isEmpty()) {
            return departUserIdMap;
        }

        for (Department department : departmentUserList) {
            departUserIdMap.put(department.getUserId(), department.getId());
        }

        return departUserIdMap;
    }


    public DataGrid deleteUsers(List<String> userIds) {
        //初始化数据不允许删除
        List<Department> departments = departmentService.findByUserIdIn(userIds);

        if (!departments.isEmpty()) {
            throw new EshipRedirectException("您选择的员工数据不存在!");
        }

        for (Department department : departments) {
            if (department.isInitData()) {
                throw new EshipRedirectException("初始化数据不允许删除!");
            }
        }

        Map<String, Department> departmentMap = departments.stream().collect(Collectors.toMap(Department::getUserId, department -> department));

        //员工下面有客户不允许删除
        List<CustomerAssign> customerAssignList = customerAssignService.findBySalesStaffIdInOrSalesManagerIdInOrCustomServiceStaffIdInOrCustomServiceManagerIdIn(userIds);
        if (!customerAssignList.isEmpty()) {
            Set<String> staffIds = new HashSet<>();
            customerAssignList.stream().forEach(customerAssign -> {
                if (StringUtils.isNotBlank(customerAssign.getSalesStaffId())) {
                    staffIds.add(customerAssign.getSalesStaffId());
                }

                if (StringUtils.isNotBlank(customerAssign.getSalesManagerId())) {
                    staffIds.add(customerAssign.getSalesManagerId());
                }

                if (StringUtils.isNotBlank(customerAssign.getCustomServiceStaffId())) {
                    staffIds.add(customerAssign.getCustomServiceStaffId());
                }

                if (StringUtils.isNotBlank(customerAssign.getCustomServiceManagerId())) {
                    staffIds.add(customerAssign.getCustomServiceManagerId());
                }
            });

            List<String> staffNameList = new ArrayList<>();
            staffIds.forEach(staffId -> {
                if (departmentMap.containsKey(staffId)) {
                    staffNameList.add(departmentMap.get(staffId).getName());
                }
            });
            String staffNameStr = staffNameList.stream().collect(Collectors.joining(","));

            throw new EshipRedirectException("员工(名称为:" + staffNameStr + ")下有客户数据, 不允许删除!");
        }

        //删除动作
        userIds.stream().forEach(userId -> {
            userService.delete(userId);
            departmentService.deleteDepartmentByUser(userId);
        });

        return new DataGrid(true, "删除成功!");
    }
}
