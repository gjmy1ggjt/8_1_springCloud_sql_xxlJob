package com.yangshan.eship.sales.service.cust;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import com.yangshan.eship.sales.dto.CustomerAssignSearchRequestDto;
import com.yangshan.eship.sales.dto.SaleAndManagerDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.repository.cust.CustomerAssignDao;
import com.yangshan.eship.sales.repository.sale.SalesStaffAssignDao;
import com.yangshan.eship.sales.repository.ware.OrgWarehouseDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CustomerAssignService implements CustomerAssignServiceI {
    private Logger logger = LoggerFactory.getLogger(CustomerAssignService.class);

    @Autowired
    UserServiceI userService;

    @Autowired
    private CustomerAssignDao customerAssignDao;

    @Autowired
    private UserRoleServiceI userRoleService;

    @Autowired
    private OrgWarehouseDao warehouseDao;

    @Autowired
    private SalesStaffAssignDao salesStaffAssignDao;

    @Override
    public DataGrid<CustomerAssign> list(CustomerAssign customerAssign) {
        return customerAssignDao.list(customerAssign);
    }

    @Override
    public int assign(List<String> ids, String customServiceManagerId, String salesManagerId, String customServiceStaffId, String salesStaffId) {
        return customerAssignDao.assign(ids, customServiceManagerId, salesManagerId, customServiceStaffId, salesStaffId);
    }

    @Override
    public CustomerAssign save(CustomerAssign customerAssign) {
        return customerAssignDao.save(customerAssign);
    }

    @Override
    public CustomerAssign findByCustomerId(String customerId) {
        return customerAssignDao.findByCustomerId(customerId);

    }

    @Override
    public List<String> findCustomerIdBySalesStaffId(String salesStaffId) {
        return customerAssignDao.findCustomerIdBySalesStaffId(salesStaffId);
    }


    @Override
    public List<CustomerAssign> findAllByCustomerId(List<String> customerIds) {
        List<CustomerAssign> customerAssignList = customerAssignDao.findByCustomerIdIn(customerIds);
        return customerAssignList;

    }

    @Override
    public List<User> findBySalesStaffId(String salesStaffId) {
        List<CustomerAssign> customers = customerAssignDao.findBySalesStaffId(salesStaffId);

        List<String> customerIds = Lists.newArrayList();
        for (CustomerAssign customerAssign : customers) {
            customerIds.add(customerAssign.getCustomerId());
        }

        List<User> userList = Lists.newArrayList();

        if (!customerIds.isEmpty()) {
            userList = userService.listByIds(customerIds);
        }
        return userList;
    }

    /**
     * 根据销售id、销售经理id、客服id、客服经理id查询客户列表
     *
     * @param assign 只支持4个id查询
     * @author Kee.Li
     * @date 2017/12/29 10:59
     */
    @Override
    public DataGrid<User> findByCustomerAssign(CustomerAssign assign) {

        DataGrid<CustomerAssign> dataGrid = customerAssignDao.findByCustomerAssign(assign);
        List<User> userList = Lists.newArrayList();
        if (dataGrid.getTotal() > 0) {
            List<String> customerIds = Lists.newArrayList();
            for (CustomerAssign customerAssign : dataGrid.getRows()) {
                customerIds.add(customerAssign.getCustomerId());
            }
            if (!customerIds.isEmpty()) {
                userList = userService.listByIds(customerIds);
            }
        }

        DataGrid<User> userDataGrid = new DataGrid<>();
        userDataGrid.setFlag(true);
        userDataGrid.setTotal(userList == null ? 0 : userList.size());
        userDataGrid.setRows(userList);

        return userDataGrid;
    }

    @Override
    public List<CustomerAssign> findByCustomServiceStaffId(String customServiceStaffId) {

        return customerAssignDao.findByCustomServiceStaffId(customServiceStaffId);
    }

    @Override
    public List<CustomerAssign> findByCustomServiceManagerId(String customServiceManagerId) {
        return customerAssignDao.findByCustomServiceManagerId(customServiceManagerId);
    }

    @Override
    public List<User> findByCustServiceStaffId(String userId) {
        List<CustomerAssign> customers = customerAssignDao.findByCustomServiceStaffId(userId);

        List<String> customerIds = Lists.newArrayList();
        for (CustomerAssign customerAssign : customers) {
            customerIds.add(customerAssign.getCustomerId());
        }

        List<User> userList = Lists.newArrayList();

        if (!customerIds.isEmpty()) {
            userList = userService.listByIds(customerIds);
        }
        return userList;
    }

    @Override
    public List<String> findBySalesManagerId(String managerId) {
        return customerAssignDao.findBySalesManagerId(managerId);
    }

    @Override
    public List<User> getUsers(List<CustomerAssign> customers) {
        List<User> users = new ArrayList<>();

        if (customers.isEmpty()) {
            return users;
        }

        Set<String> userIds = new HashSet<>();
        for (CustomerAssign customerAssign : customers) {
            if (StringUtils.isNotBlank(customerAssign.getCustomerId())) {
                userIds.add(customerAssign.getCustomerId());
            }
        }

        if (userIds.isEmpty()) {
            return users;
        }

        users = userService.listByIds(new ArrayList<>(userIds));

        return users;
    }

    @Override
    public List<String> findByUserRoleCode(List<Role> roles, String staffId, String organizationId, String warehouseId) {

        if (roles == null || roles.isEmpty()) {
            return Lists.newArrayList();
        }

        //能够查询总公司所有客户的角色
        String[] orgRoles = {User.UserRoleCode.president.name(), User.UserRoleCode.product_manager.name(), User.UserRoleCode.bloc_finance_manager.name()};
        //能够查询分公司客户的角色
        String[] filialeRoles = {User.UserRoleCode.coo.name(), User.UserRoleCode.finance_manager.name(), User.UserRoleCode.finance.name()};
        CustomerAssign assign = new CustomerAssign();
        for (Role role : roles) {
            if (User.UserRoleCode.custom_service_manager.name().equals(role.getCode())) {
                //客服经理
                assign.setCustomServiceManagerId(staffId);
            }
            if (User.UserRoleCode.custom_service.name().equals(role.getCode())) {
                //客服
                assign.setCustomServiceStaffId(staffId);
            }
            if (User.UserRoleCode.sale_manager.name().equals(role.getCode())) {
                //销售经理
                assign.setSalesManagerId(staffId);
            }
            if (User.UserRoleCode.sale.name().equals(role.getCode())) {
                //销售
                assign.setSalesStaffId(staffId);
            }
            if (Lists.newArrayList(orgRoles).contains(role.getCode())) {
                //总公司所有客户
                assign.setOrganizationId(organizationId);

                //兼容总公司角色只查询某分公司的情况，例如：orgid=null, warehouseId=1
                assign.setWarehouseId(warehouseId);
            }
            if (Lists.newArrayList(filialeRoles).contains(role.getCode())) {
                //分公司所有客户
                assign.setWarehouseId(warehouseId);
            }
        }
        DataGrid<CustomerAssign> dataGrid = customerAssignDao.listByOr(assign);

        Set<String> customerIds = Sets.newHashSet();
        if (dataGrid.getTotal() > 0) {
            dataGrid.getRows().stream().forEach(currentAssign -> {
                customerIds.add(currentAssign.getCustomerId());
            });
        }

        return new ArrayList<>(customerIds);
    }

    @Override
    public List<CustomerAssign> findBySalesIds(List<String> ids) {
        return customerAssignDao.findByStaffIds(ids);
    }

    @Override
    public CustomerAssign assignSaleAndSaleManagerForCustomer(String customerId) {
        User customer = userService.findOne(customerId);
        CustomerAssign customerAssign = customerAssignDao.findByCustomerId(customerId);

        if (customerAssign == null) {
            customerAssign = new CustomerAssign();
        }

        String warehouseId = customer.getWarehouseId();
        OrgWarehouse orgWarehouse = warehouseDao.findOne(warehouseId);

        //查找当前组织 当前分区下的销售经理
        List<User> saleManagers = userRoleService.findSaleManagers(customer.getOrganizationId(), orgWarehouse.getOriginId());

        logger.info("=============>当前分区下一共有 {} 个销售经理", saleManagers.size());

        //要分配给的销售和销售经理信息
        SaleAndManagerDto saleAndManagerDto = new SaleAndManagerDto();

        Set<String> saleManagerIds = new HashSet<>();
        if (!saleManagers.isEmpty()) {
            for (User saleManager : saleManagers) {
                saleManagerIds.add(saleManager.getId());
            }

            List<String> saleManagersIdList = new ArrayList<String>(saleManagerIds);


            saleAndManagerDto = this.assignSaleStaffBySaleManagers(saleManagersIdList);  //获取被分配到的客户id
        }

        logger.info("========================>已成功为id为 {} 的客户分配销售和销售经理, 分配的销售id: {}, 销售经理id: {}", customerId, saleAndManagerDto.getSaleId(), saleAndManagerDto.getManagerId());

        //封装customerAssign对象
        customerAssign.setCustomerId(customer.getId());
        customerAssign.setOrganizationId(customer.getOrganizationId());
        customerAssign.setSalesStaffId(saleAndManagerDto.getSaleId());
        customerAssign.setSalesManagerId(saleAndManagerDto.getManagerId());
        customerAssign.setWarehouseId(customer.getWarehouseId());
        customerAssign.setCustomerName(customer.getName());
        customerAssign.setCustomerCode(customer.getCustomerCode());
        customerAssign.setSimpleCompanyName(customer.getSimpleCompanyName());

        customerAssign = customerAssignDao.save(customerAssign);

        return customerAssign;
    }

    @Override
    public SaleAndManagerDto assignSaleStaffBySaleManagers(List<String> saleManagerIds) {
        SaleAndManagerDto saleAndManager = new SaleAndManagerDto();
        saleManagerIds = QueryPlanCacheWrapper.wrapper(saleManagerIds, "CustomerAssignService.assignSaleStaffBySaleManagers.saleManagerIds");
        List<SalesStaffAssign> salesStaffAssigns = salesStaffAssignDao.findInSaleManagerIds(saleManagerIds);

        if (salesStaffAssigns.isEmpty()) {
            return saleAndManager;
        }

        Set<String> salesIds = new HashSet<>();
        Map<String, String> saleSaleManagerMap = new HashMap<>();
        for (SalesStaffAssign salesStaffAssign : salesStaffAssigns) {
            salesIds.add(salesStaffAssign.getSalesStaffId());
            saleSaleManagerMap.put(salesStaffAssign.getSalesStaffId(), salesStaffAssign.getSalesManagerId());
        }

        //@Author: Kevin 2018-11-22 10:49
        //@Descreption: 如果这个销售也是销售经理角色, 则直接分给他自己
        for (String saleId : salesIds) {
            if (saleManagerIds.contains(saleId)) {
                return new SaleAndManagerDto(saleId, saleId);
            }
        }

        List<String> saleIdsArr = new ArrayList<>(salesIds);
        List<Object[]> salesStaffs = salesStaffAssignDao.getSaleCusNumbers(saleIdsArr);

        String saleStaffId = null;
        String saleManagerId = null;

        if (!salesStaffs.isEmpty()) {
            //遍历出查询结果==>有客户的销售id集合
            Set<String> hasCusSales = new HashSet<>();
            for (Object[] saleObject : salesStaffs) {
                hasCusSales.add(saleObject[0].toString());
            }

            if (saleIdsArr.size() - salesStaffs.size() != 0) {
                //说明在这些销售里面, 有的销售下面一个客户都没有, 优先分配给一个客户都没有的销售
                for (String noCusSale : saleIdsArr) {
                    if (!hasCusSales.contains(noCusSale)) {
                        saleStaffId = noCusSale;
                        saleManagerId = saleSaleManagerMap.get(noCusSale);

                        break;
                    }
                }

                return new SaleAndManagerDto(saleStaffId, saleManagerId);
            } else {
                //下面的每个销售都有客户, 则分配给客户最少的销售
                //获取第一条信息的销售id
                saleStaffId = String.valueOf(salesStaffs.get(0)[0]);
                saleManagerId = String.valueOf(salesStaffs.get(0)[1]);

                return new SaleAndManagerDto(saleStaffId, saleManagerId);
            }
        } else {
            //如果该地区下所有销售下面一个客户都没有, 则随机分配一个
            return new SaleAndManagerDto(salesStaffAssigns.get(0).getSalesStaffId(), salesStaffAssigns.get(0).getSalesManagerId());
        }
    }

    /**
     * @Author yuchao
     * @Description     根据销售经理Id获取所有销售
     * @Date Administrator 2019/12/16
     * @Param [orgId, saleMangId]
     */
    @Override
    public List<CustomerAssign> getByOrganizationIdAndSalesManagerId(String orgId, String saleMangId) {
        return customerAssignDao.findByOrganizationIdAndSalesManagerId(orgId, saleMangId);
    }

    @Override
    public List<CustomerAssign> getByOrganizationIdAndSalesStaffId(String orgId, String salesId) {
        return customerAssignDao.findByOrganizationIdAndSalesStaffId(orgId, salesId);
    }

    @Override
    public DataGrid<CustomerAssign> conditionQuery(CustomerAssign customerAssign) {

        return customerAssignDao.conditionQuery(customerAssign);
    }

    @Override
    public int assignSales(String salesStaffId, String customerId) {
        return customerAssignDao.assignSales(salesStaffId, customerId);
    }

    /**
     * @Author: Yifan
     * @Date: 2019/6/6 15:51
     * @Description: 返回所有
     */
    @Override
    public List<CustomerAssign> findAll(List<String> ids) {
        return customerAssignDao.findAll(ids);
    }

    @Override
    public List<CustomerAssign> findBySalesStaffIdInOrSalesManagerIdInOrCustomServiceStaffIdInOrCustomServiceManagerIdIn(List<String> userIds) {
        return customerAssignDao.findBySalesStaffIdInOrSalesManagerIdInOrCustomServiceStaffIdInOrCustomServiceManagerIdIn(userIds, userIds, userIds, userIds);
    }

    @Override
    public List<String> findIdByCustomerCode(String customerCode) {
        return customerAssignDao.findIdByCustomerCode(customerCode, customerCode);
    }

    @Override
    public List<String> findCustomerIdByCustomerService(String orgId, String wareId, String customServiceStaffId) {
        return customerAssignDao.findCustomerIdByCustomerService(orgId, wareId, customServiceStaffId);
    }

    @Override
    public List<String> findCustomerIdByCustomerServiceManager(String orgId, String wareId, String customServiceManagerId) {
        return customerAssignDao.findCustomerIdByCustomerServiceManager(orgId, wareId, customServiceManagerId);
    }

    @Override
    public List<String> findCustomerIdBySale(String orgId, String wareId, String salesStaffId) {
        return customerAssignDao.findCustomerIdBySale(orgId, wareId, salesStaffId);
    }

    @Override
    public List<String> findCustomerIdBySaleManager(String orgId, String wareId, String salesManagerId) {
        return customerAssignDao.findCustomerIdBySaleManager(orgId, wareId, salesManagerId);
    }

    @Override
    public List<String> findCustomerIdByCOO(String orgId, String wareId) {
        return customerAssignDao.findCustomerIdByCOO(orgId, wareId);
    }

    @Override
    public List<String> findCustomerCodeByCustomerService(String orgId, String wareId, String customServiceStaffId) {
        return customerAssignDao.findCustomerCodeByCustomerService(orgId, wareId, customServiceStaffId);
    }

    @Override
    public List<String> findCustomerCodeByCustomerServiceManager(String orgId, String wareId, String customServiceManagerId) {
        return customerAssignDao.findCustomerCodeByCustomerServiceManager(orgId, wareId, customServiceManagerId);
    }

    @Override
    public List<String> findCustomerCodeBySale(String orgId, String wareId, String salesStaffId) {
        return customerAssignDao.findCustomerCodeBySale(orgId, wareId, salesStaffId);
    }

    @Override
    public List<String> findCustomerCodeBySaleManager(String orgId, String wareId, String salesManagerId) {
        return customerAssignDao.findCustomerCodeBySaleManager(orgId, wareId, salesManagerId);
    }

    @Override
    public DataGrid<CustomerAssign> listCustomerAssign(CustomerAssignSearchRequestDto customerAssignSearchRequestDto) {
        if (StringUtils.isNotBlank(customerAssignSearchRequestDto.getCustomerName()) ||
                StringUtils.isNotBlank(customerAssignSearchRequestDto.getPhoneNo()) ||
                StringUtils.isNotBlank(customerAssignSearchRequestDto.getEmail()) ||
                (StringUtils.isNotBlank(customerAssignSearchRequestDto.getRegistDateStart()) &&
                        StringUtils.isNotBlank(customerAssignSearchRequestDto.getRegistDateEnd()))) {
            List<String> userIdList = userService.getUserIds(customerAssignSearchRequestDto);
            if (userIdList.isEmpty()) {
                return new DataGrid<>(true, new ArrayList<>());
            }

            customerAssignSearchRequestDto.setUserIds(userIdList);
        }

        return customerAssignDao.listCustomerAssign(customerAssignSearchRequestDto);
    }

    @Override
    public Map<String, Object> getCustomServiceCustomersInfo(String warehouseId) {
        return customerAssignDao.getCustomServiceCustomersInfo(warehouseId);
    }

    @Override
    public List<CustomerAssign> findByWarehouseId(String warehouseId) {
        return customerAssignDao.findByWarehouseId(warehouseId);
    }

    @Override
    public List<String> listWarehouseSalesStaffs(String warehouseId) {
        return customerAssignDao.listWarehouseSalesStaffs(warehouseId);
    }

    @Override
    public List<CustomerAssign> getSalesCustomersStatics(List<String> salesIds) {
        return customerAssignDao.getSalesCustomersStatics(salesIds);
    }

    @Override
    public DataGrid<CustomerAssign> listSaleCustomerAssign(CustomerAssignSearchRequestDto customerAssignSearchRequestDto) {
        return customerAssignDao.listSaleCustomerAssign(customerAssignSearchRequestDto);
    }

    @Override
    public void updateCustomerAssignSalesManagerId(List<String> salesStaffIds, String salesManagerId) {
        customerAssignDao.updateCustomerAssignSalesManagerId(salesStaffIds, salesManagerId);
    }

    @Override
    public void updateCustomerAssignCustomManagerId(List<String> customServiceIds, String customManagerId) {
        customerAssignDao.updateCustomerAssignCustomManagerId(customServiceIds, customManagerId);
    }
}