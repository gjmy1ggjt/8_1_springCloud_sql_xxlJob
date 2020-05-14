package com.yangshan.eship.sales.controller;

import com.google.common.collect.Lists;
import com.yangshan.eship.common.rabbit.RabbitMessageQueueKeyConstant;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.syst.LogisticsSupplierItem;
import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.syst.RabbitMessageServiceI;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.rabbit.RabbitMessage;
import com.yangshan.eship.common.rabbit.RabbitMessageActionsConstant;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.order.dto.labelex.ImporterAddressDto;
import com.yangshan.eship.order.dto.labelex.JapanShipperAddressDto;
import com.yangshan.eship.order.dto.labelex.LabelHandleDto;
import com.yangshan.eship.order.dto.labelex.LabelHandleGoodsDto;
import com.yangshan.eship.order.entity.exlabel.ExLabelWorkingTable;
import com.yangshan.eship.order.entity.orde.DeliveryAddress;
import com.yangshan.eship.order.service.orde.DeliveryAddressServiceI;
import com.yangshan.eship.sales.business.LabelHandleBusiness;
import com.yangshan.eship.sales.entity.label.LabelModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Author: hyl
 * @Date: 2018/3/5 14:03
 * @Description:
 */
@RestController
@RequestMapping(Version.VERSION + "/labelHandle")
@Api(value = "LabelHandleController", tags = "获取派送贴接口")
public class LabelHandleController {
    private static Logger logger = LoggerFactory.getLogger(LabelHandleController.class);

    @Autowired
    private LabelHandleBusiness labelHandleBusiness;

    @Autowired
    private RabbitMessageServiceI rabbitMessageService;

    @Autowired
    private DeliveryAddressServiceI deliveryAddressService;

    @Autowired
    private RegionServiceI regionService;

    @Value("${static.file.server}")
    private String staticFileServer;

    /**
     * 获取问题标签处理订单信息对象
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:43
     * @Description:
     */
    @RequestMapping(value = "/getOrder", method = RequestMethod.GET)
    @ApiOperation(value = "获取问题标签订单信息", notes = "by hyl", httpMethod = "GET")
    public DataGrid<LabelHandleDto> getOrder(String insideNumber) {
        //根据内单号获取订单信息
        return labelHandleBusiness.getOrder(insideNumber.trim());
    }

    //获取偏远和非偏远
    @RequestMapping(value = "/remoteSurchargeCategory", method = RequestMethod.GET)
    @ApiOperation(value = "获取偏远和非偏远", notes = "by cy", httpMethod = "GET")
    public Map<String, List<LogisticsSupplierItem>> remoteSurchargeCategory(String insideNumber, String postCode) {
        return labelHandleBusiness.remoteSurchargeCategory(insideNumber.trim(), postCode);
    }


    /**
     * 更新订单信息
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:43
     * @Description:
     */
    @RequestMapping(value = "/update", method = RequestMethod.POST)
    @ApiOperation(value = "获取派送标签", notes = "by hyl", httpMethod = "POST")
    public DataGrid<LabelHandleDto> update(@RequestBody LabelHandleDto labelHandleDto) {
        if (labelHandleDto.getLabelHandleDeliveryAddressDto().getCountry().contains("JAPAN")
                || labelHandleDto.getLabelHandleDeliveryAddressDto().getCountry().contains("日本")) {
            labelHandleBusiness.updateGoods(labelHandleDto.getLabelHandleGoodsDto().get(0));
        }
        //获取派送地址
        DeliveryAddress deliveryAddress = deliveryAddressService.findByOrderId(labelHandleDto.getOrderId());

        //@Author:hyl @Date:2019/5/10 @Description:客服-获取标签：需要客户修改国家，同步修改收件人的二字编码
        if(!deliveryAddress.getCountry().equals(labelHandleDto.getLabelHandleDeliveryAddressDto().getCountry())){

            Region region = regionService.findDestination(labelHandleDto.getLabelHandleDeliveryAddressDto().getCountry());
            if(region != null){
                //更新收件人国家信息
                deliveryAddressService.updateCountry(labelHandleDto.getOrderId(),region.getRegionName(),region.getRegionNameEn(),region.getRegionCodeTwo());
                labelHandleDto.getLabelHandleDeliveryAddressDto().setCountryCode(region.getRegionCodeTwo().toUpperCase());
                labelHandleDto.getLabelHandleDeliveryAddressDto().setCountry(region.getRegionNameEn().toUpperCase());
            }
        }

        //更新派送商信息
        labelHandleBusiness.updateLogisticsSupplier(labelHandleDto);
        List<String> orderIds = Lists.newArrayList(labelHandleDto.getOrderId());
        //更改派送商后发送消息进行后续处理
        RabbitMessage rabbitMessage = new RabbitMessage(
                RabbitMessageActionsConstant.CHANGE_LOGISTICS_SUPPLIER,
                SessionUtils.getUserId(),
                orderIds
        );
        rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.ORDER_SERVICE_QUEUEKEY,rabbitMessage);

