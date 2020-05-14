package com.yangshan.eship.sales.service.sales;

import com.google.common.collect.Lists;
import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.entity.BaseEntity;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.EshipBeanUtils;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import com.yangshan.eship.sales.dto.VisitRecordRequestVO;
import com.yangshan.eship.sales.dto.VisitRecordResponseVO;
import com.yangshan.eship.sales.entity.visit.FrequencyType;
import com.yangshan.eship.sales.entity.visit.VisitRecord;
import com.yangshan.eship.sales.repository.sale.VisitRecordDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
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
public class VisitRecordService implements VisitRecordServiceI {
    @Autowired
    private VisitRecordDao visitRecordDao;
    @Autowired
    private RegionServiceI regionServiceI;
    @Autowired
    private ProductServiceI productServiceI;

    @Override
    public void save(VisitRecordRequestVO visitRecordRequestVO) {
        VisitRecord visitRecord = new VisitRecord();
        BeanUtils.copyProperties(visitRecordRequestVO, visitRecord, BaseEntity.toIgnoreProperties());
        visitRecord.setCountryCodes(String.join(",", visitRecordRequestVO.getCountryCodes()));
        visitRecord.setProductIds(visitRecordRequestVO.getProductIds() != null && visitRecordRequestVO.getProductIds().size() > 0 ? String.join(",", visitRecordRequestVO.getProductIds()) : null);
        visitRecord.setFrequency(StringUtils.isNotBlank(visitRecordRequestVO.getFrequency()) ? FrequencyType.valueOf(visitRecordRequestVO.getFrequency()) : null);

        if (StringUtils.isNotBlank(visitRecordRequestVO.getId())) {
            VisitRecord visitRecordDB = visitRecordDao.findOne(visitRecordRequestVO.getId());
            BeanUtils.copyProperties(visitRecord, visitRecordDB, BaseEntity.toIgnoreProperties());

            visitRecordDao.save(visitRecordDB);
        } else {
            visitRecordDao.save(visitRecord);
        }
    }

    @Override
    public VisitRecordResponseVO getById(String id) {
        VisitRecord visitRecord = visitRecordDao.getOne(id);
        VisitRecordResponseVO visitRecordResponseVO = new VisitRecordResponseVO();
        BeanUtils.copyProperties(visitRecord, visitRecordResponseVO);

        return visitRecordResponseVO;
    }

    @Override
    public void deleteById(String id) {
        visitRecordDao.delete(id);
    }

    @Override
    public DataGrid<VisitRecordResponseVO> list(VisitRecordRequestVO visitRecordRequestVO) {
        DataGrid<VisitRecordResponseVO> visitRecordResponseVODataGrid = new DataGrid<VisitRecordResponseVO>();

        DataGrid<VisitRecord> visitRecordDataGrid = visitRecordDao.list(visitRecordRequestVO);
        List<VisitRecord> visitRecordList = visitRecordDataGrid.getRows();
        List<VisitRecordResponseVO> visitRecordResponseVOList = EshipBeanUtils.copyPojos2Dtos(visitRecordList, VisitRecordResponseVO.class);

        Map<String, String> regions = regionServiceI.findOriginList();
        List<Region> regionList = regionServiceI.findDestinations();
        Map<String, String> regionNameMap = regionList.stream().collect(Collectors.toMap(Region::getId, s -> s.getRegionName()));
        List<Product> productList = productServiceI.findAllOnlineProduct(SessionUtils.getOrganizationId());
        Map<String, String> productNameMap = productList.stream().collect(Collectors.toMap(Product::getId, s -> s.getName()));
        visitRecordList.stream().map(VisitRecord::getSalesManagerName).collect(Collectors.toList());

        visitRecordResponseVOList.forEach(visitRecordResponseVO -> {
            String regionCode = visitRecordResponseVO.getRegionCode();
            visitRecordResponseVO.setRegionName(regions.get(regionCode));

            List<String> countryNames = new ArrayList<String>();
            String countryCodes = visitRecordResponseVO.getCountryCodes();
            List<String> countryCodeList = Lists.newArrayList(countryCodes.split(","));
            countryCodeList.forEach(countryCode -> {
                countryNames.add(regionNameMap.get(countryCode));
            });
            visitRecordResponseVO.setCountryNames(String.join(",", countryNames));

            List<String> productNames = new ArrayList<String>();
            String productIds = visitRecordResponseVO.getProductIds();
            List<String> productIdList = StringUtils.isNotBlank(productIds) ? Lists.newArrayList(productIds.split(",")) : new ArrayList<>();
            productIdList.forEach(productId -> {
                productNames.add(productNameMap.get(productId));
            });
            visitRecordResponseVO.setProductNames(String.join(",", productNames));
            visitRecordResponseVO.setFrequencyStr(visitRecordResponseVO.getFrequency().getDesc());
        });

        visitRecordResponseVODataGrid.setRows(visitRecordResponseVOList);
        visitRecordResponseVODataGrid.setTotal(visitRecordDataGrid.getTotal());

        return visitRecordResponseVODataGrid;
    }
}