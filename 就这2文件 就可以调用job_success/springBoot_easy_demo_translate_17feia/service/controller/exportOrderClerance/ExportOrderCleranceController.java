package com.yangshan.eship.sales.controller.exportOrderClerance;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.syst.ExportExcelTemplate;
import com.yangshan.eship.author.entity.syst.ExportExcelTemplateItem;
import com.yangshan.eship.author.service.syst.ExportExcelTemplateItemServiceI;
import com.yangshan.eship.author.service.syst.ExportExcelTemplateServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.sales.business.exportOrderClerance.dto.ExportExcelTemplateItemDto;
import com.yangshan.eship.sales.controller.Version;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Author: TuShiDing
 * @Description: 清关导出模版
 * @Date: 下午 5:10 2018/07/17
 */
@RestController
@RequestMapping(Version.VERSION + "/exportOrderClerance")
public class ExportOrderCleranceController {
    @Autowired
    private ExportExcelTemplateItemServiceI exportExcelTemplateItemService;

    @Autowired
    private ExportExcelTemplateServiceI exportExcelTemplateService;


    /**
     * @Author: TuShiDing
     * @Description: 清关导出自定义模版列表
     * @Param:
     * @Date: 下午 2:05 2018/07/21
     */
    @RequestMapping(value = "/templateList", method = RequestMethod.GET)
    public DataGrid templateList() {
        List<ExportExcelTemplate> list = exportExcelTemplateService.findExportExcelTemplates(ExportExcelTemplate.ExportExcelTemplateType.ORDER_CLERANCE);
        DataGrid dataGrid = new DataGrid();
        dataGrid.setRows(list);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * @Author: TuShiDing
     * @Description: 增加 清关导出 自定义模版
     * @Param:
     * @Date: 下午 2:05 2018/07/21
     */
    @RequestMapping(value = "/addTemplate", method = RequestMethod.POST)
    public DataGrid addTemplate(String templateId,String templateName) {
        ExportExcelTemplate exportExcelTemplate = new ExportExcelTemplate();
        if(!"".equals(templateId)) {
            exportExcelTemplate.setId(templateId);
        }
        exportExcelTemplate.setTemplateName(templateName);
        exportExcelTemplate.setDefaultTemplate(true);
        exportExcelTemplate.setTemplateType(ExportExcelTemplate.ExportExcelTemplateType.ORDER_CLERANCE);
        exportExcelTemplateService.addOrUpdateExportExcelTemplate(exportExcelTemplate);
        return templateList();
    }


    /**
     * @Author: TuShiDing
     * @Description: 删除清关导出自定义模版
     * @Param:
     * @Date: 下午 2:05 2018/07/21
     */
    @RequestMapping(value = "/delTemplate", method = RequestMethod.POST)
    public DataGrid delTemplate(String templateId) {
        exportExcelTemplateService.delExportExcelTemplate(templateId);
        exportExcelTemplateItemService.delTemplateItemsByTempId(templateId);
        return templateList();
    }

    /**
     * @Author: TuShiDing
     * @Description: 查询清关导出自定义模版列表字段详情
     * @Param:
     * @Date: 下午 2:05 2018/07/21
     */
    @RequestMapping(value = "/templateItemList", method = RequestMethod.POST)
    public DataGrid templateItemList(String templateId) {
        List<ExportExcelTemplateItem> list = exportExcelTemplateItemService.findTemplateItems(templateId);
        DataGrid dataGrid = new DataGrid();
        dataGrid.setRows(list);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * @Author: TuShiDing
     * @Description: 清关导出自定义模版列表字段详情
     * @Param:
     * @Date: 下午 2:05 2018/07/21
     */
    @RequestMapping(value = "/addTemplateItem", method = RequestMethod.POST)
    public DataGrid addTemplateItem(@RequestBody ExportExcelTemplateItemDto exportExcelTemplateItemDto) {
        exportExcelTemplateItemService.delTemplateItemsByTempId(exportExcelTemplateItemDto.getTemplateId());
        exportExcelTemplateItemService.addTemplateItems(exportExcelTemplateItemDto.getItems());
        return templateItemList(exportExcelTemplateItemDto.getTemplateId());
    }


}
