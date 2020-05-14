package com.yangshan.eship.sales.controller.common;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yangshan.eship.author.dto.account.Customer;
import com.yangshan.eship.author.dto.account.RoleDto;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.author.service.syst.AirlineFlightNoServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.constants.RoleCodeConstant;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import com.yangshan.eship.product.service.zone.ZoneItemServiceI;
import com.yangshan.eship.sales.controller.Version;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: LJ
 * @Date: 2018/4/23 11:57
 * @Description: eship的全部下拉框
 */
@RestController
@RequestMapping(Version.VERSION + "/dropdownlist")
@Api(value = "DropdownListController", tags = "eship的全部下拉框")
public class DropdownListController {

    @Autowired
    private OrgWarehouseServiceI warehouseService;

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private AirlineFlightNoServiceI airlineFlightNoService;

    @Autowired
    private ProductServiceI productService;

    @Autowired
    private ZoneItemServiceI zoneItemService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private RoleServiceI roleServiceI;

    /**
     * @Author: LJ
     * @Date: 2018/4/20 15:37
     * @Description: 根据员工具有的角色找出他管理的仓库（分公司）
     */
    @RequestMapping(value = "/staffWarehouses", method = RequestMethod.GET)
    @ApiOperation(value = "获取分公司下拉框数据", notes = "根据员工具有的角色找出他管理的仓库（分公司）")
    public DataGrid<OrgWarehouse> findStaffWarehouses() {
        Customer c = SessionUtils.getCustomer();
        List<OrgWarehouse> warehouse = findStaffWarehouses(c);
        return new DataGrid<>(true, warehouse);
    }

    //查询所有的子公司
    private List<OrgWarehouse> findStaffWarehouses(Customer c) {
        List<RoleDto> roleDtos = c.getRoles();/**/

        List<String> allRolesDirectlyUnderCompany = departmentService.findAllRolesDirectlyUnderCompany(c.getOrganizationId());
        boolean directlyUnderCompany = false;

        for (RoleDto roleDto : roleDtos) {
            if (allRolesDirectlyUnderCompany.contains(roleDto.getCode())) {
                directlyUnderCompany = true;
                break;
            }
        }

        List<OrgWarehouse> warehouse = Lists.newArrayList();
        if (directlyUnderCompany) {//总公司下面
            warehouse = warehouseService.list(c.getOrganizationId(), null).getRows();

        } else {//分公司下面
            warehouse = Lists.newArrayList();
            warehouse.add(warehouseService.findOne(c.getWarehouseId()));

            if (warehouse.isEmpty()) {
                throw new EshipException("当前用户既没有分配到总公司下，没分配到分公司下");
            }

        }
        return warehouse;
    }

    /**
     * @Author: LJ
     * @Date: 2018/4/24 17:27
     * @Description: 航空主单目的地下拉框
     */
    @RequestMapping(value = "/destinationAirportCode", method = RequestMethod.GET)
    @ApiOperation(value = "获取航空主单目的地下拉框")
    public DataGrid<String> destinationAirportCode() {
        List<String> codes = airlineFlightNoService.distinctDestinationAirportCode();

        return new DataGrid<>(true, codes);
    }

    /**
     * @Author: chenyi
     * @Date: 2018/4/28 16:11
     * @Description:获取当前公司的所有产品Map的集合
     */
    @RequestMapping(value = "/productsForOrg", method = RequestMethod.GET)
    public DataGrid<Object> products() {
        DataGrid<Object> resultDataGrid = new DataGrid<Object>();
        Map<String, String> productIdNamesMapping = Maps.newHashMap();
        List<Product> productList = productService.list(SessionUtils.getOrganizationId());
        for (Product product : productList) {
            productIdNamesMapping.put(product.getId(), product.getName());
        }
        resultDataGrid.setFlag(true);
        resultDataGrid.setObj(productIdNamesMapping);
        return resultDataGrid;
    }

