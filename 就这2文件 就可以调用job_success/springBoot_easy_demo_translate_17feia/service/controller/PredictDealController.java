package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.yangshan.eship.author.service.syst.RabbitMessageServiceI;
import com.yangshan.eship.author.service.syst.RandomDeliveryAddressServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.excel.ExcelData;
import com.yangshan.eship.common.excel.ExportExcelUtils;
import com.yangshan.eship.common.rabbit.RabbitMessage;
import com.yangshan.eship.common.rabbit.RabbitMessageActionsConstant;
import com.yangshan.eship.common.rabbit.RabbitMessageQueueKeyConstant;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.constants.OperationErrorCodeConstant;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.order.dto.avia.AviaOrderStatus;
import com.yangshan.eship.order.entity.avia.AviationOrder;
import com.yangshan.eship.order.entity.orde.Order;
import com.yangshan.eship.order.entity.orde.OrderClearance;
import com.yangshan.eship.order.entity.orde.OrderClearanceInvoice;
import com.yangshan.eship.order.service.avia.AviationOrderServiceI;
import com.yangshan.eship.order.service.orde.OrderClearanceInvoiceServiceI;
import com.yangshan.eship.order.service.orde.OrderClearanceServiceI;
import com.yangshan.eship.order.service.orde.OrderServiceI;
import com.yangshan.eship.product.service.ProductServiceI;
import com.yangshan.eship.sales.business.PredictDealBusiness;
import com.yangshan.eship.sales.dto.OrderClearanceDto;
import com.yangshan.eship.sales.dto.OrderClearanceUpdateDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * @Author:
 * @Description:LiuWei
 * @Date: 下午 5:10 2018/3/19
 */


@RestController
@RequestMapping(Version.VERSION + "/predictDeal")
@Api(value = "PredictDealController", tags = "预报处理")
public class PredictDealController {
    private Logger logger = LoggerFactory.getLogger(PredictDealController.class);

    @Autowired
    private PredictDealBusiness predictDealBusiness;
    @Autowired
    private OrderClearanceServiceI orderClearanceService;
    @Autowired
    private AviationOrderServiceI aviationOrderService;
    @Autowired
    private ProductServiceI productService;
    @Autowired
    OrderServiceI orderService;
    @Autowired
    OrderClearanceInvoiceServiceI orderClearanceInvoiceService;
    @Autowired
    private RabbitMessageServiceI rabbitMessageService;
    @Autowired
    private RandomDeliveryAddressServiceI randomDeliveryAddressService;


