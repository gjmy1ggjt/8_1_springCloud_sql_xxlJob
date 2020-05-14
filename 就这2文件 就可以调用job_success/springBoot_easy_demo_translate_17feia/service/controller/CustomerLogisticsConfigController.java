package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.order.dto.logistics.CustomerLogisticsConfigDto;
import com.yangshan.eship.order.entity.logistics.CustomerLogisticsConfig;
import com.yangshan.eship.order.service.logistics.CustomerLogisticsConfigServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: LinYun
 * @Description:
 * @date: 17:40 2018/7/17
 * Modified By:
 */
@RestController
@RequestMapping(Version.VERSION + "/customerLogisticsConfig")
public class CustomerLogisticsConfigController {

    @Autowired
    private CustomerLogisticsConfigServiceI customerLogisticsConfigService;

    /**
     * 获取用户的派送商定制信息
     *
     * @param configDto
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public DataGrid<CustomerLogisticsConfigDto> list(CustomerLogisticsConfigDto configDto) {
        return customerLogisticsConfigService.findAll(configDto);
    }

    /**
     * 新增或修改用户的派送商定制信息
     *
     * @param configDto
     * @return
     */
    @RequestMapping(value = "/submit", method = RequestMethod.POST)
    public void add(@RequestBody CustomerLogisticsConfigDto configDto) {
        if (configDto != null) {
            if (StringUtils.isEmpty(configDto.getId())) {
                // id为空表示新增，需要排除同一客户、同一产品、同一国家重复设置
                CustomerLogisticsConfig customerLogisticsConfig = customerLogisticsConfigService.findByCustomerIdAndProductIdAndCountryCode(configDto);
                if (customerLogisticsConfig != null) {
                    // 存在相同的记录则更新
                    configDto.setId(customerLogisticsConfig.getId());
                    customerLogisticsConfigService.update(configDto);
                }else {
                    //查询不到新增
                    customerLogisticsConfigService.save(configDto);
                }
            }else {
                //id有值则更新
                customerLogisticsConfigService.update(configDto);
            }
        }
    }


    /**
     * 删除用户的派送商定制信息
     *
     * @param configDto
     * @return
     */
    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public void delete(@RequestBody CustomerLogisticsConfigDto configDto) {
        customerLogisticsConfigService.delete(configDto);
    }
}
