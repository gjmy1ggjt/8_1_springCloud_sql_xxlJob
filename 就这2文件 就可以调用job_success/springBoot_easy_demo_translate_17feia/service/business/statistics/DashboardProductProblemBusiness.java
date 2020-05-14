package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardParamType;
import com.yangshan.eship.sales.business.statistics.dto.dashboard.DashboardSeachKey;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import com.yangshan.eship.sales.utils.DashboardUtil;
import com.yangshan.eship.sales.vo.DashboardVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: hyl
 * @Date: 2018/12/28 9:31
 * @Description:
 */
@Service
public class DashboardProductProblemBusiness implements DashboardDrawBusinessI {

    private static final Logger logger = LoggerFactory.getLogger(DashboardProductProblemBusiness.class);

    @Value("${dashboard.statistics.url}")
    private String url;
    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Autowired
    private ProductServiceI productService;

    @Override
    public DashboardVO drawDashboard(DashboardVO vo) {
        List<DashboardParamType> types = new ArrayList<>();
        types.add(DashboardParamType.COMPANY);
        types.add(DashboardParamType.TIMESLOT);
        types.add(DashboardParamType.PRODUCT);
        Map<String, Object> map = DashboardUtil.initParams(vo, types, orgWarehouseService, productService);
        String prodId = vo.getParams().get(DashboardParamType.PRODUCT.name()).getSeachValue();
        if (StringUtils.isNotEmpty(prodId)) {
            Product product = productService.findById(prodId);
            map.put(DashboardSeachKey.PRODUCTNAME.name(), product.getName());
        }


        Map jsonMap = DashboardUtil.getDrawDashboardMap(map, url + "/view/productProblemData");
        vo.setData(jsonMap);
        return vo;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String[]{StatisticsDrawType.PRODUCTMODULE, "产品问题件统计图"};
    }
}
