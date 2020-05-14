package com.yangshan.eship.sales.business;


import com.yangshan.eship.common.rabbit.RabbitMessageQueueKeyConstant;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yangshan.eship.author.entity.syst.ExportExcelTemplate;
import com.yangshan.eship.author.entity.syst.ExportExcelTemplateItem;
import com.yangshan.eship.author.entity.syst.LogisticsSupplierItem;
import com.yangshan.eship.author.entity.syst.RandomDeliveryAddress;
import com.yangshan.eship.author.entity.upload.StaticFileObj;
import com.yangshan.eship.author.service.syst.*;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.excel.ExportExcelUtils;
import com.yangshan.eship.common.rabbit.RabbitMessage;
import com.yangshan.eship.common.rabbit.RabbitMessageActionsConstant;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.Base64Util;
import com.yangshan.eship.common.utils.JsoupUtils;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.order.dto.labelex.viaEuroup.ErrorResultObj;
import com.yangshan.eship.order.dto.labelex.viaEuroup.WaybillsRequestObj;
import com.yangshan.eship.order.entity.avia.AviationOrder;
import com.yangshan.eship.order.entity.avia.Bag;
import com.yangshan.eship.order.entity.avia.BagItem;
import com.yangshan.eship.order.entity.orde.Order;
import com.yangshan.eship.order.entity.orde.OrderClearance;
import com.yangshan.eship.order.service.avia.AviationOrderServiceI;
import com.yangshan.eship.order.service.orde.BagServiceI;
import com.yangshan.eship.order.service.orde.OrderClearanceServiceI;
import com.yangshan.eship.order.service.orde.OrderServiceI;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jsoup.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author:
 * @Description:LiuWei
 * @Date: 下午 5:20 2018/3/19
 */

@Service
public class PredictDealBusiness  {

    private final Logger logger = LoggerFactory.getLogger(PredictDealBusiness.class);

    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Value("${static.file.server}")
    private String staticFileServer;

    @Value(value = "${via.apiUrl}")
    private String viaApiUrl;

    @Autowired
    RandomDeliveryAddressServiceI randomDeliveryAddressService;

    @Autowired
    OrderClearanceServiceI orderClearanceService;

    @Autowired
    OrderServiceI orderService;

    @Autowired
    private BagServiceI bagService;

    @Autowired
    private ExportExcelTemplateItemServiceI exportExcelTemplateItemService;

    @Autowired
    private ExportExcelTemplateServiceI exportExcelTemplateService;

    @Autowired
    private AviationOrderServiceI aviationOrderService;

    @Autowired
    private LogisticsSupplierItemServiceI logisticsSupplierItemService;

    @Autowired
    private RabbitMessageServiceI rabbitMessageService;

    public void importAddress(InputStream inputStream) throws Exception {
        Workbook wb = WorkbookFactory.create(inputStream);
        int sheetNumber = wb.getNumberOfSheets();

        List<RandomDeliveryAddress> randomDeliveryAddresses = new ArrayList<>();
        for (int j = 0; j < sheetNumber; j++) {
            Sheet sheet = wb.getSheetAt(j);

            Row rowHead = sheet.getRow(0);
            // 行数
            int rowCount = sheet.getPhysicalNumberOfRows();
            // 列数
            int columnCount = rowHead.getLastCellNum();

            for (int i = 1; i < rowCount; i++) {
                RandomDeliveryAddress randomDeliveryAddress = new RandomDeliveryAddress();
                Row row = sheet.getRow(i);

                if (row == null || isBlankRow(row)) {
                    continue;
                }
                for (int k = 0; k < columnCount; k++) {
                    String value = null;
                    try {
                        Cell cell = row.getCell(k);
                        if (cell == null) {
                            value = "";
                        } else {
                            cell.setCellType(Cell.CELL_TYPE_STRING);
                            value = cell.getStringCellValue();
                        }
                    } catch (Exception e) {
                        System.out.println("###" + j + "###" + i + "###" + k);
                        throw new Exception("");
                    }
                    if (k == 0) {
                        randomDeliveryAddress.setRecipient(value);
                    }
                    if (k == 1) {
                        randomDeliveryAddress.setAddress(value);
                    }
                    if (k == 2) {
                        randomDeliveryAddress.setAddress2(value);
                    }
                    if (k == 3) {
                        randomDeliveryAddress.setCity(value);
                    }
                    if (k == 4) {
                        randomDeliveryAddress.setProvince(value);
                    }
                    if (k == 5) {
                        randomDeliveryAddress.setPostcode(value);
                    }
                    if (k == 6) {
                        randomDeliveryAddress.setCountry(value);
                    }
                    if (k == 7) {
                        randomDeliveryAddress.setPhone(value);
                    }
                    if (k == 8) {
                        randomDeliveryAddress.setCountryCode(value);
                    }
                    randomDeliveryAddresses.add(randomDeliveryAddress);
                }
            }

        }
        randomDeliveryAddressService.excelToDb(randomDeliveryAddresses);
    }


