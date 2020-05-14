package com.yangshan.eship.sales.controller.statistics;

import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.entity.cashout.BeforeBookedWay;
import com.yangshan.eship.author.entity.cashout.CashoutApplication;
import com.yangshan.eship.author.entity.cashout.CashoutState;
import com.yangshan.eship.author.entity.cashout.CashoutType;
import com.yangshan.eship.author.entity.upload.StaticFileObj;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.exception.EshipRedirectException;
import com.yangshan.eship.finance.dto.cust.WalletDto;
import com.yangshan.eship.sales.business.statistics.CashoutApplicationBusiness;
import com.yangshan.eship.sales.controller.Version;
import com.yangshan.eship.sales.dto.CashoutApplicationResponseDto;
import com.yangshan.eship.sales.dto.CashoutApplicationSearchDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Created by Hukai
 * 2018-08-13 0013 上午 11:25
 * 提现申请
 */
@RestController
@RequestMapping(Version.VERSION + "/cashoutApplication")
@Api(value = "CashoutApplicationController",  tags = "提现申请管理")
public class CashoutApplicationController {

    private Logger logger = LoggerFactory.getLogger(CashoutApplicationController.class);

    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Value("${static.file.server}")
    private String staticFileServer;

    @Autowired
    private CashoutApplicationBusiness cashoutApplicationBusiness;

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 上午 11:28
     * @Description: 查找客户自己的申请提现记录
     */
    @RequestMapping(value = "/listByCustomer", method = RequestMethod.POST)
    @ApiOperation(value = "查找客户自己的申请提现记录", notes = "by Hukai")
    public DataGrid<CashoutApplication> listByCustomer(@RequestBody CashoutApplication cashoutApplication) {

        return cashoutApplicationBusiness.listByCustomer(cashoutApplication);
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 上午 11:31
     * @Description: 查找销售下所有客户提交的申请提现记录
     */
    @RequestMapping(value = "/listBySale", method = RequestMethod.POST)
    @ApiOperation(value = "查找销售下所有客户提交的申请提现记录", notes = "by Hukai")
    public DataGrid<CashoutApplication> listBySale(@RequestBody CashoutApplication cashoutApplication) {
        cashoutApplication.setCashoutState(CashoutState.APPLYED);
        return cashoutApplicationBusiness.listBySale(cashoutApplication);
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 上午 11:33
     * @Description: 财务提现记录列表
     */
    @RequestMapping(value = "/listByFinance/pending", method = RequestMethod.POST)
    @ApiOperation(value = "财务提现记录列表-待处理tab", notes = "by Hukai")
    public DataGrid<CashoutApplication> listByFinanceOfPending(@RequestBody CashoutApplication cashoutApplication) {
        cashoutApplication.setProcessedFlag(false);
        cashoutApplication.setRoleCode(User.UserRoleCode.valueOf(SessionUtils.getCurrentRoleCode()));
        return cashoutApplicationBusiness.listByFinance(cashoutApplication);
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 上午 11:33
     * @Description: 财务提现记录列表
     */
    @RequestMapping(value = "/listByFinance/processed", method = RequestMethod.POST)
    @ApiOperation(value = "财务提现记录列表-已处理tab", notes = "by Hukai")
    public DataGrid<CashoutApplication> listByFinanceOfProcessed(@RequestBody CashoutApplication cashoutApplication) {
        cashoutApplication.setProcessedFlag(true);
        cashoutApplication.setRoleCode(User.UserRoleCode.valueOf(SessionUtils.getCurrentRoleCode()));
        return cashoutApplicationBusiness.listByFinance(cashoutApplication);
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 下午 2:29
     * @Description: 获取所有提现方式
     */
    @RequestMapping(value = "/listCashTypes", method = RequestMethod.GET)
    @ApiOperation(value = "获取所有提现方式", notes = "by Hukai")
    public DataGrid listCashTypes() {

        return new DataGrid<>(true, CashoutType.listAll());
    }

    /**
     * @Author: Kevin
     * @Date: 2019-05-23 10:46
     * @Description: 获取所有以前入账方式
     */
    @RequestMapping(value = "/listBeforeBookedWay", method = RequestMethod.GET)
    @ApiOperation(value = "获取所有以前入账方式", notes = "by Hukai")
    public DataGrid listBeforeBookedWay() {

        return new DataGrid<>(true, BeforeBookedWay.listAll());
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 下午 2:29
     * @Description: 获取所有提现状态
     */
    @RequestMapping(value = "/listCashStates", method = RequestMethod.GET)
    @ApiOperation(value = "获取所有提现状态", notes = "by Hukai")
    public DataGrid listCashStates() {

        return new DataGrid<>(true, CashoutState.listAll());
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 下午 12:43
     * @Description: 客户提交一条提现记录
     */
    @ApiOperation(value = "客户提交一条提现记录", notes = "by Hukai")
    @RequestMapping(value = "/addOrUpdate", method = RequestMethod.POST)
    public DataGrid addOrUpdate(@RequestBody CashoutApplication cashoutApplication) {

        return cashoutApplicationBusiness.addOrUpdate(cashoutApplication);
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 下午 12:45
     * @Description: 提交的记录详情
     */
    @RequestMapping(value = "/findById/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "获取单条提现记录详细信息", notes = "by Hukai")
    public DataGrid findById(@PathVariable("id") String id) {

        return cashoutApplicationBusiness.findById(id);
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 下午 12:50
     * @Description: 财务上传凭证
     */
    @ApiOperation(value = "财务上传凭证", notes = "by Hukai")
    @RequestMapping(value = "/uploadCertificate/{cashoutApplicationId}", method = RequestMethod.POST)
    public DataGrid<String> uploadCertificate(@RequestParam("file") CommonsMultipartFile file, @PathVariable("cashoutApplicationId") String cashoutApplicationId) {
        DataGrid dataGrid = new DataGrid<>();
        StaticFileObj staticFileObj = new StaticFileObj();

        try {
            file.getFileItem().getName();
            String saveDir = "customer/" + SessionUtils.getOrganizationId() + "/certificate/";
            String rootDir = staticFileUpload + saveDir;
            File root = new File(rootDir);
            if (!root.exists()) {
                root.mkdirs();
            }
            String realFileName = file.getOriginalFilename();
            int index = realFileName.lastIndexOf(".");
            String fileName = System.currentTimeMillis() + realFileName.substring(index);
            String filePath = rootDir + fileName;
            String savePath = "/" + saveDir + fileName;
            File newFile = new File(filePath);
            file.transferTo(newFile);

            staticFileObj.setDownloadPath(staticFileServer + savePath);
            staticFileObj.setFileName(file.getFileItem().getName());
            staticFileObj.setSavePath(savePath);

            dataGrid.setFlag(true);
            dataGrid.setMsg("上传成功！");
            dataGrid.setObj(staticFileObj);

            logger.debug("上传的凭证url: {}", staticFileObj.getDownloadPath());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            throw new EshipException("上传文件失败", e);
        }

        //更新凭证文件url到数据库
        cashoutApplicationBusiness.updateCertificate(cashoutApplicationId, staticFileObj.getSavePath());

        return dataGrid;
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 下午 6:13
     * @Description: 财务审核通过
     */
    @RequestMapping(value = "/applyPassed", method = RequestMethod.POST)
    @ApiOperation(value = "财务审核通过", notes = "by Hukai")
    public DataGrid applyPassed(@RequestBody CashoutApplication cashoutApplication) {

        return cashoutApplicationBusiness.applyPassed(cashoutApplication);
    }

    @RequestMapping(value = "/applyCancel/{applyId}", method = RequestMethod.POST)
    @ApiOperation(value = "撤销申请", notes = "by Hukai")
    public DataGrid applyCancel(@PathVariable String applyId) {

        return cashoutApplicationBusiness.applyCancel(applyId);
    }

    /**
     * @Author: Tsd
     * @Date: 2018-03-14
     * @Description: 获取当前客户的账户余额
     */
    @RequestMapping(value = "/getCustomerBalanceConsume/{customerId}", method = RequestMethod.GET)
    @ApiOperation(value = "获取当前客户的账户余额", notes = "by Hukai")
    public DataGrid<BigDecimal> getCustomerBalanceConsume(@PathVariable("customerId") String customerId) {
        WalletDto walletDto = cashoutApplicationBusiness.getCustomerBalanceConsume(customerId);
        DataGrid<BigDecimal> decimalDataGrid = new DataGrid<>();
        if(walletDto != null) {
            decimalDataGrid.setFlag(true);
            decimalDataGrid.setObj(walletDto.getCurrentAmount());
        }else{
            decimalDataGrid.setFlag(false);
            decimalDataGrid.setMsg("客户不存在");
        }
        return decimalDataGrid;
    }

    /**
     * @Author: HuKai
     * @Date: 2018-08-13 0013 下午 6:13
     * @Description: 财务审核驳回
     */
    @RequestMapping(value = "/applyReject", method = RequestMethod.POST)
    @ApiOperation(value = "财务审核驳回", notes = "by Hukai")
    public DataGrid applyReject(@RequestBody CashoutApplication cashoutApplication) {

        return cashoutApplicationBusiness.applyReject(cashoutApplication);
    }

    @RequestMapping(value = "/listByCoo", method = RequestMethod.POST)
    @ApiOperation(value = "运营总监-客户提现列表", notes = "by Hukai")
    public DataGrid listByCoo(@RequestBody CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }
        cashoutApplicationSearchDto.setWarehouseId(warehouseId);
        cashoutApplicationSearchDto.setRoleCode(User.UserRoleCode.valueOf(SessionUtils.getCurrentRoleCode()));

        return cashoutApplicationBusiness.listByCoo(cashoutApplicationSearchDto);
    }

    @RequestMapping(value = "/listBySalesManager", method = RequestMethod.POST)
    @ApiOperation(value = "销售经理-客户提现列表", notes = "by Hukai")
    public DataGrid listBySalesManager(@RequestBody CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }
        cashoutApplicationSearchDto.setWarehouseId(warehouseId);
        cashoutApplicationSearchDto.setRoleCode(User.UserRoleCode.valueOf(SessionUtils.getCurrentRoleCode()));

        return cashoutApplicationBusiness.listBySalesManager(cashoutApplicationSearchDto);
    }

    @RequestMapping(value = "/listBySales/pending", method = RequestMethod.POST)
    @ApiOperation(value = "销售-客户提现列表-待处理", notes = "by Hukai")
    public DataGrid listPendingDataBySales(@RequestBody CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }
        cashoutApplicationSearchDto.setRoleCode(User.UserRoleCode.valueOf(SessionUtils.getCurrentRoleCode()));
        cashoutApplicationSearchDto.setWarehouseId(warehouseId);
        cashoutApplicationSearchDto.setProcessedFlag(false);

        return cashoutApplicationBusiness.listBySales(cashoutApplicationSearchDto);
    }

    @RequestMapping(value = "/listBySalesManager/processed", method = RequestMethod.POST)
    @ApiOperation(value = "销售-客户提现列表-已处理", notes = "by Hukai")
    public DataGrid<CashoutApplicationResponseDto> listProcessedDataBySales(@RequestBody CashoutApplicationSearchDto cashoutApplicationSearchDto) {
        String warehouseId = SessionUtils.getWarehouseId();
        if (StringUtils.isBlank(warehouseId)) {
            throw new EshipRedirectException("请先登录!");
        }
        cashoutApplicationSearchDto.setWarehouseId(warehouseId);
        cashoutApplicationSearchDto.setProcessedFlag(true);
        cashoutApplicationSearchDto.setRoleCode(User.UserRoleCode.valueOf(SessionUtils.getCurrentRoleCode()));

        return cashoutApplicationBusiness.listBySales(cashoutApplicationSearchDto);
    }

    /**
     * @Author: tsd
     * @Date: 2018-08-13 001d3 上午 11:33
     * @Description: 财务提现记录个数
     */
    @RequestMapping(value = "/countFinanceOfPending", method = RequestMethod.GET)
    @ApiOperation(value = "财务提现记录列表-待处理tab", notes = "by Hukai")
    public DataGrid<CashoutApplication> countFinanceOfPending() {
        CashoutApplication cashoutApplication = new CashoutApplication();
        cashoutApplication.setProcessedFlag(false);
        cashoutApplication.setRoleCode(User.UserRoleCode.valueOf(SessionUtils.getCurrentRoleCode()));
        DataGrid dataGrid = cashoutApplicationBusiness.listByFinance(cashoutApplication);
        DataGrid returnGrid = new DataGrid();
        returnGrid.setFlag(true);
        returnGrid.setTotal(dataGrid.getTotal());
        return returnGrid;
    }
}
