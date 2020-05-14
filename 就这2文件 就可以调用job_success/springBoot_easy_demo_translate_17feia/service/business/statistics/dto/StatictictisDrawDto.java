package com.yangshan.eship.sales.business.statistics.dto;

import java.util.List;

public class StatictictisDrawDto {
    private String type;
    private List<StatitctisDrawByRole> addList;

    public List<StatitctisDrawByRole> getAddList() {
        return addList;
    }

    public void setAddList(List<StatitctisDrawByRole> addList) {
        this.addList = addList;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