    public boolean isBlankRow(Row row) {
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





    public Workbook downloadBagDetailExcel(AviationOrder aviationOrder) {
        HSSFWorkbook wb = new HSSFWorkbook();


        HSSFCellStyle headerStyle =  wb.createCellStyle();
        headerStyle.setAlignment(XSSFCellStyle.ALIGN_CENTER);// 设置居中
        headerStyle.setVerticalAlignment(XSSFCellStyle.VERTICAL_CENTER);    //设置垂直居中
        HSSFFont headerFont = wb.createFont();    //创建字体样式
        headerFont.setBoldweight(XSSFFont.BOLDWEIGHT_BOLD);    // 字体加粗
        headerFont.setFontName("楷体");    //设置字体类型
        headerFont.setFontHeightInPoints((short) 13);    //设置字体大小
        headerStyle.setFont(headerFont);    //为标题样式设置字体样式
        headerStyle.setBorderBottom(XSSFCellStyle.BORDER_THIN); // 下边框
        headerStyle.setBorderLeft(XSSFCellStyle.BORDER_THIN);// 左边框
        headerStyle.setBorderTop(XSSFCellStyle.BORDER_THIN);// 上边框
        headerStyle.setBorderRight(XSSFCellStyle.BORDER_THIN);// 右边框
        fillDataDetail(aviationOrder, wb, headerStyle);
        return wb;

    }

    public void fillDataDetail(AviationOrder aviationOrder, HSSFWorkbook workbook, HSSFCellStyle cellStyle) {
        List<Bag> bags = aviationOrder.getBagsTmp();
        List<Order> orders = aviationOrder.getOrdersTmp();
        Map<String,List<Object>> orderMap = new HashMap<>();
        for(Order order:orders){
            List<Object> list = new ArrayList<>();
            list.add(order.getInsideNumber());
            list.add(order.getDeliveryNumber());
            if(order.getBoxesTemp() ==null){
                list.add(1);
            }else {
                list.add(order.getBoxesTemp().size());
            }

            list.add(order.getActualWeight());
            orderMap.put(order.getId(),list);
        }
        Map<String,StringBuffer> bagOrderMap = new  LinkedHashMap<>();
        Map<String,String> beOrderMap = new HashMap<>();
        for(Bag bag:bags){
            List<BagItem> bagItems = bag.getBagItemsTmp();
            for(BagItem bagItem:bagItems){
                StringBuffer bid=bagOrderMap.get(bagItem.getOrdeOrderId());

                if(bid != null){
                    if(bag.getBagIndex() !=null) {
                        bid.append("," + bag.getBagIndex());
                    }else{
                        bid.append(",");
                    }
                }else{
                    if(bag.getBagIndex() !=null) {
                        bid = new StringBuffer(bag.getBagIndex() + "");
                    }else{
                        bid = new StringBuffer("");
                    }
                    bagOrderMap.put(bagItem.getOrdeOrderId(),bid);
                }
                String be=beOrderMap.get(bagItem.getOrdeOrderId());
                if(be == null){
                    beOrderMap.put(bagItem.getOrdeOrderId(),bag.getEvaluate()+"");
                }
            }
        }

        int i=1;
        Map<String,int[]> regionMap = new HashMap<>();
        for (Object key : bagOrderMap.
                keySet()) {
            StringBuffer bagIndexs = bagOrderMap.get(key);
            if(bagIndexs == null){
                bagIndexs = new StringBuffer("");
            }
            int [] region = regionMap.get("index"+bagIndexs.toString());
            if(region !=null){
                region[1]=i;
            }else{
                int [] region1 = new int[2];
                region1[0]=i;
                region1[1]=i;
                regionMap.put("index"+bagIndexs.toString(),region1);
            }
            i++;
        }

        // 开始写入excel数据
        HSSFSheet sheetDetail = workbook.createSheet("装箱详情");


        Row rowHead = sheetDetail.createRow(0);
        String[] headNames = new String[]{"包", "内单号", "转单号", "件数", "重量", "分单号"};
        for (int l = 0; l < headNames.length; l++) {
            Cell cell = rowHead.createCell(l);
            cell.setCellValue(headNames[l]);
            cell.setCellStyle(cellStyle);
            CellRangeAddress.valueOf(headNames[l]);

        }


        int rowIndex = 1;
        for (Object key : bagOrderMap.keySet()) {
            Row row = sheetDetail.createRow(rowIndex);
            Object bagIndexs = bagOrderMap.get(key);
            Cell cell0 = row.createCell(0);
            if(bagIndexs ==null){
                bagIndexs ="";
            }
            cell0.setCellValue(bagIndexs+"");
            cell0.setCellStyle(cellStyle);

            List<Object> orderList = orderMap.get(key);
            for (int l = 0; l < orderList.size(); l++) {
                Object value = orderList.get(l);
                Cell cell = row.createCell(l+1);
                if(value ==null){
                    value ="";
                }
                cell.setCellValue(value+"");
                cell.setCellStyle(cellStyle);
            }
            Object be=beOrderMap.get(key);
            Cell cell = row.createCell(orderList.size()+1);
            if(be ==null){
                be ="";
            }
            cell.setCellValue(be+"");
            cell.setCellStyle(cellStyle);
            rowIndex++;
        }
        for (Object key : regionMap.
                keySet()) {
            int [] region = regionMap.get(key);
            if(region[0] !=region[1]) {
                sheetDetail.addMergedRegion(new CellRangeAddress((short) region[0], (short) region[1], (short) 0, (short) 0));
            }
        }


    }

    public DataGrid<String> uploadViaWaybills(MultipartFile file, String aviaOrderId) {
        try {
            AviationOrder aviationOrder = aviationOrderService.findOne(aviaOrderId);
            if (aviationOrder == null) {
                throw new EshipRedirectException("主单不存在!");
            }

            String waybillsUrl = viaApiUrl + "/v4/waybills";

            DataGrid dataGrid = new DataGrid<>();
            StaticFileObj staticFileObj = new StaticFileObj();

            //主单总体积重
            Float totalVolumeWeight = 0.0f;

            List<Order> orderList = orderService.searchByAviaOrderId(aviaOrderId);
            Set<String> suppliItemIds = new HashSet<>();
            for (Order orderInfo : orderList) {
                suppliItemIds.add(orderInfo.getSupplyItemId());
                totalVolumeWeight += orderInfo.getVolumeWeight();
            }

            List<LogisticsSupplierItem> logisticsSupplierItems = logisticsSupplierItemService.findByIds(new ArrayList<>(suppliItemIds));

            boolean isViaLogistics = false;
            String token = "";
            for (LogisticsSupplierItem logisticsSupplierItem : logisticsSupplierItems) {
                if ("ViaEuropeService".equals(logisticsSupplierItem.getApiUrl())) {
                    isViaLogistics = true;
                    token = logisticsSupplierItem.getToken();
                }
            }

            if (!isViaLogistics) {
                throw new EshipRedirectException("主单下的订单不是用的 ViaEurope 物流商!");
            }

            //封装Header
            Map<String, String> headersMap = new HashMap<>();
            headersMap.put("Authorization", "Token token=\"" + token + "\"");
            headersMap.put("Content-Type", "application/json");
            headersMap.put("Accept", "application/json");

            //判断是否已经创建过提单
            if (StringUtils.isNotBlank(aviationOrder.getWaybillsPdfPath())) {
                //throw new EshipRedirectException("主单已创建过提单!");
                //先删除提单
                JsoupUtils.requestDelete(waybillsUrl, headersMap);

                //更新数据库的提单pdf路径字段
                aviationOrderService.updateAviaWaybillsPdfPath(aviaOrderId, null);

                logger.info("========================>已成功删除主单{}的提单文件", aviationOrder);
            }

            try {
                String saveDir = "sales/" + SessionUtils.getOrganizationId() + "/waybills/";
                String rootDir = staticFileUpload + saveDir;
                File root = new File(rootDir);
                if (!root.exists()) {
                    root.mkdirs();
                }
                String realFileName = file.getOriginalFilename();
                int index = realFileName.lastIndexOf(".");
                String fileName = System.currentTimeMillis() + realFileName.substring(index);
                String filePath = rootDir + fileName;
                String savePath = "/" + saveDir + fileName;
                File newFile = new File(filePath);
                file.transferTo(newFile);

                staticFileObj.setDownloadPath(staticFileServer + savePath);
                staticFileObj.setFileName(file.getName());
                staticFileObj.setSavePath(savePath);

                dataGrid.setFlag(true);
                dataGrid.setObj(staticFileObj);

                logger.debug("=====================>主单{}上传的提单文件pdf url: {}", aviationOrder.getAviationOrderNo(), staticFileObj.getDownloadPath());
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw new EshipRedirectException("上传提单文件失败! " + e.getMessage());
            }

            //根据返回的pdfUrl转成base64
            String waybillsPdfBase64 = Base64Util.pdfUrlToBase64(staticFileObj.getDownloadPath());

            WaybillsRequestObj waybillsRequestObj = new WaybillsRequestObj();
            waybillsRequestObj.setType("sea");
            waybillsRequestObj.setCarrier_ref(aviationOrder.getAviationOrderNo());
            waybillsRequestObj.setClient_ref(aviationOrder.getAviationOrderNo());
            waybillsRequestObj.setUnits(orderList.size());
            waybillsRequestObj.setVol_weight(totalVolumeWeight.floatValue() * 1000);
            waybillsRequestObj.setManifest_base64(waybillsPdfBase64);
            //waybillsRequestObj.setAddress_shortcode("NL1437EP-2");

            ObjectMapper mapper = new ObjectMapper();
            String waybillsRequestJson = mapper.writeValueAsString(waybillsRequestObj);
            logger.info("=================>主单{}提交提单文件请求参数: {}", aviationOrder.getAviationOrderNo(), waybillsRequestObj.toString());

            String commitWaybillsResponseResult = JsoupUtils.request(waybillsUrl, waybillsRequestJson, headersMap, null, Connection.Method.POST);
            logger.info("=================>主单{}提交提单文件result: {}", aviationOrder.getAviationOrderNo(), commitWaybillsResponseResult);

            //如果发现via那边已经创建过这个提单, 需要先调删除接口
            if (commitWaybillsResponseResult.indexOf("Client ref has already been taken") >= 0) {
                JsoupUtils.requestDelete(waybillsUrl, headersMap);

                //重新提交
                commitWaybillsResponseResult = JsoupUtils.request(waybillsUrl, waybillsRequestJson, headersMap, null, Connection.Method.POST);
            }

            if (commitWaybillsResponseResult.indexOf("errors") >= 0) {
                ErrorResultObj errorResult = mapper.readValue(commitWaybillsResponseResult, ErrorResultObj.class);
                StringBuilder errorBuilder = new StringBuilder();
                for (String errorMsg : errorResult.getErrors()) {
                    errorBuilder.append(errorMsg).append("; ");
                }

                return new DataGrid<>(false, errorBuilder.toString());
            } else {
                //更新数据库的提单pdf路径字段
                aviationOrderService.updateAviaWaybillsPdfPath(aviaOrderId, staticFileObj.getSavePath());

                //@Author: Kevin 2019-07-15 10:21
                //@Descreption: 发消息将提单关联到主单下的袋子(外箱)
                List<Bag> bagList = bagService.getAviaOrderBags(aviaOrderId, null);
                for (Bag bag : bagList) {
                    logger.info("============================>开始发送消息同步via的外箱和提单, 外箱号: {}, 提单号: {}", bag.getBagNo(), aviationOrder.getAviationOrderNo());
                    Map<String, String> paramMap = new HashMap<>();
                    paramMap.put("token", token);
                    paramMap.put("waybillsRef", aviationOrder.getAviationOrderNo());

                    RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.LINKED_PARCELS_TO_WAYBILLS, bag.getBagNo(), paramMap);
                    rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.ORDER_SERVICE_QUEUEKEY,rabbitMessage);
                }

                return new DataGrid<>(true, "上传提单文件成功!");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new EshipRedirectException("上传提单文件失败! " + e.getMessage());
        }
    }
}
