package com.yangshan.eship.sales.service.ware;

import com.google.common.collect.Maps;
import com.yangshan.eship.author.dto.account.ExtraInfoObj;
import com.yangshan.eship.author.dto.account.UserEditDto;
import com.yangshan.eship.author.entity.account.Organization;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.account.OrganizationServiceI;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.jpa.PagingUtils;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.EshipBeanUtils;
import com.yangshan.eship.common.utils.JsonUtils;
import com.yangshan.eship.common.utils.SeachPageUtil;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.sales.dto.OrgWarehouseDto;
import com.yangshan.eship.sales.entity.sale.Department;
import com.yangshan.eship.sales.entity.sale.DepartmentNodeType;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.repository.sale.DepartmentDao;
import com.yangshan.eship.sales.repository.sale.SalesStaffAssignDao;
import com.yangshan.eship.sales.repository.serv.CustomServiceAssignDao;
import com.yangshan.eship.sales.repository.ware.OrgWarehouseDao;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仓库管理
 *
 * @author: Kee.Li
 * @date: 2017/10/19 16:15
 */
@Service
@Transactional
public class OrgWarehouseService implements OrgWarehouseServiceI {

    @Autowired
    private OrgWarehouseDao orgWarehouseDao;

    @Autowired
    private CustomServiceAssignDao customServiceAssignDao;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private RegionServiceI regionService;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private DepartmentDao departmentDao;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private OrganizationServiceI organizationService;

    @Autowired
    private RoleServiceI roleService;

    @Autowired
    private SalesStaffAssignDao salesStaffAssignDao;


    @Override
    public List<OrgWarehouse> findInfoByIdIn(List<String> warehouseIds) {
        return SeachPageUtil.invokeMethod(new ArrayList<OrgWarehouse>(), orgWarehouseDao, "findInfoByIdIn", warehouseIds);
    }

    /**
     * 根据组织id获取出发地
     *
     * @param organizationId
     * @return java.util.List<com.yangshan.eship.sales.dto.ware.OrgWarehouse>
     * @author: Kee.Li
     * @date: 2017/10/19 16:15
     */
    @Override
    public List<OrgWarehouse> findOriginByOrgId(String organizationId) {
        List<OrgWarehouse> orgWarehouses = orgWarehouseDao.findOriginByOrgId(organizationId);

        Map<String, String> originNoNameMapping = Maps.newHashMap();

        ListIterator<OrgWarehouse> orgWarehouseListIterator = orgWarehouses.listIterator();
        while (orgWarehouseListIterator.hasNext()) {
            OrgWarehouse orgWarehouse = orgWarehouseListIterator.next();
            if (originNoNameMapping.containsKey(orgWarehouse.getOriginNo())) {
                orgWarehouseListIterator.remove();
            } else {
                originNoNameMapping.put(orgWarehouse.getOriginNo(), orgWarehouse.getOriginName());
            }
        }
        return orgWarehouses;
    }

    @Override
    public DataGrid<OrgWarehouse> list(String organizationId, PagingDto pagingDto) {
        Map<String, Object> parmasMap = new HashMap<>();
        parmasMap.put("organizationId", organizationId);
        return PagingUtils.exeHql(entityManager, "from OrgWarehouse z where z.organizationId=:organizationId order by z.createdDate desc",
                parmasMap, pagingDto, "z");
    }

