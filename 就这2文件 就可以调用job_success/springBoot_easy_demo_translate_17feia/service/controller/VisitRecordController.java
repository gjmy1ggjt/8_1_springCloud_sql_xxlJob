package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.dto.VisitRecordRequestVO;
import com.yangshan.eship.sales.dto.VisitRecordResponseVO;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.entity.visit.FrequencyType;
import com.yangshan.eship.sales.entity.visit.VisitRecord;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignService;
import com.yangshan.eship.sales.service.sales.VisitRecordServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
@RequestMapping(Version.VERSION + "/visitRecord")
@Api(value = "VisitRecordController", tags = "拜访记录Controller")
public class VisitRecordController {
    private static Logger logger = LoggerFactory.getLogger(VisitRecordController.class);

    @Autowired
    private SalesStaffAssignService salesStaffAssignService;
    @Autowired
    private VisitRecordServiceI visitRecordServiceI;
    @Autowired
    private RoleServiceI roleServiceI;

    @RequestMapping(value = "/getFrequency", method = RequestMethod.GET)
    @ApiOperation(value = "跟进频率下拉", notes = "by Yifan")
    public DataGrid getFrequency() {
        DataGrid dataGrid = new DataGrid();
        try {
            dataGrid.setObj(FrequencyType.listFrequencyType());
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
    public DataGrid save(@RequestBody VisitRecordRequestVO visitRecordRequestVO) {
        DataGrid<VisitRecord> dataGrid = new DataGrid<VisitRecord>();
        try {
            visitRecordRequestVO.setSalesStaffId(SessionUtils.getUserId());
            visitRecordRequestVO.setSalesStaffName(SessionUtils.getUserName());
            visitRecordRequestVO.setOrganizationId(SessionUtils.getOrganizationId());
            List<SalesStaffAssign> salesStaffAssigns = salesStaffAssignService.findBySalesStaffId(SessionUtils.getUserId());
            if (salesStaffAssigns.size() > 0) {
                visitRecordRequestVO.setSalesManagerId(salesStaffAssigns.get(0).getSalesManagerId());
                visitRecordRequestVO.setSalesManagerName(salesStaffAssigns.get(0).getSalesManagerName());
            }
            visitRecordServiceI.save(visitRecordRequestVO);

            dataGrid.setFlag(true);
            dataGrid.setMsg("操作成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("操作失败");
        }

        return dataGrid;
    }

    @RequestMapping(value = "/findById/{id}", method = RequestMethod.GET)
    @ApiOperation(value = "根据ID查询", notes = "by Yifan")
    public DataGrid findById(@PathVariable("id") String id) {
        DataGrid<VisitRecordResponseVO> dataGrid = new DataGrid<VisitRecordResponseVO>();
        try {
            VisitRecordResponseVO visitRecordResponseVO = visitRecordServiceI.getById(id);

            dataGrid.setObj(visitRecordResponseVO);
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
            visitRecordServiceI.deleteById(id);

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
    public DataGrid list(@RequestBody VisitRecordRequestVO visitRecordRequestVO) {
        DataGrid dataGrid = new DataGrid();
        try {
            //获取当前登录用户的最大角色
            String userId = SessionUtils.getUserId();
            Role role = roleServiceI.findByUserId(userId);
            if (User.UserRoleCode.coo.name().equals(role.getCode())) {
                visitRecordRequestVO.setOrganizationId(SessionUtils.getOrganizationId());
            } else if (User.UserRoleCode.sale_manager.name().equals(role.getCode())) {
                visitRecordRequestVO.setSalesManagerId(userId);
            } else if (User.UserRoleCode.sale.name().equals(role.getCode())) {
                visitRecordRequestVO.setSalesStaffId(userId);
            }

            dataGrid = visitRecordServiceI.list(visitRecordRequestVO);

            dataGrid.setFlag(true);
            dataGrid.setMsg("查询成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("查询失败");
        }

        return dataGrid;
    }
}
