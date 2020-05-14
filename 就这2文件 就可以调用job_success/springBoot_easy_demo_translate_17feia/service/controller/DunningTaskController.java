package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.finance.entity.fina.DunningTask;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.sales.business.DunningTaskSalesBusiness;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Hukai
 * 2018/3/23 11:32
 */
@RestController("dunningTaskControllerInSalesLib")
@RequestMapping(Version.VERSION + "/dunningTask")
public class DunningTaskController {

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private DunningTaskSalesBusiness dunningTaskBusiness;

    /**
    * @Author: HuKai
    * @Date: 2018/3/27 13:34
    * @Description: 客服催款任务==>针对当前客服下的客户
    */
    @RequestMapping(value = "/list/{roleName}", method = RequestMethod.GET)
    public DataGrid<DunningTask> listByCustService(@PathVariable("roleName") String roleName, DunningTask dunningTask) {
        List<User> users = new ArrayList<>();

        if (StringUtils.isBlank(SessionUtils.getOrganizationId()) || StringUtils.isBlank(SessionUtils.getWarehouseId())) {
            return new DataGrid<>();
        }

        if (User.UserRoleCode.sale.name().equals(roleName)) {
            //销售下的客户催款记录
            users = customerAssignService.findBySalesStaffId(SessionUtils.getUserId());
        }

        if (User.UserRoleCode.custom_service.name().equals(roleName)) {
            //销售下的客户催款记录
            List<CustomerAssign> customers = customerAssignService.findByCustomServiceStaffId(SessionUtils.getUserId());

            users = customerAssignService.getUsers(customers);
        }

        dunningTask.setOrganizationId(SessionUtils.getOrganizationId());
        dunningTask.setWarehouseId(SessionUtils.getWarehouseId());

        return dunningTaskBusiness.listDunningTask(dunningTask, users);
    }

    /**
    * @Author: HuKai
    * @Date: 2018/3/27 14:20
    * @Description: 查询 当前客服/当前销售 的客户
    */
    @RequestMapping(value = "/customerSearch/{roleName}", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id,loginId,name,customerCode,simpleCompanyName")
    public DataGrid<User> customerSearch(@PathVariable("roleName") String roleName) {
        List<User> users = new ArrayList<>();
        if (User.UserRoleCode.sale.name().equals(roleName)) {
            //销售下的客户
            users = customerAssignService.findBySalesStaffId(SessionUtils.getUserId());
        }

        if (User.UserRoleCode.custom_service.name().equals(roleName)) {
            //销售下的客户
            List<CustomerAssign> customers = customerAssignService.findByCustomServiceStaffId(SessionUtils.getUserId());

            users = customerAssignService.getUsers(customers);
        }

        return new DataGrid<>(true, users);
    }

}