    @RequestMapping(value = "/importAddress", method = RequestMethod.POST)
    public DataGrid importAddress(@RequestParam("file") MultipartFile file) throws Exception {
        DataGrid dataGrid = new DataGrid();
        InputStream fileInputStream = file.getInputStream();
        predictDealBusiness.importAddress(fileInputStream);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * add tsd 下载发件人地址模板
     * @param httpServletResponse
     * @throws Exception
     */
    @RequestMapping(value = "/downloadAddressExcel", method = RequestMethod.GET)
    public void downloadAddressExcel(HttpServletResponse httpServletResponse) throws Exception{
        ExcelData excelData = new ExcelData();
        List<String> titles = Lists.newArrayList();
        titles.add("收件人");
        titles.add("地址1");
        titles.add("地址2");
        titles.add("城市");
        titles.add("省份");
        titles.add("邮编");
        titles.add("国家");
        titles.add("电话");
        titles.add("国家二字编码");
        List<List<Object>> rows = new ArrayList<>();
        excelData.setTitles(titles);
        excelData.setRows(rows);
        ExportExcelUtils.exportExcel(httpServletResponse,"收件人地址.xlsx", excelData);
    }

    /**
     * @Author: LiuWei
     * @Description: 清关订单数据搜索
     * @Param:
     * @Date: 下午 2:05 2018/3/21
     */
    @RequestMapping(value = "/searchOrderClearances", method = RequestMethod.POST)
    public DataGrid importAddress(@RequestBody OrderClearanceDto orderClearanceDto) {
        orderClearanceDto.setOrgId(SessionUtils.getOrganizationId());
        DataGrid dataGrid = orderClearanceService.searchOrderClearances(orderClearanceDto);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 查询主单装袋完成到清关完成之前的主单
     * @Param:
     * @Date: 下午 2:05 2018/3/21
     */
    @RequestMapping(value = "/beforeClearCompletedAviaOrders", method = RequestMethod.GET)
    @JsonFilter(type = AviationOrder.class, include = "id,aviationOrderNo,name")
    public DataGrid beforeClearCompletedAviaOrders() {

        String warehouseId = SessionUtils.getWarehouseId();
        DataGrid dataGrid = new DataGrid();
        List<AviaOrderStatus> aviaOrderStatus = new ArrayList<>();
        aviaOrderStatus.add(AviaOrderStatus.BAGGING_FINISHED);
        aviaOrderStatus.add(AviaOrderStatus.TAKE_OFF);
        aviaOrderStatus.add(AviaOrderStatus.LANDING);
        aviaOrderStatus.add(AviaOrderStatus.CLEARING);


        List<AviationOrder> aviationOrders = aviationOrderService.findByStatusListAndWarehouseId(aviaOrderStatus, warehouseId);
        dataGrid.setFlag(true);
        dataGrid.setRows(aviationOrders);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 查询主单清关完成的主单
     * @Param:
     * @Date: 下午 2:05 2018/3/21
     */
    @RequestMapping(value = "/clearCompletedAviaOrders", method = RequestMethod.GET)
    @JsonFilter(type = AviationOrder.class, include = "id,aviationOrderNo")
    public DataGrid clearCompletedAviaOrders() {
        String warehouseId = SessionUtils.getWarehouseId();
        DataGrid dataGrid = new DataGrid();
        List<AviaOrderStatus> aviaOrderStatus = new ArrayList<>();
        aviaOrderStatus.add(AviaOrderStatus.CLEARING);
        aviaOrderStatus.add(AviaOrderStatus.CLEARANCE_COMPLETED);

        List<AviationOrder> aviationOrders = aviationOrderService.findByStatusListAndWarehouseId(aviaOrderStatus, warehouseId);
        dataGrid.setFlag(true);
        dataGrid.setRows(aviationOrders);
        return dataGrid;
    }


    /**
     * @Author: tsd
     * @Description: 批量修改申报价值或者高低估值或品名
     * @Param:
     * @Date: 下午 2:05 2019/11/27
     */
    @RequestMapping(value = "/batchModify", method = RequestMethod.POST)
    public DataGrid batchModify(@RequestBody OrderClearanceUpdateDto updateDto) {
        DataGrid dataGrid = new DataGrid();
        if (StringUtils.isEmpty(updateDto.getEvaluate()) && StringUtils.isEmpty(updateDto.getReportPrice())
                && StringUtils.isEmpty(updateDto.getGoodsNameEn()) && StringUtils.isEmpty(updateDto.getGoodsName())) {
            throw new EshipException("保存");
        }
        if (!StringUtils.isEmpty(updateDto.getReportPrice())) {
            orderClearanceService.reportPriceBatchModify(updateDto.getReportPrice(), updateDto.getIds());
        }
        if (!StringUtils.isEmpty(updateDto.getEvaluate())) {
            orderClearanceService.evaluateBatchModify(updateDto.getEvaluate(), updateDto.getIds());
        }
        if (!StringUtils.isEmpty(updateDto.getGoodsNameEn()) || !StringUtils.isEmpty(updateDto.getGoodsName())) {
            orderClearanceService.goodsTypeEnBatchModify(updateDto.getGoodsNameEn(), updateDto.getGoodsName(),updateDto.getIds());
        }
        dataGrid.setFlag(true);
        return dataGrid;
    }
    /**
     * @Author: tsd
     * @Description: 单个修改预报数据
     * @Param:
     * @Date: 下午 2:05 2019/11/27
     */
    @RequestMapping(value = "/updateOrderClearance", method = RequestMethod.POST)
    public DataGrid updateOrderClearance(@RequestBody OrderClearance orderClearance) {
        DataGrid dataGrid = new DataGrid();
        try {
            orderClearanceService.updateOrderClearance(orderClearance);
        }catch (Exception e){
            dataGrid.setFlag(false);
        }
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 批量保存修改后的数据
     * @Param:
     * @Date: 下午 3:52 2018/3/21
     */
    @RequestMapping(value = "/saveAll", method = RequestMethod.POST)
    public DataGrid saveAll(@RequestBody List<OrderClearance> orderClearances) {
        DataGrid dataGrid = new DataGrid();
        orderClearanceService.saveAll(orderClearances);
        List<String> ids = new ArrayList<>();
        for (OrderClearance orderClearance : orderClearances) {
            ids.add(orderClearance.getId());
        }
        orderClearanceService.clearanceStatusBatchModify(true, ids);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 一键拆单
     * @Param:
     * @Date: 下午 3:52 2018/3/21
     */
    @RequestMapping(value = "/batchSplitOrderClerances", method = RequestMethod.GET)
    public DataGrid batchSplitOrderClerances(String avaitionOrderId) {
        DataGrid dataGrid = new DataGrid();
        List<String> ids = new ArrayList<>();
        List<Order> orders = orderService.searchByAviaOrderId(avaitionOrderId);
        if(orders == null || orders.size() ==0){
            return dataGrid;
        }
        for(Order order : orders){
            ids.add(order.getId());
        }
        // 需要判断哪些是自有VAT，哪些不是
        Map<String, String> map = orderService.getOrderIdAndProductIdMap(ids);
        Map<String, List<String>> orderIdMap = productService.judgeOrderProductType(map);

        List<String> orderClearanceIds = new ArrayList<>();

        if (!orderIdMap.get("vat").isEmpty()) {
            orderClearanceService.splitVatOrderClearance(orderIdMap.get("vat"));
        }
        if (!orderIdMap.get("noVat").isEmpty()) {
            dataGrid = orderClearanceService.splitOrderClerances(orderIdMap.get("noVat"));
        }
        return dataGrid;
    }

    /**
     * @Author: LiuWei
     * @Description: 单件拆单
     * @Param:
     * @Date: 下午 3:52 2018/3/21
     */
    @RequestMapping(value = "/singleSplitOrderClerances", method = RequestMethod.GET)
    public DataGrid singleSplitOrderClerances(String orderId) {
        DataGrid dataGrid = new DataGrid();
        // 需要判断是否是自有VAT
        List<String> orderIds = new ArrayList<>();
        orderIds.add(orderId);
        Map<String, String> map = orderService.getOrderIdAndProductIdMap(orderIds);
        Map<String, List<String>> orderIdMap = productService.judgeOrderProductType(map);

        List<String> orderClearanceIds;

        if (!orderIdMap.get("vat").isEmpty()) {
            //VAT
            orderClearanceService.splitVatOrderClearance(orderIds);
        } else {
            dataGrid = orderClearanceService.splitOrderClerances(orderIds);
        }
        return dataGrid;
    }


    /**
     * @Author: LiuWei
     * @Description: 合单
     * @Param:
     * @Date: 下午 3:52 2018/3/21
     */
    @RequestMapping(value = "/mergeOrderClerances", method = RequestMethod.GET)
    public DataGrid mergeOrderClerances(String orderId, String avaitionOrderId) {
        DataGrid dataGrid = new DataGrid();
        orderClearanceService.mergeOrderClerances(orderId, avaitionOrderId);
        dataGrid.setFlag(true);
        return dataGrid;
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
        /**
         * @Author: tsd
         * @Date: 2019/11/28 16:28
         * @Description: 导出预报文件
         */
    @RequestMapping(value = "/exportOrderClerance", method = RequestMethod.GET)
    public DataGrid exportOrderClerance(String avaitionOrderId, String avaitionOrderNumber, String templateId) {
        //predictDealBusiness.exportOrderClerance(response, avaitionOrderId, avaitionOrderNumber, templateId);

        DataGrid dataGrid = new DataGrid();
        try {
            Map param = new HashMap<String, String>();
            param.put("avaitionOrderId", avaitionOrderId);
            param.put("avaitionOrderNumber", avaitionOrderNumber);
            param.put("templateId", templateId);

            Map<String, String> map = new HashMap<String, String>();
            map.put("json", JSON.toJSONString(param));
            map.put("businessName", "OrderCleranceExportBusiness");
            map.put("modulName", "预报处理-导出预报文件");
            //map.put("taskName", "导出预报文件(" + SessionUtils.getCustomer().getName() + ")" + DateUtil.formatSecond(new Date()));
            map.put("taskName", "导出预报文件" + DateUtil.formatSecond(new Date()));
            //发消息
            RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.DOWNLOAD_EXPORTDATA_SALES_TASK, SessionUtils.getUserId(), map);
            rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.DOWNLOAD_CUSTOMER_QUEUEKEY, rabbitMessage);
            dataGrid.setFlag(true);
            dataGrid.setMsg("导出任务已经提交,请去任务列表下载");
        } catch (Exception e) {
            logger.info(e.getMessage(), e);
            dataGrid.setMsg("导出预报文件错误" + e.getMessage());
        }

        return dataGrid;
    }

    @RequestMapping(value = "/bagsDetail")
    public void bagsDetail(String aviaOrderId, HttpServletResponse response) {
        AviationOrder aviationOrder = aviationOrderService.bagsDetail(aviaOrderId);
        Workbook workbook = predictDealBusiness.downloadBagDetailExcel(aviationOrder);
        try {
            String fileName = "装箱清单-" + aviationOrder.getAviationOrderNo() + ".xls";
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + new String(fileName.getBytes("UTF-8"), "ISO-8859-1"));// 指定下载的文件名
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml." + "" + "sheet;charset=utf-8");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Cache-Control", "no-cache");
            response.setDateHeader("Expires", 0);
            response.setCharacterEncoding("utf-8");
            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new EshipException(OperationErrorCodeConstant.FILE_DOWNLOAD_ERROR, "装箱清单下载错误", e);
        }
    }

    /**
     * @Author: LiuWei
     * @Description:
     * @Param:
     * @Date: 下午 3:52 2018/3/21
     */
    @RequestMapping(value = "/orderClearanceInvoices", method = RequestMethod.GET)
    public DataGrid orderClearanceInvoices(String orderClearanceId) {
        DataGrid dataGrid = new DataGrid();
        List<OrderClearanceInvoice> orderClearanceInvoices = orderClearanceInvoiceService.findByOrderClearanceId(orderClearanceId);
        if (orderClearanceInvoices == null || orderClearanceInvoices.size() == 0) {
            orderClearanceService.createOrderClearanceInvoiceById(orderClearanceId);
            orderClearanceInvoices = orderClearanceInvoiceService.findByOrderClearanceId(orderClearanceId);
        }
        dataGrid.setFlag(true);
        dataGrid.setRows(orderClearanceInvoices);
        return dataGrid;
    }


    /**
     * @Author: LiuWei
     * @Description: 添加和编辑发票
     * @Param:
     * @Date: 下午 3:52 2018/3/21
     */
    @RequestMapping(value = "/saveAndEditInvoice", method = RequestMethod.POST)
    public DataGrid saveAndEditInvoice(@RequestBody String params) {
        JSONObject jsonObject = JSONObject.parseObject(params);
        String orderClearanceId = jsonObject.getString("orderClearanceId");
        List<OrderClearanceInvoice> orderClearanceInvoices = jsonObject.getJSONArray("orderClearanceInvoices").toJavaList(OrderClearanceInvoice.class);

        DataGrid dataGrid = new DataGrid();
        orderClearanceInvoiceService.edit(orderClearanceInvoices, orderClearanceId);
        List<String> ids = new ArrayList<>();
        ids.add(orderClearanceId);
        orderClearanceService.clearanceStatusBatchModify(true, ids);
        String name = "";
        String nameEn = "";
        float total = 0f;
        for (int i = 0; i < orderClearanceInvoices.size(); i++) {
            total += orderClearanceInvoices.get(i).getTotalReportPrice();
            if (i == orderClearanceInvoices.size() - 1) {
                name += orderClearanceInvoices.get(i).getGoodsName();
                nameEn += orderClearanceInvoices.get(i).getGoodsNameEn();
            } else {
                name += orderClearanceInvoices.get(i).getGoodsName() + ",";
                nameEn += orderClearanceInvoices.get(i).getGoodsNameEn() + ",";
            }
        }
        orderClearanceService.goodsTypeEnBatchModify(nameEn, name, ids);
        orderClearanceService.reportPriceBatchModify(total, ids);
        dataGrid.setFlag(true);
        return dataGrid;
    }


    /**
     * @Author: tsd
     * @Description: 删除发票数据
     * @Param:
     * @Date:
     */
    @RequestMapping(value = "/deleteClearanceInvoice/{orderClearanceId}", method = RequestMethod.GET)
    public DataGrid deleteClearanceInvoice(@PathVariable("orderClearanceId") String orderClearanceId) {
        DataGrid dataGrid = new DataGrid();
        orderClearanceInvoiceService.deleteById(orderClearanceId);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    @ApiOperation(value = "via一键提交申报信息", notes = "by Hukai")
    @PostMapping(value = "/batchCommitViaDeclarationInfo/{aviaOrderId}")
    public DataGrid batchCommitViaDeclarationInfo(@PathVariable("aviaOrderId") String aviaOrderId) {
        List<OrderClearance> orderClearanceList = orderClearanceService.findByAvaitionOrderId(aviaOrderId);
        return orderClearanceService.batchCommitViaDeclarationInfo(orderClearanceList);
    }


    @ApiOperation(value = "sodexi一键提交申报信息", notes = "by hyl")
    @PostMapping(value = "/batchCommitSodexiDeclarationInfo/{aviaOrderId}")
    public DataGrid batchCommitSodexiDeclarationInfo(@PathVariable("aviaOrderId") String aviaOrderId) {
        List<OrderClearance> orderClearanceList = orderClearanceService.findByAvaitionOrderId(aviaOrderId);
        return orderClearanceService.batchCommitSodexiDeclarationInfo(orderClearanceList);
    }

    @ApiOperation(value = "bdm一键提交申报信息", notes = "by hyl")
    @PostMapping(value = "/batchCommitBdmDeclarationInfo/{aviaOrderId}")
    public DataGrid batchCommitBdmDeclarationInfo(@PathVariable("aviaOrderId") String aviaOrderId) {
        List<OrderClearance> orderClearanceList = orderClearanceService.findByAvaitionOrderId(aviaOrderId);
        return orderClearanceService.batchCommitBdmDeclarationInfo(orderClearanceList);
    }


    @ApiOperation(value = "Via上传提单文件", notes = "by Hukai")
    @RequestMapping(value = "/uploadViaWaybills/{aviaOrderId}", method = RequestMethod.POST)
    public DataGrid<String> uploadViaWaybills(@RequestParam("file") MultipartFile file, @PathVariable String aviaOrderId) {
        return predictDealBusiness.uploadViaWaybills(file, aviaOrderId);
    }

}
