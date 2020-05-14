package com.yangshan.eship.sales.controller;


import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.business.CustomServiceAssignBusiness;
import com.yangshan.eship.sales.dto.CustomServiceAssignDto;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping(Version.VERSION + "/customServiceAssign")
public class CustomServiceAssignController {

    @Autowired
    private CustomServiceAssignBusiness customServiceAssignBusiness;

    /**
     * 查询客服列表
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/11/8 13:42
     */
    @RequestMapping(method = RequestMethod.GET)
    public DataGrid<CustomServiceAssignDto> list(CustomServiceAssign assign) {

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        assign.setOrganizationId(organizationId);
        String customServiceManagerId = SessionUtils.getUserId();
        assign.setCustomServiceManagerId(customServiceManagerId);

        return customServiceAssignBusiness.list(assign);
    }

    /**
     * 更新客服的区域
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/11/8 14:31
     */
    @RequestMapping(method = RequestMethod.PUT)
    public DataGrid updateZone(String ids, String serviceStaffZoneId) {

        if (StringUtils.isBlank(serviceStaffZoneId)) {
            //取消区域
            serviceStaffZoneId = null;
        }

        if(StringUtils.isNotBlank(ids)){
            String [] idArray = ids.split(",");
            customServiceAssignBusiness.update(Arrays.asList(idArray), serviceStaffZoneId);
        }

        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);

        return dataGrid;
    }


}
