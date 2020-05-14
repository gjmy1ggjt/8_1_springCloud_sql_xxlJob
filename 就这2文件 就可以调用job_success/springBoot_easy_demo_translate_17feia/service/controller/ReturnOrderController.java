package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.yangshan.eship.author.dto.account.Customer;
import com.yangshan.eship.author.entity.account.Role;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.excel.ExcelData;
import com.yangshan.eship.common.excel.ExportExcelUtils;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.order.entity.orde.ReturnOrder;
import com.yangshan.eship.order.service.orde.ReturnOrderServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.*;

/**
 * 退仓rest接口
 *
 * @author Kee.Li
 * @date 2019/1/2 11:40
 */
@RestController
@RequestMapping(Version.VERSION + "/returnOrder")
@Api(value = "ReturnOrderController", tags = "弃件管理，退仓")
public class ReturnOrderController {

    @Autowired
    private ReturnOrderServiceI returnOrderService;

    @Autowired
    private RoleServiceI roleService;

    /**
     * 查询已上架货物（已退货）
     *
     * @param returnOrder
     * @return
     */
    @RequestMapping(method = RequestMethod.GET)
    @JsonFilter(type = ReturnOrder.class,exclude = "version,startDate,endDate,startTime,endTime")
    @ApiOperation(value = "查询列表", notes = "by kee，查询参数：deliveryNumber、shelfNumber、startDate、endDate、platformType、pagingDto", httpMethod = "GET")
    public DataGrid<ReturnOrder> list(ReturnOrder returnOrder) {
        return getReturnOrderDataGrid(returnOrder);

    }

    private DataGrid<ReturnOrder> getReturnOrderDataGrid(ReturnOrder returnOrder) {
        //时间
        if (StringUtils.isNotBlank(returnOrder.getStartDate()) && StringUtils.isNotBlank(returnOrder.getEndDate())) {
            try {
                Date startTime = DateUtil.parse(DateUtil.SECOND_DF, returnOrder.getStartDate());
                Date endTime = DateUtil.parse(DateUtil.SECOND_DF, returnOrder.getEndDate());

                returnOrder.setStartTime(startTime);
                returnOrder.setEndTime(endTime);
            } catch (Exception e) {
                throw new EshipException("Parameters startDate or endDate format error! Correct format：yyyy-MM-dd HH:mm:ss");
            }
        }

        List<Role> roles = roleService.findAllRolesByStaffId(SessionUtils.getUserId());
        if(roles != null && !roles.isEmpty()){
            for (Role role : roles) {
                if (User.UserRoleCode.overseas_agent.name().equals(role.getCode())) {
                    //操作员
                    returnOrder.setOperatorId(SessionUtils.getUserId());
                }
            }
        }
        returnOrder.setOrganizationId(SessionUtils.getOrganizationId());

        return returnOrderService.list(returnOrder);
    }


    /**
     * 上架操作
     *
     * @param returnOrder
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    @JsonFilter(type=ReturnOrder.class,exclude = "version,startDate,endDate,startTime,endTime")
    @ApiOperation(value = "上架操作", notes = "by kee，参数：deliveryNumber，shelfNumber", httpMethod = "POST")
    public DataGrid<ReturnOrder> putOnShelf(@RequestBody ReturnOrder returnOrder) {

        ReturnOrder queryResult = null;
        try {
            if(StringUtils.isBlank(returnOrder.getDeliveryNumber())){
                throw new EshipException("Parcel NO can not be empty!");
            }

            //校验是否已经上架
            ReturnOrder query = new ReturnOrder();
            query.setDeliveryNumber(returnOrder.getDeliveryNumber());
            query.setOrganizationId(SessionUtils.getOrganizationId());
            DataGrid<ReturnOrder> dataGrid = returnOrderService.list(query);
            if (dataGrid.getTotal() > 0) {
                queryResult = dataGrid.getRows().get(0);
                throw new EshipException("The current order is on shelf!");
            }

            returnOrder.setOrganizationId(SessionUtils.getOrganizationId());
            returnOrder.setOperatorId(SessionUtils.getUserId());

            Customer customer = SessionUtils.getCustomer();
            returnOrder.setOperatorName(customer == null ? "" : customer.getName());

            return returnOrderService.putOnShelf(returnOrder);

        } catch (EshipException e) {
            DataGrid<ReturnOrder> dataGrid = new DataGrid<>(false, queryResult);
            dataGrid.setMsg(e.getErrorCode());
            return dataGrid;
        }

    }

    /**
     * 导出订单
     *
     * @param returnOrder
     * @param response
     */
    @RequestMapping(value = "/exportOrder", method = RequestMethod.GET)
    @ApiOperation(value = "导出订单列表，返回excel文件流", notes = "by kee，参数就是查询条件里的：deliveryNumber、shelfNumber、startDate、endDate、platformType", httpMethod = "GET")
    public void exportReturnOrder(ReturnOrder returnOrder, HttpServletResponse response) throws Exception {

        returnOrder.setPagingDto(null);
        DataGrid<ReturnOrder> dataGrid = getReturnOrderDataGrid(returnOrder);

        List<ExcelData> excelDataList = new ArrayList<>();
        excelDataList.add(getReturnOrderExcelData(dataGrid));

        String fileName = "退货列表-" + System.currentTimeMillis();
        ExportExcelUtils.exportExcel(response, fileName + ".xlsx", excelDataList);

    }

