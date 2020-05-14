package com.yangshan.eship.sales.service.sales;

import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.common.utils.ObjectUtils;
import com.yangshan.eship.sales.entity.performance.PerformanceAppraisal;
import com.yangshan.eship.sales.repository.sale.PerformanceAppraisalDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * @Author yuchao
 * @Description 绩效考核
 * @Date Administrator 2019/12/25
 * @Param
 */

@Service
@Transactional
public class PerformanceAppraisalService implements PerformanceAppraisalServiceI {

    @Autowired
    private PerformanceAppraisalDao performanceAppraisalDao;

    @Override
    public DataGrid<String> save(PerformanceAppraisal performanceAppraisal) {

        String year = DateUtil.format(new Date(), "yyyy");

        PerformanceAppraisal performanceAppraisalDb = performanceAppraisalDao.findByOrganizationIdAndYear(performanceAppraisal.getOrganizationId(), year);

        if (ObjectUtils.isNotEmpty(performanceAppraisalDb)) {

            performanceAppraisalDb.setGrossProfitPercentage(performanceAppraisal.getGrossProfitPercentage());

            performanceAppraisalDb.setHospitalityPercentage(performanceAppraisal.getHospitalityPercentage());

            performanceAppraisalDb.setNewCustomerPercentage(performanceAppraisal.getNewCustomerPercentage());

            performanceAppraisalDb.setNewProductPercentage(performanceAppraisal.getNewProductPercentage());

            performanceAppraisalDb.setTurnover(performanceAppraisal.getTurnover());

            performanceAppraisalDao.save(performanceAppraisalDb);

        } else {

            performanceAppraisal.setYear(year);

            performanceAppraisalDao.save(performanceAppraisal);
        }

        return new DataGrid<>(true, "保存成功");
    }
}
