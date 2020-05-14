package com.yangshan.eship.sales.service.serv;

import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson.JSONArray;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.sales.entity.serv.CustomServiceZone;
import com.yangshan.eship.sales.entity.serv.CustomServiceZoneItem;
import com.yangshan.eship.sales.repository.serv.CustomServiceAssignDao;
import com.yangshan.eship.sales.repository.serv.CustomServiceZoneDao;
import com.yangshan.eship.sales.repository.serv.CustomServiceZoneItemDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class CustomServiceZoneService implements CustomServiceZoneServiceI {

    @Autowired
    private CustomServiceZoneDao customServiceZoneDao;

    @Autowired
    private CustomServiceZoneItemDao customServiceZoneItemDao;

    @Autowired
    private CustomServiceAssignDao customServiceAssignDao;

    @Override
    public DataGrid<CustomServiceZone> list(CustomServiceZone customServiceZone) {

        DataGrid<CustomServiceZone> dataGrid = customServiceZoneDao.list(customServiceZone);
        if(dataGrid.getTotal() > 0){
            for(CustomServiceZone csz : dataGrid.getRows()){
                Set<CustomServiceZoneItem> zoneItems =  csz.getCustomServiceZoneItems();
                if(zoneItems == null || zoneItems.isEmpty()){
                    csz.setCountry("");
                    continue;
                }
                StringBuilder countryShow = new StringBuilder();
                for (CustomServiceZoneItem item : zoneItems){
                    countryShow.append(item.getDestinationCountryName()).append(",");
                }
                if(countryShow.length() > 0){
                    csz.setCountry(countryShow.substring(0,countryShow.length()-1));
                }
            }
        }

        return dataGrid;
    }

    @Override
    public CustomServiceZone saveOrUpdate(CustomServiceZone customServiceZone) {

        if(StringUtils.isNotBlank(customServiceZone.getId())){
            //更新
            CustomServiceZone cszdb = customServiceZoneDao.findOne(customServiceZone.getId());

            //删除items
             customServiceZoneItemDao.delete(cszdb.getCustomServiceZoneItems());

            //更新并保存item
            List<CustomServiceZoneItem> itemList = JSONArray.parseArray(customServiceZone.getCountry(),CustomServiceZoneItem.class);
            BeanUtils.copyProperties(customServiceZone,cszdb);
            if(itemList != null) {
                for (CustomServiceZoneItem customServiceZoneItem : itemList) {
                    customServiceZoneItem.setCustomServiceZone(cszdb);
                }
                cszdb.setCustomServiceZoneItems(new HashSet<>(itemList));
            }

            return customServiceZoneDao.save(cszdb);
        }else{
            //保存
            List<CustomServiceZoneItem> itemList = JSONArray.parseArray(customServiceZone.getCountry(),CustomServiceZoneItem.class);
            if(itemList != null) {
                for (CustomServiceZoneItem customServiceZoneItem : itemList) {
                    customServiceZoneItem.setCustomServiceZone(customServiceZone);
                }
                customServiceZone.setCustomServiceZoneItems(new HashSet<>(itemList));
            }
            return customServiceZoneDao.save(customServiceZone);
        }
    }

    @Override
    public void delete(String customServiceZoneId) {
        CustomServiceZone cszdb = customServiceZoneDao.findOne(customServiceZoneId);
        customServiceZoneDao.delete(cszdb);

        //清空已分配给客服的信息
        customServiceAssignDao.cancelAssign(cszdb.getId());
    }

    @Override
    public List<CustomServiceZoneItem> listItems(String organizationId,String customServiceManagerId) {
        return customServiceZoneItemDao.findCsmZoneItems(organizationId,customServiceManagerId).getRows();
    }

    @Override
    public CustomServiceZone findById(String id) {

        CustomServiceZone customServiceZone = customServiceZoneDao.findOne(id);

        Set<CustomServiceZoneItem> zoneItems =  customServiceZone.getCustomServiceZoneItems();
        if(zoneItems != null) {
            StringBuilder countryShow = new StringBuilder();
            for (CustomServiceZoneItem item : zoneItems) {
                countryShow.append(item.getDestinationCountryNo()).append(",");
            }
            if (countryShow.length() > 0) {
                customServiceZone.setCountry(countryShow.substring(0, countryShow.length() - 1));
            }
        }

        return customServiceZone;
    }

    @Override
    public List<CustomServiceZone> listByIds(List<String> ids) {
        ids = QueryPlanCacheWrapper.wrapper(ids,"CustomServiceZoneService.listByIds.ids");
        return customServiceZoneDao.listByIds(ids);
    }
}