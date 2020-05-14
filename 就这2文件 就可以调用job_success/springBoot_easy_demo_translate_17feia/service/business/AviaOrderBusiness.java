package com.yangshan.eship.sales.business;

import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.entity.syst.LogisticsSupplierItem;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.author.service.syst.LogisticsSupplierItemServiceI;
import com.yangshan.eship.author.service.syst.RabbitMessageServiceI;
import com.yangshan.eship.business.CommonOrderSearchBusiness;
import com.yangshan.eship.business.OrderSurchargeBusiness;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.label.BusinessType;
import com.yangshan.eship.common.rabbit.RabbitMessage;
import com.yangshan.eship.common.rabbit.RabbitMessageActionsConstant;
import com.yangshan.eship.common.rabbit.RabbitMessageQueueKeyConstant;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.order.dto.avia.AviaOrderStatus;
import com.yangshan.eship.order.dto.orde.*;
import com.yangshan.eship.order.entity.avia.AviationOrder;
import com.yangshan.eship.order.entity.orde.DeliveryAddress;
import com.yangshan.eship.order.entity.orde.OperateHistory;
import com.yangshan.eship.order.entity.orde.Order;
import com.yangshan.eship.order.entity.orde.OrderFromType;
import com.yangshan.eship.order.entity.transfer.ReceiveStatusType;
import com.yangshan.eship.order.service.avia.AviationOrderServiceI;
import com.yangshan.eship.order.service.labelex.TrackingInformationServiceI;
import com.yangshan.eship.order.service.orde.DeliveryAddressServiceI;
import com.yangshan.eship.order.service.orde.OrderSearchServiceI;
import com.yangshan.eship.product.dto.ProductCalcPriceResult;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.*;

@Service
public class AviaOrderBusiness {

    private static Logger logger = LoggerFactory.getLogger(AviaOrderBusiness.class);

    @Value("${rest.exportData.orderSearchUrl}")
    private String orderSearchUrl;

    @Value("${rest.exportData.orderStaticsUrl}")
    private String orderStaticsUrl;

    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Value("${static.file.server}")
    private String staticFileServer;

    @Autowired
    private CommonOrderSearchBusiness commonOrderSearchBusiness;

    @Autowired
    private OrderSearchServiceI orderSearchService;

    @Autowired
    private ProductServiceI productService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private AviationOrderServiceI aviationOrderService;

    @Autowired
    private LogisticsSupplierItemServiceI logisticsSupplierItemService;

    @Autowired
    private TrackingInformationServiceI trackingInformationService;

    @Autowired
    private DeliveryAddressServiceI deliveryAddressService;

    @Value("${aviaOrder.clearanceNoTrackInfoTime}")
    private String clearanceNoTrackInfoTime;

    @Value("${aviaOrder.clearanceUnSignTime}")
    private String clearanceUnSignTime;

    @Autowired
    private RabbitMessageServiceI rabbitMessageService;

    /**
     * 同时发送消息到api系统和finance系统
     */
    @Resource(name = "apiAndFinanceTemplate")
    private AmqpTemplate apiAndFinanceTemplate;

    @Autowired
    private OrderSurchargeBusiness orderSurchargeBusiness;

    private static final String NO_TRACKING_DESC = "超时无追踪信息";
    private static final String UN_SIGN_DESC = "超时未签收";

