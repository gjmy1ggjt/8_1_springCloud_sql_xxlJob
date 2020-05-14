package com.example.demo.utils;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * DataGrid 数据返回模型
 *
 * @author fansheng
 */
@Data
public class DataGrid<T> implements Serializable {

    //---------------------------------普通返回 start
    private T obj;

    private boolean flag;

    private String msg;

    private String code;
    //---------------------------------普通返回 end

    //---------------------------------分页 start
    //数量
    private long total;
    //所有数据
    private List<T> rows = new ArrayList<>();

    public DataGrid() {
        super();
    }

    public DataGrid(boolean flag, List<T> rows) {
        this.flag = flag;
        this.rows = rows;
        if(rows != null){
            this.total = rows.size();
        }
    }
    public DataGrid(boolean flag, List<T> rows, long total) {
        this.flag = flag;
        this.rows = rows;
        if(rows != null){
            this.total = total;
        }
    }
    public DataGrid(boolean flag, T obj) {
        this.obj = obj;
        this.flag = flag;
    }

    public DataGrid(boolean flag, String msg) {
        this.msg = msg;
        this.flag = flag;
    }

    public DataGrid(long total, List<T> rows) {
        super();
        this.total = total;
        this.rows = rows;
    }

    public DataGrid(boolean flag, long total) {
        this.flag = flag;
        this.total = total;
    }


}
