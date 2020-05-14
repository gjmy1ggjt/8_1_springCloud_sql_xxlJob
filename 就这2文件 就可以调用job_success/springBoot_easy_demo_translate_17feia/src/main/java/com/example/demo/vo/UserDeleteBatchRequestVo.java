package com.example.demo.vo;

import lombok.Data;

import java.util.List;

/**
 * Created by Administrator on 2020/3/8.
 */
@Data
public class UserDeleteBatchRequestVo {

   private List<String> listUserId;

}
