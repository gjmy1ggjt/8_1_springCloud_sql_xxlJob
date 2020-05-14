package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.dto.PerformanceTargetRequestVO;
import com.yangshan.eship.sales.dto.PerformanceTargetResponseVO;
import com.yangshan.eship.sales.entity.performance.PerformanceTarget;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.service.performance.PerformanceTargetServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author: Yifan
 * @Description:
 * @date: 2019/12/12
 * Modified By:
 */
@RestController
@RequestMapping(Version.VERSION + "/performanceTarget")
@Api(value = "PerformanceTargetController", tags = "绩效目标Controller")
public class PerformanceTargetController {
    private static Logger logger = LoggerFactory.getLogger(PerformanceTargetController.class);

    @Autowired
    private PerformanceTargetServiceI performanceTargetServiceI;
    @Autowired
    private RoleServiceI roleServiceI;

    @RequestMapping(value = "/getSales", method = RequestMethod.GET)
    @ApiOperation(value = "销售经理下的所有销售", notes = "by Yifan")
    public DataGrid getSales() {
        DataGrid dataGrid = new DataGrid();
        try {
            List<SalesStaffAssign> salesStaffAssignList = performanceTargetServiceI.getSales(SessionUtils.getUserId());

            dataGrid.setRows(salesStaffAssignList);
            dataGrid.setFlag(true);
            dataGrid.setMsg("获取成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("获取失败");
        }

        return dataGrid;
    }

    @RequestMapping(value = "/add", method = RequestMethod.POST)
    @ApiOperation(value = "新增", notes = "by Yifan")
    public DataGrid save(@RequestBody PerformanceTarget performanceTarget) {
        DataGrid<PerformanceTarget> dataGrid = new DataGrid<PerformanceTarget>();
        try {
            performanceTarget.setOrganizationId(SessionUtils.getOrganizationId());
            performanceTarget.setSalesManagerId(SessionUtils.getUserId());
            String msg = performanceTargetServiceI.save(performanceTarget);
            if (StringUtils.isNotBlank(msg)) {
                dataGrid.setMsg(msg);
            } else {
                dataGrid.setMsg("操作成功");
                dataGrid.setFlag(true);
            }
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("操作失败");
        }

        return dataGrid;
    }

    @RequestMapping(value = "/findById/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "根据ID查询", notes = "by Yifan")
    public DataGrid findById(@PathVariable("id") String id) {
        DataGrid<PerformanceTargetResponseVO> dataGrid = new DataGrid<PerformanceTargetResponseVO>();
        try {
            PerformanceTargetResponseVO performanceTargetResponseVO = performanceTargetServiceI.getById(id);

            dataGrid.setObj(performanceTargetResponseVO);
            dataGrid.setFlag(true);
            dataGrid.setMsg("查询成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("查询失败");
        }

        return dataGrid;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    @ApiOperation(value = "根据ID删除", notes = "by Yifan")
    public DataGrid deleteById(@PathVariable("id") String id) {
        DataGrid dataGrid = new DataGrid();
        try {
            performanceTargetServiceI.deleteById(id);

            dataGrid.setFlag(true);
            dataGrid.setMsg("删除成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("删除失败");
        }

        return dataGrid;
    }

    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @ApiOperation(value = "列表", notes = "by Yifan")
    public DataGrid list(@RequestBody PerformanceTargetRequestVO PerformanceTargetRequestVO) {
        DataGrid dataGrid = new DataGrid();
        try {
            String userId = SessionUtils.getUserId();
            PerformanceTargetRequestVO.setSalesManagerId(userId);

            dataGrid = performanceTargetServiceI.list(PerformanceTargetRequestVO);

            dataGrid.setFlag(true);
            dataGrid.setMsg("查询成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("查询失败");
        }

        return dataGrid;
    }
}