    @Override
    public void update(OrgWarehouse orgWarehouse) {

        OrgWarehouse orgWarehouseDb = orgWarehouseDao.findOne(orgWarehouse.getId());

        String oldCooId = orgWarehouseDb.getCooId();
        String newCooId = orgWarehouse.getCooId();
        if (StringUtils.isNotBlank(oldCooId) && !oldCooId.equals(newCooId)) {

            //校验新的coo是否已经分配了分公司
            List<OrgWarehouse> cooWarehouses = orgWarehouseDao.findByCooId(orgWarehouse.getCooId());
            if (cooWarehouses != null && !cooWarehouses.isEmpty()) {
                throw new EshipException("SALES_WAREHOUSE_ASSIGN_COO_EXIST", "员工[{0}]不能分配给多个分公司！", true, orgWarehouse.getCooName());
            }

            //如果修改了运营总监，删除旧的运营总监角色
            removeCooRole(oldCooId);
        }

        Region origin = regionService.findOrigin(orgWarehouse.getOriginNo());
        if (origin != null) {
            orgWarehouseDb.setOriginName(origin.getRegionName());
        }

        //更新分公司数据
        orgWarehouseDb.setVersion(orgWarehouse.getVersion());
        orgWarehouseDb.setName(orgWarehouse.getName());
        orgWarehouseDb.setOriginId(orgWarehouse.getOriginId());
        orgWarehouseDb.setOriginNo(orgWarehouse.getOriginNo());
//        orgWarehouseDb.setOriginName(orgWarehouse.getOriginName());
        orgWarehouseDb.setAddress(orgWarehouse.getAddress());
        orgWarehouseDb.setCooId(orgWarehouse.getCooId());
        orgWarehouseDb.setCooName(orgWarehouse.getCooName());

        //添加运营总监角色
        addCooRole(newCooId, orgWarehouseDb);

        //修改树
        List<Department> departments = departmentDao.findByOrganizationIdAndWarehouseId(orgWarehouseDb.getOrganizationId(), orgWarehouseDb.getId());
        if (departments == null || departments.isEmpty()) {
            //该分公司没有树结构，添加
            List<Department> parentCompanies = departmentDao.findByOrganizationIdAndWarehouseId(orgWarehouseDb.getOrganizationId(), null);
            if (parentCompanies != null && !parentCompanies.isEmpty()) {
                //存在总公司树节点
                Department parentCompany = parentCompanies.get(0);
                addFiliale(orgWarehouseDb, parentCompany);
            } else {
                //不存在总公司树节点
                Department parentCompany = getParentCompany(orgWarehouseDb);
                addFiliale(orgWarehouseDb, parentCompany);
            }
        } else {
            //该公司有树结构，修改
            Department filiale = departments.get(0);
            filiale.setName(orgWarehouseDb.getName() + "(" + orgWarehouseDb.getCooName() + ")");
            filiale.setUserId(orgWarehouseDb.getCooId());
            departmentDao.save(filiale);
        }


    }

    @Override
    public void save(OrgWarehouse orgWarehouse) {

        List<OrgWarehouse> cooWarehouses = orgWarehouseDao.findByCooId(orgWarehouse.getCooId());
        if (cooWarehouses != null && !cooWarehouses.isEmpty()) {
            throw new EshipException("SALES_WAREHOUSE_ASSIGN_COO_EXIST", "员工[{0}]不能分配给多个分公司！", true, orgWarehouse.getCooName());
        }

        OrgWarehouse warehouse = orgWarehouseDao.save(orgWarehouse);
        //添加运营总监角色
        addCooRole(warehouse.getCooId(), warehouse);

        //1. 添加树结构
        // 如果是第一次添加树结构，需要添加总公司节点
        Department parentCompany = getParentCompany(warehouse);

        //添加分公司树结构
        addFiliale(warehouse, parentCompany);

    }

    /**
     * 添加运营总监的角色
     *
     * @param userId
     * @param warehouse
     */
    private void addCooRole(String userId, OrgWarehouse warehouse) {

        userRoleService.addUserRole(userId, User.UserRoleCode.coo.name(), warehouse.getId(), warehouse.getOriginId());

    }

    /**
     * 删除运营总监的角色
     *
     * @param userId
     */
    private void removeCooRole(String userId) {
        userRoleService.removeUserRole(userId, User.UserRoleCode.coo.name());
    }

    /**
     * 获取总公司
     *
     * @param warehouse
     * @return
     */
    private Department getParentCompany(OrgWarehouse warehouse) {
        List<Department> departments = departmentDao.findByOrganizationIdAndParentId(warehouse.getOrganizationId(), DepartmentServiceI.ADMIN_PARENT_ID);
        Department parentCompany = null;
        if (departments == null || departments.isEmpty()) {
            //第一次添加树结构

            Department department = new Department();
            department.setOrganizationId(warehouse.getOrganizationId());
            department.setNodeType(DepartmentNodeType.COMPANY_NODE);
            department.setParentId(DepartmentServiceI.ADMIN_PARENT_ID);
            department.setUserId(SessionUtils.getUserId());
            department.setName("总公司");
            parentCompany = departmentDao.save(department);

        } else {
            parentCompany = departments.get(0);
        }
        return parentCompany;
    }

