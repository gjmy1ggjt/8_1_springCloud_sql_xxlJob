package com.yangshan.eship.sales.service.statistic;

import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import com.yangshan.eship.sales.entity.statistic.StatictisDrawToRole;
import com.yangshan.eship.sales.repository.statistic.StatictisDrawToRoleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Transactional
@Service
public class StatictisDrawToRoleService implements StatictisDrawToRoleServiceI {
    @Autowired
    StatictisDrawToRoleDao statictisDrawToRoleDao;

    /**
     * 查询可显示的图表
     *
     * @author: TuShiDing
     * @date: 2018/06/08 11:12:00
     */
    @Override
    public List<StatictisDrawToRole> findByRoleCodeAndModuleName(String type, List<Role> roles, String moduleName, String organizationId) {
        List<String> roleCodes = new ArrayList<>();
        for (Role role : roles) {
            roleCodes.add(role.getCode());
        }
        roleCodes = QueryPlanCacheWrapper.wrapper(roleCodes,"StatictisDrawToRoleService.findByRoleCodeAndModuleName.roleCodes");
        return statictisDrawToRoleDao.findByRoleCodeAndModuleNameAndOrganizationId(type, roleCodes, organizationId);
    }

    /**
     * 查询已选中的图表
     *
     * @author: TuShiDing
     * @date: 2018/06/19 11:12:00
     */
    @Override
    public List<StatictisDrawToRole> findByRoleCode(String type, String roleCode, String organizationId) {
        return statictisDrawToRoleDao.findByTypeRoleCodeAndOrganizationId(type, roleCode, organizationId);
    }

    /**
     * 修改可显示的图表
     *
     * @author: TuShiDing
     * @date: 2018/06/08 11:12:00
     */
    @Override
    public void updateVisableByDrawId(String drawId, Boolean leftMoveCenter) {
        statictisDrawToRoleDao.updateVisableByDrawId(drawId, leftMoveCenter);
    }

    /**
     * 设置角色能显示的图表
     *
     * @author: TuShiDing
     * @date: 2018/06/13 15:12:00
     */
    @Override
    public void addVisable(String type, List<Object[]> saveData, String organizationId) {
        for (Object[] objs : saveData) {
            String roleCode = (String) objs[0];
            statictisDrawToRoleDao.deleteVisableByRoleCodeAndOrganizationId(type, roleCode, organizationId);
            List<Object[]> drawBusinessNames = (List<Object[]>) objs[1];
            for (Object[] drawBusinessName : drawBusinessNames) {
                if ("true".equals(drawBusinessName[2])) {
                    StatictisDrawToRole sdtr = new StatictisDrawToRole();
                    sdtr.setRoleCode(roleCode);
                    sdtr.setDrawServiceName(drawBusinessName[1] + "");
                    sdtr.setModuleName(drawBusinessName[0] + "");
                    sdtr.setLeftMoveCenter(false);
                    sdtr.setOrganizationId(organizationId);
                    sdtr.setType(type);
                    statictisDrawToRoleDao.save(sdtr);
                }
            }
        }
    }

    /**
     * @Author: Yifan
     * @Date: 2019/7/22 18:21
     * @Description: 保存
     * @Param statitctisDrawByRoleMap 角色代码 - 图表集合（模块名、图表名、是否勾选）
     * @Param organizationId 组织ID
     * @Return:
     */
    @Override
    public void addVisable(Map<String, List<String[]>> statitctisDrawByRoleMap, String organizationId) {
        //已勾选DB（角色代码 - 关系表集合）
        List<StatictisDrawToRole> statictisDrawToRoleList = statictisDrawToRoleDao.findByOrganizationId(organizationId);
        Map<String, List<StatictisDrawToRole>> statictisDrawToRoleMap = statictisDrawToRoleList.stream().collect(Collectors.groupingBy(StatictisDrawToRole::getRoleCode));

        statitctisDrawByRoleMap.forEach((roleCode, drawBusinessNames) -> {
            //已勾选DB（DrawServiceName - 关系表ID）
            List<StatictisDrawToRole> statictisDrawToRoleSub = statictisDrawToRoleMap.get(roleCode);
            Map<String, String> statictisDrawToRoleSubMap = new HashMap<>();
            if (statictisDrawToRoleSub != null && statictisDrawToRoleSub.size() != 0) {
                statictisDrawToRoleSubMap = statictisDrawToRoleSub.stream().collect(Collectors.toMap(StatictisDrawToRole::getDrawServiceName, statictisDrawToRole -> statictisDrawToRole.getId()));
            }
            Map<String, String> finalStatictisDrawToRoleSubMap = statictisDrawToRoleSubMap;

            drawBusinessNames.forEach(drawBusinessName -> {
                if (!finalStatictisDrawToRoleSubMap.containsKey(drawBusinessName[1]) && "true".equals(drawBusinessName[2])) {
                    StatictisDrawToRole sdtr = new StatictisDrawToRole();
                    sdtr.setRoleCode(roleCode);
                    sdtr.setDrawServiceName(drawBusinessName[1]);
                    sdtr.setModuleName(drawBusinessName[0]);
                    sdtr.setLeftMoveCenter(false);
                    sdtr.setOrganizationId(organizationId);
                    statictisDrawToRoleDao.save(sdtr);
                } else if (finalStatictisDrawToRoleSubMap.containsKey(drawBusinessName[1]) && "false".equals(drawBusinessName[2])) {
                    statictisDrawToRoleDao.delete(finalStatictisDrawToRoleSubMap.get(drawBusinessName[1]));
                }
            });
        });
    }

    /**
     * @Author: Yifan
     * @Date: 2019/7/22 16:53
     * @Description: 根据角色代码和组织ID查询关系表
     */
    @Override
    public List<StatictisDrawToRole> findByRoleCodeAndOrganizationId(String roleCode, String organizationId) {
        return statictisDrawToRoleDao.findByRoleCodeAndOrganizationId(roleCode, organizationId);
    }
}
