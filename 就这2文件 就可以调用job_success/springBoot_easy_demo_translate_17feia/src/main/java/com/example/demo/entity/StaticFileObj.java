package com.example.demo.entity;

import lombok.Data;

/**
 * Created by Administrator on 2020/3/7.
 */
@Data
public class StaticFileObj {

    //文件名
    private String fileName ;

    //保存路径
    private String savePath;

    //下载路径
    private String downloadPath;
}
