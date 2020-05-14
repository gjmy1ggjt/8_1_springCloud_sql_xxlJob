package com.example.demo.vo;

import com.example.demo.utils.PagingDto;
import lombok.Data;

/**
 * Created by Administrator on 2020/1/23.
 */
@Data
public class UserListRequestVo {

//    @ApiModelProperty(name = "aviaOrderNo", value = "用户名称")
    private String userName;

//    @ApiModelProperty(name = "supplierServiceId", value = "用户id")
    private String userId;

//    @ApiModelProperty(name = "pageable", value = "分页对象")
    private PagingDto pagingDto;
}
