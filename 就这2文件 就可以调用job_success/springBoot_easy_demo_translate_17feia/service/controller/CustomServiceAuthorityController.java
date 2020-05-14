package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.dto.account.UserDto;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.sales.dto.customServiceAuthorityDto;
import com.yangshan.eship.sales.service.cust.DataPermissionsServiceI;
import com.yangshan.eship.sales.service.serv.CustomServiceAssignServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

/**
 * Created by Hukai
 * 2018-04-16 09:58
 * 客服经理下的客服权限管理
 */
@RestController
@RequestMapping(Version.VERSION + "/customServiceAuthority")
@Api(value = "CustomServiceAuthorityController", tags = "客服代班")
public class CustomServiceAuthorityController {

    @Autowired
    private CustomServiceAssignServiceI customServiceAssignService;

    @Autowired
    private DataPermissionsServiceI dataPermissionsService;

    /**
    * @Author: HuKai
    * @Date: 2018-04-16 10:00
    * @Description: 查询当前登录的客服经理下面所有客服
    */
    @RequestMapping(value = "/listMyCustomServices", method = RequestMethod.GET)
    @JsonFilter(type = User.class, include = "id, name, nickName,targetCount")
    @ApiOperation(value = "查询客服经理客服、代班客服数", notes = "by hukai. 可用字段：id, name, nickName,targetCount", httpMethod = "GET")
    public DataGrid<User> listMyCustomServices() {

        return customServiceAssignService.listMyCustomServices(SessionUtils.getUserId());
    }

    @RequestMapping(value = "/getCustomServiceAuthority/{customServiceId}", method = RequestMethod.GET)
    @JsonFilter(type = UserDto.class, include = "id, name, nickName, allocated, dataPermissionId")
    @ApiOperation(value = "查询客服代班的客服列表", notes = "by hukai. 可用字段：id, name, nickName, allocated, dataPermissionId", httpMethod = "GET")
    public DataGrid<UserDto> getCustomServiceAuthority(@PathVariable("customServiceId") String customServiceId) {

        return dataPermissionsService.listPermissionUsers(customServiceId, User.UserRoleCode.custom_service.name());
    }

    @RequestMapping(value = "/saveCustomServiceAuthority", method = RequestMethod.POST)
    @ApiOperation(value = "保存客服代班信息", notes = "by Hukai")
    public DataGrid saveCustomServiceAuthority(@RequestBody customServiceAuthorityDto customServiceAuthorityDto) {

        return dataPermissionsService.saveCustomServiceAuthority(customServiceAuthorityDto.getUserId(), customServiceAuthorityDto.getDataPermissionIds(), customServiceAuthorityDto.getUserIds(), customServiceAuthorityDto.getAlloweds(), User.UserRoleCode.custom_service.name());
    }
}