    public DataGrid<OrderSearchDto> findOrderByAviaOrderId(OrderSearchDto orderSearchDto) {
        DataGrid<OrderSearchDto> orderGrid = new DataGrid<>();

        AviationOrder aviationOrder = aviationOrderService.findOne(orderSearchDto.getAviaOrderId());

        DataGrid<Order> grid = orderSearchService.findOrderByAviaOrderId(orderSearchDto);

        Map<String, Boolean>  clearanceTrackAbnormalMap = new HashMap<>();
        Map<String, Boolean>  clearanceSignAbnormalMap = new HashMap<>();

        if (grid.getRows().isEmpty()) {
            return new DataGrid<>(true, new ArrayList<OrderSearchDto>());
        }

        List<OrderSearchDto> orderSearchDtos = new ArrayList<>();

        long noTrackTimeoutTimeMillis = Integer.parseInt(clearanceNoTrackInfoTime) * 60 * 60 * 1000L;
        long unSignTimeoutTimeMillis = Integer.parseInt(clearanceUnSignTime) * 60 * 60 * 1000L;

        //拿到主单清关完成时间和当前时间的间隔
        long currentTimeMillis = System.currentTimeMillis();
        long clearanceTimeMillis = 0L;

        if (AviaOrderStatus.CLEARING.equals(aviationOrder.getStatus())) {
            clearanceTimeMillis = aviationOrder.getClearanceTime().getTime();
        }

        //获取订单对应的产品
        List<Order> list = grid.getRows();
        Set<String> orderIds = new HashSet<>();
        Set<String> productIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();

        for (Order order : list) {
            orderIds.add(order.getId());

            if (StringUtils.isNotBlank(order.getProdProductId())) {
                productIds.add(order.getProdProductId());
            }

            if (StringUtils.isNotBlank(order.getUserId())) {
                userIds.add(order.getUserId());
            }

            //@Author: Kevin 2018-11-07 11:23
            //@Descreption: 找出主单下异常订单
            if (AviaOrderStatus.CLEARING.equals(aviationOrder.getStatus())) {
                //找出主单清关完成一段时间后还没有追踪信息的订单
                if (order.getTransferTime() == null && (currentTimeMillis - clearanceTimeMillis > noTrackTimeoutTimeMillis)) {
                    clearanceTrackAbnormalMap.put(order.getId(), Boolean.TRUE);
                } else {
                    clearanceTrackAbnormalMap.put(order.getId(), Boolean.FALSE);
                }

                //找出主单清关完成一段时间后还没有签收的订单
                if ((!OrderStatus.COMPLETED.equals(order.getStatus())) && (currentTimeMillis - clearanceTimeMillis > unSignTimeoutTimeMillis)) {
                    clearanceSignAbnormalMap.put(order.getId(), Boolean.TRUE);
                } else {
                    clearanceSignAbnormalMap.put(order.getId(), Boolean.FALSE);
                }
            } else {
                //防止报空指针异常
                clearanceTrackAbnormalMap.put(order.getId(), Boolean.FALSE);
                clearanceSignAbnormalMap.put(order.getId(), Boolean.FALSE);
            }
        }

        List<Product> products = productIds.isEmpty() ? new ArrayList<>() : productService.listByProductIds(new ArrayList<String>(productIds));

        Map<String, User> users = userService.mapByIds(new ArrayList<>(userIds));

        Map<String, String> prodNameMaps = new HashMap<>();

        for (Product product : products) {
            prodNameMaps.put(product.getId(), product.getName());
        }

        //处理主单下的订单
        for (Order order : list) {
            OrderSearchDto searchDto = this.dealOrderInfo(order, prodNameMaps, users);

            //初始化追踪异常的属性
            searchDto.setClearanceTrackAbnormal(false);
            searchDto.setClearanceSignAbnormal(false);

            //设置对应订单是否是异常订单
            if (Boolean.TRUE.equals(clearanceTrackAbnormalMap.get(order.getId())) || Boolean.TRUE.equals(clearanceTrackAbnormalMap.get(order.getParentId()))) {
                searchDto.setClearanceTrackAbnormal(true);
            }

            if (Boolean.TRUE.equals(clearanceSignAbnormalMap.get(order.getId())) || Boolean.TRUE.equals(clearanceSignAbnormalMap.get(order.getParentId()))) {
                searchDto.setClearanceSignAbnormal(true);
            }

            orderSearchDtos.add(searchDto);
        }

        //对orderSearchDtos进行排序
        Collections.sort(orderSearchDtos, new Comparator<OrderSearchDto>() {
            @Override
            public int compare(OrderSearchDto o1, OrderSearchDto o2) {
                return o1.getLargeSmallOrderSortName().compareTo(o2.getLargeSmallOrderSortName());
            }
        });

        BeanUtils.copyProperties(grid, orderGrid);

        orderGrid.setRows(orderSearchDtos);

        return orderGrid;
    }

    private OrderSearchDto dealOrderInfo(Order order, Map<String, String> prodNameMaps, Map<String, User> users) {
        OrderSearchDto searchDto = new OrderSearchDto();
        BeanUtils.copyProperties(order, searchDto);

        ProductCalcPriceResult feeDetail = orderSearchService.createFeeDetailObj(searchDto.getFeeDetail());

        float orderStatisticalWeight = (feeDetail != null ? feeDetail.getTotalWeight() : 0);

        //统计金额(总申报价值)
        float orderDeclaredValue = (feeDetail != null ? feeDetail.getTotalPrice() : 0);

        //订单状态
        searchDto.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : "");
        searchDto.setOrderStatusStr(order.getStatus() != null ? order.getStatus().getDesc() : "");
        searchDto.setOrderType(order.getOrderType() != null ? order.getOrderType().name() : "");
        searchDto.setOrderTypeStr(order.getOrderType() != null ? order.getOrderType().getDesc() : "");

