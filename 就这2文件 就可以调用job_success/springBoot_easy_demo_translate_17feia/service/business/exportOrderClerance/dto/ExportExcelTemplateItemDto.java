package com.yangshan.eship.sales.business.exportOrderClerance.dto;

import com.yangshan.eship.author.entity.syst.ExportExcelTemplateItem;

import java.io.Serializable;
import java.util.List;

public class ExportExcelTemplateItemDto implements Serializable {
    private String templateId;
    private List<ExportExcelTemplateItem> items;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public List<ExportExcelTemplateItem> getItems() {
        return items;
    }

    public void setItems(List<ExportExcelTemplateItem> items) {
        this.items = items;
    }
}