    /**
     * 添加分公司树节点
     *
     * @param warehouse
     * @param parentCompany
     */
    private void addFiliale(OrgWarehouse warehouse, Department parentCompany) {
        Department filiale = new Department();
        filiale.setOrganizationId(warehouse.getOrganizationId());
        filiale.setName(warehouse.getName() + "(" + warehouse.getCooName() + ")");
        filiale.setNodeType(DepartmentNodeType.COMPANY_NODE);
        filiale.setParentId(parentCompany.getId());
        filiale.setParentIdUrl(parentCompany.getId());
        filiale.setWarehouseId(warehouse.getId());
        filiale.setUserId(warehouse.getCooId());
        departmentDao.save(filiale);
    }

    @Override
    public void delete(String id) {

        List<Department> staffs = departmentDao.findByWarehouseIdAndNodeType(id, DepartmentNodeType.STAFF_NODE);

        if (staffs != null && !staffs.isEmpty()) {
            //@Author: Hukai 2020-01-09 14:52
            //@Descreption: 过滤掉初始化的数据
            List<Department> realUserList = staffs.stream().filter(department -> !department.isInitData()).collect(Collectors.toList());

            if (!realUserList.isEmpty()) {
                throw new EshipRedirectException("当前分公司已分配员工，请先在组织架构中删除！");
            }
        }

        OrgWarehouse warehouse = orgWarehouseDao.findOne(id);
        orgWarehouseDao.delete(warehouse);

        //删除分公司运营总监角色
        //removeCooRole(warehouse.getCooId());

        //1. 删除分公司节点
        List<Department> filiales = departmentDao.findByWarehouseIdAndNodeType(id, DepartmentNodeType.COMPANY_NODE);
        if (filiales != null && !filiales.isEmpty()) {
            filiales.stream().forEach(filiale ->
                    departmentService.deleteDepartment(filiale.getId())
            );
        }

        //2. 删除分公司员工的分公司id
        //userService.removeUserWarehouse(warehouse.getId());

        //@Author: Hukai 2020-01-09 17:11
        //@Descreption: 3.删除员工user表数据(防止重新创建该分公司时提示loginId已存在)
        //排除掉运营总监user
        List<String> userIds = staffs.stream().map(department -> department.getUserId())
                .filter(userId -> !warehouse.getCooId().equals(userId)).collect(Collectors.toList());

        userService.deleteUsers(userIds);
    }

    @Deprecated
    @Override
    public List<OrgWarehouse> findCustomerServiceOrgWarehouses(String organizationId, String customerServiceId) {

        //查询客服的出发地id
        List<CustomServiceAssign> customServiceAssigns = customServiceAssignDao.findByCustomServiceStaffId(customerServiceId);
        if (customServiceAssigns != null && !customServiceAssigns.isEmpty()) {
            String customServiceManagerId = customServiceAssigns.get(0).getCustomServiceManagerId();
            String originId = userService.findCustomServiceManagerOrigin(customServiceManagerId);
            if (StringUtils.isNotBlank(originId)) {
                return orgWarehouseDao.findByOrganizationIdAndOriginId(organizationId, originId);
            }
        }

        return null;
    }

    @Override
    public OrgWarehouse findOne(String warehouseId) {
        return orgWarehouseDao.findOne(warehouseId);
    }

    @Override
    public OrgWarehouse findbyOriginId(String customerOriginId, String organizationId) {
        return this.orgWarehouseDao.findByOriginIdAndOrganizationId(customerOriginId, organizationId);
    }

    @Override
    public OrgWarehouse findOneByOriginNo(String departureCode, String organizationId) {
        return orgWarehouseDao.findOneByOriginNoAndOrganizationId(departureCode, organizationId);
    }

