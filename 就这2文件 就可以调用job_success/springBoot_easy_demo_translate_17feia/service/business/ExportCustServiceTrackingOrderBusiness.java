package com.yangshan.eship.sales.business;

import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yangshan.eship.business.DownloadTaskBusiness;
import com.yangshan.eship.finance.service.export.DownloadTaskServiceI;
import com.yangshan.eship.order.dto.orde.OrderSearchDto;
import com.yangshan.eship.order.dto.orde.OrderSearchRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: Kevin
 * @Date: 2019-04-04 13:31
 * @Description: 客服-主单追踪-订单导出
 */
@Service
public class ExportCustServiceTrackingOrderBusiness extends DownloadTaskBusiness {
    @Autowired
    private AviaOrderBusiness aviaOrderBusiness;

    @Autowired
    private DownloadTaskServiceI downloadTaskService;

    @Override
    protected DownloadTaskServiceI getDownloadTaskService() {
        return downloadTaskService;
    }

    @Override
    protected String generateFile(String json) {
        String url ="";
        try {
            ObjectMapper mapper = new ObjectMapper();
            OrderSearchRequestDto orderSearchRequestDto = mapper.readValue(json, OrderSearchRequestDto.class);

            //url = orderSearchBusiness.exportCustServiceTrackingOrder(orderSearchDto);
            url = aviaOrderBusiness.exportCustServiceTrackingOrder(orderSearchRequestDto);
        }catch (Exception e){
            return e.getMessage();
        }
        return url;
    }
}
