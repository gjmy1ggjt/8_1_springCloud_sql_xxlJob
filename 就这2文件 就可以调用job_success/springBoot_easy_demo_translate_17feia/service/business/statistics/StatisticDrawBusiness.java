package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;
import com.yangshan.eship.sales.entity.statistic.StatictisDrawToRole;
import com.yangshan.eship.sales.service.statistic.StatictisDrawToRoleServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询首页需显示的统计图表与快捷通道
 *
 * @Author: tsding
 * @Date: 2018/6/6 11:40
 * @Description:
 */
@Service
public class StatisticDrawBusiness {
    @Autowired
    private StatictisDrawToRoleServiceI statictisDrawToRoleService;

    @Autowired
    private List<OneStatisticsDrawBusinessI> oneStatisticsDrawBusinessList;

    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) throws ParseException {
        StatisticsDrawDto dto = null;
        OneStatisticsDrawBusinessI statisticsDrawBusiness = oneStatisticsDrawBusinessList.stream()
                .filter(ins -> ins.getDrawBusinessName()[1].equalsIgnoreCase(statisticsDrawsDto.getDrawBusinessName()))
                .findFirst().orElse(null);

        if (statisticsDrawBusiness != null) {
            dto = statisticsDrawBusiness.draw(statisticsDrawsDto);
            String[] businessName = statisticsDrawBusiness.getDrawBusinessName();
            if(businessName[0].equals(statisticsDrawsDto.getModul()) && dto.getDetailUrl() != null && !"".equals(dto.getDetailUrl())){
                dto.setShowDetail(true);
            }else{
                dto.setShowDetail(false);
            }
        }

        return dto;
    }

    public List<Object[]> getDrawBusinessNames(String roleCode,String organizationId) {
        List<String[]> rawBusinessNames = Lists.newArrayList();
        oneStatisticsDrawBusinessList.forEach(ins -> rawBusinessNames.add(ins.getDrawBusinessName()));
        List<StatictisDrawToRole> statictisDrawToRoles = statictisDrawToRoleService.findByRoleCode("statistic",roleCode,organizationId);
        List<Object[]> returnData = Lists.newArrayList();
        for(String[] names : rawBusinessNames){
            Object[] returns = new Object[3];
            returns[0] = names[0];returns[1] = names[1];
            boolean isSelect = false;
            for(StatictisDrawToRole sdt : statictisDrawToRoles){
                if(sdt.getDrawServiceName().equals(names[1])){
                    isSelect = true;
                    break;
                }
            }
            returns[2]=isSelect;
            returnData.add(returns);
        }

        return returnData;
    }
}
