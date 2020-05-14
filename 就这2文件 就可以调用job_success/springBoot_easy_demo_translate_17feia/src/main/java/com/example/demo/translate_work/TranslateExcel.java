package com.example.demo.translate_work;

import lombok.Data;

import java.util.List;

/**
 * Created by Administrator on 2020/3/1.
 */
@Data
public class TranslateExcel {

    private int num;

    private String className;

    private String methodName;

    private String mean;

    private List<String> chinese;

    private List<String> english;

}
