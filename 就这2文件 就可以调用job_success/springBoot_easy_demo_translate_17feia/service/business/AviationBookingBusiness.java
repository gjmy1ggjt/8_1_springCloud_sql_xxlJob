package com.yangshan.eship.sales.business;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.yangshan.eship.author.entity.syst.BookingParty;
import com.yangshan.eship.author.service.syst.BookingPartyServiceI;
import com.yangshan.eship.business.DownloadTaskBusiness;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.excel.ExcelData;
import com.yangshan.eship.common.excel.ExportExcelUtils;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.finance.service.export.DownloadTaskServiceI;
import com.yangshan.eship.order.dto.orde.OrderSearchRequestDto;
import com.yangshan.eship.order.entity.avia.AviationBooking;
import com.yangshan.eship.order.entity.orde.OrderExportResultDb;
import com.yangshan.eship.order.service.avia.AviationBookingServiceI;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class AviationBookingBusiness extends DownloadTaskBusiness {
    private final Logger logger = LoggerFactory.getLogger(AviationBookingBusiness.class);

    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Value("${static.file.server}")
    private String staticFileServer;


    @Autowired
    private DownloadTaskServiceI downloadTaskService;

    @Autowired
    private AviationBookingServiceI aviationBookingService;

    @Autowired
    private BookingPartyServiceI bookingPartyService;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;



    @Override
    protected DownloadTaskServiceI getDownloadTaskService() {
        return downloadTaskService;
    }

    @Override
    protected String generateFile(String json) {
        String url = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            AviationBooking aviationBooking = mapper.readValue(json, AviationBooking.class);
            DataGrid<AviationBooking> dataGrid = aviationBookingService.list(aviationBooking);

            List<BookingParty> bookingParties = bookingPartyService.findByOrgId(aviationBooking.getOrganizationId());

            // key:编号，value:名称
            Map<String, String> bookingPartyMap = new HashMap<>();
            if (bookingParties != null && !bookingParties.isEmpty()) {
                for (BookingParty bookingParty : bookingParties) {
                    bookingPartyMap.put(bookingParty.getCompanyNo(), bookingParty.getName());
                }
            }

            List<OrgWarehouse> orgWarehouses = orgWarehouseService.findOriginByOrgId(aviationBooking.getOrganizationId());
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
            String fileUrl = new StringBuilder("upload/")
                    .append(DateUtil.format("yyyy/MM/", new Date()))
                    .append(aviationBooking.getOrganizationId()).toString();
            url = fileUrl + "/导出订舱信息" + System.currentTimeMillis() + ".xlsx";
            ExcelData data = setExportData(dataGrid.getRows());
            ExportExcelUtils.exportExcel(null, staticFileUpload + url,data);
        } catch (Exception e) {
            logger.info("导出订舱信息" + e.getMessage(), e);
            return "导出订舱信息失败" + e.getMessage();

        }
        return staticFileServer + url;
    }

    private ExcelData setExportData(List<AviationBooking> dataList){
        ExcelData excelData = new ExcelData();
        List<String> tities = new ArrayList<>();
        tities.add("创建时间");
        tities.add("主单号");
        tities.add("订舱公司");
        tities.add("航空公司");
        tities.add("航线");
        tities.add("头程航班号");
        tities.add("二程航班号");
        tities.add("出发地");
        tities.add("中转地");
        tities.add("目的地");
        tities.add("起飞时间");
        tities.add("预估重量(kg)");
        tities.add("总票数");
        tities.add("总件数");
        tities.add("重量(kg)");
        tities.add("总袋数");
        tities.add("是否有效");
        tities.add("使用状态");
        List<List<Object>> rows = new ArrayList<>();
        if(dataList != null) {
            for (AviationBooking booking : dataList){
                List<Object> row = new ArrayList<>();
                row.add(booking.getCreatedDate());
                row.add(booking.getFlightNo());
                row.add(booking.getSystBookingPartyNo());
                row.add(booking.getSystAirlineCompany());
                row.add(booking.getSystAirlineNo());
                row.add(booking.getFirstFlightNo());
                row.add(booking.getSecondFlightNo());
                row.add(booking.getOriginNo());
                row.add(booking.getTransitNo());
                row.add(booking.getDestinationNo());
                row.add(booking.getTakeOffTime());
                row.add(booking.getWeight());
                row.add(booking.getOrderCount());
                row.add(booking.getBoxCount());
                row.add(booking.getWeightSum());
                row.add(booking.getBagCount());
                if(booking.getAvailable() != null && booking.getAvailable()) {
                    row.add("是");
                }else{
                    row.add("否");
                }
                if(booking.getUsed() != null && booking.getUsed()) {
                    row.add("是");
                }else{
                    row.add("否");
                }
                rows.add(row);
            }
        }
        excelData.setRows(rows);
        excelData.setTitles(tities);
        return excelData;
    }
}
