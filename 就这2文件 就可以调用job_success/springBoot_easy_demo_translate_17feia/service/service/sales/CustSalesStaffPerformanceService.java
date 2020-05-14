package com.yangshan.eship.sales.service.sales;

import org.springframework.stereotype.Service;
import com.yangshan.eship.sales.entity.sale.CustSalesStaffPerformance;
import com.yangshan.eship.sales.repository.sale.SalesStaffPerformanceDao;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Service
public class CustSalesStaffPerformanceService implements  CustSalesStaffPerformanceServiceI {

    @Autowired
    private SalesStaffPerformanceDao staffOerfirnabceDao;

    @Override
    public void saveCustSalesStaffPerformance(CustSalesStaffPerformance custSalesStaffPerformance) {
        staffOerfirnabceDao.save(custSalesStaffPerformance);
    }

    @Override
    public List<CustSalesStaffPerformance> findCustSalesStaffPerformances(CustSalesStaffPerformance cust) {
        return staffOerfirnabceDao.findCustSalesStaffPerformances(cust.getSalesStaffId(),cust.getStatisticsYear(),cust.getStatisticsMonth());
    }
}