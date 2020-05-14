package com.yangshan.eship.sales.business.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.StringUtil;
import com.yangshan.eship.finance.service.fina.FinanceOrderItemStaticsServiceI;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import com.yangshan.eship.sales.business.statistics.dto.DrawItem;
import com.yangshan.eship.sales.business.statistics.dto.StatisticParamDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawDto;
import com.yangshan.eship.sales.business.statistics.dto.StatisticsDrawType;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**
 * product模块的 国家金额信息统计((单柱图)
 * @author:  TuShiDing
 * @date: 2018/06/08 11:12:00
 */
@Service
public class ProductStatisticDrawCylinderBusiness implements OneStatisticsDrawBusinessI {
    @Autowired
    private ProductServiceI productServiceI;

    @Autowired
    private FinanceOrderItemStaticsServiceI financeOrderItemStaticsServiceI;

    @Autowired
    RoleServiceI roleService;

    @Autowired
    CustomerAssignServiceI customerAssignService;

    @Override
    public StatisticsDrawDto draw(StatisticsDrawDto statisticsDrawsDto) {

        StatisticsDrawDto statisticsDrawDto = new StatisticsDrawDto();
        //设置图标类型
        statisticsDrawDto.setDrawType(StatisticsDrawType.DRAWCYLINEDER);
        statisticsDrawDto.setSearchParamId(statisticsDrawsDto.getSearchParamId());
        if(StringUtils.isEmpty(statisticsDrawsDto.getSearchParamId())) {
            //查询参数设置下拉列表
            List<Product> productList = productServiceI.list(SessionUtils.getOrganizationId());
            statisticsDrawDto.setParamTitle("产品列表");
            List<StatisticParamDto> statisticParamDtos = new ArrayList<>();
            StatisticParamDto statisticParamDtoAll = new StatisticParamDto();
            statisticParamDtoAll.setParamId("0");
            statisticParamDtoAll.setParamName("所有产品");
            statisticParamDtos.add(statisticParamDtoAll);
            int index =0;
            for(Product p:productList) {
                if(index==0){
                    statisticsDrawDto.setSearchParamId(p.getId());
                    statisticsDrawsDto.setSearchParamId(p.getId());
                    index++;
                }
                StatisticParamDto statisticParamDto = new StatisticParamDto();
                statisticParamDto.setParamId(p.getId());
                statisticParamDto.setParamName(p.getName()+"-"+p.getPCode());
                statisticParamDtos.add(statisticParamDto);
            }
            statisticsDrawDto.setParamsSelect(statisticParamDtos);
        }
        if("0".equals(statisticsDrawsDto.getSearchParamId())){
            statisticsDrawsDto.setSearchParamId("");
        }

        //数据start
        String staffId = SessionUtils.getUserId();
        String orgId = SessionUtils.getOrganizationId();
        String warehouseId = SessionUtils.getWarehouseId();
        List<Role> allRolesByStaffId = roleService.findAllRolesByStaffId(staffId);
        List<String> customerIds = customerAssignService.findByUserRoleCode(allRolesByStaffId, staffId, orgId, warehouseId);

        List<Object[]> result = financeOrderItemStaticsServiceI.findDestinationInfoChartData(customerIds,statisticsDrawsDto.getSearchParamId(),statisticsDrawsDto.getSearchStartDateTime(),statisticsDrawsDto.getSearchEndDateTime());
        List<DrawItem> drawItems = new ArrayList<>();
        DecimalFormat decimalFormat=new DecimalFormat(".00");
        for(int i=0;i<result.size();i++){
            DrawItem item = new DrawItem();
            item.setName(String.valueOf(result.get(i)[0]));
            item.setValue(decimalFormat.format(result.get(i)[1]));
            drawItems.add(item);
        }
        statisticsDrawDto.setDrawPieAndCylinderItems(drawItems);
        //数据end


        return statisticsDrawDto;
    }

    @Override
    public String[] getDrawBusinessName() {
        return new String []{StatisticsDrawType.PRODUCTMODULE,"单个产品-国家-总金额"};
    }

}