    /**
     * @Author: chenyi
     * @Date: 2018/4/28 16:11
     * @Description:获取当前公司的所有的客户Map的集合
     */
    @RequestMapping(value = "/customers", method = RequestMethod.GET)
    public DataGrid<Object> customers() {
        DataGrid<Object> resultDataGrid = new DataGrid<Object>();

        Customer customer = SessionUtils.getCustomer();
        List<OrgWarehouse> warehouses = findStaffWarehouses(customer);
        List<String> warehouseList = Lists.newArrayList();
        for (OrgWarehouse orgWarehouse : warehouses) {
            warehouseList.add(orgWarehouse.getId());
        }

        Map<String, List<Map>> warehouseUserMap = Maps.newHashMap();
        //查当前组织下的所有用户
        List<User> allUsers = userService.findCustomerByOrganizationIdForDrodown(SessionUtils.getOrganizationId());
        for (User user : allUsers) {
            if (warehouseList.contains(user.getWarehouseId())) {

                Map<String, String> customersMapping = Maps.newHashMap();

                String companyName = user.getSimpleCompanyName();
                String name = user.getCustomerCode();
                if (StringUtils.isNotBlank(companyName) && !companyName.trim().equalsIgnoreCase("null")) {
                    name = MessageFormat.format("{0}({1})", user.getCustomerCode(), companyName);
                }
                customersMapping.put("key", name);
                customersMapping.put("value", user.getId());
                customersMapping.put("code", user.getCustomerCode());
                processCustomerMap(warehouseUserMap, customersMapping, user);
            }
        }

        resultDataGrid.setFlag(true);
        resultDataGrid.setObj(warehouseUserMap);
        return resultDataGrid;
    }

    //处理数据
    private void processCustomerMap(Map<String, List<Map>> warehouseUserMap, Map<String, String> customersMapping, User user) {
        if (warehouseUserMap.containsKey(user.getWarehouseId())) {
            warehouseUserMap.get(user.getWarehouseId()).add(customersMapping);
        } else {
            List<Map> userList = Lists.newArrayList();
            userList.add(customersMapping);
            warehouseUserMap.put(user.getWarehouseId(), userList);
        }
    }

    /**
     * @Author: chenyi
     * @Date: 2018/4/28 16:11
     * @Description:获取角色下的所有的销售Map的集合
     */
    @RequestMapping(value = "/staffs", method = RequestMethod.GET)
    public DataGrid<Object> staffs() {
        DataGrid<Object> resultDataGrid = new DataGrid<Object>();
        Map<String, String> staffsMapping = Maps.newHashMap();
        Customer customer = SessionUtils.getCustomer();
        List<OrgWarehouse> warehouses = findStaffWarehouses(customer);
        //查询下面所有的客户和销售的
        List<String> warehouseList = Lists.newArrayList();
        for (OrgWarehouse orgWarehouse : warehouses) {
            warehouseList.add(orgWarehouse.getId());
        }

        //查询下面所有的客户和销售的
        Map<String, List<Map>> warehouseSalesMap = Maps.newHashMap();
        //查当前组织下的所有用户
        List<User> allUsers = userService.findCustomerByOrganizationId(RoleCodeConstant.ROLE_SALE, SessionUtils.getOrganizationId());
        for (User user : allUsers) {
            if (warehouseList.contains(user.getWarehouseId())) {
                Map<String, String> salesMapping = Maps.newHashMap();
                salesMapping.put("key", user.getName());
                salesMapping.put("value", user.getId());
                processCustomerMap(warehouseSalesMap, salesMapping, user);
            }
        }

        resultDataGrid.setFlag(true);
        resultDataGrid.setObj(warehouseSalesMap);
        return resultDataGrid;
    }

    /**
     * @Author: Kevin
     * @Date: 2019-05-30 19:22
     * @Description: 获取所有已上线产品(下拉框数据)
     */
    @RequestMapping(value = "/getOnLineProducts", method = RequestMethod.GET)
    @ApiOperation(value = "获取所有已上线产品(下拉框数据)", notes = "by 胡凯")
    public DataGrid getOnLineProducts() {
        DataGrid dataGrid = new DataGrid();

        Customer c = SessionUtils.getCustomer();
        List<OrgWarehouse> warehouseList = findStaffWarehouses(c);

        List<String> originNos = new ArrayList<>();
        for (OrgWarehouse warehouse : warehouseList) {
            originNos.add(warehouse.getOriginNo());
        }

        List<Product> products = productService.findByOriginNos(originNos, SessionUtils.getOrganizationId());

        //        ---------- yuchao  给产品下拉框添加 分公司名称 只在产品经理这加 其他角色不改变

        if (warehouseList.size() > 1) {

            for (Product product : products) {
                //@Author: Hukai 2020-01-14 16:11
                //@Descreption: 避免页面出现null数据
                if (StringUtils.isNotBlank(product.getOriginName())) {
                    product.setName("(" + product.getOriginName() + ")");
                }
            }
        }

//        ---------- yuchao  给产品下拉框添加 分公司名称

        dataGrid.setFlag(true);
        dataGrid.setRows(products);
        return dataGrid;
    }


