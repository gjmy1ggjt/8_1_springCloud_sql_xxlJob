package com.yangshan.eship.sales.service.sales;

import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.entity.BaseEntity;
import com.yangshan.eship.common.utils.EshipBeanUtils;
import com.yangshan.eship.sales.dto.WarehouseAddressRequestVO;
import com.yangshan.eship.sales.dto.WarehouseAddressResponseVO;
import com.yangshan.eship.sales.entity.address.AddressType;
import com.yangshan.eship.sales.entity.address.WarehouseAddress;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.repository.sale.WarehouseAddressDao;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: Yifan
 * @Date: 2019/12/12 15:06
 * @Description:
 */
@Service
@Transactional
public class WarehouseAddressService implements WarehouseAddressServiceI {
    @Autowired
    private WarehouseAddressDao warehouseAddressDao;
    @Autowired
    private RegionServiceI regionServiceI;
    @Autowired
    private OrgWarehouseServiceI orgWarehouseServiceI;

    @Override
    public String save(WarehouseAddress warehouseAddress) {
        if (AddressType.ENABLE.equals(warehouseAddress.getAddressType())) {
            List<WarehouseAddress> warehouseAddressList = warehouseAddressDao.findByOrgWarehouseIdAndAddressType(warehouseAddress.getOrgWarehouseId(), AddressType.ENABLE);
            if (warehouseAddressList.size() > 0) {
                return "分公司有已启用的仓库地址";
            }
        }

        if (StringUtils.isNotBlank(warehouseAddress.getId())) {
            WarehouseAddress warehouseAddressDB = warehouseAddressDao.findOne(warehouseAddress.getId());
            BeanUtils.copyProperties(warehouseAddress, warehouseAddressDB, BaseEntity.toIgnoreProperties());

            warehouseAddressDao.save(warehouseAddressDB);
        } else {
            warehouseAddressDao.save(warehouseAddress);
        }

        return "";
    }

    @Override
    public WarehouseAddressResponseVO getById(String id) {
        WarehouseAddress warehouseAddress = warehouseAddressDao.getOne(id);

        WarehouseAddressResponseVO warehouseAddressResponseVO = new WarehouseAddressResponseVO();
        BeanUtils.copyProperties(warehouseAddress, warehouseAddressResponseVO);

        return warehouseAddressResponseVO;
    }

    @Override
    public void deleteById(String id) {
        warehouseAddressDao.delete(id);
    }

    @Override
    public DataGrid<WarehouseAddressResponseVO> list(WarehouseAddressRequestVO warehouseAddressRequestVO) {
        DataGrid<WarehouseAddressResponseVO> warehouseAddressResponseVODataGrid = new DataGrid<WarehouseAddressResponseVO>();

        DataGrid<WarehouseAddress> warehouseAddressDataGrid = warehouseAddressDao.list(warehouseAddressRequestVO);

        List<WarehouseAddress> warehouseAddressList = warehouseAddressDataGrid.getRows();
        List<WarehouseAddressResponseVO> warehouseAddressResponseVOList = EshipBeanUtils.copyPojos2Dtos(warehouseAddressList, WarehouseAddressResponseVO.class);

        DataGrid<OrgWarehouse> dataGrid = orgWarehouseServiceI.list(warehouseAddressRequestVO.getOrganizationId(), null);
        List<OrgWarehouse> orgWarehouseList = dataGrid.getRows();
        Map<String, String> orgWarehouseMap = orgWarehouseList.stream().collect(Collectors.toMap(OrgWarehouse::getId, s -> s.getName()));

        Map<String, String> regions = regionServiceI.findOriginList();

        warehouseAddressResponseVOList.forEach(warehouseAddressResponseVO -> {
            warehouseAddressResponseVO.setAddressTypeStr(warehouseAddressResponseVO.getAddressType().getDesc());
            warehouseAddressResponseVO.setOrgWarehouseName(orgWarehouseMap.get(warehouseAddressResponseVO.getOrgWarehouseId()));
            warehouseAddressResponseVO.setRegionName(regions.get(warehouseAddressResponseVO.getRegionCode()));
        });

        warehouseAddressResponseVODataGrid.setRows(warehouseAddressResponseVOList);
        warehouseAddressResponseVODataGrid.setTotal(warehouseAddressDataGrid.getTotal());

        return warehouseAddressResponseVODataGrid;
    }
}