        //更新订单信息
        labelHandleBusiness.update(labelHandleDto);

        return getOrder(labelHandleDto.getInsideNumber());

    }

    /**
     * 获取标签
     *
     * @Author: hyl
     * @Date: 2018/3/9 14:42
     * @Description:
     */
    @JsonFilter(type = LabelModel.class, include = "apiErrorMsg")
    @RequestMapping(value = "/syncGeneratePdf", method = RequestMethod.GET)
    @ApiOperation(value = "获取标签", notes = "by hyl", httpMethod = "GET")
    public ExLabelWorkingTable syncGeneratePdf(String orderId, String importerAddress) {
        ExLabelWorkingTable exLabelWorkingTable = labelHandleBusiness.syncGeneratePdf(orderId, importerAddress);
        exLabelWorkingTable.setPdfPath(staticFileServer + exLabelWorkingTable.getPdfPath());
        return exLabelWorkingTable;
    }


    /**
     * 更新发件人地址
     *
     * @Author: hyl
     * @Date: 2018/3/9 14:42
     * @Description:
     */
    @RequestMapping(value = "/updateJapanShipperAddress", method = RequestMethod.POST)
    public DataGrid<JapanShipperAddressDto> updateJapanShipperAddress(@RequestBody JapanShipperAddressDto japanShipperAddressDto) {
        DataGrid dataGrid = new DataGrid();
        JapanShipperAddressDto japanShipperAddressDtoDone = labelHandleBusiness.updateJapanShipperAddress(japanShipperAddressDto);
        dataGrid.setObj(japanShipperAddressDtoDone);
        dataGrid.setFlag(japanShipperAddressDtoDone != null);
        return dataGrid;
    }

    /**
     * @Author: hyl
     * @Date: 2018/3/9 14:42
     * @Description:更新进口商地址
     */
    @RequestMapping(value = "/updateImporterAddress", method = RequestMethod.POST)
    public DataGrid<JapanShipperAddressDto> updateImporterAddress(@RequestBody ImporterAddressDto importerAddressDto) {
        DataGrid dataGrid = new DataGrid();
        ImporterAddressDto importerAddressDtoDone = labelHandleBusiness.updateImporterAddress(importerAddressDto);
        dataGrid.setObj(importerAddressDtoDone);
        dataGrid.setFlag(importerAddressDtoDone != null);
        return dataGrid;
    }


    /**
     * 更新品名
     *
     * @Author: hyl
     * @Date: 2018/3/9 14:42
     * @Description:
     */
    @RequestMapping(value = "/updateGoods", method = RequestMethod.POST)
    @ApiOperation(value = "更新品名", notes = "by hyl", httpMethod = "POST")
    public DataGrid<LabelHandleGoodsDto> updateGoods(@RequestBody LabelHandleGoodsDto labelHandleGoodsDto) {
        DataGrid dataGrid = new DataGrid();
        LabelHandleGoodsDto labelHandleGoodsDtoDone = labelHandleBusiness.updateGoods(labelHandleGoodsDto);
        dataGrid.setObj(labelHandleGoodsDtoDone);
        dataGrid.setFlag(labelHandleGoodsDtoDone != null);
        return dataGrid;
    }
}