    /**
     * @Author: tsd
     * @Date: 2019-07-25 19:22
     * @Description: 获取所有已隐藏与已下线产品(下拉框数据)
     */
    @RequestMapping(value = "/getNoLineProducts", method = RequestMethod.GET)
    @ApiOperation(value = "获取所有已隐藏与已下线产品(下拉框数据)", notes = "by tsd")
    public DataGrid getNoLineProducts() {
        DataGrid dataGrid = new DataGrid();

        Customer c = SessionUtils.getCustomer();
        List<OrgWarehouse> warehouseList = findStaffWarehouses(c);

        List<String> originNos = new ArrayList<>();
        for (OrgWarehouse warehouse : warehouseList) {
            originNos.add(warehouse.getOriginNo());
        }

        List<Product> products = productService.findByOriginNosNoLine(originNos, SessionUtils.getOrganizationId());

        //        ---------- yuchao  给产品下拉框添加 分公司名称 只在产品经理这加 其他角色不改变

        if (warehouseList.size() > 1) {

            for (Product product : products) {

                String productId = product.getId();

                StringBuilder productName = new StringBuilder(product.getName());

                product.setName(productName.append("(").append(product.getOriginName()).append(")").toString());

            }
        }

//        ---------- yuchao  给产品下拉框添加 分公司名称

        dataGrid.setFlag(true);
        dataGrid.setRows(products);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 通过用户查询其分公司然后查询分公司下产品(所有产品)
     * @Param:
     * @Date: 上午 11:17 2018/5/29
     */
    @RequestMapping(value = "/getProductsWithWarehouse", method = RequestMethod.GET)
    @ApiOperation(value="分公司下所有产品下拉列表", notes = "by Hukai")
    public DataGrid getProductsWithWarehouse() {
        Customer c = SessionUtils.getCustomer();
        List<OrgWarehouse> warehouseList = findStaffWarehouses(c);

        List<String> originNos = new ArrayList<>();
        for (OrgWarehouse warehouse : warehouseList) {
            originNos.add(warehouse.getOriginNo());
        }
        //List<Product> products = productService.findByOriginNos(originNos, SessionUtils.getOrganizationId());
        //@Author: Kevin 2019-05-13 11:45
        //@Descreption: 已经下线的产品也要查出来
        List<Product> products = productService.findAllProductByOriginNos(originNos, SessionUtils.getOrganizationId());

        //        ---------- yuchao  给产品下拉框添加 分公司名称 只在产品经理这加 其他角色不改变

        if (warehouseList.size() > 1) {

            for (Product product : products) {

                String productId = product.getId();

                StringBuilder productName = new StringBuilder(product.getName());

                product.setName(productName.append("(").append(product.getOriginName()).append(")").toString());

            }
        }

//        ---------- yuchao  给产品下拉框添加 分公司名称

        return new DataGrid(true, products);

    }

    /**
     * @Author: tsd
     * @Description: 财务中心渠道统计页面通过用户查询其分公司然后查询分公司下产品
     * @Param:
     * @Date: 上午 11:17 2019/5/8
     */
    @RequestMapping(value = "/getProductsWithFina", method = RequestMethod.GET)
    public DataGrid getProductsWithFina() {
        DataGrid dataGrid = new DataGrid();

        Customer c = SessionUtils.getCustomer();
        List<OrgWarehouse> warehouseList = findStaffWarehouses(c);

        List<String> originNos = new ArrayList<>();
        for (OrgWarehouse warehouse : warehouseList) {
            originNos.add(warehouse.getOriginNo());
        }
        List<Product> products = productService.findByOriginNoAlls(originNos, SessionUtils.getOrganizationId());

        dataGrid.setFlag(true);
        dataGrid.setRows(products);

        return dataGrid;

    }

    /**
     * @author LinYun
     * @date 14:04 2018/7/19
     * @description 根据用户id查询客户名下产品
     */
    @RequestMapping(value = "/getProductsWithCustomerId", method = RequestMethod.GET)
    @ApiOperation(value = "根据用户id查询客户名下产品")
    public DataGrid getProductsWithCustomerId(String customerId) {
        DataGrid dataGrid = new DataGrid();

        dataGrid.setRows(productService.findCustomerProducts(customerId));

        if (dataGrid.getRows() != null && dataGrid.getRows().size() > 0) {
            dataGrid.setFlag(true);
            dataGrid.setTotal(dataGrid.getRows().size());
        } else {
            dataGrid.setFlag(false);
        }

        return dataGrid;

    }

    /**
     * @author LinYun
     * @date 14:59 2018/7/19
     * @description 通过产品id获取zoneItems
     */
    @RequestMapping(value = "/getZoneItemsWithProductId", method = RequestMethod.GET)
    @ApiOperation(value = "通过产品id获取zoneItems")
    public DataGrid getZoneItemsWithProductId(String productId) {
        DataGrid dataGrid = new DataGrid();

        dataGrid.setRows(zoneItemService.findByProductId(productId));

        if (dataGrid.getRows() != null && dataGrid.getRows().size() > 0) {
            dataGrid.setFlag(true);
            dataGrid.setTotal(dataGrid.getRows().size());
        } else {
            dataGrid.setFlag(false);
        }

        return dataGrid;

    }

    /*
     * @Author yuchao
     * @Description
     * @Date Administrator 2019/6/29
     * @Param
     **/
    @RequestMapping(value = "/getProductByWareHouseId", method = RequestMethod.GET)
    public DataGrid<Product> getProductByWareHouseId(String wareHouseId, String productName) {

        DataGrid<Product> dataGrid = new DataGrid();

        String prodNameLike = "%" + productName + "%";

        List<Product> listProductDb = null;

        if (StringUtils.isEmpty(wareHouseId)) {

//            先判断角色
            String userId = SessionUtils.getUserId();

            List<Role> listRole = roleServiceI.findAllRolesByStaffId(userId);

            List<String> listRoleCode = listRole.stream().map(role -> role.getCode()).collect(Collectors.toList());

            if (listRoleCode.contains(User.UserRoleCode.bloc_finance_manager.name())) {

                listProductDb = productService.findByOrgnizationIdAndProductStatusAndNameLike(SessionUtils.getOrganizationId(), Product.ProductStatus.ONLINE, prodNameLike);

            } else {

                String wareHouseIdDb = SessionUtils.getWarehouseId();

                OrgWarehouse orgWarehouse = warehouseService.findOne(wareHouseIdDb);

                listProductDb = productService.findByOrgnizationIdAndProductStatusAndEequalsOrginAndProdNameLike(SessionUtils.getOrganizationId(), Product.ProductStatus.ONLINE, orgWarehouse.getOriginNo(), prodNameLike);

            }

        } else {

            OrgWarehouse orgWarehouse = warehouseService.findOne(wareHouseId);

            listProductDb = productService.findByOrgnizationIdAndProductStatusAndEequalsOrginAndProdNameLike(SessionUtils.getOrganizationId(), Product.ProductStatus.ONLINE, orgWarehouse.getOriginNo(), prodNameLike);

        }

        dataGrid.setFlag(true);

        dataGrid.setMsg("查询成功");

        dataGrid.setRows(listProductDb);

        dataGrid.setTotal(listProductDb.size());

        return dataGrid;
    }

    @RequestMapping(value = "/getCustomerByWareHouseId", method = RequestMethod.GET)
    public DataGrid<User> getCustomerByWareHouseId(String wareHouseId, String customerName) {

        DataGrid<User> dataGrid = new DataGrid();

        String customerNameLike = "%" + customerName + "%";

        List<User> listUser = null;

        String wareHouseIdDb = SessionUtils.getWarehouseId();

        if (StringUtils.isEmpty(wareHouseId)) {

            //            先判断角色
            String userId = SessionUtils.getUserId();

            List<Role> listRole = roleServiceI.findAllRolesByStaffId(userId);

            List<String> listRoleCode = listRole.stream().map(role -> role.getCode()).collect(Collectors.toList());

//            集团财务经理可以看所有用户
            if (listRoleCode.contains(User.UserRoleCode.bloc_finance_manager.name())) {

                if (StringUtils.isEmpty(customerName)) {

                    listUser = userService.findCustomerByOrganizationId(SessionUtils.getOrganizationId());

                } else {

                    listUser = userService.findCustomerByOrganizationIdAndUserNameLike(SessionUtils.getOrganizationId(), customerNameLike);

                }

            } else {

                if (StringUtils.isEmpty(customerName)) {

                    listUser = userService.findCustomerByOrganizationIdAndWarehouseIdAndRoleCode(SessionUtils.getOrganizationId(), wareHouseIdDb);

                } else {

                    listUser = userService.findByOrgIdAndWarehouseIdAndRoleCodeAndUserNameLike(SessionUtils.getOrganizationId(), wareHouseIdDb, User.UserRoleCode.customer.name(), customerNameLike);

                }
            }
        } else {

            if (StringUtils.isEmpty(customerName)) {

                listUser = userService.findByOrgIdAndWarehouseIdAndRoleCode(SessionUtils.getOrganizationId(), wareHouseId, User.UserRoleCode.customer.name());

            } else {

                listUser = userService.findByOrgIdAndWarehouseIdAndRoleCodeAndUserNameLike(SessionUtils.getOrganizationId(), wareHouseId, User.UserRoleCode.customer.name(), customerNameLike);

            }


        }

        dataGrid.setFlag(true);

        dataGrid.setMsg("查询成功");

        dataGrid.setRows(listUser);

        dataGrid.setTotal(listUser.size());

        return dataGrid;
    }

}
