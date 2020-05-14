package com.yangshan.eship.sales.service.cust;

import com.google.common.collect.Lists;
import com.yangshan.eship.author.dto.account.UserDto;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.entity.cust.DataPermissions;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import com.yangshan.eship.sales.repository.cust.DataPermissionsDao;
import com.yangshan.eship.sales.repository.sale.SalesStaffAssignDao;
import com.yangshan.eship.sales.repository.serv.CustomServiceAssignDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author: LinYun
 * @Description:
 * @date: 15:47 2018/4/3
 * Modified By:
 */
@Service
@Transactional
public class DataPermissionsService implements DataPermissionsServiceI {
    @Autowired
    private DataPermissionsDao dataPermissionsDao;

    @Autowired
    private CustomServiceAssignDao customServiceAssignDao;

    @Autowired
    private SalesStaffAssignDao salesStaffAssignDao;

    @Autowired
    private UserServiceI userService;

    @Override
    public List<DataPermissions> findByUserId(String userId) {
        return dataPermissionsDao.findAllByUserId(userId);
    }

    @Override
    public List<DataPermissions> findAllByUserIdAndTargetRoleCode(String userId, String targetRoleCode) {
        return dataPermissionsDao.findAllByUserIdAndTargetRoleCode(userId, targetRoleCode);
    }

    @Override
    public DataGrid<UserDto> listPermissionUsers(String userId, String targetRoleCode) {
        DataGrid<UserDto> userDtosGrid = new DataGrid<>();
        List<UserDto> userDtos = new ArrayList<>();

        Map<String, UserDto> userDtoMap = new HashMap<>();
        Set<String> userIds = new HashSet<>();
        Map<String, User> userMap = new HashMap<>();

        if (User.UserRoleCode.custom_service.name().equals(targetRoleCode)) {
            // 查看客服经理下面的所有客服
            List<CustomServiceAssign> customServiceAssigns = customServiceAssignDao.findByCustomServiceManagerId(SessionUtils.getUserId());

            if (customServiceAssigns.size() > 0) {
                for (CustomServiceAssign customServiceAssign : customServiceAssigns) {
                    userIds.add(customServiceAssign.getCustomServiceStaffId());
                }

                userMap = userService.mapByIds(new ArrayList<>(userIds));

                for (CustomServiceAssign customServiceAssign : customServiceAssigns) {
                    UserDto userDto = new UserDto();
                    User user = userMap.get(customServiceAssign.getCustomServiceStaffId());

                    if (user != null) {
                        userDto.setId(user.getId());
                        userDto.setName(user.getName());
                        userDto.setCustomerCode(user.getCustomerCode());
                    }

                    userDtoMap.put(customServiceAssign.getCustomServiceStaffId(), userDto);
                }
            }

        }

        if (User.UserRoleCode.sale.name().equals(targetRoleCode)) {
            // 查看销售经理下面的所有销售
            List<SalesStaffAssign> salesStaffAssigns = salesStaffAssignDao.findBySalesManagerId(SessionUtils.getUserId());

            if (salesStaffAssigns.size() > 0) {
                for (SalesStaffAssign salesStaffAssign : salesStaffAssigns) {
                    userIds.add(salesStaffAssign.getSalesStaffId());
                }

                userMap = userService.mapByIds(new ArrayList<>(userIds));

                for (SalesStaffAssign salesStaffAssign : salesStaffAssigns) {
                    UserDto userDto = new UserDto();
                    User user = userMap.get(salesStaffAssign.getSalesStaffId());

                    if (user != null) {
                        userDto.setId(user.getId());
                        userDto.setName(user.getName());
                        userDto.setCustomerCode(user.getCustomerCode());
                    }

                    userDtoMap.put(salesStaffAssign.getSalesStaffId(), userDto);
                }
            }

        }

        if (User.UserRoleCode.finance.name().equals(targetRoleCode)) {
            // KEETODO: 查看财务经理下面的所有财务
        }


        List<DataPermissions> dataPermissions = dataPermissionsDao.findAllByUserIdAndTargetRoleCode(userId, User.UserRoleCode.valueOf(targetRoleCode).name());

        if (dataPermissions.size() > 0) {
            for (DataPermissions dataPermission : dataPermissions) {
                if (userIds.contains(dataPermission.getTargetUserId())) {
                    UserDto userDto = userDtoMap.get(dataPermission.getTargetUserId());
                    userDto.setAllocated(true);
                    userDto.setDataPermissionId(dataPermission.getId());
                    userDtoMap.put(dataPermission.getTargetUserId(), userDto);
                }
            }
        }

        userDtoMap.forEach((key, value) -> {
            userDtos.add(value);
        });

        userDtosGrid.setTotal(userDtos.size());
        userDtosGrid.setRows(userDtos);

        return userDtosGrid;
    }

    @Override
    public DataGrid saveCustomServiceAuthority(String userId, String dataPermissionIds, String userIds, String alloweds, String roleCode) {
        String[] dataPermissionIdArr = StringUtils.split(dataPermissionIds, ",");
        String[] userIdArr = StringUtils.split(userIds, ",");
        String[] allowedArr = StringUtils.split(alloweds, ",");

        Set<String> permissionIds = new HashSet<>();
        for (String dataPermissionId : Lists.newArrayList(dataPermissionIdArr)) {
            if (!"empty".equals(dataPermissionId)) {
                permissionIds.add(dataPermissionId);
            }
        }
        List<DataPermissions> dataPermissions = permissionIds.size() > 0 ? dataPermissionsDao.findByIdIn(Lists.newArrayList(dataPermissionIdArr)) : new ArrayList<>();

        Map<String, DataPermissions> permissionsMap = new HashMap<>();

        if (dataPermissions.size() > 0) {
            for (DataPermissions permissions : dataPermissions) {
                permissionsMap.put(permissions.getId(), permissions);
            }
        }

        for (int i = 0; i < dataPermissionIdArr.length; i++) {
            if (!"empty".equals(dataPermissionIdArr[i])) {
                DataPermissions permissions = permissionsMap.get(dataPermissionIdArr[i]);

                if ("0".equals(allowedArr[i])) {
                    //删除该客服的权限
                    dataPermissionsDao.delete(permissions);
                }
            } else {
                if ("1".equals(allowedArr[i])) {
                    //添加权限
                    DataPermissions permissions = new DataPermissions();
                    permissions.setUserId(userId);
                    permissions.setTargetUserId(userIdArr[i]);
                    permissions.setTargetRoleCode(roleCode);
                    permissions.setOrganizationId(SessionUtils.getOrganizationId());

                    dataPermissionsDao.save(permissions);
                }
            }
        }

        return new DataGrid();
    }
}