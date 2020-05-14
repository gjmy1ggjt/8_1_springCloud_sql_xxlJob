package com.yangshan.eship.sales.controller.statistics;


import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.business.statistics.DashboardDrawBusiness;
import com.yangshan.eship.sales.business.statistics.StatisticDrawBusiness;
import com.yangshan.eship.sales.business.statistics.dto.StatictictisDrawDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;
import com.yangshan.eship.sales.business.statistics.dto.StatitctisDrawByRole;
import com.yangshan.eship.sales.controller.Version;
import com.yangshan.eship.sales.entity.statistic.StatictisDrawToRole;
import com.yangshan.eship.sales.service.statistic.StatictisDrawToRoleServiceI;
import com.yangshan.eship.sales.vo.DashboardVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图表统计入口
 *
 * @author: TuShiDing
 * @date: 2018/06/08 11:12:00
 */
@Slf4j
@RestController
@RequestMapping(Version.VERSION + "/statistics")
@Api(value = "StatisticsDrawController", tags = "图表统计入口")
public class StatisticsDrawController {
    private Logger logger = LoggerFactory.getLogger(StatisticsDrawController.class);

    @Autowired
    private StatisticDrawBusiness statisticDrawBusiness;
    @Autowired
    private DashboardDrawBusiness dashboardDrawBusiness;
    @Autowired
    private StatictisDrawToRoleServiceI statictisDrawToRoleService;
    @Autowired
    private RoleServiceI roleServiceI;

    /**
     * 返回统计类型，数据，查询条件
     *
     * @author: TuShiDing
     * @date: 2018/06/08 11:12:00
     */
    @RequestMapping("/draw")
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawDto) throws ParseException {
        return statisticDrawBusiness.draw(statisticsDrawDto);
    }

    /**
     * 新图表接口
     *
     * @author: TuShiDing
     * @date: 2018/12/25
     */
    @RequestMapping(value = "/drawDashboard", method = RequestMethod.POST)
    public DashboardVO drawDashboard(@RequestBody DashboardVO vo) throws ParseException {
        return dashboardDrawBusiness.drawDashboard(vo);
    }

    /**
     * 查询可显示的图表
     *
     * @author: TuShiDing
     * @date: 2018/06/08 11:12:00
     */
    @RequestMapping("/visable/{type}")
    public List<StatictisDrawToRole> moduleVisable(@PathVariable("type") String type) {
        User user = new User();
        user.setId(SessionUtils.getUserId());
        List<Role> roles = roleServiceI.findAllRolesByStaffId(SessionUtils.getUserId());

        return statictisDrawToRoleService.findByRoleCodeAndModuleName(type, roles, type, SessionUtils.getOrganizationId());

    }

    /**
     * 修改可显示的图表
     *
     * @author: TuShiDing
     * @date: 2018/06/08 11:12:00
     */
    @RequestMapping("/updateVisable")
    public void updateVisable(StatictisDrawToRole statictisDrawToRole) {
        statictisDrawToRoleService.updateVisableByDrawId(statictisDrawToRole.getId(), statictisDrawToRole.getLeftMoveCenter());
    }

    /**
     * 设置角色能显示的图表
     *
     * @author: TuShiDing
     * @date: 2018/06/13 15:12:00
     */
    @RequestMapping(value = "/addVisable", method = RequestMethod.POST)
    public void addVisable(@RequestBody StatictictisDrawDto statictictisDrawDto) {
        List<Object[]> saveData = new ArrayList<>();
        for (StatitctisDrawByRole statitctisDrawByRole : statictictisDrawDto.getAddList()) {
            Object[] objs = new Object[2];
            objs[0] = statitctisDrawByRole.getRoleCode();
            objs[1] = statitctisDrawByRole.getDrawBusinessNames();
            saveData.add(objs);
        }
        statictisDrawToRoleService.addVisable(statictictisDrawDto.getType(), saveData, SessionUtils.getOrganizationId());
    }

    /**
     * 查询已有图表，页面勾选设置用
     *
     * @author: TuShiDing
     * @date: 2018/06/08 11:12:00
     */
    @RequestMapping("/drawBusinessNames/{roleCode}")
    public List<Object[]> getDrawServiceNames(@PathVariable("roleCode") String roleCode) {
        return statisticDrawBusiness.getDrawBusinessNames(roleCode, SessionUtils.getOrganizationId());
    }

    /**
     * 根据角色查询图表集合（新版）
     *
     * @author: TuShiDing
     * @date: 2018/06/08 11:12:00
     */
    @RequestMapping("/getDashboardServiceNames/{roleCode}")
    @ApiOperation(value = "根据角色查询图表集合", notes = "by yifan")
    public DataGrid getDashboardServiceNames(@PathVariable("roleCode") String roleCode) {
        DataGrid dataGrid = new DataGrid();
        try {
            String organziationId = SessionUtils.getOrganizationId();
            if (StringUtils.isBlank(organziationId)) {
                throw new Exception("未登录");
            }

            Map<String, List<Object[]>> map = dashboardDrawBusiness.getDrawBusinessNames(roleCode, organziationId);

            dataGrid.setObj(map);
            dataGrid.setMsg("获取成功");
            dataGrid.setFlag(true);
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("获取异常：" + e.getMessage());
        }

        return dataGrid;
    }

    /**
     * @Author: Yifan
     * @Date: 2019/7/22 18:22
     * @Description: 保存图表集合（新版）
     */
    @RequestMapping(value = "/addVisableSelect", method = RequestMethod.POST)
    @ApiOperation(value = "保存图表集合", notes = "by yifan")
    public DataGrid addVisableSelect(@RequestBody List<StatitctisDrawByRole> statitctisDrawByRoleList) {
        DataGrid dataGrid = new DataGrid();
        try {
            String organziationId = SessionUtils.getOrganizationId();
            if (StringUtils.isBlank(organziationId)) {
                throw new Exception("未登录");
            }

            //角色代码 - 图表集合（模块名、图表名、是否勾选）
            Map<String, List<String[]>> statitctisDrawByRoleMap = statitctisDrawByRoleList.stream().collect(Collectors.toMap(StatitctisDrawByRole::getRoleCode, statitctisDrawByRole -> statitctisDrawByRole.getDrawBusinessNames()));
            statictisDrawToRoleService.addVisable(statitctisDrawByRoleMap, organziationId);

            dataGrid.setFlag(true);
            dataGrid.setMsg("保存成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("保存异常：" + e.getMessage());
        }

        return dataGrid;
    }
}
