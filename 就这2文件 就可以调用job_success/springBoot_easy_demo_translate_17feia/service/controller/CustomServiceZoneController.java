package com.yangshan.eship.sales.controller;


import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson.JSONArray;
import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.entity.serv.CustomServiceZone;
import com.yangshan.eship.sales.entity.serv.CustomServiceZoneItem;
import com.yangshan.eship.sales.service.serv.CustomServiceZoneServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 客户分配
 *
 * @author: Kee.Li
 * @date: 2017/10/31 15:47
 */
@RestController
@RequestMapping(Version.VERSION + "/customerServiceZone")
public class CustomServiceZoneController {

    @Autowired
    private CustomServiceZoneServiceI customServiceZoneService;

    @Autowired
    private RegionServiceI regionService;

    /**
     * 查询语种区域
     *
     * @param customServiceZone
     * @return
     * @author: Kee.Li
     * @date: 2017/11/2 17:35
     */
    @RequestMapping(method = RequestMethod.GET)
    public DataGrid<CustomServiceZone> list(CustomServiceZone customServiceZone) {

        setOrgIdAndCsmId(customServiceZone);

        return customServiceZoneService.list(customServiceZone);

    }

    /**
     * 设置组织id和客服经理id
     * @param
     * @author Kee.Li
     * @date 2017/12/12 14:17
     */
    private void setOrgIdAndCsmId(CustomServiceZone customServiceZone) {
        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        customServiceZone.setOrganizationId(organizationId);
        String customServiceManagerId = SessionUtils.getUserId();
        customServiceZone.setCustomServiceManagerId(customServiceManagerId);
        customServiceZone.setWarehouseId(SessionUtils.getWarehouseId());
    }

    /**
     * 根据id查询
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/11/3 17:07
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public DataGrid<CustomServiceZone> findOne(@PathVariable("id") String id) {

        CustomServiceZone customServiceZone = customServiceZoneService.findById(id);

        DataGrid<CustomServiceZone> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        dataGrid.setTotal(1);
        dataGrid.setObj(customServiceZone);

        return dataGrid;
    }

    /**
     * 保存区域
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/11/3 9:59
     */
    @RequestMapping(method = RequestMethod.POST)
    public DataGrid<CustomServiceZone> save(@RequestBody CustomServiceZone customServiceZone) {

//        CustomServiceZone cszdb = customServiceZoneService.saveOrUpdate(customServiceZone);

        // 从session中获取组织id
        setOrgIdAndCsmId(customServiceZone);

        //先从把所选的国家编号查询出来，封装成item
        String country = customServiceZone.getCountry();
        if (StringUtils.isNotBlank(country)) {
            String[] countryNos = country.split(",");
            List<Region> regions = regionService.findDestByRegionCodes(Lists.newArrayList(countryNos));

            Set<CustomServiceZoneItem> customServiceZoneItems = new HashSet<>();
            for (Region region : regions) {
                CustomServiceZoneItem item = new CustomServiceZoneItem();
//                item.setCustomServiceZone(customServiceZone);
                item.setDestinationCountryNo(region.getRegionCode());
                item.setDestinationCountryName(region.getRegionName());
                item.setOrganizationId(customServiceZone.getOrganizationId());
                customServiceZoneItems.add(item);
            }
//            customServiceZone.setCustomServiceZoneItems(customServiceZoneItems);
            customServiceZone.setCountry(JSONArray.toJSONString(customServiceZoneItems));
        }

        customServiceZoneService.saveOrUpdate(customServiceZone);

        DataGrid<CustomServiceZone> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        dataGrid.setTotal(0);

        return dataGrid;
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public DataGrid<CustomServiceZone> delete(@PathVariable("id") String customServiceZoneId) {

        customServiceZoneService.delete(customServiceZoneId);

        DataGrid<CustomServiceZone> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        dataGrid.setTotal(0);

        return dataGrid;
    }

    /**
     * 查询某组织，某客服经理的语种区域
     *
     * @param
     * @return
     * @author: Kee.Li
     * @date: 2017/11/3 15:49
     */
    @RequestMapping(value = "/listItems", method = RequestMethod.GET)
    public DataGrid<CustomServiceZoneItem> listItems(String organizationId) {

        // 从session中获取组织id
        String orgId = SessionUtils.getOrganizationId();
        String customServiceManagerId = SessionUtils.getUserId();
        //需要添加查询条件
        List<CustomServiceZoneItem> items = customServiceZoneService.listItems(orgId,customServiceManagerId);

        DataGrid<CustomServiceZoneItem> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        if (items != null) {
            dataGrid.setTotal(items.size());
            dataGrid.setRows(items);
        } else {
            dataGrid.setTotal(0);
        }
        return dataGrid;
    }

}
