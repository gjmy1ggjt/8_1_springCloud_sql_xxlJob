package com.example.demo.job;

/**
 * Created by Administrator on 2020/4/28.
 */

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import org.springframework.stereotype.Component;

import static com.xxl.job.core.biz.model.ReturnT.SUCCESS;

/**
 * 任务Handler示例（Bean模式）
 * 开发步骤：
 * 1、继承"IJobHandler"：“com.xxl.job.core.handler.IJobHandler”；
 * 2、注册到Spring容器：添加“@Component”注解，被Spring容器扫描为Bean实例；
 * 3、注册到执行器工厂：添加“@JobHandler(value="自定义jobhandler名称")”注解，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 4、执行日志：需要通过 "XxlJobLogger.log" 打印执行日志；
 * 5、任务执行结果枚举：SUCCESS、FAIL、FAIL_TIMEOUT
 */
//@JobHandler(value = "testJobHandler")
@Component
public class DemoJobHandler  {
//public class DemoJobHandler extends IJobHandler {

    @XxlJob(value = "testJobHandler")
    public ReturnT<String> execute(String param) throws Exception{
        XxlJobLogger.log("XXL-JOB, testJobHandler.");
        System.out.println("XXL-JOB测试");
        return SUCCESS;
    }

}
