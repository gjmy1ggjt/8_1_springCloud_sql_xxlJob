package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.finance.service.fina.LogisticOrderCommissionServiceI;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;

/**
 * @Author:
 * @Description:TuShiDing
 * @Date: 上午 9:33 2018/4/17
 */
@RestController("LogisticOrderCommissionControllerInSalesLib")
@RequestMapping(Version.VERSION  + "/logisticOrder")
public class LogisticOrderCommissionController {



    @Autowired
    LogisticOrderCommissionServiceI logisticOrderCommissionService;


    @RequestMapping(value = "/generateStaffCommission", method = RequestMethod.GET)
    public DataGrid generateStaffCommission(String endTime) throws ParseException {
        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        return dataGrid;
    }


    /**
     * @Author: LiuWei
     * @Description: 物流订单提成查询
     * @Param:
     * @Date: 下午 7:09 2018/6/7
     */
    @RequestMapping(value = "/logisticOrderCommissions", method = RequestMethod.POST)
    public DataGrid logisticOrderCommissions(@RequestBody  String params)  {
        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        return dataGrid;
    }



}