    private ExcelData getReturnOrderExcelData(DataGrid<ReturnOrder> dataGrid) {
        ExcelData data = new ExcelData();
        data.setName("退货列表");
        String[] titles = {"下单时间", "订单号", "派送单号", "参考号", "货物名称", "货物英文名","实重","计费重", "客户编号", "客户公司简称", "电商平台URL", "退货时间", "退货原因"};
        data.setTitles(Arrays.asList(titles));

        List<List<Object>> rows = new ArrayList();
        if (dataGrid.getTotal() > 0) {
            for (ReturnOrder returnOrder : dataGrid.getRows()) {
                List<Object> row = new ArrayList();
                row.add(returnOrder.getOrderCreateTime());
                row.add(returnOrder.getInsideNumber());
                row.add(returnOrder.getDeliveryNumber());
                row.add(returnOrder.getReferenceNo());
                row.add(returnOrder.getGoodsName());
                row.add(returnOrder.getGoodsNameEn());
                row.add(returnOrder.getActualWeight());
                row.add(returnOrder.getChargedWeight());
                row.add(returnOrder.getCustomerCode());
                row.add(returnOrder.getSimpleCompanyName());
                row.add(StringUtils.isBlank(returnOrder.getOrderReferenceUrl()) ? "" : returnOrder.getOrderReferenceUrl());
                row.add(returnOrder.getCreatedDate());
                row.add(returnOrder.getReturnReason());
                rows.add(row);
            }
        }
        data.setRows(rows);
        return data;
    }


    /**
     * 导入订单
     *
     * @param file
     * @return
     */
    @RequestMapping(value = "/importOrder",method = RequestMethod.POST)
    @ApiOperation(value = "导入订单列表，主要是导入电商平台的URL", notes = "by kee 参数：导出订单的Excel；返回值说明：map包含insideNumber,status,message,记录订单导入情况", httpMethod = "POST")
    public DataGrid<Map<String, Object>> importOrderUrl(@RequestParam("file") MultipartFile file) throws Exception {

        InputStream fileInputStream = file.getInputStream();
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
        List<ReturnOrder> returnOrders = Lists.newArrayList();
        for (int j = 1; j < rowCount; j++) {
            Row row = sheet.getRow(j);
            if (isBlankRow(row)) {
                continue;
            }
            ReturnOrder returnOrder = new ReturnOrder();
            returnOrder.setOrganizationId(SessionUtils.getOrganizationId());
            for (int k = 0; k < columnCount; k++) {
                String columeValue = transferCellType(row.getCell(k));
                switch (headNames.get(k)) {
                    case "订单号" :
                        returnOrder.setInsideNumber(columeValue);
                        break;
                    case "派送单号" :
                        returnOrder.setDeliveryNumber(columeValue);
                        break;
                    case "电商平台URL" :
                        returnOrder.setOrderReferenceUrl(columeValue);
                        break;
                }
            }
            returnOrders.add(returnOrder);
        }

        return returnOrderService.updateOrder(returnOrders);
    }

    public static boolean isBlankRow(Row row) {
        boolean blank = true;

        if(row == null){
            return true;
        }
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

    public static String transferCellType(Cell cell) {
        if (cell == null) {
            return null;
        }
        cell.setCellType(Cell.CELL_TYPE_STRING);
        return cell.getStringCellValue();
    }
}
