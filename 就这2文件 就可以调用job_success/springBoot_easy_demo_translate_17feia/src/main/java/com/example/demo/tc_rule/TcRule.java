package com.example.demo.tc_rule;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Administrator on 2020/4/28.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TcRule {

    //0表示无期限; 1表示指定时间范围

    private int timeType;

//规则开始时间和结束时间。当timeType=1时，有效。格式如"2020-01-01"

    private String startDate;

    private String endDate;

//timeType=1时，有效。生效的星期几，取值0-6，周日是0,用|分隔。如果星期1, 2，则存储 1|2

    private String week;

//timeType=1时，有效。每天生效的时间。精确到分钟。格式如“21:00”

    private String startTime;

    private String endTime;

//0表示所有员工，1表示指定的员工

    private int personType;

//如果personType=1时，有效。用竖线隔开，每个都是员工ID，为整数。比如1|210

    private String personList;

}
