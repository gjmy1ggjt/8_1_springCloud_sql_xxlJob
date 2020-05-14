package com.yangshan.eship.sales.controller;


import com.alibaba.fastjson.JSON;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.sales.dto.CancelRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.yangshan.eship.author.entity.syst.BookingParty;
import com.yangshan.eship.author.service.syst.BookingPartyServiceI;
import com.yangshan.eship.author.service.syst.RabbitMessageServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.rabbit.RabbitMessage;
import com.yangshan.eship.common.rabbit.RabbitMessageActionsConstant;
import com.yangshan.eship.common.rabbit.RabbitMessageQueueKeyConstant;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.order.dto.avia.AviaOrderInfoDto;
import com.yangshan.eship.order.dto.avia.SupplierServiceDto;
import com.yangshan.eship.order.dto.avia.SupplierServiceType;
import com.yangshan.eship.order.dto.orde.OrderSearchRequestDto;
import com.yangshan.eship.order.dto.orde.OrderSearchResponseDto;
import com.yangshan.eship.order.dto.orde.OrderStaticsResultObj;
import com.yangshan.eship.order.entity.avia.AviationBooking;
import com.yangshan.eship.order.entity.avia.AviationOrder;
import com.yangshan.eship.order.entity.orde.Order;
import com.yangshan.eship.order.service.avia.AviationBookingServiceI;
import com.yangshan.eship.order.service.avia.AviationOrderServiceI;
import com.yangshan.eship.sales.business.AviaOrderBusiness;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import com.yangshan.eship.sales.vo.ServiceTypeVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订舱管理
 */
@RestController("aviationBookingControllerInSalesLib")
@RequestMapping(Version.VERSION + "/aviationBooking")
@Api(value = "AviationBookingController", tags = "主单相关接口")
public class AviationBookingController {

    private static final Logger logger = LoggerFactory.getLogger(AviationBookingController.class);

    @Autowired
    private AviationBookingServiceI aviationBookingService;

    @Autowired
    private BookingPartyServiceI bookingPartyService;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Autowired
    private AviaOrderBusiness aviaOrderBusiness;

    @Autowired
    private AviationOrderServiceI aviationOrderService;

    @Autowired
    private RabbitMessageServiceI rabbitMessageService;

    /**
     * 查询tsd
     *
     * @param aviationBooking
     * @return
     */
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public DataGrid<AviationBooking> list(@RequestBody AviationBooking aviationBooking) {

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        aviationBooking.setOrganizationId(organizationId);

//        String customerServiceId = SessionUtils.getCustomerId();
//        List<OrgWarehouse> warehouses = orgWarehouseService.findCustomerServiceOrgWarehouses(organizationId,customerServiceId);
//        if(warehouses == null || warehouses.isEmpty()){
//            throw new EshipException("AVIATION_BOOKING_CUSTOMER_SERVICE_NOT_HAVA_WAREHOUSE","该客服所在地不存在仓库", false);
//        }
        aviationBooking.setWarehouseId(SessionUtils.getWarehouseId());

        DataGrid<AviationBooking> dataGrid = aviationBookingService.list(aviationBooking);

        List<BookingParty> bookingParties = bookingPartyService.findByOrgId(aviationBooking.getOrganizationId());

        // key:编号，value:名称
        Map<String, String> bookingPartyMap = new HashMap<>();
        if (bookingParties != null && !bookingParties.isEmpty()) {
            for (BookingParty bookingParty : bookingParties) {
                bookingPartyMap.put(bookingParty.getCompanyNo(), bookingParty.getName());
            }
        }

        List<OrgWarehouse> orgWarehouses = orgWarehouseService.findOriginByOrgId(organizationId);
        Map<String, String> warehouseMap = Maps.newHashMap();
        if (orgWarehouses != null && !orgWarehouses.isEmpty()) {
            for (OrgWarehouse warehouse : orgWarehouses) {
                warehouseMap.put(warehouse.getId(), warehouse.getName());
            }
        }

        if (dataGrid.getTotal() > 0) {
            for (AviationBooking booking : dataGrid.getRows()) {
                booking.setSystBookingPartyNo(bookingPartyMap.get(booking.getSystBookingPartyNo()));
                if (StringUtils.isNotBlank(booking.getWarehouseId())) {
                    booking.setWarehouseName(warehouseMap.get(booking.getWarehouseId()));
                }
            }
        }


        return dataGrid;

    }

