package com.yangshan.eship.sales.business.statistics.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(value = "ParameterOptionDto", description = "保存图表参数")
public class StatitctisDrawByRole {
    @ApiModelProperty(name = "roleCode", value = "角色代码")
    private String roleCode;

    @ApiModelProperty(name = "drawBusinessNames", value = "图表集合")
    private List<String[]> drawBusinessNames;
}
