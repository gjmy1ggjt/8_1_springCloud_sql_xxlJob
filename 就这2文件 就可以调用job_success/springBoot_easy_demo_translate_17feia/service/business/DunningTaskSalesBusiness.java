package com.yangshan.eship.sales.business;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.finance.entity.fina.DunningTask;
import com.yangshan.eship.finance.service.fina.DunningTaskServiceI;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by Hukai
 * 2018-05-08 11:07
 */
@Service
public class DunningTaskSalesBusiness {
    private static final Logger logger = LoggerFactory.getLogger(DunningTaskSalesBusiness.class);

    @Autowired
    private DunningTaskServiceI dunningTaskService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    public DataGrid<DunningTask> listDunningTask(DunningTask dunningTask, List<User> users) {
        DataGrid<DunningTask> grid = dunningTaskService.listDunningTask(dunningTask, users);

        return this.formatResult(grid);
    }

    private DataGrid<DunningTask> formatResult(DataGrid<DunningTask> grid) {
        if (!grid.getRows().isEmpty()) {
            Set<String> userIds = new HashSet<>();

            for (DunningTask dunningTaskObj : grid.getRows()) {
                if (StringUtils.isNotBlank(dunningTaskObj.getCustomerId())) {
                    userIds.add(dunningTaskObj.getCustomerId());
                }
            }

            List<CustomerAssign> customerAssignList = new ArrayList<>();
            if (!userIds.isEmpty()) {
                List<String> userIdList = new ArrayList<>(userIds);
                customerAssignList = customerAssignService.findAllByCustomerId(userIdList);
            }

            Map<String, CustomerAssign> customerAssignMap = new HashMap<>();
            if (!customerAssignList.isEmpty()) {
                for (CustomerAssign customerAssign : customerAssignList) {
                    customerAssignMap.put(customerAssign.getCustomerId(), customerAssign);

                    if (StringUtils.isNotBlank(customerAssign.getSalesStaffId())) {
                        userIds.add(customerAssign.getSalesStaffId());
                    }

                    if (StringUtils.isNotBlank(customerAssign.getCustomServiceStaffId())) {
                        userIds.add(customerAssign.getCustomServiceStaffId());
                    }
                }
            }

            Map<String, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

            for (DunningTask dunningTaskObj : grid.getRows()) {
                dunningTaskObj.setSimpleCompanyName(userMap.get(dunningTaskObj.getCustomerId()).getSimpleCompanyName());
                CustomerAssign customerAssign = customerAssignMap.get(dunningTaskObj.getCustomerId()) != null ? customerAssignMap.get(dunningTaskObj.getCustomerId()) : null;

                String saleStaffId = "";
                String saleStaff = "";
                String cusServiceStaffId = "";
                String cusServiceStaff = "";

                if (customerAssign != null) {
                    saleStaffId = customerAssign.getSalesStaffId() == null ? "" :  customerAssign.getSalesStaffId();
                    saleStaff = customerAssign.getSalesStaffId() == null ? "" :  userMap.get(customerAssign.getSalesStaffId()).getName();
                    cusServiceStaffId = StringUtils.isBlank(customerAssign.getCustomServiceStaffId()) ? "" :  customerAssign.getCustomServiceStaffId();
                    cusServiceStaff = StringUtils.isBlank(customerAssign.getCustomServiceStaffId()) ? "" :  userMap.get(customerAssign.getCustomServiceStaffId()).getName();
                }

                dunningTaskObj.setCustomerCode(userMap.get(dunningTaskObj.getCustomerId()).getCustomerCode());
                dunningTaskObj.setCustomerPhone(userMap.get(dunningTaskObj.getCustomerId()).getPhone());
                dunningTaskObj.setCustomerName(userMap.get(dunningTaskObj.getCustomerId()).getName());
                dunningTaskObj.setSalesStaffId(saleStaffId);
                dunningTaskObj.setSalesStaff(saleStaff);
                dunningTaskObj.setCustomServiceStaffId(cusServiceStaffId);
                dunningTaskObj.setCustomServiceStaff(cusServiceStaff);
            }

        }

        return grid;
    }
}
