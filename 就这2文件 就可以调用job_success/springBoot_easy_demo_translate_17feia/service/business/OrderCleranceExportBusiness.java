package com.yangshan.eship.sales.business;

import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yangshan.eship.author.entity.syst.ExportExcelTemplate;
import com.yangshan.eship.author.entity.syst.ExportExcelTemplateItem;
import com.yangshan.eship.author.service.syst.ExportExcelTemplateItemServiceI;
import com.yangshan.eship.author.service.syst.ExportExcelTemplateServiceI;
import com.yangshan.eship.business.DownloadTaskBusiness;
import com.yangshan.eship.common.excel.ExcelData;
import com.yangshan.eship.common.excel.ExportExcelUtils;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.finance.service.export.DownloadTaskServiceI;
import com.yangshan.eship.order.entity.avia.AviationOrder;
import com.yangshan.eship.order.entity.orde.OrderBoxClearance;
import com.yangshan.eship.order.entity.orde.OrderClearance;
import com.yangshan.eship.order.entity.orde.OrderGoodsClearance;
import com.yangshan.eship.order.service.avia.AviationOrderServiceI;
import com.yangshan.eship.order.service.orde.OrderClearanceServiceI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @Author: Yifan
 * @Date: 2019/9/20 15:33
 * @Description:
 */
@Service
public class OrderCleranceExportBusiness extends DownloadTaskBusiness {
    private Logger logger = LoggerFactory.getLogger(OrderCleranceExportBusiness.class);

    @Value("${static.file.server}")
    private String staticFileServer;
    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Autowired
    private DownloadTaskServiceI downloadTaskService;
    @Autowired
    private AviationOrderServiceI aviationOrderService;
    @Autowired
    OrderClearanceServiceI orderClearanceService;
    @Autowired
    private ExportExcelTemplateItemServiceI exportExcelTemplateItemService;
    @Autowired
    private ExportExcelTemplateServiceI exportExcelTemplateService;

    @Override
    protected DownloadTaskServiceI getDownloadTaskService() {
        return downloadTaskService;
    }

