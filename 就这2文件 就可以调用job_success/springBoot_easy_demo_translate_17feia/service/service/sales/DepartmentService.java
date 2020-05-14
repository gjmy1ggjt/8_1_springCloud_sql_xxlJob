package com.yangshan.eship.sales.service.sales;

import com.yangshan.eship.sales.dto.UnDistributeStaffsRequestDto;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.dto.account.DepartmentRequestDto;
import com.yangshan.eship.author.dto.account.TreeRoleInfoDto;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import com.yangshan.eship.exception.EshipRedirectException;
import org.springframework.stereotype.Service;
import com.google.common.collect.Lists;
import com.yangshan.eship.author.dto.account.UserDto;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.Department;
import com.yangshan.eship.sales.entity.sale.DepartmentNodeType;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.repository.cust.CustomerAssignDao;
import com.yangshan.eship.sales.repository.sale.DepartmentDao;
import com.yangshan.eship.sales.repository.sale.SalesStaffAssignDao;
import com.yangshan.eship.sales.repository.serv.CustomServiceAssignDao;
import com.yangshan.eship.sales.repository.ware.OrgWarehouseDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author Kee.Li
 * @date 2018/3/8 9:09
 */
@Service
@Transactional
public class DepartmentService implements DepartmentServiceI {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentService.class);

    @Autowired
    private DepartmentDao departmentDao;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private RoleServiceI roleService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private OrgWarehouseDao warehouseDao;

    @Autowired
    private SalesStaffAssignDao salesStaffAssignDao;

    @Autowired
    private CustomServiceAssignDao customServiceAssignDao;

    @Autowired
    private CustomerAssignDao customerAssignDao;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Override
    public Department addManageRoleUser(Department department) {


        if (StringUtils.isNotBlank(department.getParentId())) {

            //首先判断该员工是否已经加过了
            List<Department> departments = departmentDao.findByParentIdAndUserId(department.getParentId(), department.getUserId());
            if (departments != null && !departments.isEmpty()) {
                throw new EshipException("", "该节点已经添加过了！", true);
            }

            //设置parent_id_url
            //查询所在部门
            Department parentDep = departmentDao.findOne(department.getParentId());


            //判断当前用户是否已经分配了当前角色
            List<Department> memberRoles = departmentDao.findStaffByManagerRoleAndUserId(parentDep.getManagerRole(), department.getUserId());
            if (memberRoles != null && !memberRoles.isEmpty()) {
                throw new EshipException("", "当前员工已经分配过该角色了！", true);
            }

            department.setWarehouseId(parentDep.getWarehouseId());
            department.setParentIdUrl(parentDep.getParentIdUrl() + "_" + department.getParentId());
            department.setManagerRoleUser(true);
            department.setManagerRole(parentDep.getManagerRole());
            department.setMemberRole(parentDep.getMemberRole());

            User user = userService.findOne(department.getUserId());
            department.setName(user.getName());
            //1. 添加树节点
            Department managerRole = departmentDao.save(department);

            //2. 给用户分配角色
            String roleCode = parentDep.getManagerRole();
            addUserRole(department, parentDep, roleCode);

            //3. 重建关系（如果部门已经有游离的员工，加到新添加的管理角色下面）
            // 删除管理角色时，回会删除用户角色，这里重建没有意义
//            List<Department> dissociativeStaffs = departmentDao.getAllDissociativeStaffByDep(parentDep);
//            if(dissociativeStaffs != null){
//                dissociativeStaffs.stream().forEach(staff -> {
//                    staff.setUserParentId(managerRole.getCustomerId());
//                    staff.setParentId(managerRole.getId());
//                    staff.setParentIdUrl(managerRole.getParentIdUrl() + "_" + managerRole.getId());
//                    departmentDao.save(staff);
//                });
//            }

            //4. 同步到客服分配、销售分配关系表

            return managerRole;
        }

        return null;
    }

    @Override
    public Department addMemberRoleUser(Department department) {

        if (StringUtils.isNotBlank(department.getParentId())) {

            //首先判断该员工是否已经加过了
            List<Department> departments = departmentDao.findByParentIdAndUserId(department.getParentId(), department.getUserId());
            if (departments != null && !departments.isEmpty()) {
                throw new EshipException("", "该节点已经添加过了！", true);
            }

            //设置parent_id_url
            //查询直接管理节点
            Department parentDep = departmentDao.findOne(department.getParentId());

            if (StringUtils.isBlank(parentDep.getMemberRole())) {
                throw new EshipException("", "该部门尚未分配普通岗位角色！", true);
            }

            //判断当前用户是否已经分配了当前角色
            List<Department> memberRoles = departmentDao.findStaffByMemberRoleAndUserId(parentDep.getMemberRole(), department.getUserId());
            if (memberRoles != null && !memberRoles.isEmpty()) {
                throw new EshipException("", "当前员工已经分配过该角色了！", true);
            }

            department.setWarehouseId(parentDep.getWarehouseId());
            department.setParentIdUrl(parentDep.getParentIdUrl() + "_" + department.getParentId());
            department.setManagerRoleUser(false);
            department.setUserParentId(parentDep.getUserId());
            department.setManagerRole(parentDep.getManagerRole());
            department.setMemberRole(parentDep.getMemberRole());

            User user = userService.findOne(department.getUserId());
            department.setName(user.getName());
            //1. 添加树节点
            Department staff = departmentDao.save(department);

            //2. 给用户分配角色
            String roleCode = parentDep.getMemberRole();
            addUserRole(department, parentDep, roleCode);

            //3. 同步到已有的分配表（销售经理与销售、客服经理与客服）
            if (User.UserRoleCode.sale.name().equals(staff.getMemberRole())) {
                //销售与销售经理关系表
                SalesStaffAssign salesStaffAssign = new SalesStaffAssign();
                salesStaffAssign.setSalesManagerId(staff.getUserParentId());
                salesStaffAssign.setSalesStaffId(staff.getUserId());
                salesStaffAssign.setSalesManagerName(parentDep.getName());
                salesStaffAssign.setSalesStaffName(staff.getName());
                salesStaffAssign.setOrganizationId(parentDep.getOrganizationId());
                salesStaffAssign.setWarehouseId(parentDep.getWarehouseId());
                salesStaffAssignDao.save(salesStaffAssign);
            } else if (User.UserRoleCode.custom_service.name().equals(staff.getMemberRole())) {
                //客服与客服经理的关系表
                CustomServiceAssign customServiceAssign = new CustomServiceAssign();
                customServiceAssign.setCustomServiceManagerId(staff.getUserParentId());
                customServiceAssign.setCustomServiceStaffId(staff.getUserId());
                customServiceAssign.setCustomServiceManagerName(parentDep.getName());
                customServiceAssign.setCustomServiceStaffName(staff.getName());
                customServiceAssign.setOrganizationId(parentDep.getOrganizationId());
                customServiceAssign.setWarehouseId(parentDep.getWarehouseId());
                customServiceAssignDao.save(customServiceAssign);
            }

            return staff;
        }

        return null;
    }

    /**
     * 添加用户角色
     *
     * @param department
     * @param parentDep
     * @param roleCode
     */
    private void addUserRole(Department department, Department parentDep, String roleCode) {
        String warehouseId = null;
        String originId = null;
        if (StringUtils.isNotBlank(parentDep.getWarehouseId())) {
            OrgWarehouse warehouse = warehouseDao.findOne(parentDep.getWarehouseId());
            if (warehouse != null) {
                warehouseId = warehouse.getId();
                originId = warehouse.getOriginId();
            }
        }

        //添加角色
        userRoleService.addUserRole(department.getUserId(), roleCode, warehouseId, originId);

        //更新用户表的warehouseId
        userService.updateUserWarehouseId(department.getUserId(), warehouseId);
    }

    @Override
    public Department deleteDepartment(String id) {
        Department department = departmentDao.findOne(id);

        if (DepartmentNodeType.DEPARTMENT_NODE.name().equals(department.getNodeType().name()) && department.isInitData()) {
            throw new EshipRedirectException("初始化数据不允许删除!");
        }

        //判断当前节点下的员工是否分配了客户
        boolean hasCustomer = checkDepartmentCustomer(id);
        if (hasCustomer) {
            throw new EshipRedirectException("该" + department.getNodeType().getLabel() + "下有关联数据无法删除!");
        }

        switch (department.getNodeType()) {

            case COMPANY_NODE:
                deleteFiliale(department);
                break;
            case DEPARTMENT_NODE:
                //删除部门
                deleteDep(department);
                break;
            case STAFF_NODE:
                //删除员工
                deleteStaff(department);
                break;

            default:
                departmentDao.delete(id);
                break;
        }

        return department;
    }


    /**
     * 删除分公司
     *
     * @param department
     */
    private void deleteFiliale(Department department) {

        //1. 删除分公司的所有部门
        Department filiale = departmentDao.findOne(department.getId());
        if (filiale == null) {
            return;
        }

        List<Department> depList = departmentDao.findByWarehouseIdAndNodeType(filiale.getWarehouseId(), DepartmentNodeType.DEPARTMENT_NODE);
        if (depList != null && !depList.isEmpty()) {
            depList.stream().forEach(dep -> {
                deleteDep(dep);
            });
        }

        //2. 删除分公司
        departmentDao.delete(filiale);
    }


    /**
     * 删除部门
     *
     * @param department
     */
    private void deleteDep(Department department) {

        Department dep = departmentDao.findOne(department.getId());

        //1. 删除部门下面所有员工
        deleteStaffsByDep(dep);

        //2. 删除部门
        departmentDao.delete(dep);
    }

    /**
     * 删除部门下所有员工
     *
     * @param dep
     */
    private void deleteStaffsByDep(Department dep) {
        List<Department> staffs = departmentDao.getAllByParentIdUrl(dep.getParentIdUrl() + "_" + dep.getId());
        if (staffs != null && !staffs.isEmpty()) {
            staffs.stream().forEach(staff ->
                    deleteStaff(staff)
            );
        }
    }


    /**
     * 删除员工
     *
     * @param staff
     */
    private void deleteStaff(Department staff) {

        if (staff.isManagerRoleUser()) {
            //删除管理角色
            deleteManagerStaff(staff);
        } else {
            //删除普通角色
            deleteMemberStaff(staff);
        }

    }


    /**
     * 删除管理角色员工
     *
     * @param department
     */
    private void deleteManagerStaff(Department department) {

        //删除管理角色员工

        Department manager = departmentDao.findOne(department.getId());

        //1. 删除用户角色
        List<Department> departments = departmentDao.findStaffByManagerRoleAndUserId(manager.getManagerRole(), manager.getUserId());
        if (departments != null && departments.size() < 2) {
            //如果用户在其他部门还有同样的角色，则不删除角色
            userRoleService.removeUserRole(manager.getUserId(), manager.getManagerRole());
        }
        //2. 删除其下所有普通用户
        List<Department> staffs = departmentDao.findByParentId(manager.getId());
        if (staffs != null && !staffs.isEmpty()) {
            staffs.stream().forEach(staff ->
                    deleteMemberStaff(staff)
            );
        }

        //3. 删除树节点
        departmentDao.delete(manager.getId());

    }

    /**
     * 检查当前节点，以及以下节点的员工是否有关联客户
     *
     * @param departmentId
     * @return true:有客户，false:没有客户
     */
    private boolean checkDepartmentCustomer(String departmentId) {
        List<Department> staffs = departmentDao.findDirectlyStaffs("%" + departmentId + "%", departmentId);
        if (staffs == null || staffs.isEmpty()) {
            return false;
        }

//        List<String> staffIds = Lists.newArrayList();
//        staffs.stream().forEach(staff -> {
//            staffIds.add(staff.getCustomerId());
//        });
//
//        List<CustomerAssign> customerAssigns = customerAssignDao.findByStaffIds(staffIds);
//        if(customerAssigns != null && !customerAssigns.isEmpty()){
//            return true;
//        }

        for (Department staff : staffs) {
            String roleCode = null;
            if (staff.isManagerRoleUser()) {
                roleCode = staff.getManagerRole();
            } else {
                roleCode = staff.getMemberRole();
            }
            if (StringUtils.isNotBlank(roleCode)) {
                if (User.UserRoleCode.sale_manager.name().equals(roleCode)) {
                    //销售经理
                    List<String> smCustomerAssigns = customerAssignDao.findBySalesManagerId(staff.getUserId());
                    if (smCustomerAssigns != null && !smCustomerAssigns.isEmpty()) {
                        return true;
                    }
                } else if (User.UserRoleCode.sale.name().equals(roleCode)) {
                    //销售
                    List<CustomerAssign> customerAssigns = customerAssignDao.findBySalesStaffId(staff.getUserId());
                    if (customerAssigns != null && !customerAssigns.isEmpty()) {
                        return true;
                    }
                } else if (User.UserRoleCode.custom_service_manager.name().equals(roleCode)) {
                    //客服经理
                    List<CustomerAssign> customerAssigns = customerAssignDao.findByCustomServiceManagerId(staff.getUserId());
                    if (customerAssigns != null && !customerAssigns.isEmpty()) {
                        return true;
                    }
                } else if (User.UserRoleCode.custom_service.name().equals(roleCode)) {
                    //客服
                    List<CustomerAssign> customerAssigns = customerAssignDao.findByCustomServiceStaffId(staff.getUserId());
                    if (customerAssigns != null && !customerAssigns.isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * 删除普通角色员工
     *
     * @param department
     */
    private void deleteMemberStaff(Department department) {

        //删除普通角色

        Department staff = departmentDao.findOne(department.getId());

        if (staff == null) {
            return;
        }
        //1. 删除用户角色
        List<Department> departments = departmentDao.findStaffByMemberRoleAndUserId(staff.getMemberRole(), staff.getUserId());
        if (departments != null && departments.size() < 2) {
            //如果用户在其他部门还有同样的角色，则不删除角色
            userRoleService.removeUserRole(staff.getUserId(), staff.getMemberRole());
        }

        //2. 删除已有关系表数据
        if (User.UserRoleCode.sale.name().equals(staff.getMemberRole())) {
            //销售与销售经理关系表
            salesStaffAssignDao.deleteBySalesStaffId(staff.getUserId());

        } else if (User.UserRoleCode.custom_service.name().equals(staff.getMemberRole())) {
            //客服与客服经理的关系表
            customServiceAssignDao.deleteByCustomServiceStaffId(staff.getUserId());
        }

        //3. 删除树节点
        departmentDao.delete(staff.getId());

    }


    @Override
    public Department updateDepartment(Department department) {
        //1. 查询当前department是否存在
        if (StringUtils.isNotBlank(department.getId())) {
            Department oldDepartment = departmentDao.findOne(department.getId());

            String oldManagerRole = oldDepartment.getManagerRole();
            String oldMemberRole = oldDepartment.getMemberRole();

            if (!oldDepartment.getName().equals(department.getName())) {
                List<Department> departments = departmentDao.findChildren(oldDepartment.getParentId(), department.getName());
                if (departments != null && !departments.isEmpty()) {
                    throw new EshipException("", "该公司下已经存在名称为【" + department.getName() + "】的部门！", true, department.getName());
                }
            }

            oldDepartment.setName(department.getName());
            oldDepartment.setManagerRole(department.getManagerRole());
            oldDepartment.setMemberRole(department.getMemberRole());

            //2. 更新
            Department newDepartment = departmentDao.save(oldDepartment);

            //3. 如果角色有变化，需要删除此部门之前配置的员工
            if (!oldManagerRole.equals(department.getManagerRole())) {
                // 管理角色修改，删除部门所有员工
                deleteStaffsByDep(oldDepartment);

            } else if (!oldMemberRole.equals(department.getMemberRole())) {
                // 员工角色修改，删除部门普通员工
                List<Department> staffs = departmentDao.getAllByParentIdUrl(oldDepartment.getParentIdUrl() + "_" + oldDepartment.getId());
                if (staffs != null && !staffs.isEmpty()) {
                    staffs.stream().forEach(staff -> {
                        if (!staff.isManagerRoleUser()) {
                            deleteStaff(staff);
                        }
                    });
                }
                //修改管理角色的memberRole
                List<Department> managers = departmentDao.findByWarehouseIdAndManagerRoleUser(oldDepartment.getWarehouseId(), true);
                if (managers != null && !managers.isEmpty()) {
                    managers.stream().forEach(manager -> {
                        manager.setMemberRole(department.getMemberRole());
                        departmentDao.save(manager);
                    });
                }


            }
            return newDepartment;
        }
        return null;
    }

    @Override
    public Department addDepartment(Department department) {

        List<Department> departments = departmentDao.findChildren(department.getParentId(), department.getName());
        if (departments != null && !departments.isEmpty()) {
            throw new EshipException("", "该公司下已经存在名称为【" + department.getName() + "】的部门！", true, department.getName());
        }

        if (StringUtils.isNotBlank(department.getParentId())) {
            Department parentDep = departmentDao.findOne(department.getParentId());
            if (StringUtils.isNotBlank(parentDep.getParentIdUrl())) {
                department.setWarehouseId(parentDep.getWarehouseId());
                department.setParentIdUrl(parentDep.getParentIdUrl() + "_" + department.getParentId());
            } else if (ADMIN_PARENT_ID.equals(parentDep.getParentId())) {
                //直接往总公司添加部门
                department.setParentIdUrl(parentDep.getId());
            }
        }

        return departmentDao.save(department);
    }

    @Override
    public Department getAllDepartments(String organizationId, String userId) {
        //1. 获取所有的组织记录
        List<Department> departmentList = departmentDao.findDepartments(organizationId);

        // 2.在内存中构建树结构
        List<Department> departments = buildTree(departmentList);

        //运营总监特殊处理
        boolean existRole = userRoleService.checkUserRole(userId, User.UserRoleCode.coo);
        if (existRole) {
            //运营总监
            if (departments != null && !departments.isEmpty()) {
                List<Department> result = Lists.newArrayList();
                getUserDepartment(result, departments, userId);
                if (!result.isEmpty()) {
                    return result.get(0);
                }
            }
        } else {
            if (departments != null && !departments.isEmpty()) {
                return departments.get(0);
            }
        }
        return null;
    }

    @Override
    public Department getDepartmentList(String organizationId, String userId) {
        User currentUser = userService.findById(userId);

        Department department = new Department();

        List<Department> companyList = new ArrayList<>();

        /*----------------------运营总监start----------------------*/
        List<User> cooUserList = userRoleService.findUserByRoleCode(User.UserRoleCode.coo.name());
        Map<String, User> cooUserMap = cooUserList.stream().collect(Collectors.toMap(User::getId, user -> user));
        if (cooUserMap.containsKey(userId)) {
            //运营总监对应的department
            Department cooDepartment = departmentDao.findByWarehouseIdAndUserIdAndNodeType(currentUser.getWarehouseId(), userId, DepartmentNodeType.COMPANY_NODE);
            cooDepartment.setWarehouseName(orgWarehouseService.findOne(currentUser.getWarehouseId()).getName());
            companyList.add(cooDepartment);
            department = this.getCompanyDepartmentData(cooDepartment, organizationId, cooDepartment.getId());
        } else {
            department = this.getCompanyDepartmentData(department, organizationId, "root");

            //查询分公司
            companyList = departmentDao.findByOrganizationIdAndParentIdAndNodeType(organizationId, department.getId(), DepartmentNodeType.COMPANY_NODE);
        }
        /*----------------------运营总监end----------------------*/

        List<Department> departmentList = department.getChildren();
        for (Department company : companyList) {
            company = this.getCompanyDepartmentData(company, organizationId, company.getId());
            if (!(cooUserMap.containsKey(userId) && company.getWarehouseId().equals(currentUser.getWarehouseId()))) {
                departmentList.add(company);
            }
        }

        department.setChildren(departmentList);

        return department;
    }

    private Department getCompanyDepartmentData(Department department, String organizationId, String parentId) {
        List<Department> rootDepartmentList = departmentDao.findByOrganizationIdAndParentId(organizationId, parentId);
        if (rootDepartmentList.isEmpty()) {
            return department;
        }

        OrgWarehouse orgWarehouse = null;
        if ("root".equals(parentId)) {
            department = rootDepartmentList.get(0);
        } else {
            orgWarehouse = warehouseDao.findOne(department.getId());
            department.setChildren(rootDepartmentList);
        }

        //获取部门
        String rootDepartmentId = department.getId();
        List<Department> departmentList = departmentDao.findByOrganizationIdAndParentIdAndNodeType(organizationId, rootDepartmentId, DepartmentNodeType.DEPARTMENT_NODE);
        Set<String> roleCodes = new HashSet<>();

        Set<String> managerRoleCodeList = new HashSet<>();
        Set<String> memberRoleCodeList = new HashSet<>();

        List<String> departmentIds = new ArrayList<>();
        for (Department departmentData : departmentList) {
            departmentIds.add(departmentData.getId());

            if (StringUtils.isNotBlank(departmentData.getManagerRole())) {
                managerRoleCodeList.add(departmentData.getManagerRole());
                roleCodes.add(departmentData.getManagerRole());
            }

            if (StringUtils.isNotBlank(departmentData.getMemberRole())) {
                memberRoleCodeList.add(departmentData.getMemberRole());
                roleCodes.add(departmentData.getMemberRole());
            }
        }

        //部门下的角色
        List<String> roleCodeList = new ArrayList<>(roleCodes);
        List<Role> roleList = roleCodeList.isEmpty() ? new ArrayList<>() : roleService.getRoleNameByRoleCodes(roleCodeList);
        Map<String, String> roleNameMap = new HashMap<>();

        Map<String, Integer> memberRoleCountMap = new HashMap<>();

        for (Role role : roleList) {
            roleNameMap.put(role.getCode(), role.getName());
        }

        //获取所有管理角色的Id和parentId
        List<Department> managerStaffList = departmentDao.getInfoByOrganizationIdAndIsManagerRoleUserAndNodeType(organizationId, DepartmentNodeType.STAFF_NODE);
        Map<String, String> managerStaffIdMap = new HashMap<>();
        for (Department managerStaff : managerStaffList) {
            //部门id, 部门里面管理角色的id
            managerStaffIdMap.put(managerStaff.getUserId(), managerStaff.getName());
        }

        //部门下角色里面的员工数量(分公司下需要传入部门id)
        List<Department> managerRoleCountResult = managerRoleCodeList.isEmpty() ? new ArrayList<>() : departmentDao.findUserCountByRoleCodes(organizationId, departmentIds, new ArrayList<>(managerRoleCodeList), false);
        List<Department> memberRoleCountResult = memberRoleCodeList.isEmpty() ? new ArrayList<>() : departmentDao.findUserCountByRoleCodes(organizationId, departmentIds, new ArrayList<>(memberRoleCodeList), true);

        for (Department managerRoleDepartment : managerRoleCountResult) {
            memberRoleCountMap.put(managerRoleDepartment.getMemberRole() + "-" + managerRoleDepartment.getParentId(), managerRoleDepartment.getRoleUserCount().intValue());
        }

        //相同层级的数量累加
        Map<String, Integer> memberRoleCountMapTmp = new HashMap<>();
        for (Department memberRoleDepartment : memberRoleCountResult) {
            if (memberRoleCountMapTmp.containsKey(memberRoleDepartment.getMemberRole())) {
                memberRoleCountMapTmp.put(memberRoleDepartment.getMemberRole(), memberRoleCountMapTmp.get(memberRoleDepartment.getMemberRole()) + memberRoleDepartment.getRoleUserCount().intValue());
            } else {
                memberRoleCountMapTmp.put(memberRoleDepartment.getMemberRole(), memberRoleDepartment.getRoleUserCount().intValue());
            }
        }

        for (Department memberRoleDepartment : memberRoleCountResult) {
            memberRoleCountMap.put(memberRoleDepartment.getMemberRole() + "-" + memberRoleDepartment.getParentId(), memberRoleDepartment.getRoleUserCount().intValue());
            memberRoleCountMap.put(memberRoleDepartment.getMemberRole(), memberRoleCountMapTmp.get(memberRoleDepartment.getMemberRole()));
        }

        for (Department departmentData : departmentList) {
            List<TreeRoleInfoDto> treeRoleInfoDtoList = new ArrayList<>();

            if (StringUtils.isNotBlank(departmentData.getManagerRole())) {
                TreeRoleInfoDto treeManageRoleInfoDto = new TreeRoleInfoDto();
                treeManageRoleInfoDto.setMemberCount(0);
                treeManageRoleInfoDto.setRoleCode(departmentData.getManagerRole());
                treeManageRoleInfoDto.setRoleName(roleNameMap.get(departmentData.getManagerRole()));
                treeManageRoleInfoDto.setWarehouseId(departmentData.getWarehouseId());
                treeManageRoleInfoDto.setManagerRoleUser(true);
                treeManageRoleInfoDto.setParentId(departmentData.getId());
                treeManageRoleInfoDto.setParentIdUrl(departmentData.getParentIdUrl() + "_" + departmentData.getId());
                treeManageRoleInfoDto.setOrganizationId(departmentData.getOrganizationId());
                treeManageRoleInfoDto.setWarehouseOriginId(orgWarehouse != null ? orgWarehouse.getOriginId() : null);

                String key = departmentData.getManagerRole() + "-" + departmentData.getId();
                if (memberRoleCountMap.containsKey(key)) {
                    treeManageRoleInfoDto.setMemberCount(memberRoleCountMap.get(key));
                }

                treeRoleInfoDtoList.add(treeManageRoleInfoDto);
            }

            if (StringUtils.isNotBlank(departmentData.getMemberRole())) {
                String managerStaffId = managerStaffIdMap.get(departmentData.getId());

                TreeRoleInfoDto treeMemberRoleInfoDto = new TreeRoleInfoDto();
                treeMemberRoleInfoDto.setMemberCount(0);
                treeMemberRoleInfoDto.setRoleCode(departmentData.getMemberRole());
                treeMemberRoleInfoDto.setRoleName(roleNameMap.get(departmentData.getMemberRole()));
                treeMemberRoleInfoDto.setWarehouseId(departmentData.getWarehouseId());
                treeMemberRoleInfoDto.setManagerRoleUser(false);
                treeMemberRoleInfoDto.setOrganizationId(departmentData.getOrganizationId());

                //@Author: Hukai 2020-01-19 15:00
                //@Descreption: 防止产生null拼接在后面
                if (StringUtils.isNotBlank(managerStaffId)) {
                    treeMemberRoleInfoDto.setParentId(managerStaffId);
                    treeMemberRoleInfoDto.setParentIdUrl(departmentData.getParentIdUrl() + "_" + departmentData.getId() + "_" + managerStaffId);
                } else {
                    treeMemberRoleInfoDto.setParentId(departmentData.getId());
                    treeMemberRoleInfoDto.setParentIdUrl(departmentData.getParentIdUrl() + "_" + departmentData.getId());
                }

                String key = departmentData.getMemberRole() + "-" + departmentData.getId();
                if (memberRoleCountMap.containsKey(key)) {
                    treeMemberRoleInfoDto.setMemberCount(memberRoleCountMap.get(key));
                }

                if (memberRoleCountMap.containsKey(departmentData.getMemberRole())) {
                    treeMemberRoleInfoDto.setMemberCount(memberRoleCountMap.get(departmentData.getMemberRole()));
                }

                treeRoleInfoDtoList.add(treeMemberRoleInfoDto);
            }

            departmentData.setTreeRoleInfoDtos(treeRoleInfoDtoList);
        }

        department.setChildren(departmentList);

        return department;
    }

    /**
     * 获取用户的树
     * 注意：如果用户在树上存在多个节点，找到第一个就返回
     *
     * @param departments
     * @param userId
     * @return
     */
    private void getUserDepartment(List<Department> result, List<Department> departments, String userId) {

        for (Department department : departments) {
            if (DepartmentNodeType.COMPANY_NODE.equals(department.getNodeType()) && userId.equals(department.getUserId())) {
                result.add(department);
            } else {
                List<Department> children = department.getChildren();
                if (children != null && !children.isEmpty()) {
                    getUserDepartment(result, children, userId);
                }
            }
        }
    }


    /**
     * 两层循环实现建树
     *
     * @param departments 传入的树节点列表
     * @return
     */
    private List<Department> buildTree(List<Department> departments) {

        List<Department> trees = new ArrayList<>();

        for (Department department : departments) {

            //根节点
            if (ADMIN_PARENT_ID.equals(department.getParentId())) {
                trees.add(department);
            }

            for (Department dep : departments) {
                if (dep.getParentId().equals(department.getId())) {
                    if (department.getChildren() == null) {
                        department.setChildren(new ArrayList<>());
                    }
                    department.getChildren().add(dep);
                }
            }
        }
        return trees;
    }

    @Override
    public User getLeaderByStaff(String userId) {

        //1. 先查询该用户的部门节点，看是否有user_parent_id；如果有，则是普通员；如果没有，则是管理岗位员工；如果记录都查不到，则是运营总监

        // 查询当前用户在部门的节点
        List<Department> staffs = departmentDao.findByUserId(userId);
        if (staffs != null && !staffs.isEmpty()) {
            //获取最低职位的节点
            Department lowestStaff = getLowestStaff(staffs);

            logger.debug("## orgId:" + lowestStaff.getOrganizationId() + ",warehouseId:" + lowestStaff.getWarehouseId());

            if (DepartmentNodeType.STAFF_NODE.equals(lowestStaff.getNodeType()) && StringUtils.isNotBlank(lowestStaff.getUserParentId())) {
                //1. 如果是普通员工,leader是经理职位
                Department leader = departmentDao.findOne(lowestStaff.getParentId());
                logger.debug("## leader,userId:" + leader.getUserId());
                User user = userService.getUser(leader.getUserId(), leader.getManagerRole());
                return user;

            }

            if (DepartmentNodeType.COMPANY_NODE.equals(lowestStaff.getNodeType()) && StringUtils.isNotBlank(lowestStaff.getWarehouseId())
                    || DepartmentNodeType.STAFF_NODE.equals(lowestStaff.getNodeType()) && StringUtils.isBlank(lowestStaff.getWarehouseId())) {
                //2.1 如果是分公司经理,leader是总经理
                //2.2 如果是总公司下面的管理岗位，例如产品经理，leader是总经理
                List<Department> admins = departmentDao.findByOrganizationIdAndParentId(lowestStaff.getOrganizationId(), ADMIN_PARENT_ID);
                if (admins != null && !admins.isEmpty()) {
                    logger.debug("## admin,userId:" + admins.get(0).getUserId());
                    User user = userService.getUser(admins.get(0).getUserId(), User.UserRoleCode.admin.name());
                    return user;
                }
            }

            if (DepartmentNodeType.STAFF_NODE.equals(lowestStaff.getNodeType())) {
                //3. 如果是销售经理之类的，leader是运营总监
                //查询分公司
                List<Department> filiales = departmentDao.findByWarehouseIdAndNodeType(lowestStaff.getWarehouseId(),DepartmentNodeType.COMPANY_NODE);
                if (filiales != null && !filiales.isEmpty()) {
                    Department filiale = filiales.get(0);
                    logger.debug("## filiale,userId:" + filiale.getUserId());
                    User user = userService.getUser(filiale.getUserId(), User.UserRoleCode.coo.name());
                    return user;
                }
            }
        }
        return null;
    }

    /**
     * 如果根据用户id查询有多个几点，则获取最低职位的节点
     *
     * @param staffs
     * @return
     */
    private Department getLowestStaff(List<Department> staffs) {
        //找最低职位的节点
        Department lowestStaff = staffs.get(0);
        for (Department staff : staffs) {
            String userParentId = staff.getUserParentId();
            DepartmentNodeType currentNodeType = staff.getNodeType();
            if (DepartmentNodeType.STAFF_NODE.equals(currentNodeType) && StringUtils.isNotBlank(userParentId)) {
                //最低职位为普通员工
                lowestStaff = staff;
                break;
            }

            if (DepartmentNodeType.STAFF_NODE.equals(currentNodeType) && DepartmentNodeType.COMPANY_NODE.equals(lowestStaff.getNodeType())) {
                //如果当前节点比最低节点更低
                //最低职位为销售经理等
                lowestStaff = staff;
            }
        }
        return lowestStaff;
    }

    @Override
    public void updateDepartmentByUser(UserDto userDto) {

        if (StringUtils.isNotBlank(userDto.getId())) {
            User user = userService.findOne(userDto.getId());
            if (user != null && StringUtils.isNotBlank(userDto.getName())) {
                departmentDao.updateDepartmentByUser(user.getId(), userDto.getName());
            }
        }

    }

    @Override
    public void deleteDepartmentByUser(String userId) {

        List<Department> departments = departmentDao.findByUserId(userId);
        if (departments == null || departments.isEmpty()) {
            return;
        } else {
            throw new EshipRedirectException("", "当前员工已分配角色，请先在组织架构中删除！", true);
        }

//        //1.是否存在公司节点
//        boolean existCompanyNode = false;
//        for (Department department : departments) {
//            if (DepartmentNodeType.COMPANY_NODE.equals(department.getNodeType())) {
//                existCompanyNode = true;
//                break;
//            }
//        }
//
//        //2. 通知前端存在公司节点
//        // 不会全部删除，因为删除员工的时候删除所有组织结构不太合理
//        if (existCompanyNode) {
//            throw new EshipException("", "当前用户是分公司运营总监，不能删除；如需删除，请先解除分公司关联关系！", true);
//        }
//
//        //3.删除员工节点
//        for (Department department : departments) {
//            deleteStaff(department);
//        }

    }

    /**
     * @Author: LJ
     * @Date: 2018/4/20 15:14
     * @Description: 找总公司直属部门的全部角色
     */
    public List<String> findAllRolesDirectlyUnderCompany(String orgId) {
        List<String> allRoles = Lists.newArrayList();

        List<Object[]> deps = departmentDao.findAllDirectlyUnderCompany(orgId);

        deps.forEach(dep -> {
            allRoles.add((String) dep[0]);
            allRoles.add((String) dep[1]);
        });

        return allRoles;
    }

    @Override
    public List<String> findByUserRoleCode(List<Role> allRolesByStaffId, String staffId, String organizationId, String warehouseId) {
        //能够查询总公司所有客户的角色
        String[] orgRoles = {User.UserRoleCode.president.name(), User.UserRoleCode.product_manager.name(), User.UserRoleCode.bloc_finance_manager.name()};
        //能够查询分公司客户的角色
        String[] filialeRoles = {User.UserRoleCode.coo.name(), User.UserRoleCode.finance_manager.name(), User.UserRoleCode.finance.name()};
        Department department = new Department();
        for (Role role : allRolesByStaffId) {
            if (User.UserRoleCode.sale_manager.name().equals(role.getCode())) {
                //销售经理
                department.setUserId(staffId);
            }
            if (Lists.newArrayList(orgRoles).contains(role.getCode())) {
                //总公司所有客户
                department.setOrganizationId(organizationId);
                //兼容总公司角色只查询某分公司的情况，例如：orgid=null, warehouseId=1
                department.setWarehouseId(warehouseId);
            }
            if (Lists.newArrayList(filialeRoles).contains(role.getCode())) {
                //分公司所有客户
                department.setWarehouseId(warehouseId);
            }
        }

        List<String> saleIds = new ArrayList<>();
        DataGrid<Department> departmentDataGrid = departmentDao.listByOr(department);
        for (Department dep : departmentDataGrid.getRows()) {
            if(dep.getUserId() != null) {
                saleIds.add(dep.getUserId());
            }
        }
        return saleIds;
    }

    @Override
    public List<Department> getRoleUserIdList(TreeRoleInfoDto treeRoleInfoDto) {
        return departmentDao.getRoleUserIdList(treeRoleInfoDto);
    }

    @Override
    public DataGrid addOrUpdate(DepartmentRequestDto departmentRequestDto) {
        Department department = null;
        if (StringUtils.isNotBlank(departmentRequestDto.getId())) {
            //修改部门信息
            department = departmentDao.findOne(departmentRequestDto.getId());
            if (department == null) {
                throw new EshipRedirectException("部门不存在");
            }
        } else {
            department = new Department();
            department.setNodeType(DepartmentNodeType.DEPARTMENT_NODE);
            department.setParentId(departmentRequestDto.getParentId());

            //根据parentId找出organizationId
            String organizationId = departmentDao.getOrganizationId(departmentRequestDto.getParentId());
            department.setOrganizationId(organizationId);

            String parentIdUrl = null;
            if (StringUtils.isNotBlank(departmentRequestDto.getWarehouseId())) {
                parentIdUrl = departmentRequestDto.getWarehouseId() + "_" + departmentRequestDto.getParentId();
            } else {
                parentIdUrl = departmentRequestDto.getParentId();
            }
            department.setParentIdUrl(parentIdUrl);
        }

        department.setName(departmentRequestDto.getName());
        department.setManagerRole(departmentRequestDto.getManagerRole());
        department.setMemberRole(departmentRequestDto.getMemberRole());
        department.setWarehouseId(departmentRequestDto.getWarehouseId());

        departmentDao.save(department);

        return new DataGrid(true, "部门信息" + (StringUtils.isNotBlank(departmentRequestDto.getId()) ? "修改" : "添加") + "成功!");
    }

    @Override
    public List<User> getNamesByUserIds(List<String> userIdList) {
        userIdList = QueryPlanCacheWrapper.wrapper(userIdList,"DepartmentService.getNamesByUserIds.userIdList");
        return departmentDao.getNamesByUserIds(userIdList, DepartmentNodeType.STAFF_NODE);
    }

    @Override
    public DataGrid removeStaffFromRole(String departmentId, String userIds, TreeRoleInfoDto treeRoleInfoDto) {
        if (StringUtils.isBlank(userIds)) {
            throw new EshipRedirectException("用户ID不能为空!");
        }

        if (treeRoleInfoDto == null || StringUtils.isBlank(treeRoleInfoDto.getRoleCode())) {
            throw new EshipRedirectException("参数不全!");
        }

        //针对移出管理角色==>如果有普通角色 不能直接移出
        if (treeRoleInfoDto.isManagerRoleUser()) {
            Long memberUserCount = departmentDao.countByParentIdAndNodeTypeAndManagerRoleUser(departmentId, DepartmentNodeType.STAFF_NODE, false);
            if (memberUserCount.intValue() > 0) {
                throw new EshipRedirectException("当前部门下还有普通角色员工数据, 不能直接移除!");
            }
        }

        List<String> userIdList = Lists.newArrayList(StringUtils.split(userIds, ','));

        //从Department表中删除
        departmentDao.removeStaffFromRole(userIdList, treeRoleInfoDto);

        //从UserRole表中删除
        userRoleService.removeUserRoles(userIdList, treeRoleInfoDto.getRoleCode());

        return new DataGrid(true, "成功将员工从" + treeRoleInfoDto.getRoleName() + "中移除!");
    }

    @Override
    public Department getManagerAndMemberRoleCode(String parentId) {
        return departmentDao.getManagerAndMemberRoleCode(parentId);
    }

    @Override
    public Department save(Department department) {
        return departmentDao.save(department);
    }

    @Override
    public Map<String, Object> findUserIdByRoleCode(List<String> userIdList, String roleCode) {
        boolean isManagerRoleUser = false;
        DepartmentNodeType nodeType = DepartmentNodeType.STAFF_NODE;
        userIdList = QueryPlanCacheWrapper.wrapper(userIdList,"DepartmentService.findUserIdByRoleCode.userIdList");
        List<Department> departmentList = departmentDao.getUserIdByRoleCode(userIdList, isManagerRoleUser, roleCode, nodeType);

        if (departmentList.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> resultMap = new HashMap<>();
        for (Department department : departmentList) {
            resultMap.put(department.getUserId(), department.getName());
        }

        return resultMap;
    }

    @Override
    public Long countByParentIdAndManagerRoleAndManagerRoleUser(String parentId, String roleCode, boolean isManagerRoleUser) {
        return departmentDao.countByParentIdAndManagerRoleAndManagerRoleUser(parentId, roleCode, isManagerRoleUser);
    }

    @Override
    public List<Department> findByUserIdAndNodeTypeAndManagerRoleUser(String userId, DepartmentNodeType nodeType, boolean isManagerRoleUser) {
        return departmentDao.findByUserIdAndNodeTypeAndManagerRoleUser(userId, nodeType, isManagerRoleUser);
    }

    @Override
    public List<Department> findByParentIdInAndNodeTypeAndManagerRoleUser(List<String> userIds, DepartmentNodeType nodeType, boolean isManagerRoleUser) {
        return departmentDao.findByParentIdInAndNodeTypeAndManagerRoleUser(userIds, nodeType, isManagerRoleUser);
    }

    @Override
    public void updateParentIdAndParentIdUrl(String oldParentIdUrl, boolean isManagerRoleUser, DepartmentNodeType nodeType, String parentId, String parentIdUrl) {
        departmentDao.updateParentIdAndParentIdUrl("%" + oldParentIdUrl + "%", isManagerRoleUser, nodeType, parentId, parentIdUrl);
    }

    @Override
    public int countByParentIdInAndNodeTypeAndManagerRoleUser(List<String> parentIds, DepartmentNodeType nodeType, boolean isManagerRoleUser) {
        Long total = departmentDao.countByParentIdInAndNodeTypeAndManagerRoleUser(parentIds, nodeType, isManagerRoleUser);
        return total.intValue();
    }

    @Override
    public List<Department> findByUserId(String userId) {
        return departmentDao.findByUserId(userId);
    }

    @Override
    public DataGrid<Department> getUnDistributeStaffs(UnDistributeStaffsRequestDto distributeStaffsRequestDto) {
        return departmentDao.getUnDistributeStaffs(distributeStaffsRequestDto);
    }

    @Override
    public List<Department> findByUserIdIn(List<String> userIds) {
        return departmentDao.findByUserIdIn(userIds);
    }
}