    /**
     * @Author: tsd
     * @Date: 2020/02/27 16:28
     * @Description: 导出订舱信息
     */
    @RequestMapping(value = "/exportAviaBooking", method = RequestMethod.POST)
    public DataGrid exportAviaBooking(@RequestBody AviationBooking aviationBooking) {
        //predictDealBusiness.exportOrderClerance(response, avaitionOrderId, avaitionOrderNumber, templateId);
        String organizationId = SessionUtils.getOrganizationId();
        aviationBooking.setOrganizationId(organizationId);

        aviationBooking.setWarehouseId(SessionUtils.getWarehouseId());
        DataGrid dataGrid = new DataGrid();
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put("json", JSON.toJSONString(aviationBooking));
            map.put("businessName", "AviationBookingBusiness");
            map.put("modulName", "航空主单-导出订舱信息");
            //map.put("taskName", "导出预报文件(" + SessionUtils.getCustomer().getName() + ")" + DateUtil.formatSecond(new Date()));
            map.put("taskName", "导出订舱信息" + DateUtil.formatSecond(new Date()));
            //发消息
            RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.DOWNLOAD_EXPORTDATA_SALES_TASK, SessionUtils.getUserId(), map);
            rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.DOWNLOAD_CUSTOMER_QUEUEKEY, rabbitMessage);
            dataGrid.setFlag(true);
            dataGrid.setMsg("导出任务已经提交,请去任务列表下载");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("导出订舱信息错误" + e.getMessage());
        }

        return dataGrid;
    }

    /**
     * 根据id查询
     *
     * @param
     * @return
     * @author: tsd
     * @date: 2017/10/25 9:58
     */
    @RequestMapping(value = "/findById/{id}", method = RequestMethod.GET)
    public DataGrid<AviationBooking> findOne(@PathVariable("id") String aviationBookingId) {
        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        dataGrid.setCode(HttpStatus.OK.value() + "");
        dataGrid.setMsg(HttpStatus.OK.name());
        dataGrid.setTotal(1);
        AviationBooking aviationBooking = aviationBookingService.findOne(aviationBookingId);
        dataGrid.setObj(aviationBooking);
        return dataGrid;
    }

    /**
     * 根据id删除
     *
     * @param
     * @return
     * @author: tsd
     * @date: 2017/10/25 10:02
     */
    @RequestMapping(value = "/deleteById/{id}", method = RequestMethod.GET)
    public DataGrid<AviationBooking> deleteOne(@PathVariable("id") String aviationBookingId) {

        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        dataGrid.setCode(HttpStatus.OK.value() + "");
        dataGrid.setMsg("删除记录成功");
        AviationBooking aviationBooking = null;
        try {
            aviationBooking = aviationBookingService.findOne(aviationBookingId);
        } catch (Exception e) {
            dataGrid.setMsg("订舱信息ID [" + aviationBookingId + "] 不存在");
        }
        List<AviationOrder> aviationOrders = aviationOrderService.findAviaOrderByBookingId(aviationBookingId);
        if (aviationOrders != null && aviationOrders.size() > 0) {
            dataGrid.setMsg("订舱信息正在使用,不能删除!");
            return dataGrid;
        }
        if (aviationBooking != null) {
            aviationBookingService.delete(aviationBooking);
        }

//        dataGrid.setObj(aviationBooking);

        return dataGrid;
    }


    /**
     * 添加tsd
     *
     * @param aviationBooking
     * @return
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST, consumes = "application/json")
    public DataGrid add(@RequestBody AviationBooking aviationBooking) {

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        aviationBooking.setOrganizationId(organizationId);

        AviationBooking oldAviationBooking = null;
        if (StringUtils.isNotBlank(aviationBooking.getId())) {
            oldAviationBooking = aviationBookingService.findOne(aviationBooking.getId());
        }

        AviationBooking aviationBookingDB = null;
        if (oldAviationBooking != null) {
            if (StringUtils.isNotBlank(oldAviationBooking.getFlightNo()) && !oldAviationBooking.getFlightNo().equals(aviationBooking.getFlightNo())) {
                checkFlightNo(aviationBooking);
            }
            //更新
            // 更新人
            aviationBooking.setLastUpdatedDate(new Date());
            aviationBooking.setUsed(oldAviationBooking.getUsed());
            aviationBooking.setWarehouseId(oldAviationBooking.getWarehouseId());
            aviationBooking.setLastUpdatedBy(SessionUtils.getUserId());
            aviationBookingDB = aviationBookingService.save(aviationBooking);
        } else {
            //保存
            checkFlightNo(aviationBooking);
            aviationBooking.setWarehouseId(SessionUtils.getWarehouseId());
            aviationBooking.setOrganizationId(organizationId);
            aviationBooking.setUsed(false);
            aviationBookingDB = aviationBookingService.save(aviationBooking);
        }

        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        dataGrid.setCode(HttpStatus.OK.value() + "");
        dataGrid.setMsg("保存订舱信息成功");
        dataGrid.setTotal(1);

        dataGrid.setObj(aviationBookingDB);

        return dataGrid;
    }

    /**
     * 查询所有关联供应商列表tsd
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/getMapListSupplierService", method = RequestMethod.GET)
    public DataGrid<SupplierServiceDto> getMapListSupplierService() {
        DataGrid dataGrid = new DataGrid();
        String orgid = SessionUtils.getOrganizationId();
        dataGrid.setRows(aviationBookingService.getMapListSupplierService(orgid));
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * 保存或修改派送商类型别名tsd
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/saveOrUpdateTypeName", method = RequestMethod.POST)
    public DataGrid saveOrUpdateTypeName(@RequestBody ServiceTypeVo vo) {
        DataGrid dataGrid = new DataGrid();
        String organizationId = SessionUtils.getOrganizationId();
        aviationBookingService.saveOrUpdateTypeName(organizationId, SupplierServiceType.valueOf(vo.getSupplierServiceType()), vo.getSupplierServiceShowName());
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * 检查派送商类型是否被使用tsd
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/checkServiceType", method = RequestMethod.POST)
    public DataGrid checkServiceType(@RequestBody ServiceTypeVo vo) {
        DataGrid dataGrid = new DataGrid();
        String organizationId = SessionUtils.getOrganizationId();
        boolean bool =aviationBookingService.checkServiceType(organizationId,SupplierServiceType.valueOf(vo.getSupplierServiceType()),vo.getSupplierServiceId());
        dataGrid.setFlag(bool);
        return dataGrid;
    }

    /**
     * 查询所有供应商类别列表tsd
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/getSupplierServiceTypeList", method = RequestMethod.GET)
    public DataGrid<SupplierServiceDto> getSupplierServiceTypeList() {
        DataGrid dataGrid = new DataGrid();
        String orgId = SessionUtils.getOrganizationId();
        dataGrid.setRows(aviationBookingService.getSupplierServiceTypeList(orgId));
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * 查询选中供应商类别列表tsd
     *
     * @param
     * @return
     */
    @RequestMapping(value = "/getSupplierServiceTypeSelectList", method = RequestMethod.GET)
    public DataGrid<SupplierServiceDto> getSupplierServiceTypeSelectList() {
        DataGrid dataGrid = new DataGrid();
        String organizationId = SessionUtils.getOrganizationId();
        dataGrid.setRows(aviationBookingService.getSupplierServiceTypeSelectList(organizationId));
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * 检查主单号是否已经被使用
     *
     * @param aviationBooking
     */
    private void checkFlightNo(@RequestBody AviationBooking aviationBooking) {
        List<AviationBooking> aviationBookings = aviationBookingService.findByFlightNo(aviationBooking.getFlightNo());
        if (aviationBookings != null && !aviationBookings.isEmpty()) {
            throw new EshipException("SALES_AVIATION_BOOKING_FLIGHT_NO_EXIST", "主单号[{0}]已经被使用", true, aviationBooking.getFlightNo());
        }
    }

    @RequestMapping(value = "/findOrdersByAviaOrderId/{aviaOrderId}", method = RequestMethod.POST)
    @ApiOperation(value = "主单追踪==>根据主单id查询下面的订单列表", notes = "by Hukai")
    public DataGrid<OrderSearchResponseDto> findOrdersByAviaOrderId(@PathVariable("aviaOrderId") String aviaOrderId, @RequestBody OrderSearchRequestDto orderSearchRequestDto) {
        orderSearchRequestDto.setAviationOrderId(aviaOrderId);
        return aviaOrderBusiness.findCatchOrdersList(orderSearchRequestDto);
    }

    @RequestMapping(value = "/staticsOrdersByAviaOrderId/{aviaOrderId}", method = RequestMethod.POST)
    @ApiOperation(value = "主单追踪==>根据主单id查询下面的订单列表数据统计", notes = "by Hukai")
    public DataGrid<OrderStaticsResultObj> staticsOrdersByAviaOrderId(@PathVariable("aviaOrderId") String aviaOrderId, @RequestBody OrderSearchRequestDto orderSearchRequestDto) {
        orderSearchRequestDto.setAviationOrderId(aviaOrderId);
        return aviaOrderBusiness.staticsOrdersByAviaOrderId(orderSearchRequestDto);
    }

    @RequestMapping(value = "/findOrdersByBatchNumber/{batchNumber}", method = RequestMethod.POST)
    @ApiOperation(value = "转仓追踪==>根据转仓批次号查询下面的订单列表", notes = "by Hukai")
    public DataGrid<OrderSearchResponseDto> findOrdersByBatchNo(@PathVariable("batchNumber") String batchNumber, @RequestBody OrderSearchRequestDto orderSearchRequestDto) {
        orderSearchRequestDto.setBatchNumber(batchNumber);
        return aviaOrderBusiness.findCatchOrdersList(orderSearchRequestDto);
    }

    @RequestMapping(value = "/staticsOrdersByBatchNumber/{batchNumber}", method = RequestMethod.POST)
    @ApiOperation(value = "转仓追踪==>根据转仓批次号查询下面的订单列表数据统计", notes = "by Hukai")
    public DataGrid<OrderStaticsResultObj> staticsOrdersByBatchNumber(@PathVariable("batchNumber") String batchNumber, @RequestBody OrderSearchRequestDto orderSearchRequestDto) {
        orderSearchRequestDto.setBatchNumber(batchNumber);
        return aviaOrderBusiness.staticsOrdersByAviaOrderId(orderSearchRequestDto);
    }

    @RequestMapping(value = "/commitTrackContent", method = RequestMethod.POST)
    @ApiOperation(value = "客服提交备注信息", notes = "by Hukai")
    public DataGrid commitTrackContent(@RequestBody CancelRequestDto cancelRequestDto) {
        Order order = new Order();
        order.setId(cancelRequestDto.getId());
        order.setTrackContent(cancelRequestDto.getReason());

        return aviaOrderBusiness.commitTrackContent(order);
    }


    @RequestMapping(value = "/commitOperateContent", method = RequestMethod.POST)
    @ApiOperation(value = "操作提交备注信息", notes = "by Hukai")
    public DataGrid commitOperateContent(@RequestBody CancelRequestDto cancelRequestDto) {
        Order order = new Order();
        order.setId(cancelRequestDto.getId());
        order.setOperateContent(cancelRequestDto.getReason());

        return aviaOrderBusiness.commitTrackContent(order);
    }

    @RequestMapping(value = "/exportOrders/{aviaOrderId}", method = RequestMethod.POST)
    @ApiOperation(value = "主单追踪==>导出订单", notes = "by Hukai")
    public DataGrid exportOrders(@PathVariable("aviaOrderId") String aviaOrderId, @RequestBody OrderSearchRequestDto orderSearchRequestDto) {
        DataGrid dataGrid = new DataGrid();

        try {
            AviaOrderInfoDto aviationOrder = aviationOrderService.getAviaOrderInfo(aviaOrderId);
            if (aviationOrder == null) {
                throw new EshipRedirectException("主单不存在");
            }

            orderSearchRequestDto.setOrganizationId(SessionUtils.getOrganizationId());
            orderSearchRequestDto.setWarehouseId(SessionUtils.getWarehouseId());
            orderSearchRequestDto.setAviationOrderId(aviaOrderId);
            orderSearchRequestDto.setPagingDto(null);

            ObjectMapper mapper = new ObjectMapper();

            Map<String, String> map = new HashMap<>();
            String json = mapper.writeValueAsString(orderSearchRequestDto);
            map.put("json", json);
            map.put("businessName", "ExportCustServiceTrackingOrderBusiness");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            map.put("taskName", "主单追踪_(主单号:" + aviationOrder.getAviaOrderNo() + ")订单导出(" + SessionUtils.getCustomer().getName() + ")" + sdf.format(new Date()));

            RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.DOWNLOAD_EXPORTDATA_CUSTOMER_TASK, SessionUtils.getUserId(), map);
            rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.DOWNLOAD_CUSTOMER_QUEUEKEY, rabbitMessage);

            dataGrid.setFlag(true);
            dataGrid.setMsg("导入任务已经提交,请去任务列表查看");
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }

        return dataGrid;
    }

    @RequestMapping(value = "/exportBatchOrders/{batchNumber}", method = RequestMethod.POST)
    @ApiOperation(value = "转仓追踪==>导出订单", notes = "by Hukai")
    public DataGrid exportBatchOrders(@PathVariable("batchNumber") String batchNumber, @RequestBody OrderSearchRequestDto orderSearchRequestDto) {
        try {
            orderSearchRequestDto.setBatchNumber(batchNumber);

            PagingDto pagingDto = orderSearchRequestDto.getPagingDto();
            pagingDto.setPageNo(null);
            pagingDto.setPageSize(null);

            ObjectMapper mapper = new ObjectMapper();

            Map<String, String> map = new HashMap<>();
            String json = mapper.writeValueAsString(orderSearchRequestDto);
            map.put("json", json);
            map.put("businessName", "ExportCustServiceTrackingOrderBusiness");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            map.put("taskName", "转仓追踪_(批次号:" + batchNumber + ")订单导出(" + SessionUtils.getCustomer().getName() + ")" + sdf.format(new Date()));

            RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.DOWNLOAD_EXPORTDATA_CUSTOMER_TASK, SessionUtils.getUserId(), map);
            rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.DOWNLOAD_CUSTOMER_QUEUEKEY, rabbitMessage);
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }

        return new DataGrid(true, null);
    }

    @RequestMapping(value = "/import/{aviaOrderId}", method = RequestMethod.POST)
    @ApiOperation(value = "主单状态=>导入excel 批量修改派送商信息")
    public DataGrid<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file, String aviaOrderId) {

        return aviaOrderBusiness.importExcel(file, aviaOrderId);
    }

}