        searchDto.setGoodsNumber(order.getBoxCount());

        searchDto.setShowWeight(orderStatisticalWeight);
        searchDto.setShowMoney(orderDeclaredValue);

        //收货人信息--->小包打大包要用到
        DeliveryAddress deliveryAddress = deliveryAddressService.findByOrder(order.getId());
        searchDto.setConsignee(deliveryAddress != null ? deliveryAddress.getConsignee() : "");

        //产品名称
        searchDto.setProdName(order.getProdProductId() != null ? prodNameMaps.get(order.getProdProductId()) : "");

        //客户编号
        searchDto.setCustomerCode(order.getUserId() != null ? (users.get(order.getUserId()) != null ? users.get(order.getUserId()).getCustomerCode() : "") : "");

        //客户公司简称
        searchDto.setSimpleCompanyName(order.getUserId() != null ? users.get(order.getUserId()).getSimpleCompanyName() : "");

        //客户名称
        searchDto.setCustomerName(order.getUserId() != null ? users.get(order.getUserId()).getName() : "");

        return searchDto;
    }

    public DataGrid<Map<String, Object>> importExcel(MultipartFile file, String aviaOrderId) {
        DataGrid<Map<String, Object>> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        List<Order> orders = null;

        List<Map<String, Object>> resultMapList = new ArrayList<>();

        try {

            InputStream fileInputStream = file.getInputStream();


            orders = this.importExcelData(fileInputStream);

            Set<String> insideNumberSet = new HashSet<>();
            for (Order order : orders) {
                insideNumberSet.add(order.getInsideNumber());
            }

            List<String> insideNumberList = new ArrayList<>(insideNumberSet);
            List<Order> orderList = insideNumberList.isEmpty() ? new ArrayList<>() : orderSearchService.getInfoByInsideNumberIn(insideNumberList);

            Map<String, Object> orderMap = new HashMap<>();
            for (Order order : orderList) {
                orderMap.put(order.getInsideNumber(), order);
            }

            //@Author: Kevin 2018-12-20 11:14
            //@Descreption: 用于统计成功和失败的条数
            int totalCount = orders.size();
            int successCount = 0;
            int failCount = 0;

            Set<String> oldDeliveryNumbers = new HashSet<>();
            List<String> successOrderIds = new ArrayList<>();
            for (Order order : orders) {
                Map<String, Object> singleResultMap = new HashMap<>();

                Order dbOrder = (Order) orderMap.get(order.getInsideNumber());
                DataGrid dg = updateDeliveryCompanyInfo(order, aviaOrderId);
                if (dg.isFlag() && dg.getObj() != null) {
                    oldDeliveryNumbers.add(dg.getObj().toString());
                }

                singleResultMap.put("insideNumber", order.getInsideNumber());
                singleResultMap.put("result", dg.isFlag());

                String result = "";
                if (dg.isFlag()) {
                    successCount++;
                    result = String.format("修改成功, %s", dg.getMsg());

                    successOrderIds.add(dbOrder.getId());
                } else {
                    failCount++;
                    result = String.format("修改失败, 原因：%s", dg.getMsg());
                }
                singleResultMap.put("resultMsg", result);

                resultMapList.add(singleResultMap);

                //修改成功要发消息
                if (dg.isFlag() && StringUtils.isNotBlank(dg.getMsg())) {
                    //生成订单日志记录
                    String oId = dbOrder.getId();
                    RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.ADD_ORDER_LOG, oId, new OperateHistory(oId, SessionUtils.getUserId(), "修改转运单号和派送商", dg.getMsg(), new Date()));
                    rabbitMessageService.sendAddOrderLogMessage(rabbitMessage);

                    //@Descreption: 如果是代理订单需要同步追踪单号
                    if (StringUtils.isNotBlank(dbOrder.getCallbackUrl())) {
                        logger.info("================================> 开始发送通过代理产品下的订单 {} 修改订单追踪单号的消息...", order.getInsideNumber());

                        Map<String, Object> messageMap = new HashMap<>();
                        messageMap.put("insideNumber", dbOrder.getReferenceNo());
                        messageMap.put("deliveryNumber", order.getDeliveryNumber());
                        messageMap.put("lastUpdatedDate", new Date());
                        messageMap.put("proxyToken", dbOrder.getProxyToken());

                        RabbitMessage proxyMsg = new RabbitMessage(RabbitMessageActionsConstant.UPDATE_PROXY_ORDER_TRACK_NUMBER, oId, messageMap);
                        rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.ESHIP_API_QUEUEKEY,proxyMsg);
                    }

                    //@Author: Kevin 2019-04-28 9:10
                    //@Descreption: 如果是乐迪订单, 需要发消息将订单追踪单号等信息同步到乐迪
                    if (OrderFromType.LEDI.name().equals(dbOrder.getOrderFromType().name())) {
                        Map<String, Object> lediOrderDataMap = new HashMap<>();
                        lediOrderDataMap.put("orderFromType", dbOrder.getOrderFromType().name());
                        lediOrderDataMap.put("orderId", dbOrder.getId());
                        lediOrderDataMap.put("referenceNo", dbOrder.getReferenceNo());
                        lediOrderDataMap.put("proxyToken", dbOrder.getProxyToken());
                        lediOrderDataMap.put("deliveryNumber", order.getDeliveryNumber());
                        lediOrderDataMap.put("pdfPath", "");

                        RabbitMessage proxyMsg = new RabbitMessage(RabbitMessageActionsConstant.DELIVERY_PDF_REQUEST_SUCCESS, oId, lediOrderDataMap);
//                        rabbitMessageService.sendMessage(proxyMsg);
                        apiAndFinanceTemplate.convertAndSend(proxyMsg);
                    }
                }
            }

            //更改派送商后发送消息进行后续处理
            RabbitMessage rabbitMessage = new RabbitMessage(
                    RabbitMessageActionsConstant.CHANGE_LOGISTICS_SUPPLIER,
                    SessionUtils.getUserId(),
                    successOrderIds
            );
            rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.ORDER_SERVICE_QUEUEKEY,rabbitMessage);

            //@Author: Kevin 2019-06-25 9:37
            //@Descreption: 删除就单号对应的追踪信息
            if (!oldDeliveryNumbers.isEmpty()) {
                List<String> oldDeliveryNumberList = new ArrayList<>(oldDeliveryNumbers);
                trackingInformationService.removeByDeliveryNumbers(oldDeliveryNumberList);
            }

            dataGrid.setMsg("一共导入 " + totalCount + " 条数据, " + successCount + " 条修改成功, " + failCount + " 条修改失败");
            dataGrid.setRows(resultMapList);

            return dataGrid;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            dataGrid.setFlag(false);
            dataGrid.setMsg(e.getMessage());
            return dataGrid;
        }
    }

    private DataGrid updateDeliveryCompanyInfo(Order order, String aviaOrderId) {
        DataGrid dataGrid = new DataGrid();

        dataGrid.setFlag(false);

        Order dbOrder = orderSearchService.findByInsideNumber(order.getInsideNumber());

        if (dbOrder == null) {
            dataGrid.setMsg("订单不存在");
            return dataGrid;
        }

        LogisticsSupplierItem oldLogisticsSupplierItem = StringUtils.isNoneBlank(dbOrder.getSupplyItemId()) ? logisticsSupplierItemService.findOne(dbOrder.getSupplyItemId()) : null;

        String oldDeliveryCompany = oldLogisticsSupplierItem != null ? oldLogisticsSupplierItem.getLogisticsName() : "空";
        String oldDeliveryNumber = StringUtils.isNoneBlank(dbOrder.getDeliveryNumber()) ? dbOrder.getDeliveryNumber() : "空";

        if (StringUtils.isBlank(order.getDeliveryCompany())) {
            dataGrid.setMsg("派送商商为空");
            return dataGrid;
        }

        if (StringUtils.isBlank(order.getDeliveryNumber())) {
            dataGrid.setMsg("派送单号为空");
            return dataGrid;
        }

        /*if (order.getDeliveryNumber().length() > 50) {
            dataGrid.setMsg("派送单号过长");
            return dataGrid;
        }*/

        String deliveryNumber = order.getDeliveryNumber();
        deliveryNumber = deliveryNumber.replace(" ", "");
        deliveryNumber = deliveryNumber.replace("，", ",");

        //获取派送商id
        String cellDeliveryCompany = order.getDeliveryCompany();
        int idStart = cellDeliveryCompany.lastIndexOf("(");
        int idEnd = cellDeliveryCompany.lastIndexOf(")");

        String deliveryCompanyName = cellDeliveryCompany.substring(0, idStart);
        String supplyItemId = cellDeliveryCompany.substring(idStart + 1, idEnd);

        LogisticsSupplierItem logisticsSupplierItem = logisticsSupplierItemService.findOne(supplyItemId);

        if (logisticsSupplierItem == null) {
            dataGrid.setMsg("派送商 [ " + deliveryCompanyName + " ] 不存在");
            return dataGrid;
        }

        if (!logisticsSupplierItem.getAvailable()) {
            dataGrid.setMsg("派送商 [ " + deliveryCompanyName + " ] 不可用");
            return dataGrid;
        }

        Order aviaOrder = orderSearchService.getAviaInfoByOrderId(dbOrder.getId());

        //判断是否在当前主单内
        if (!aviaOrderId.equals(aviaOrder.getAviaOrderId())) {
            dataGrid.setMsg("订单不在当前主单中");
            return dataGrid;
        }

        String[] deliveryNumberArr = StringUtils.split(deliveryNumber, ",");

        boolean existDeliveryNumber = false;
        int i = 0;
        StringBuilder existDeliveryNumberData = new StringBuilder();
        for (String deliveryNo : deliveryNumberArr) {
            if (!deliveryNo.equals(oldDeliveryNumber)) {
                existDeliveryNumber = orderSearchService.existDeliveryNumber(deliveryNo, SessionUtils.getOrganizationId());
                if (existDeliveryNumber) {
                    existDeliveryNumberData.append(deliveryNo);
                    if (i != deliveryNumberArr.length - 1) {
                        existDeliveryNumberData.append(",");
                    }
                }
            }
            i++;
        }
        if (existDeliveryNumber) {
            dataGrid.setMsg("派送单号 " + existDeliveryNumberData.toString() + " 已存在!");
            return dataGrid;
        }

        dbOrder.setDeliveryCompany(logisticsSupplierItem.getLogisticsName());
        dbOrder.setDeliveryNumber(deliveryNumberArr[0]);
        dbOrder.setAllDeliveryNumber(deliveryNumber);
        dbOrder.setSupplyItemId(logisticsSupplierItem.getId());

        //@Author: Kevin 2019-08-12 13:57
        //@Descreption: 将末条追踪信息置空
        dbOrder.setLastTrackInfo(null);

        //@Author: Kevin 2019-07-19 9:30
        //@Descreption: 根据新修改的派送商是否实现追踪来修改订单表中is_stop_track字段
        boolean achieveTrack = logisticsSupplierItem.getAchieveTrack() == null ? false : logisticsSupplierItem.getAchieveTrack().booleanValue();
        dbOrder.setStopTrackFlag(!achieveTrack);
        logger.info("=============================>订单{}修改派送商成功, 已将订单表中is_stop_track字段改为{}", dbOrder.getInsideNumber(), !achieveTrack);

        dbOrder = orderSearchService.saveOrderContainsAviaOrder(dbOrder, aviaOrder.getAviaOrderId());
        dataGrid.setFlag(true);

        StringBuilder msgBuilder = new StringBuilder();

        if (!oldDeliveryCompany.equals(dbOrder.getDeliveryCompany())) {
            msgBuilder.append("将派送商由 ").append(oldDeliveryCompany).append(" 修改为: ")
                    .append(dbOrder.getDeliveryCompany()).append("; ");
        }

        if (!oldDeliveryNumber.equals(dbOrder.getDeliveryNumber())) {
            msgBuilder.append("将派送单号由 ").append(oldDeliveryNumber)
                    .append(" 修改为: ").append(deliveryNumber).append(";");

            //@Author: Kevin 2019-06-25 9:34
            //@Descreption: 记录旧单号, 更新后将旧单号对应的追踪信息删除
            if (StringUtils.isNotBlank(oldDeliveryNumber)) {
                dataGrid.setObj(oldDeliveryNumber);
            }
        }

        dataGrid.setMsg(msgBuilder.toString());

        return dataGrid;
    }

    private List<Order> importExcelData(InputStream fileInputStream) throws Exception {
        Workbook wb = WorkbookFactory.create(fileInputStream);
        Sheet sheet = wb.getSheetAt(0);

        // 标题，也是要导入的字段
        Row rowHead = sheet.getRow(0);

        // 行数
        int rowCount = sheet.getPhysicalNumberOfRows();

        // 列数
        int columnCount = rowHead.getLastCellNum();

        // 头部
        List<String> headNames = new ArrayList<>();

        for (int i = 0; i < columnCount; i++) {
            String headName = rowHead.getCell(i).getStringCellValue();
            headNames.add(headName);
        }
        //将excel行数据转为对象
        List<Order> orders = new ArrayList<>();
        Class orderClass = Order.class;
        for (int j = 1; j < rowCount; j++) {
            Row row = sheet.getRow(j);
            if (isBlankRow(row)) {
                continue;
            }

            Order order = (Order) orderClass.newInstance();

            for (int k = 0; k < columnCount; k++) {

                String columeValue = transferCellType(row.getCell(k));
                switch (headNames.get(k)) {
                    case "订单号":
                        order.setInsideNumber(columeValue);
                        break;
                    case "派送商":
                        order.setDeliveryCompany(columeValue);
                        break;
                    case "派送单号":
                        order.setDeliveryNumber(columeValue);
                        break;
                }
            }
            orders.add(order);
        }
        return orders;
    }

    private static boolean isBlankRow(Row row) {
        boolean blank = true;

        Iterator<Cell> cellItr = row.iterator();
        while (cellItr.hasNext()) {
            Cell c = cellItr.next();
            if (c.getCellType() != HSSFCell.CELL_TYPE_BLANK) {
                blank = false;
                break;
            }
        }
        return blank;
    }


    private static String transferCellType(Cell cell) {
        if (cell == null) {
            return null;
        }
        cell.setCellType(Cell.CELL_TYPE_STRING);
        return cell.getStringCellValue();
    }

    public DataGrid commitTrackContent(Order order) {

        return orderSearchService.commitTrackContent(order);
    }

    public DataGrid<OrderSearchResponseDto> findCatchOrdersList(OrderSearchRequestDto orderSearchRequestDto) {
        DataGrid<OrderSearchResponseDto> dataGrid = new DataGrid<>();

        //hukai 2020-02-25 如果是消息处理的那么就不能从sessionUtil中获取
        if (StringUtils.isBlank(orderSearchRequestDto.getOrganizationId())) {
            orderSearchRequestDto.setOrganizationId(SessionUtils.getOrganizationId());
        }
        if (StringUtils.isBlank(orderSearchRequestDto.getWarehouseId())) {
            orderSearchRequestDto.setWarehouseId(SessionUtils.getWarehouseId());
        }

        orderSearchRequestDto.setOrderSearchUrl(orderSearchUrl);
        orderSearchRequestDto.setSortColumn("take_delivery_time");

        dataGrid = commonOrderSearchBusiness.searchOrderData(orderSearchRequestDto);

        if (dataGrid.isFlag()) {
            //各种标签
            List<String> tags = new ArrayList<>();

            long noTrackTimeoutTimeMillis = Integer.parseInt(clearanceNoTrackInfoTime) * 60 * 60 * 1000L;
            long unSignTimeoutTimeMillis = Integer.parseInt(clearanceUnSignTime) * 60 * 60 * 1000L;

            //获取拥有的角标
            dataGrid.setRows(commonOrderSearchBusiness.getOrderTag(dataGrid.getRows()));

            dataGrid.getRows().stream().forEach((orderSearchResponseDto) -> {
                //订单类型
                if (StringUtils.isNotBlank(orderSearchResponseDto.getOrderType())) {
                    orderSearchResponseDto.setOrderType(OrderType.valueOf(orderSearchResponseDto.getOrderType()).name());
                    orderSearchResponseDto.setOrderTypeDesc(OrderType.valueOf(orderSearchResponseDto.getOrderType()).getDesc());
                }

                //订单状态
                if (StringUtils.isNotBlank(orderSearchResponseDto.getOrderStatus())) {
                    orderSearchResponseDto.setOrderStatus(OrderStatus.valueOf(String.valueOf(orderSearchResponseDto.getOrderStatus())).name());
                    orderSearchResponseDto.setOrderStatusDesc(OrderStatus.valueOf(String.valueOf(orderSearchResponseDto.getOrderStatus())).getDesc());
                }

                //转仓订单状态
                if (StringUtils.isNotBlank(orderSearchResponseDto.getReceiveStatus())) {
                    orderSearchResponseDto.setReceiveStatus(ReceiveStatusType.valueOf(String.valueOf(orderSearchResponseDto.getReceiveStatus())).name());
                    orderSearchResponseDto.setReceiveStatusDesc(ReceiveStatusType.valueOf(String.valueOf(orderSearchResponseDto.getReceiveStatus())).getDesc());
                }

                //渠道类型
                if (StringUtils.isNotBlank(orderSearchResponseDto.getBusinessType())) {
                    orderSearchResponseDto.setBusinessType(BusinessType.valueOf(String.valueOf(orderSearchResponseDto.getBusinessType())).name());
                    orderSearchResponseDto.setBusinessTypeDesc(BusinessType.valueOf(String.valueOf(orderSearchResponseDto.getBusinessType())).getSpec());
                }

                //拿到主单清关完成时间和当前时间的间隔
                long currentTimeMillis = System.currentTimeMillis();
                long clearanceTimeMillis = 0L;

                //@Author: Kevin 2018-11-07 11:23
                //@Descreption: 找出主单下异常订单
                if (orderSearchResponseDto.getAviaOrderStatus() != null && AviaOrderStatus.CLEARING.name().equals(orderSearchResponseDto.getAviaOrderStatus())) {
                    //找出主单清关完成一段时间后还没有追踪信息的订单
                    if (StringUtils.isBlank(orderSearchResponseDto.getTransferTime()) && (currentTimeMillis - clearanceTimeMillis > noTrackTimeoutTimeMillis)) {
                        orderSearchResponseDto.setDeliveryStatusDesc(NO_TRACKING_DESC);
                    }

                    //找出主单清关完成一段时间后还没有签收的订单
                    if (orderSearchResponseDto.getAviaOrderStatus() != null && (!OrderStatus.COMPLETED.name().equals(orderSearchResponseDto.getAviaOrderStatus())) && (currentTimeMillis - clearanceTimeMillis > unSignTimeoutTimeMillis)) {
                        orderSearchResponseDto.setDeliveryStatusDesc(UN_SIGN_DESC);
                    }
                }

                //末条追踪时间
                if (StringUtils.isNotBlank(orderSearchResponseDto.getLastTrackInfo())) {
                    orderSearchResponseDto.setLastTrackInfoTime(orderSearchResponseDto.getLastTrackInfo().substring(0, 19));
                }

                orderSearchResponseDto.setFeeDetail(null);

                orderSearchResponseDto.setTags(tags);
            });
        }

        return dataGrid;
    }

    public DataGrid<OrderStaticsResultObj> staticsOrdersByAviaOrderId(OrderSearchRequestDto orderSearchRequestDto) {
        orderSearchRequestDto.setOrganizationId(SessionUtils.getOrganizationId());
        orderSearchRequestDto.setOrderStaticsUrl(orderStaticsUrl);

        return commonOrderSearchBusiness.statisticsOrderData(orderSearchRequestDto);
    }

    public String exportCustServiceTrackingOrder(OrderSearchRequestDto orderSearchRequestDto) {
        List<OrderSearchResponseDto> orderSearchResponseDtoList = this.findCatchOrdersList(orderSearchRequestDto).getRows();

        String fileurl = null;
        OutputStream out = null;
        try {
            if (orderSearchResponseDtoList.isEmpty()) {
                throw new Exception("未查询到符合条件的数据");
            }

            HSSFWorkbook workbook = this.exportTrackingOrderToExcel(orderSearchResponseDtoList);
            //@Author: Kevin 2019-07-23 15:12
            //@Descreption: 统一规范nfs存储目录，用时间做上层目录结构
            //@Author: Kevin 2020-02-03 目录细化到 “uplad/年/月/组织id/文件名”
            String fileUrl = new StringBuilder("upload/")
                    .append(DateUtil.format("yyyy/MM/", new Date()))
                    .append(SessionUtils.getOrganizationId()).toString();
            //文件夹
            File fileDir = new File(staticFileUpload +fileUrl);
            if (!fileDir.exists()) {
                fileDir.mkdirs();
            }
            fileurl = fileUrl + "/订单信息表" + System.currentTimeMillis() + ".xls";
            out = new FileOutputStream(new File(staticFileUpload + fileurl));
            workbook.write(out);
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return staticFileServer + fileurl;
    }

    private HSSFWorkbook exportTrackingOrderToExcel(List<OrderSearchResponseDto> orderSearchResponseDtoList) {
        String[] headers = new String[]{"订单号", "签收状态", "末条追踪信息", "下单时间", "客户编号",
                "客户公司简称", "派送单号", "目的地", "订单类型", "重量", "快递类别", "订单状态", "备注信息", "入库时间", "派送渠道"};

        // 声明一个工作薄
        HSSFWorkbook workbook = new HSSFWorkbook();
        // 生成一个表格
        HSSFSheet sheet = workbook.createSheet("订单记录");
        // 设置表格默认列宽度为15个字节
        sheet.setDefaultColumnWidth(15);

        //设置列宽
        sheet.setColumnWidth(0, 256*20+184);
        sheet.setColumnWidth(2, 256*80+184);
        sheet.setColumnWidth(3, 256*20+184);
        sheet.setColumnWidth(4, 256*25+184);
        sheet.setColumnWidth(5, 256*20+184);
        sheet.setColumnWidth(6, 256*25+184);
        sheet.setColumnWidth(10, 256*40+184);
        sheet.setColumnWidth(12, 256*40+184);
        sheet.setColumnWidth(13, 256*20+184);

        // 生成一个样式
        HSSFCellStyle style = workbook.createCellStyle();
        // 设置这些样式
        style.setFillForegroundColor(HSSFColor.SKY_BLUE.index);
        style.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        style.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style.setBorderTop(HSSFCellStyle.BORDER_THIN);
        style.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        // 生成一个字体
        HSSFFont font = workbook.createFont();
        font.setColor(HSSFColor.VIOLET.index);
        font.setFontHeightInPoints((short) 12);
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        // 把字体应用到当前的样式
        style.setFont(font);
        // 生成并设置另一个样式
        HSSFCellStyle style2 = workbook.createCellStyle();
        style2.setFillForegroundColor(HSSFColor.LIGHT_YELLOW.index);
        style2.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
        style2.setBorderBottom(HSSFCellStyle.BORDER_THIN);
        style2.setBorderLeft(HSSFCellStyle.BORDER_THIN);
        style2.setBorderRight(HSSFCellStyle.BORDER_THIN);
        style2.setBorderTop(HSSFCellStyle.BORDER_THIN);
        style2.setAlignment(HSSFCellStyle.ALIGN_CENTER);
        style2.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);
        // 生成另一个字体
        HSSFFont font2 = workbook.createFont();
        font2.setBoldweight(HSSFFont.BOLDWEIGHT_NORMAL);
        // 把字体应用到当前的样式
        style2.setFont(font2);

        // 产生表格标题行
        HSSFRow row = sheet.createRow(0);

        int i = 0;
        for (String header : headers) {
            HSSFCell cell = row.createCell(i);
            cell.setCellStyle(style);
            HSSFRichTextString text = new HSSFRichTextString(header);
            cell.setCellValue(text);

            i++;
        }

        // 遍历集合数据，产生数据行
        if (!orderSearchResponseDtoList.isEmpty()) {
            int index = 0;
            for (OrderSearchResponseDto orderSearchResponseDto : orderSearchResponseDtoList) {
                index++;
                row = sheet.createRow(index);
                row.setHeight((short) 350);	//设置行高
                HSSFCell cell = null;

                cell = row.createCell(0);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getInsideNumber());

                cell = row.createCell(1);
                cell.setCellStyle(style2);
                cell.setCellValue(OrderStatus.COMPLETED.name().equals(orderSearchResponseDto.getOrderStatus()) ? "已签收" : "未签收");

                cell = row.createCell(2);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getLastTrackInfo());

                cell = row.createCell(3);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getCreatedDate());

                cell = row.createCell(4);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getCustomerCode());

                cell = row.createCell(5);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getSimpleCompanyName());

                cell = row.createCell(6);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getDeliveryNumber());

                cell = row.createCell(7);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getDestinationName());

                cell = row.createCell(8);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getOrderTypeDesc());

                cell = row.createCell(9);
                cell.setCellStyle(style2);
                cell.setCellValue(String.valueOf(orderSearchResponseDto.getSalesPrice()));

                cell = row.createCell(10);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getProductName());

                cell = row.createCell(11);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getOrderStatusDesc());

                cell = row.createCell(12);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getCustomerServiceComment());

                cell = row.createCell(13);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getTakeDeliveryTime());

                cell = row.createCell(14);
                cell.setCellStyle(style2);
                cell.setCellValue(orderSearchResponseDto.getLogisticsName());
            }

        }
        return workbook;
    }
}
