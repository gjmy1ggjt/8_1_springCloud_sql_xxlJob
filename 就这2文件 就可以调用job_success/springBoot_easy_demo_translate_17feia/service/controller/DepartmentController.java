package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.entity.sale.Department;
import com.yangshan.eship.sales.entity.sale.DepartmentNodeType;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Kee.Li
 * @date 2018/3/8 9:19
 */
@RestController
@RequestMapping(Version.VERSION + "/department")
@Api(value = "DepartmentController", tags = "组织架构管理")
public class DepartmentController {

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private OrgWarehouseServiceI warehouseService;


    /**
     * 根据组织id获取该组织的所有树节点
     *
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation(value = "根据组织id获取该组织的所有树节点", notes = "by 胡凯")
    public DataGrid<Department> listAllTree() {

        String organizationId = SessionUtils.getOrganizationId();

        //处理当前角色是运营总监，运营总监只看到自己分公司的数据
        String userId = SessionUtils.getUserId();

        Department department = departmentService.getAllDepartments(organizationId, userId);

        List<Department> departments = Lists.newArrayList();
        if (department != null) {
            departments.add(department);
        }

        DataGrid<Department> dataGrid = new DataGrid<>(true, departments);

        return dataGrid;
    }

    /**
     * 添加部门
     *
     * @param department
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    public DataGrid<Department> add(@RequestBody Department department) {

        department.setOrganizationId(SessionUtils.getOrganizationId());
        Department departmentDb = departmentService.addDepartment(department);

        return new DataGrid<>(true, departmentDb);
    }


    /**
     * 更新部门
     *
     * @param department
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public DataGrid<Department> update(@PathVariable("id") String id, @RequestBody Department department) {

        department.setOrganizationId(SessionUtils.getOrganizationId());
        department.setId(id);
        Department departmentDb = departmentService.updateDepartment(department);

        return new DataGrid<>(true, departmentDb);
    }

    /**
     * 删除节点
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.POST)
    public DataGrid<Department> deleteDepartment(@PathVariable("id") String id) {

        Department department = departmentService.deleteDepartment(id);

        return new DataGrid<>(true, department);
    }

    /**
     * 添加管理角色员工
     *
     * @param depId
     * @param staffId
     * @return
     */
    @RequestMapping(value = "/{depId}/{staffId}", method = RequestMethod.POST)
    public DataGrid<Department> addManagerRoleStaff(@PathVariable("depId") String depId, @PathVariable("staffId") String staffId) {

        Department department = new Department();
        department.setParentId(depId);
        department.setUserId(staffId);
        department.setOrganizationId(SessionUtils.getOrganizationId());
        department.setNodeType(DepartmentNodeType.STAFF_NODE);

        Department newDepartment = departmentService.addManageRoleUser(department);

        return new DataGrid<>(true, newDepartment);
    }

    /**
     * 添加普通角色员工
     *
     * @param managerStaffId
     * @param staffId
     * @return
     */
    @RequestMapping(value = "/managerStaff/{id}/{staffId}", method = RequestMethod.POST)
    public DataGrid<Department> addMemberRoleStaff(@PathVariable("id") String managerStaffId, @PathVariable("staffId") String staffId) {

        Department department = new Department();
        department.setParentId(managerStaffId);
        department.setUserId(staffId);
        department.setOrganizationId(SessionUtils.getOrganizationId());
        department.setNodeType(DepartmentNodeType.STAFF_NODE);

        Department newDepartment = departmentService.addMemberRoleUser(department);

        return new DataGrid<>(true, newDepartment);
    }

}
