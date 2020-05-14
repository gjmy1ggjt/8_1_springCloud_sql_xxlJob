package com.yangshan.eship.sales.controller;

import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.dto.WarehouseAddressRequestVO;
import com.yangshan.eship.sales.dto.WarehouseAddressResponseVO;
import com.yangshan.eship.sales.entity.address.AddressType;
import com.yangshan.eship.sales.entity.address.WarehouseAddress;
import com.yangshan.eship.sales.service.sales.WarehouseAddressServiceI;
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

/**
 * @Author: Yifan
 * @Date: 2019/12/17 14:06
 * @Description:
 */
@RestController
@RequestMapping(Version.VERSION + "/warehouseAddress")
@Api(value = "WarehouseAddressController", tags = "拜访记录Controller")
public class WarehouseAddressController {
    private static Logger logger = LoggerFactory.getLogger(WarehouseAddressController.class);

    @Autowired
    private WarehouseAddressServiceI warehouseAddressServiceI;

    @RequestMapping(value = "/getAddressType", method = RequestMethod.GET)
    @ApiOperation(value = "状态下拉", notes = "by Yifan")
    public DataGrid getAddressType() {
        DataGrid dataGrid = new DataGrid();
        try {
            dataGrid.setObj(AddressType.listAddressType());
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
    public DataGrid save(@RequestBody WarehouseAddress warehouseAddress) {
        DataGrid dataGrid = new DataGrid();
        try {
            warehouseAddress.setOrganizationId(SessionUtils.getOrganizationId());
            String msg = warehouseAddressServiceI.save(warehouseAddress);
            if (StringUtils.isNotBlank(msg)) {
                dataGrid.setMsg(msg);
            } else {
                dataGrid.setFlag(true);
                dataGrid.setMsg("操作成功");
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
        DataGrid dataGrid = new DataGrid();
        try {
            WarehouseAddressResponseVO warehouseAddressResponseVO = warehouseAddressServiceI.getById(id);

            dataGrid.setObj(warehouseAddressResponseVO);
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
            warehouseAddressServiceI.deleteById(id);

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
    public DataGrid list(@RequestBody WarehouseAddressRequestVO warehouseAddressRequestVO) {
        DataGrid dataGrid = new DataGrid();
        try {
            warehouseAddressRequestVO.setOrganizationId(SessionUtils.getOrganizationId());

            dataGrid = warehouseAddressServiceI.list(warehouseAddressRequestVO);

            dataGrid.setFlag(true);
            dataGrid.setMsg("查询成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("查询失败");
        }

        return dataGrid;
    }

    @RequestMapping(value = "/getWarehouseAddress", method = RequestMethod.GET)
    @ApiOperation(value = "当前分公司已启用仓库地址", notes = "by Yifan")
    public DataGrid getWarehouseAddress() {
        DataGrid dataGrid = new DataGrid();
        try {
            WarehouseAddressRequestVO warehouseAddressRequestVO = new WarehouseAddressRequestVO();
            warehouseAddressRequestVO.setOrganizationId(SessionUtils.getOrganizationId());
            warehouseAddressRequestVO.setOrgWarehouseId(SessionUtils.getWarehouseId());
            warehouseAddressRequestVO.setAddressType(AddressType.ENABLE);

            dataGrid = warehouseAddressServiceI.list(warehouseAddressRequestVO);

            dataGrid.setFlag(true);
            dataGrid.setMsg("查询成功");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("查询失败");
        }

        return dataGrid;
    }
}
