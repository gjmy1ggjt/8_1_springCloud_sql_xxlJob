package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.author.service.account.UserRoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.product.entity.zone.Zone;
import com.yangshan.eship.product.service.ApprovalServiceI;
import com.yangshan.eship.product.service.zone.ZoneServiceI;
import com.yangshan.eship.sales.dto.CustomerViewRequestDto;
import com.yangshan.eship.sales.dto.CustomerViewResponseDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.product.service.zone.ZoneReportServiceI;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @Author: LiuWei
 * @Description: 销售报价
 * @Date: 11:08 2017/11/30 Modified By:
 */
@RestController
@RequestMapping(Version.VERSION + "/salesReport")
public class SalesReportController {

    @Autowired
    private ZoneReportServiceI zoneReportService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private ZoneServiceI zoneService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private DepartmentServiceI departmentService;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private ApprovalServiceI approvalService;

    @Autowired
    private RoleServiceI roleService;

    @Autowired
    private OrgWarehouseServiceI warehouseService;

    @Autowired
    private UserRoleServiceI userRoleService;


    /**
     * @Description: 销售报价设置 报价视图查询
     * @return:
     * @params: 出发地编码，目的地编码，产品名称
     * @Date: 19:47 2017/11/2
     */
    @RequestMapping(value = "/reportView", method = RequestMethod.POST)
    public DataGrid reportView(@RequestBody String reportViewPrams) {
        DataGrid dataGrid = zoneReportService.reportViewSearch(reportViewPrams, SessionUtils.getOrganizationId());
        return dataGrid;
    }


    /**
     * @Description:当前角色(销售和销售经理)在报价下已分配的客户
     * @return:
     * @params:
     * @Date: 19:47 2017/11/2
     */
    @RequestMapping(value = "/assignedCustomer", method = RequestMethod.GET)
    public DataGrid assignedCustomer(String zoneReportId) {
        DataGrid dataGrid = new DataGrid();
        CustomerAssign assign = new CustomerAssign();
        assign.setSalesStaffId(SessionUtils.getUserId());
        DataGrid<User> userDataGrid = customerAssignService.findByCustomerAssign(assign);
        if (userDataGrid.getTotal() < 1) {
            return userDataGrid;
        }
        Map<String, User> userMap = new HashMap<>();
        for (User user : userDataGrid.getRows()) {
            userMap.put(user.getId(), user);
        }

        List<String> ids = zoneReportService.assignedCustomer(zoneReportId);
        List list = new ArrayList();
        for (String id : ids) {
            String[] strings = id.split(",");
            String userId = strings[0];
            if (userMap.containsKey(userId)) {
                User user = userMap.get(userId);
                Map map = new HashMap();
                map.put("id", userId);
                map.put("loginId", user.getLoginId());
                map.put("customerCode", user.getCustomerCode());
                map.put("companyName", user.getSimpleCompanyName());
                map.put("customerReportPriceId", strings[1]);
                list.add(map);
            }

        }
        dataGrid.setRows(list);
        return dataGrid;
    }

    /**
     * @Description:所有未分配的客户
     * @return:
     * @params:
     * @Date: 19:47 2017/11/2
     */
    @RequestMapping(value = "/noAssignedCustomer", method = RequestMethod.GET)
    public DataGrid<User> noAssignedCustomer(String zoneId) {
        DataGrid<User> dataGrid = new DataGrid();
        // 所有的客户
        CustomerAssign assign = new CustomerAssign();
        assign.setSalesStaffId(SessionUtils.getUserId());
        DataGrid<User> userDataGrid = customerAssignService.findByCustomerAssign(assign);
        if (userDataGrid.getTotal() < 1) {
            return userDataGrid;
        }

        // 当前分区下已经分配报价的客户
        List<String> customerIds = zoneReportService.zoneAssignedCustomer(zoneId);
        Iterator iterator = userDataGrid.getRows().iterator();
        while (iterator.hasNext()) {
            User user = (User) iterator.next();
            if (customerIds.contains(user.getId())) {
                iterator.remove();
            }
        }
        dataGrid.setRows(userDataGrid.getRows());
        return dataGrid;
    }


