package com.example.demo.translate_work.excel;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class ExcelData implements Serializable{

    private static final long serialVersionUID = 4444017239100620999L;
    // 定制表头(多个)
    private List<List<String>> titleList;
    // 表头
    private List<String> titles;

    // 数据
    private List<List<Object>> rows;

    // 定制表尾(多个)
    private List<List<Object>> endList;

    //设置下拉选项 key:下标 value:下拉数据
    private Map<Integer,String[]> cellRangeMap;

    // 页签名称
    private String name;

    //单元格合并坐标定[开始行,结束行,开始列,结束列]
    private List<Integer[]> cellRangeArray;
}
