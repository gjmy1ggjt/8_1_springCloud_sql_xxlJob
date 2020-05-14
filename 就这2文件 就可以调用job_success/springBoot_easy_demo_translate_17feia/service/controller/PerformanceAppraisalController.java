package com.yangshan.eship.sales.controller;

import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.entity.performance.PerformanceAppraisal;
import com.yangshan.eship.sales.service.sales.PerformanceAppraisalService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author yuchao
 * @Description     运营总监
 * @Date Administrator 2020/1/2
 * @Param 
 */
@RestController
@RequestMapping(Version.VERSION  + "/performanceAppraisal")
public class PerformanceAppraisalController {

    @Autowired
    private PerformanceAppraisalService performanceAppraisalService;

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    @ApiOperation(value = "绩效考核-运营总监新增", notes = "by yuchao")
    public DataGrid search(@RequestBody PerformanceAppraisal performanceAppraisal) {
        performanceAppraisal.setOrganizationId(SessionUtils.getOrganizationId());
        return performanceAppraisalService.save(performanceAppraisal);

    }



}
