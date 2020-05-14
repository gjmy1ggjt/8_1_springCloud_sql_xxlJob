package com.yangshan.eship.sales.business.statistics;

import com.yangshan.eship.sales.vo.DashboardVO;

public interface DashboardDrawBusinessI {

    DashboardVO drawDashboard(DashboardVO vo);

    //返回元素0：模块名， 1：统计图标识title
    String[] getDrawBusinessName();
}
