package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawEnum;
import com.yangshan.eship.sales.entity.statistic.StatictisDrawToRole;
import com.yangshan.eship.sales.service.statistic.StatictisDrawToRoleServiceI;
import com.yangshan.eship.sales.vo.DashboardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardDrawBusiness {

    @Autowired
    private StatictisDrawToRoleServiceI statictisDrawToRoleService;

    @Autowired
    private List<DashboardDrawBusinessI> dashboardDrawBusinessList;

    public DashboardVO drawDashboard(DashboardVO vo) {
        DashboardVO dto = null;
        DashboardDrawBusinessI statisticsDrawBusiness = dashboardDrawBusinessList.stream()
                .filter(ins -> ins.getDrawBusinessName()[1].equalsIgnoreCase(vo.getDrawBusinessName()))
                .findFirst().orElse(null);

        if (statisticsDrawBusiness != null) {
            dto = statisticsDrawBusiness.drawDashboard(vo);
        }

        return dto;
    }


    public Map<String, List<Object[]>> getDrawBusinessNames(String roleCode, String organizationId) {
        Map<String, List<Object[]>> returnData = new HashMap<String, List<Object[]>>();

        List<StatictisDrawToRole> statictisDrawToRoles = statictisDrawToRoleService.findByRoleCodeAndOrganizationId(roleCode, organizationId);

        List<String[]> rawBusinessNames = Lists.newArrayList();
        dashboardDrawBusinessList.forEach(ins -> rawBusinessNames.add(ins.getDrawBusinessName()));
        System.out.println(JSON.toJSON(rawBusinessNames));
        for (String[] names : rawBusinessNames) {
            Object[] returns = new Object[3];
            returns[0] = names[0];
            returns[1] = names[1];
            boolean isSelect = false;
            if (statictisDrawToRoles.stream().anyMatch(statictisDrawToRole -> statictisDrawToRole.getDrawServiceName().equals(names[1]))) {
                isSelect = true;
            }
            returns[2] = isSelect;

            //update by yifan 2019-07-17
            String descCN = StatisticsDrawEnum.valueOf(String.valueOf(returns[0])).getDesc();
            if (returnData.containsKey(descCN)) {
                returnData.get(descCN).add(returns);
            } else {
                List<Object[]> list = new ArrayList<Object[]>();
                list.add(returns);
                returnData.put(descCN, list);
            }
        }

        return returnData;
    }
}