    @Override
    protected String generateFile(String json) {
        String fileUrl = new StringBuilder("upload/").append(new SimpleDateFormat("yyyy/MM/dd").format(new Date())).toString();
        String url = "";
        try {
            JSONObject jsonObject = JSON.parseObject(json);
            String avaitionOrderId = jsonObject.getString("avaitionOrderId");
            String avaitionOrderNumber = jsonObject.getString("avaitionOrderNumber");
            String templateId = jsonObject.getString("templateId");


            //AviationOrder aviationOrder = aviationOrderService.bagsDetail(avaitionOrderId);
            AviationOrder aviationOrder = aviationOrderService.findOne(avaitionOrderId);
            List<OrderClearance> dataList = orderClearanceService.findByAvaitionOrderIdAndEvaluate(avaitionOrderId);
            List<ExportExcelTemplateItem> items = exportExcelTemplateItemService.findTemplateItemsByTempIdAndSelect(templateId);
            ExportExcelTemplate ex = exportExcelTemplateService.findOne(templateId);

            url = fileUrl + "/导出预报文件-" + ex.getTemplateName() + "-" + aviationOrder.getAviationOrderNo() + ".xlsx";
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            ExcelData data = setExportData(items,dataList);
            ExportExcelUtils.exportExcel(null, staticFileUpload + url,data);
        } catch (Exception e) {
            logger.info("导出预报文件" + e.getMessage(), e);
            return "导出预报文件失败" + e.getMessage();
        }

        return staticFileServer + url;
    }
    public ExcelData setExportData(List<ExportExcelTemplateItem> templateItems,List<OrderClearance> clearanceList) throws Exception{
        ExcelData data = new ExcelData();
        data.setName("预报文件");
        //1. 表头
        List<String> titleEns = Lists.newArrayList();
        if (templateItems != null && !templateItems.isEmpty()) {
            List<String> titles = Lists.newArrayList();
            for (ExportExcelTemplateItem item : templateItems) {
                titles.add(item.getFieldName());
                titleEns.add(item.getFieldNameEn());
            }
            data.setTitles(titles);
        }

        //2. 所有字段,方法
        Field[] fields = OrderClearance.class.getDeclaredFields();
        Field[] fields1 = OrderBoxClearance.class.getDeclaredFields();
        Field[] fields2 = OrderGoodsClearance.class.getDeclaredFields();
        Map<String, Method> fieldMethods = Maps.newHashMap();
        Map<String, Method> fieldMethods1 = Maps.newHashMap();
        Map<String, Method> fieldMethods2 = Maps.newHashMap();
        for (Field field : fields) {
                PropertyDescriptor pd = new PropertyDescriptor(field.getName(), OrderClearance.class);
                Method getMethod = pd.getReadMethod();
                fieldMethods.put(field.getName(), getMethod);
        }
        for (Field field : fields1) {
                PropertyDescriptor pd = new PropertyDescriptor(field.getName(), OrderBoxClearance.class);
                Method getMethod = pd.getReadMethod();
                fieldMethods1.put(field.getName(), getMethod);
        }
        for (Field field : fields2) {
                PropertyDescriptor pd = new PropertyDescriptor(field.getName(), OrderGoodsClearance.class);
                Method getMethod = pd.getReadMethod();
                fieldMethods2.put(field.getName(), getMethod);
        }

        //3. 生成excel行数据
        List<List<Object>> rows = new ArrayList();
        if (clearanceList != null && !clearanceList.isEmpty()) {
            //单元格合并坐标定[开始行,结束行,开始列,结束列]
             List<Integer[]> cellRangeArray = new ArrayList<>();
            Map<String,Integer[]> cellRangeMap = new HashMap<>();
            for (OrderClearance item : clearanceList) {
                int orderRowCont = 0;

                for(OrderBoxClearance boxClearance : item.getOrderBoxClearanceList()){
                    orderRowCont+=boxClearance.getOrderGoodsClearanceList().size();
                }
               for(OrderBoxClearance boxClearance : item.getOrderBoxClearanceList()){
                   int goodsRowCont = boxClearance.getOrderGoodsClearanceList().size();

                   for(OrderGoodsClearance goodsClearance : boxClearance.getOrderGoodsClearanceList()){
                       List<Object> row = new ArrayList();
                       //获取item中的值，并添加到row
                       int cloumIndex = 0;
                       for (String titleEn : titleEns) {
                           if (fieldMethods.containsKey(titleEn)) {
                               Method get = fieldMethods.get(titleEn);
                               Object value = get.invoke(item);
                               //简单类型，通用处理
                               row.add(value);

                               //计算合并
                               if(orderRowCont > 1){
                                   Integer [] orderCell = cellRangeMap.get(item.getId()+titleEn);
                                   if(orderCell == null){
                                       orderCell = new Integer[4];
                                       orderCell[0]=rows.size()+1;
                                       orderCell[1]=rows.size() + orderRowCont;
                                       orderCell[2]=cloumIndex;
                                       orderCell[3]=cloumIndex;
                                   }
                                   cellRangeMap.put(item.getId()+titleEn,orderCell);
                               }
                           }else if (fieldMethods1.containsKey(titleEn)) {
                               Method get = fieldMethods1.get(titleEn);
                               Object value = get.invoke(boxClearance);
                               //简单类型，通用处理
                               row.add(value);

                               //计算合并
                               if(goodsRowCont > 1){
                                   Integer [] orderCell = cellRangeMap.get(boxClearance.getId()+titleEn);
                                   if(orderCell == null){
                                       orderCell = new Integer[4];
                                       orderCell[0]=rows.size()+1;
                                       orderCell[1]=rows.size() + goodsRowCont;
                                       orderCell[2]=cloumIndex;
                                       orderCell[3]=cloumIndex;
                                   }
                                   cellRangeMap.put(boxClearance.getId()+titleEn,orderCell);
                               }
                           }else if (fieldMethods2.containsKey(titleEn)) {
                               Method get = fieldMethods2.get(titleEn);
                               Object value = get.invoke(goodsClearance);
                               //简单类型，通用处理
                               row.add(value);
                           }
                           cloumIndex++;
                       }
                       rows.add(row);
                   }
               }
            }
            //封装需合并的行列
            for (Map.Entry<String, Integer[]> entry : cellRangeMap.entrySet()) {
                cellRangeArray.add(entry.getValue());
            }
            data.setCellRangeArray(cellRangeArray);
        }
        data.setRows(rows);
        return data;
    }
}
