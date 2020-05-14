package com.yangshan.eship.sales.controller;

import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.finance.entity.fina.ProdCommission;
import com.yangshan.eship.finance.service.fina.ProdCommissionServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.finance.entity.fina.CustomerProdCommission;
import com.yangshan.eship.finance.service.fina.CustomerProdCommissionServiceI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * @author: chenyi
 * @Description:自定义提成
 * @Date: 11:44 2017/10/25
 * Modified By:
 */
@RestController
@RequestMapping(Version.VERSION + "/customerProductCommission")
public class CustomerProdCommissionController {
    private static final Logger logger = LoggerFactory.getLogger(CustomerProdCommissionController.class);

    @Autowired
    private CustomerProdCommissionServiceI customerProdCommissionService;

    @Autowired
    private ProdCommissionServiceI prodCommissionService;
    /**
     * add tsd 默认提成查询
     * @param prodCommission
     * @return
     */
    @RequestMapping(value = "/defaultList",method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<ProdCommission> defaultList(ProdCommission prodCommission){
        String orgId = SessionUtils.getOrganizationId();
        prodCommission.setOrganizationId(orgId);
        return prodCommissionService.findByProdCommissionOrderByProduct(prodCommission);
    }

    /**
     * 定制提成查询
     * @param customerProdCommission
     * @return
     */
    @RequestMapping(value = "/commissionList",method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<CustomerProdCommission> commissionList(@RequestBody CustomerProdCommission customerProdCommission){
        String wid = SessionUtils.getWarehouseId();
        customerProdCommission.setWarehouseId(wid);
        return customerProdCommissionService.findByCustomerProdCommission(customerProdCommission);
    }

    @RequestMapping(value = "/commissionList/edit",method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<CustomerProdCommission> commissionListEdit(@RequestBody CustomerProdCommission customerProdCommission){

        DataGrid<CustomerProdCommission> resultDataGrid = new DataGrid<CustomerProdCommission>();
        String wid = SessionUtils.getWarehouseId();
        customerProdCommission.setWarehouseId(wid);
        if(StringUtils.isNotBlank(customerProdCommission.getId())){
            customerProdCommissionService.update(customerProdCommission);
        }else{
            customerProdCommissionService.save(customerProdCommission);
        }
        resultDataGrid.setFlag(true);
        return resultDataGrid;
    }
    @RequestMapping(value = "/commissionList/del",method = RequestMethod.POST)
    @ResponseBody
    public DataGrid<CustomerProdCommission> commissionListDel(@RequestBody CustomerProdCommission customerProdCommission){
        DataGrid<CustomerProdCommission> resultDataGrid = new DataGrid<CustomerProdCommission>();
        customerProdCommissionService.delete(customerProdCommission);
        resultDataGrid.setFlag(true);
        return resultDataGrid;
    }
}