    /**
     * @Author: LiuWei
     * @Description: 分区下报价
     * @Param:
     * @Date: 14:59 2017/12/4
     */
    @RequestMapping(value = "/zoneReportprice", method = RequestMethod.GET)
    public DataGrid zoneReportprice(String zoneId) {
        DataGrid dataGrid = zoneReportService.zoneReportprice(zoneId);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 为客户分配报价
     * @Param:
     * @Date: 14:59 2017/12/4
     */
    @RequestMapping(value = "/assignCustomerReport", method = RequestMethod.POST)
    public DataGrid assignCustomerReport(String zoneReportId, String customerId) {
        DataGrid dataGrid = new DataGrid();
        zoneReportService.assignCustomerReport(zoneReportId, customerId, SessionUtils.getUserId(), SessionUtils.getLoginId(), SessionUtils.getOrganizationId());
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 报价详情
     * @Param:
     * @Date: 14:59 2017/12/4
     */
    @RequestMapping(value = "/zoneReportPriceDetails", method = RequestMethod.GET)
    public DataGrid zoneReportPriceDetails(String zoneReportId) {
        DataGrid dataGrid = zoneReportService.zoneReportPriceDetails(zoneReportId);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 产品分区列表
     * @Param:
     * @Date: 14:59 2017/12/4
     */
    @RequestMapping(value = "/zoneList", method = RequestMethod.GET)
    public DataGrid zoneList(String productId) {
        DataGrid dataGrid = new DataGrid();
        List<Zone> zoneList = zoneService.getByProductId(productId);
        dataGrid.setObj(zoneList);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 取消客户报价
     * @Param:
     * @Date: 14:59 2017/12/4
     */
    @RequestMapping(value = "/cancelCustomerReport", method = RequestMethod.POST)
    public DataGrid cancelCustomerReport(String customerReportPriceId) {
        DataGrid dataGrid = new DataGrid();
        zoneReportService.cancelCustomerReport(customerReportPriceId);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 报价设置 客户视图 报价切换请求
     * @Param:
     * @Date: 14:12 2018/1/19
     */
    @RequestMapping(value = "/switchReportPrice", method = RequestMethod.GET)
    public DataGrid switchReportPrice(String customerId, String zoneReportPriceId) {
        DataGrid dataGrid = new DataGrid();
        zoneReportService.switchReportPrice(customerId, zoneReportPriceId);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 产品是否对客户可见
     * @Param:
     * @Date: 14:53 2018/1/19
     */
    @RequestMapping(value = "/productIsVisibleToUser", method = RequestMethod.GET)
    public DataGrid productIsVisibleToUser(Boolean visible, String customerId, String productId) {
        DataGrid dataGrid = new DataGrid();
        zoneReportService.productIsVisibleToUser(visible, customerId, productId, SessionUtils.getUserId(), SessionUtils.getLoginId());
        return dataGrid;
    }


    /**
     * @Description:所有未分配的客户
     * @return:
     * @params:
     * @Date: 19:47 2017/11/2
     */
    @RequestMapping(value = "/customerByStaffSaleId", method = RequestMethod.GET)
    public DataGrid<User> customerByStaffSaleId(String zoneId) {
        DataGrid<User> dataGrid = new DataGrid();
        // 所有的客户
        CustomerAssign assign = new CustomerAssign();
        assign.setSalesStaffId(SessionUtils.getUserId());
        DataGrid<User> userDataGrid = customerAssignService.findByCustomerAssign(assign);
        return userDataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 保存客户报价
     * @Param:
     * @Date: 下午 3:35 2018/3/7
     */
    @RequestMapping(value = "/saveZoneReportPrice", method = RequestMethod.POST)
    public DataGrid saveZoneReportPrice(@RequestBody String params) {
        DataGrid dataGrid = new DataGrid();
        Map<String, Object> returnMap = zoneReportService.saveZoneReportPrice(params, SessionUtils.getOrganizationId(), SessionUtils.getUserId());
        String warehouseId = SessionUtils.getWarehouseId();
        if (returnMap != null) {
            returnMap.put("warehouseId", warehouseId);

            String saleId = SessionUtils.getUserId();
            Integer level = (Integer) returnMap.get("level");
            User saleManager = departmentService.getLeaderByStaff(saleId);
            User operationDirector = null;
            User generalManager = null;
            // 肯定需要销售经理进行审核
            returnMap.put("saleManager", saleManager);
            returnMap.put("orgId", SessionUtils.getOrganizationId());

            if (level == 2) {
                // 需要销售经理和运营总监进行审核
                /*operationDirector = departmentService.getLeaderByStaff(saleManager.getId());
                returnMap.put("operationDirector", operationDirector);*/
                //@Author: Kevin 2019-04-26 15:51
                //@Descreption: 查找分公司的运营总监
                List<User> cooUsers = userRoleService.findWarehouseUserByRoleCode(warehouseId, User.UserRoleCode.coo.name());
                if (!cooUsers.isEmpty()) {
                    returnMap.put("operationDirector", cooUsers.get(0));
                }
            }

            //@Author: Kevin 2019-05-07 17:20
            //@Descreption: 需要财务总监审批
            if (level == 3) {
                // 需要销售经理，运营总监，财务总监进行审核
                List<User> cooUsers = userRoleService.findWarehouseUserByRoleCode(warehouseId, User.UserRoleCode.coo.name());
                if (!cooUsers.isEmpty()) {
                    returnMap.put("operationDirector", cooUsers.get(0));
                }

                List<User> financeCooUsers = userRoleService.findUserByRoleCode(User.UserRoleCode.bloc_finance_manager.name());
                if (!financeCooUsers.isEmpty()) {
                    User approvalUser = null;
                    //@Author: Kevin 2019-05-28 17:41
                    //@Descreption: 集团财务只分配给杨小春
                    for (User user : financeCooUsers) {
                        if ("杨小春".equals(user.getName())) {
                            approvalUser = user;
                        }
                    }
                    returnMap.put("blocFinanceManager", approvalUser != null ? approvalUser : financeCooUsers.get(0));
                }
            }

            //@Author: HuKai @Date: 2018-05-18 14:10 @Description:记录当前提交人信息(name)
            returnMap.put("commitUserName", SessionUtils.getCustomer().getName());
            // 请求审核服务，生成审核记录
            approvalService.generateCustomerReportPriceRecords(returnMap);

        }


        dataGrid.setFlag(true);
        return dataGrid;
    }


    @RequestMapping(value = "/judgeExistApprovalRecord", method = RequestMethod.GET)
    public DataGrid judgeExistApprovalRecord(String customerId, String zoneId) {
        DataGrid dataGrid = new DataGrid();
        Boolean hasApprovalRecord = approvalService.hasCustomerReportPriceApprovalRecord(customerId, zoneId);
        dataGrid.setFlag(true);
        dataGrid.setObj(hasApprovalRecord);
        return dataGrid;
    }

    @RequestMapping(value = "/surchargeConstants", method = RequestMethod.GET)
    public DataGrid surchargeConstants(String id, String productSurchargeId) {
        DataGrid dataGrid = zoneReportService.surchargeConstants(id, productSurchargeId);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    @RequestMapping(value = "/getReportPriceIds", method = RequestMethod.GET)
    public DataGrid getReportPriceIds(String reportPriceId) {
        DataGrid dataGrid = new DataGrid();

        String userId = SessionUtils.getUserId();
        Role role = roleService.findByUserId(userId);

        List<String> ids = zoneReportService.findIds(reportPriceId, role.getCode());
        ids.add(reportPriceId);
        dataGrid.setRows(ids);
        return dataGrid;

    }


}