    @Override
    public Map<String, String> getOrgAndWarehourseInfo(String warehouseId) {
        Map<String, String> resultMap = Maps.newHashMap();
        OrgWarehouse orgWarehouse = orgWarehouseDao.findOne(warehouseId);
        resultMap.put("houseName", orgWarehouse.getName());
        resultMap.put("orgId", orgWarehouse.getOrganizationId());
        return resultMap;
    }

    public List<OrgWarehouse> findByIds(List<String> warehouseIds) {
        return orgWarehouseDao.findByIdIn(warehouseIds);
    }

    @Override
    public List<OrgWarehouseDto> getOrgWarehouseDto() {
        if (StringUtils.isBlank(SessionUtils.getWarehouseId())) {
            return new ArrayList<>();
        }
        List<OrgWarehouseDto> orgWarehouseDtoList = new ArrayList<>();
        List<String> regionIds = new ArrayList<>();

        OrgWarehouse orgWarehouse = orgWarehouseDao.findOne(SessionUtils.getWarehouseId());

        regionIds.add(orgWarehouse.getOriginId());
        OrgWarehouseDto orgWarehouseDto = new OrgWarehouseDto();
        orgWarehouseDto.setName(orgWarehouse.getOriginName());
        orgWarehouseDto.setParentId(orgWarehouse.getId());
        orgWarehouseDtoList.add(orgWarehouseDto);

        List<OrgWarehouseDto> regionDtoList = regionService.findByParentIdIn(regionIds);
        return regionDtoList;
    }

    @Override
    public Region getRegionByCustomerId(String customerId) {
        User user = userService.findOne(customerId);
        if (user == null || StringUtils.isBlank(user.getWarehouseId())) {
            return null;
        }
        OrgWarehouse warehouse = orgWarehouseDao.findOne(user.getWarehouseId());

        return regionService.findById(warehouse.getOriginId());
    }

    @Override
    public List<OrgWarehouse> findByOrganizationId(String orgId) {
        return orgWarehouseDao.findByOrganizationId(orgId);
    }

    @Override
    public DataGrid updateWarehouse(OrgWarehouse orgWarehouse) {
        OrgWarehouse orgWarehouseDb = orgWarehouseDao.findOne(orgWarehouse.getId());
        if (orgWarehouseDb == null) {
            throw new EshipRedirectException("分公司id为" + orgWarehouse.getId() + "的数据不存在");
        }

        EshipBeanUtils.copyExclude(orgWarehouse, orgWarehouseDb);
        orgWarehouseDao.save(orgWarehouseDb);

        return new DataGrid(true, "分公司信息修改成功");
    }

    @Override
    public DataGrid addWarehouse(OrgWarehouse orgWarehouse) {
        //查看分公司个数是否已经达到限制个数
        Organization organization = organizationService.findOne(orgWarehouse.getOrganizationId());
        int warehouseCount = orgWarehouseDao.countByOrganizationId(orgWarehouse.getOrganizationId()).intValue();
        ExtraInfoObj extraInfoObj = JsonUtils.jsonToBean(organization.getExtraInfo(), ExtraInfoObj.class, null);

        if (warehouseCount >= extraInfoObj.getWarehouseLimit()) {
            throw new EshipRedirectException("当前组织下最多允许创建" + extraInfoObj.getWarehouseLimit() + "个分公司!");
        }

        //查看该组织下面是否一个分公司都没有创建
        if (warehouseCount == 0) {
            //初始化总公司部门角色和账号
            initOrgWarehouseData(organization);
        }

        List<OrgWarehouse> cooWarehouses = orgWarehouseDao.findByCooId(orgWarehouse.getCooId());
        if (cooWarehouses != null && !cooWarehouses.isEmpty()) {
            throw new EshipRedirectException("SALES_WAREHOUSE_ASSIGN_COO_EXIST", "员工[{0}]不能分配给多个分公司！", true, orgWarehouse.getCooName());
        }

        orgWarehouse.setCreatedDate(new Date());
        OrgWarehouse warehouse = orgWarehouseDao.save(orgWarehouse);
        //添加运营总监角色
        addCooRole(warehouse.getCooId(), warehouse);

        //初始化分公司部门角色和账号
        initWarehouseData(organization, warehouse);

        return new DataGrid(true, "添加成功!");
    }

