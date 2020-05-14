package com.yangshan.eship.sales.service.performance;

import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.sales.dto.PerformanceTargetRequestVO;
import com.yangshan.eship.sales.dto.PerformanceTargetResponseVO;
import com.yangshan.eship.sales.entity.performance.PerformanceTarget;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;

import java.util.List;

/**
 * @Author: Yifan
 * @Date: 2019/12/12 15:14
 * @Description:
 */
public interface PerformanceTargetServiceI {
    List<SalesStaffAssign> getSales(String userId);

    String save(PerformanceTarget performanceTarget);

    PerformanceTargetResponseVO getById(String id);

    void deleteById(String id);

    DataGrid<PerformanceTargetResponseVO> list(PerformanceTargetRequestVO performanceTargetRequestVO);

    Long countBySalesIdAndCreatedDate(String salesId);
}
 