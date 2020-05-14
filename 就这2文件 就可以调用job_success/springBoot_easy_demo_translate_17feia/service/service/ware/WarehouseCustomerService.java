package com.yangshan.eship.sales.service.ware;

import org.springframework.stereotype.Service;
import com.yangshan.eship.sales.entity.ware.WarehouseCustomer;
import com.yangshan.eship.sales.repository.ware.WarehouseCustomerDao;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;

/**
 * 分公司对应账号管理
 *
 * @author: Kee.Li
 * @date: 2017/10/19 16:15
 */
@Service
@Transactional
public class WarehouseCustomerService implements WarehouseCustomerServiceI{
    @Autowired
    private WarehouseCustomerDao warehouseCustomerDao;
    @Override
    public WarehouseCustomer findOneWarehouseCustomerByWarehouseId(String srcWarehouseId, String targetWarehouseId) {
        return warehouseCustomerDao.findOneWarehouseCustomerByWarehouseId(srcWarehouseId,targetWarehouseId);
    }
}