    /**
     * @Author: Kevin
     * @Date: 2019-07-30 14:59
     * @Description: 初始化总公司部门角色账号数据
     */
    private void initOrgWarehouseData(Organization organization) {
        //根节点
        Department rootDepartment = new Department();
        rootDepartment.setInitData(true);
        rootDepartment.setNodeType(DepartmentNodeType.COMPANY_NODE);
        rootDepartment.setParentId("root");
        rootDepartment.setOrganizationId(organization.getId());

        //-------------添加一个总经理(部门节点)-------------
        User ceoUser = addUserRoleData(organization, null, User.UserRoleCode.president.name());
        rootDepartment.setUserId(ceoUser.getId());

        rootDepartment = departmentDao.save(rootDepartment);

        //-------------添加总经办(部门节点)-------------
        Department ceoDepart = addDepartmentData(organization, User.UserRoleCode.president.name(), null, "总经办", rootDepartment, rootDepartment.getId(), null, null);

        //添加总经办下面的总经理(员工节点)
        addStaffNodeData(ceoDepart, User.UserRoleCode.president.name(), null, true, ceoUser, organization, null, ceoDepart.getId());

        //-------------添加产品经理(部门节点)-------------
        Department productMangagerDepart = addDepartmentData(organization, User.UserRoleCode.product_manager.name(), null, "产品部", rootDepartment, rootDepartment.getId(), null, null);
        User productManagerUser = addUserRoleData(organization, null, User.UserRoleCode.product_manager.name());

        //添加产品经理(员工节点)
        addStaffNodeData(productMangagerDepart, User.UserRoleCode.product_manager.name(), null, true, productManagerUser, organization, null, productMangagerDepart.getId());

        //-------------添加集团财务部(部门节点)-------------
        Department blocFinaDepart = addDepartmentData(organization, User.UserRoleCode.bloc_finance_manager.name(), User.UserRoleCode.bloc_finance.name(), "集团财务部", rootDepartment, rootDepartment.getId(), null, null);

        User blocFinanceManagerUser = addUserRoleData(organization, null, User.UserRoleCode.bloc_finance_manager.name());
        User blocFinanceUser = addUserRoleData(organization, null, User.UserRoleCode.bloc_finance.name());

        //添加集团财务经理(员工节点)
        Department blocFinanceManagerStaff = addStaffNodeData(blocFinaDepart, User.UserRoleCode.bloc_finance_manager.name(), User.UserRoleCode.bloc_finance.name(), true, blocFinanceManagerUser, organization, null, blocFinanceManagerUser.getId());

        //添加集团财务(员工节点)
        addStaffNodeData(blocFinaDepart, User.UserRoleCode.bloc_finance_manager.name(), User.UserRoleCode.bloc_finance.name(), false, blocFinanceUser, organization, null, blocFinanceManagerStaff.getId());
    }

