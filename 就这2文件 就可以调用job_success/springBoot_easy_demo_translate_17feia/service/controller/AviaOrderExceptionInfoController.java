package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.order.entity.avia.AviaOrderExceptionInfo;
import com.yangshan.eship.order.service.avia.AviaOrderExceptionInfoServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Author: Kevin
 * @Date: 2018-11-15 16:40
 * @Description:
 */
@RestController
@RequestMapping(Version.VERSION + "/aviaOrderExceptionInfo")
@Api(value = "AviaOrderExceptionInfoController", tags = "主单异常状态")
public class AviaOrderExceptionInfoController {
    private Logger logger = LoggerFactory.getLogger(AviaOrderExceptionInfoController.class);

    @Autowired
    private AviaOrderExceptionInfoServiceI aviaOrderExceptionInfoService;

    /**
     * @Author: Kevin
     * @Date: 2018-11-15 16:49
     * @Description: 查询主单的异常信息
     */
    @RequestMapping(value = "/{aviaOrderId}", method = RequestMethod.GET)
    @ApiOperation(value = "查询主单的异常信息列表", notes = "by Hukai")
    public DataGrid<AviaOrderExceptionInfo> list(@PathVariable("aviaOrderId") String aviaOrderId) {
        List<AviaOrderExceptionInfo> aviaOrderExceptionInfos = aviaOrderExceptionInfoService.listByAviaOrderId(aviaOrderId);

        return new DataGrid<>(true, aviaOrderExceptionInfos);
    }

    /**
     * @Author: Kevin
     * @Date: 2018-11-15 16:49
     * @Description: 客服添加/修改异常信息
     */
    /*@RequestMapping(value = "/addOrUpdate", method = RequestMethod.POST)
    @ApiOperation(value = "客服添加/修改异常信息", notes = "by Hukai")
    public DataGrid add(@RequestBody AviaOrderExceptionInfo aviaOrderExceptionInfo) {
        AviaOrderExceptionInfo exceptionInfo = aviaOrderExceptionInfoService.save(aviaOrderExceptionInfo);

        return new DataGrid<>(true, exceptionInfo);
    }*/


    /**
     * @Author: Kevin
     * @Date: 2018-11-15 16:49
     * @Description: 客服删除异常信息
     */
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "客服删除异常信息", notes = "by Hukai")
    public DataGrid delete(@PathVariable("id") String id) {
        aviaOrderExceptionInfoService.delete(id);

        return new DataGrid<>(true, null);
    }
}
