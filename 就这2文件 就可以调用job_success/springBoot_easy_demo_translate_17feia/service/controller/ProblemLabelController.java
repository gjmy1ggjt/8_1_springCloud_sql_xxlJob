package com.yangshan.eship.sales.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yangshan.eship.common.excel.ExcelData;
import com.yangshan.eship.common.excel.ExportExcelUtils;
import com.yangshan.eship.order.entity.avia.AviationOrder;
import com.yangshan.eship.order.service.avia.AviationOrderServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.syst.LogisticsSupplierItem;
import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.syst.LogisticsSupplierItemServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.order.dto.labelex.ExLabelWorkingTableDto;
import com.yangshan.eship.order.dto.labelex.ProblemLabelSearchDataDto;
import com.yangshan.eship.sales.business.ProblemLabelBusiness;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author: hyl
 * @Date: 2018/3/5 14:00
 * @Description:
 */
@RestController
@RequestMapping(Version.VERSION + "/problemLabel")
@Api(value = "ProblemLabelController", tags = "问题标签列表")
public class ProblemLabelController {
    private static Logger logger = LoggerFactory.getLogger(ProblemLabelController.class);

    @Autowired
    private ProblemLabelBusiness problemLabelBusiness;

    @Autowired
    private LogisticsSupplierItemServiceI logisticsSupplierItemService;

    @Autowired
    private AviationOrderServiceI aviationOrderService;

    /**
     * 根据搜索条件查询问题标签列表
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:43
     * @Description:
     */
    @RequestMapping(value = "/list", method = RequestMethod.POST)
    @JsonFilter(type = ExLabelWorkingTableDto.class, exclude = "userId,createdDate,prodProductId")
    @ApiOperation(value = "获取问题标签列表", notes = "by hyl", httpMethod = "POST")
    public DataGrid<ExLabelWorkingTableDto> getProblemLabelList(@RequestBody ProblemLabelSearchDataDto problemLabelSearchDataDto) {
        //设置组织id
        problemLabelSearchDataDto.setOrganizationId(SessionUtils.getOrganizationId());
        problemLabelSearchDataDto.setWarehouseId(SessionUtils.getWarehouseId());
        //根据搜索条件查询问题标签列表
        return problemLabelBusiness.getProblemLabelList(problemLabelSearchDataDto);
    }

    @RequestMapping(value = "/aviaOrderNoSearch", method = RequestMethod.GET)
    @JsonFilter(type = AviationOrder.class, include = "id,aviationOrderNo")
    @ApiOperation(value = "根据主单号查询主单", notes = "by hyl", httpMethod = "GET")
    public DataGrid<AviationOrder> aviaOrderNoSearch(String query) {
        return aviationOrderService.findByAviationOrderNoLikeAndWarehouseId(query, SessionUtils.getWarehouseId(), 0, 10);
    }

    /**
     * 根据搜索条件查询问题标签列表
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:43
     * @Description:
     */
    @RequestMapping(value = "/exportProblemLabelExcelList", method = RequestMethod.POST)
    @JsonFilter(type = ExLabelWorkingTableDto.class, exclude = "userId,createdDate,prodProductId")
    @ApiOperation(value = "导出问题标签列表", notes = "by hyl", httpMethod = "POST")
    public void exportProblemLabelExcelList(@RequestBody ProblemLabelSearchDataDto problemLabelSearchDataDto, HttpServletResponse httpServletResponse) throws Exception {
        //设置组织id
        problemLabelSearchDataDto.setOrganizationId(SessionUtils.getOrganizationId());
        problemLabelSearchDataDto.setWarehouseId(SessionUtils.getWarehouseId());
        //根据搜索条件查询问题标签列表
        DataGrid<ExLabelWorkingTableDto> dataGrid = problemLabelBusiness.getProblemLabelList(problemLabelSearchDataDto);

        List<ExLabelWorkingTableDto> exLabelWorkingTableDtos = dataGrid.getRows();

        ExcelData excelData = new ExcelData();
        List<String> titles = Lists.newArrayList();

        //titleValueMap
        Map<String, String> titleValueMap = Maps.newLinkedHashMap();
        titleValueMap.put("失败时间", "labelCreatedDateStr");
        titleValueMap.put("主单号", "aviaOrderNo");
        titleValueMap.put("内单号", "insideNumber");
        titleValueMap.put("参考号", "referenceNo");
        titleValueMap.put("客户编号", "customerCode");
        titleValueMap.put("渠道名称", "productName");
        titleValueMap.put("包裹重量", "actualWeight");
        titleValueMap.put("目的国名称", "destinationName");
        titleValueMap.put("下单时间", "createdDateStr");
        titleValueMap.put("收货时间", "takeDeliveryTimeStr");
        titleValueMap.put("派送商名称", "logisticsName");
        titleValueMap.put("问题描述", "apiErrorMsg");

        //加入title
        for (Map.Entry<String, String> titleValueMapEntry : titleValueMap.entrySet()) {
            titles.add(titleValueMapEntry.getKey());
        }
        //开始加入值、
        List<List<Object>> rows = Lists.newArrayList();
        if (exLabelWorkingTableDtos.size() > 0) {
            for (int i = 0; i < exLabelWorkingTableDtos.size(); i++) {
                ExLabelWorkingTableDto exLabelWorkingTableDto = exLabelWorkingTableDtos.get(i);
                List<Object> row = Lists.newArrayList();
                for (String key : titles) {
                    String value = titleValueMap.get(key);
                    Field field = exLabelWorkingTableDto.getClass().getDeclaredField(value);
                    field.setAccessible(true);
                    row.add(field.get(exLabelWorkingTableDto));
                    field.setAccessible(false);
                }
                rows.add(row);
            }
        }
        excelData.setTitles(titles);
        excelData.setRows(rows);

        ExportExcelUtils.exportExcel(
                httpServletResponse,
                MessageFormat.format("问题标签列表-{0}.xlsx", new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date())),
                excelData
        );
    }


    @RequestMapping(value = "/getLogistics", method = RequestMethod.GET)
    @ApiOperation(value = "获取派送商列表", notes = "by hyl", httpMethod = "GET")
    public DataGrid<LogisticsSupplierItem> getLogistics() {
        DataGrid<LogisticsSupplierItem> dataGrid = logisticsSupplierItemService.findByCountryNos(null, SessionUtils.getOrganizationId());
        dataGrid.setFlag(true);
        dataGrid.setTotal(dataGrid.getRows().size());
        return dataGrid;
    }


    /**
     * 获取目的国列表
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:43
     * @Description:
     */
    @RequestMapping(value = "/getCountryList", method = RequestMethod.GET)
    @JsonFilter(type = ExLabelWorkingTableDto.class, exclude = "userId,createdDate,prodProductId")
    @ApiOperation(value = "获取目的国列表", notes = "by hyl", httpMethod = "GET")
    public DataGrid<Region> getCountryList() {
        return problemLabelBusiness.getCountryList();
    }


}