    /**
     * @Author: Kevin
     * @Date: 2019-07-30 14:59
     * @Description: 初始化分公司下的部门和员工数据
     */
    private void initWarehouseData(Organization organization, OrgWarehouse orgWarehouse) {
        //获取根节点id作为parentId
        String rootId = departmentDao.getIdByParentIdAndOrganizationId("root", organization.getId());

        //-------------添加一个分公司-------------
        Department warehouseDepartment = new Department();
        warehouseDepartment.setOrganizationId(organization.getId());
        warehouseDepartment.setManagerRole(User.UserRoleCode.coo.name());
        warehouseDepartment.setNodeType(DepartmentNodeType.COMPANY_NODE);
        warehouseDepartment.setName(orgWarehouse.getName());
        warehouseDepartment.setInitData(true);
        warehouseDepartment.setParentId(rootId);
        warehouseDepartment.setParentIdUrl(rootId);
        warehouseDepartment.setUserId(orgWarehouse.getCooId());
        warehouseDepartment.setWarehouseId(orgWarehouse.getId());

        warehouseDepartment = departmentDao.save(warehouseDepartment);

        User cooUser = userService.findById(orgWarehouse.getCooId());

        String parentIdUrl = rootId + "_" + warehouseDepartment.getId();

        //-------------分公司下添加综合部-------------
        Department cooDepartment = addDepartmentData(organization, User.UserRoleCode.coo.name(), null, "综合部", warehouseDepartment, parentIdUrl, orgWarehouse, orgWarehouse.getCooId());
        addStaffNodeData(cooDepartment, User.UserRoleCode.coo.name(), null, true, cooUser, organization, orgWarehouse, cooDepartment.getId());

        //-------------分公司下添加销售部-------------
        Department saleDepartment = addDepartmentData(organization, User.UserRoleCode.sale_manager.name(), User.UserRoleCode.sale.name(), "销售部", warehouseDepartment, parentIdUrl, orgWarehouse, null);
        //添加销售经理和销售
        User salesManagerUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.sale_manager.name());
        User salesUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.sale.name());
        Department saleManagerStaff = addStaffNodeData(saleDepartment, User.UserRoleCode.sale_manager.name(), User.UserRoleCode.sale.name(), true, salesManagerUser, organization, orgWarehouse, saleDepartment.getId());
        addStaffNodeData(saleDepartment, User.UserRoleCode.sale_manager.name(), User.UserRoleCode.sale.name(), false, salesUser, organization, orgWarehouse, saleManagerStaff.getId());
        //向sales_staff_assign表中添加一条记录
        addSalesStaffAssignData(organization.getId(), salesManagerUser, salesUser);

        //-------------分公司下面添加客服部-------------
        Department customServiceDepartment = addDepartmentData(organization, User.UserRoleCode.custom_service_manager.name(), User.UserRoleCode.custom_service.name(), "客服部", warehouseDepartment, parentIdUrl, orgWarehouse, null);
        //添加客服经理和客服
        User customServiceManagerUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.custom_service_manager.name());
        User customServiceUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.custom_service.name());
        Department customServiceManagerStaff = addStaffNodeData(customServiceDepartment, User.UserRoleCode.custom_service_manager.name(), User.UserRoleCode.custom_service.name(), true, customServiceManagerUser, organization, orgWarehouse, customServiceDepartment.getId());
        addStaffNodeData(customServiceDepartment, User.UserRoleCode.custom_service_manager.name(), User.UserRoleCode.custom_service.name(), false, customServiceUser, organization, orgWarehouse, customServiceManagerStaff.getId());
        //向custom_service_assign表中添加一条记录
        addCustomServiceAssignData(organization.getId(), customServiceManagerUser, customServiceUser);

        //-------------分公司下面添加操作部-------------
        Department operatorDepartment = addDepartmentData(organization, User.UserRoleCode.operator_manager.name(), User.UserRoleCode.operator.name(), "操作部", warehouseDepartment, parentIdUrl, orgWarehouse, null);
        //添加操作经理和操作
        User operatorManagerUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.operator_manager.name());
        User operatorUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.operator.name());
        Department operatorManagerStaff = addStaffNodeData(operatorDepartment, User.UserRoleCode.operator_manager.name(), User.UserRoleCode.operator.name(), true, operatorManagerUser, organization, orgWarehouse, operatorDepartment.getId());
        addStaffNodeData(operatorDepartment, User.UserRoleCode.operator_manager.name(), User.UserRoleCode.operator.name(), false, operatorUser, organization, orgWarehouse, operatorManagerStaff.getId());

        //-------------分公司下面添加财务部-------------
        Department finaceDepartment = addDepartmentData(organization, User.UserRoleCode.finance_manager.name(), User.UserRoleCode.finance.name(), "财务部", warehouseDepartment, parentIdUrl, orgWarehouse, null);
        //添加财务经理和财务
        User financeManagerUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.finance_manager.name());
        User financeUser = addUserRoleData(organization, orgWarehouse, User.UserRoleCode.finance.name());
        Department financeManagerStaff = addStaffNodeData(finaceDepartment, User.UserRoleCode.finance_manager.name(), User.UserRoleCode.finance.name(), true, financeManagerUser, organization, orgWarehouse, finaceDepartment.getId());
        addStaffNodeData(finaceDepartment, User.UserRoleCode.finance_manager.name(), User.UserRoleCode.finance.name(), false, financeUser, organization, orgWarehouse, financeManagerStaff.getId());
    }

    /**
     * @Author: Kevin
     * @Date: 2019-08-01 10:36
     * @Description: 向数据表sales_staff_assign表中添加数据
     */
    private void addSalesStaffAssignData(String organizationId, User salesManagerUser, User salesUser) {
        SalesStaffAssign salesStaffAssign = new SalesStaffAssign();
        salesStaffAssign.setOrganizationId(organizationId);
        salesStaffAssign.setWarehouseId(salesUser.getWarehouseId());
        salesStaffAssign.setSalesStaffId(salesUser.getId());
        salesStaffAssign.setSalesManagerId(salesManagerUser.getId());
        salesStaffAssign.setSalesStaffName(salesUser.getName());
        salesStaffAssign.setSalesManagerName(salesManagerUser.getName());

        salesStaffAssignDao.save(salesStaffAssign);
    }

    /**
     * @Author: Kevin
     * @Date: 2019-08-01 10:37
     * @Description: 向数据表custom_service_assign表中添加数据
     */
    private void addCustomServiceAssignData(String organizationId, User customServiceManagerUser, User customServiceUser) {
        CustomServiceAssign customServiceAssign = new CustomServiceAssign();
        customServiceAssign.setOrganizationId(organizationId);
        customServiceAssign.setCustomServiceStaffId(customServiceUser.getId());
        customServiceAssign.setCustomServiceManagerId(customServiceManagerUser.getId());
        customServiceAssign.setCustomServiceStaffName(customServiceUser.getName());
        customServiceAssign.setCustomServiceManagerName(customServiceManagerUser.getName());

        customServiceAssignDao.save(customServiceAssign);
    }

    /**
     * @param parentDepart    父节点(部门)
     * @param managerRoleCode 管理角色code
     *                        * @param memberRoleCode 普通角色code
     * @param isManagerUser   是否是管理角色
     * @param staffUser       员工User表中的对象
     * @param organization    组织对象
     * @param orgWarehouse    分公司(添加总公司下的员工时传null)
     * @param parentId        父集id==>对于管理角色员工来说parentId是部门id, 对于普通员工来说parentId是同级管理角色员工的id
     * @Author: Kevin
     * @Date: 2019-07-30 14:23
     * @Description: 初始化部门下的员工数据
     */
    private Department addStaffNodeData(Department parentDepart, String managerRoleCode, String memberRoleCode, boolean isManagerUser, User staffUser, Organization organization, OrgWarehouse orgWarehouse, String parentId) {
        Department staffDepartment = new Department();
        staffDepartment.setInitData(true);
        staffDepartment.setNodeType(DepartmentNodeType.STAFF_NODE);
        staffDepartment.setParentId(parentId);

        //管理角色parentIdUrl格式(分公司为例): 总公司id_分公司id_部门id
        //普通角色parentIdUrl格式(分公司为例): 总公司id_分公司id_部门id_同级管理角色员工id
        if (parentDepart.getId().equals(parentId)) {
            staffDepartment.setParentIdUrl(parentDepart.getParentIdUrl() + "_" + parentDepart.getId());
        } else {
            staffDepartment.setParentIdUrl(parentDepart.getParentIdUrl() + "_" + parentDepart.getId() + "_" + parentId);
        }

        staffDepartment.setManagerRole(managerRoleCode);
        staffDepartment.setMemberRole(memberRoleCode);
        staffDepartment.setManagerRoleUser(isManagerUser);
        staffDepartment.setUserId(staffUser.getId());
        staffDepartment.setOrganizationId(organization.getId());
        staffDepartment.setName(staffUser.getName());
        if (orgWarehouse != null) {
            staffDepartment.setWarehouseId(orgWarehouse.getId());
        }

        staffDepartment = departmentDao.save(staffDepartment);
        ;
        return staffDepartment;
    }

    /**
     * @param organization        组织
     * @param managerRoleCode     管理角色code
     * @param memberRoleCode      普通角色code
     * @param departName          部门名称
     * @param warehouseDepartment department表中分公司数据对象
     * @param parentIdUrl         parentIdUrl
     * @param managerUserId       用户id
     * @Author: Kevin
     * @Date: 2019-07-30 14:43
     * @Description: 初始化部门数据
     */
    private Department addDepartmentData(Organization organization, String managerRoleCode, String memberRoleCode, String departName, Department warehouseDepartment, String parentIdUrl, OrgWarehouse orgWarehouse, String managerUserId) {
        Department warehouseDepartDepartment = new Department();
        warehouseDepartDepartment.setOrganizationId(organization.getId());
        warehouseDepartDepartment.setManagerRole(managerRoleCode);
        warehouseDepartDepartment.setMemberRole(memberRoleCode);
        warehouseDepartDepartment.setNodeType(DepartmentNodeType.DEPARTMENT_NODE);
        warehouseDepartDepartment.setName(departName);
        warehouseDepartDepartment.setInitData(true);
        warehouseDepartDepartment.setParentId(warehouseDepartment.getId());
        warehouseDepartDepartment.setParentIdUrl(parentIdUrl);
        if(orgWarehouse != null) {
            warehouseDepartDepartment.setWarehouseId(orgWarehouse.getId());
        }
        warehouseDepartDepartment.setUserId(managerUserId);

        warehouseDepartDepartment = departmentDao.save(warehouseDepartDepartment);
        return warehouseDepartDepartment;
    }

    /**
     * @Author: Kevin
     * @Date: 2019-07-30 13:59
     * @Description: 向user表中添加员工数据
     */
    private User addUserRoleData(Organization organization, OrgWarehouse orgWarehouse, String roleCode) {
        UserEditDto userEditDto = new UserEditDto();
        userEditDto.setOrganizationId(organization.getId());
        userEditDto.setLoginId(generateLoginId(organization, orgWarehouse, roleCode));
        userEditDto.setName(userEditDto.getLoginId());
        userEditDto.setNickName(userEditDto.getLoginId());
        userEditDto.setStaff(Boolean.TRUE);
        userEditDto.setPassword("123456");

        if (orgWarehouse != null) {
            userEditDto.setWarehouseId(orgWarehouse.getId());
            userEditDto.setDestinationId(orgWarehouse.getOriginId());
        }

        String roleId = roleService.getIdByRoleCode(roleCode);
        userEditDto.setRoles(new String[]{roleId});

        User user = userService.edit(userEditDto);
        return user;
    }

    @Override
    public Long countByOrganizationId(String organizationId) {
        return orgWarehouseDao.countByOrganizationId(organizationId);
    }

    /**
     * @Author: Kevin
     * @Date: 2019-07-31 13:55
     * @Description: 按规则自动生成LoginId
     */
    public String generateLoginId(Organization organization, OrgWarehouse orgWarehouse, String roleCode) {
        String dominName = organization.getDominName();
        String domin = StringUtils.split(dominName, '.')[1];

        String loginId;

        //处理roleCode(如finance_manager-->fm, operator-->operator)
        String simpleRoleCode = getSimpleRoleCode(roleCode);
        if (orgWarehouse != null) {
            //获取分公司出发地二字编码
            Region region = regionService.findById(orgWarehouse.getOriginId());
            loginId = domin + "_" + region.getRegionCodeTwo() + "_" + simpleRoleCode;
        } else {
            loginId = domin + "_" + simpleRoleCode;
        }

        return loginId;
    }

    /**
     * @Author: Kevin
     * @Date: 2019-08-01 10:28
     * @Description: 处理roleCode(如finance_manager - - > fm, operator - - > operator)
     */
    private String getSimpleRoleCode(String roleCode) {
        String[] roleCodeArr = StringUtils.split(roleCode, '_');
        if (roleCodeArr.length <= 1) {
            return roleCode;
        }

        StringBuilder builder = new StringBuilder();
        for (String code : roleCodeArr) {
            builder.append(code.substring(0, 1));
        }

        return builder.toString();
    }
}