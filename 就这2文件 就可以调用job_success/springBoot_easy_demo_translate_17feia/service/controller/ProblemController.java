package com.yangshan.eship.sales.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.business.CommonProblemBusiness;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.json.ResponseJsonData;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.Calc;
import com.yangshan.eship.finance.dto.fina.FinaProblemType;
import com.yangshan.eship.finance.entity.fina.FinanceCustomerCorrelation;
import com.yangshan.eship.finance.service.fina.FinanceCustomerCorrelationServiceI;
import com.yangshan.eship.order.dto.orde.BatchReturnDto;
import com.yangshan.eship.order.dto.orde.OrderResendDto;
import com.yangshan.eship.order.dto.orde.ProblemCountListDto;
import com.yangshan.eship.order.dto.orde.ProblemDto;
import com.yangshan.eship.order.entity.orde.Order;
import com.yangshan.eship.order.service.orde.OrderServiceI;
import com.yangshan.eship.sales.business.ProblemSalesBusiness;
import com.yangshan.eship.sales.utils.ExportExcelUtil;
import com.yangshan.eship.sales.utils.ExportExcelWrapper;
import com.yangshan.eship.sales.vo.CheckSuchargeVO;
import com.yangshan.eship.sales.vo.ExportProblemVO;
import com.yangshan.eship.sales.vo.FinaProblemInfoVO;
import com.yangshan.eship.sales.vo.OrderResend;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: LinYun
 * @Description:    财务问题件
 * @Date: 11:44 2017/10/25
 * Modified By:
 */
@RestController("problemControllerInSalesLib")
@RequestMapping(Version.VERSION + "/problem")
@Api(value = "ProblemController", tags = "财务问题件")
public class ProblemController {
    private static final Logger logger = LoggerFactory.getLogger(ProblemController.class);
    @Autowired
    private ProblemSalesBusiness problemSalesBusiness;

    @Autowired
    private CommonProblemBusiness commonProblemBusiness;

    @Autowired
    private OrderServiceI orderService;

    @Autowired
    private FinanceCustomerCorrelationServiceI financeCustomerCorrelationServic;

    @Autowired
    private UserServiceI userService;

    /**
     * 获取国内问题件统计列表
     *
     * @param problemDto
     * @return
     */
    @RequestMapping(value = "/findCountList")
    public DataGrid<ProblemCountListDto> findCountList(ProblemDto problemDto) {
        problemDto.setWarehouseId(SessionUtils.getWarehouseId());
        return problemSalesBusiness.findCountList(problemDto);
    }

    /**
     * 获取操作和财务问题件，并进行假分页
     *
     * @param problemDto
     * @return
     */
    @RequestMapping(value = "/findAll")
    public DataGrid<ProblemDto> findAll(ProblemDto problemDto) {
        problemDto.setWarehouseId(SessionUtils.getWarehouseId());
        return problemSalesBusiness.findAll(problemDto);
    }

    /**
     * 获取问题件类型Map
     *
     * @return
     */
    @RequestMapping(value = "/getProblemTypeList")
    public Map getProblemTypeList() {
        return problemSalesBusiness.getProblemTypeList();
    }

    @ApiOperation(value = "完结问题件-一个", notes = "by linyun")
    @RequestMapping(value = "/updateProblem", method = RequestMethod.POST)
    public ResponseJsonData updateProblem(@RequestBody ProblemDto problemDto) {
        return problemSalesBusiness.updateProblem(problemDto);
    }

    @ApiOperation(value = "完结问题件-批量", notes = "by linyun")
    @RequestMapping(value = "/batchUpdateProblem", method = RequestMethod.POST)
    public ResponseJsonData batchUpdateProblem(@RequestBody List<ProblemDto> problemDtos) {
        ResponseJsonData jsonResult = new ResponseJsonData();
        StringBuilder stringBuilder = new StringBuilder();
        for (ProblemDto problemDto : problemDtos) {
            jsonResult = problemSalesBusiness.updateProblem(problemDto);
            if (StringUtils.isNotBlank(jsonResult.getMsg())) {
                stringBuilder.append("操作失败: ");
                stringBuilder.append(problemDto.getInsideNumber() + ",");
            }
        }
        jsonResult.setMsg(stringBuilder.toString());
        return jsonResult;
    }


    /**
     * 国外问题件统计
     *
     * @param problemDto
     * @return
     */
    @RequestMapping(value = "/findForeignCountList")
    public DataGrid<ProblemCountListDto> findForeignCountList(ProblemDto problemDto) {
        problemDto.setWarehouseId(SessionUtils.getWarehouseId());
        return problemSalesBusiness.findForeignCountList(problemDto);
    }

    /**
     * 国外问题件详细
     *
     * @param problemDto
     * @return
     */
    @RequestMapping(value = "/findForeignAll")
    public DataGrid<ProblemDto> findForeignAll(ProblemDto problemDto) {
        problemDto.setWarehouseId(SessionUtils.getWarehouseId());
        return problemSalesBusiness.findForeignAll(problemDto);
    }

    /**
     * 获取国外问题件类型Map
     *
     * @return
     */
    @RequestMapping(value = "/getForeignProblemTypeList")
    public Map getForeignProblemTypeList() {
        return problemSalesBusiness.getForeignProblemTypeList();
    }


    /**
     * 新增派送商问题件
     */
    @RequestMapping(value = "/addLogisticsProblem", method = RequestMethod.POST)
    @ApiOperation(value = "新增派送商问题件")
    public DataGrid addLogisticsProblem(@RequestBody ProblemDto problemDto) {
        DataGrid dataGrid = problemSalesBusiness.checkOrderStatus(problemDto);
        if (dataGrid.isFlag()) {
            return commonProblemBusiness.addProblem(problemDto, null);
        } else {
            return dataGrid;
        }
    }

    @RequestMapping(value = "/import")
    @ApiOperation(value = "导入", notes = "by linyun")
    public DataGrid<Map<String, String>> importExcel(@RequestParam("file") MultipartFile file) {

        DataGrid<Map<String, String>> dataGrid = new DataGrid<>();
        dataGrid.setFlag(true);
        List<ProblemDto> problemDtos = null;
        try {

            InputStream fileInputStream = file.getInputStream();


            problemDtos = problemSalesBusiness.importExcel(fileInputStream);
            StringBuilder rtnNotice = new StringBuilder();
            for (ProblemDto problemDto : problemDtos) {
                DataGrid dg = addLogisticsProblem(problemDto);
                rtnNotice.append(String.format("订单信息：%s+%s; 导入状态：%s \n", problemDto.getInsideNumber(), problemDto.getProblemName(), dg.isFlag() ? "成功" : String.format("失败,原因：%s", dg.getMsg())));
            }
            dataGrid.setMsg(rtnNotice.toString());
            return dataGrid;

        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            dataGrid.setFlag(false);
            dataGrid.setMsg(e.getMessage());
            return dataGrid;
        }

    }

    /**
     * 获取重派订单信息
     *
     * @return
     */
    @RequestMapping(value = "/getOrderResend", method = RequestMethod.GET)
    public OrderResendDto getOrderResend(OrderResend orderResend) {
        return problemSalesBusiness.getOrderResend(orderResend.getProblemId(), orderResend.getOrdeId());
    }

    /**
     * 获取重派国外订单信息
     *
     * @param ordeId
     * @return
     */
    @RequestMapping(value = "/getForeignOrderResend", method = RequestMethod.GET)
    public OrderResendDto getForeignOrderResend(String ordeId) {
        return problemSalesBusiness.getForeignOrderResend(ordeId);
    }

    /**
     * 订单重派
     *
     * @param orderResendDto
     * @return
     */
    @RequestMapping(value = "/saveOrderResend", method = RequestMethod.POST)
    public DataGrid saveOrderResend(@RequestBody OrderResendDto orderResendDto) {
        orderResendDto.setResendFee(Calc.currency(orderResendDto.getResendFee()));
        return problemSalesBusiness.saveOrderResend(orderResendDto);
    }

    /**
     * 是否收取偏远费
     * @param vo
     * @return
     */
    @RequestMapping(value = "/checkExtendedAreaSucharge", method = RequestMethod.POST)
    public DataGrid checkExtendedAreaSucharge(@RequestBody CheckSuchargeVO vo){
        DataGrid dataGrid = new DataGrid();
        String result = orderService.checkExtendedAreaSucharge(vo.getInsideNumber(),vo.getPostCode());
        dataGrid.setFlag(true);
        dataGrid.setObj(result);
        return dataGrid;
    }

    /**
     * 查询自有VAT问题件
     *
     * @param problemDto
     * @return
     */
    @RequestMapping(value = "/findFBAProblemAll", method = RequestMethod.GET)
    public DataGrid<ProblemDto> findFBAProblemAll(ProblemDto problemDto) {
        problemDto.setWarehouseId(SessionUtils.getWarehouseId());
        problemDto.setProblemType(FinaProblemType.FBA_TAXES.name());
        return problemSalesBusiness.findFinanceAll(problemDto);
    }

    /**
     * @author LinYun
     * @date 17:31 2018/5/14
     * @description 自有VAT问题件完结
     */
    @RequestMapping(value = "/finishFBAProblem", method = RequestMethod.POST)
    public DataGrid finishFBAProblem(@RequestBody ProblemDto problemDto) {
        return problemSalesBusiness.finishFBAProblem(problemDto);
    }

    /**
     * 国内问题件批量退回
     */
    @RequestMapping(value = "/batchReturnDomestic", method = RequestMethod.POST)
    @ApiOperation(value = "国内问题件批量退回")
    public DataGrid batchReturnDomestic(@RequestBody BatchReturnDto batchReturnDto) {
        batchReturnDto.setReturnFee(Calc.currency(batchReturnDto.getReturnFee()));
        return problemSalesBusiness.batchReturnDomestic(batchReturnDto);
    }

    /**
     * 国外订单退回
     *
     * @param batchReturnDto
     * @return
     */
    @RequestMapping(value = "/foreignOrderReturn", method = RequestMethod.POST)
    @ApiOperation(value = "国外订单退回")
    public DataGrid foreignOrderReturn(@RequestBody BatchReturnDto batchReturnDto) {
        batchReturnDto.setReturnFee(Calc.currency(batchReturnDto.getReturnFee()));
        return problemSalesBusiness.foreignOrderReturn(batchReturnDto);
    }

    /**
     * 国外订单重派
     *
     * @param orderResendDto
     * @return
     */
    @RequestMapping(value = "/foreignOrderResend", method = RequestMethod.POST)
    @ApiOperation(value = "国外订单重派")
    public DataGrid foreignOrderResend(@RequestBody OrderResendDto orderResendDto) {
        orderResendDto.setResendFee(Calc.currency(orderResendDto.getResendFee()));
        return problemSalesBusiness.foreignOrderResend(orderResendDto);
    }

    /**
     * 国外订单弃件
     *
     * @param batchReturnDto
     * @return
     */
    @RequestMapping(value = "/foreignOrderDiscard", method = RequestMethod.POST)
    @ApiOperation(value = "国外订单弃件")
    public DataGrid foreignOrderDiscard(@RequestBody BatchReturnDto batchReturnDto) {
        batchReturnDto.setReturnFee(Calc.currency(batchReturnDto.getReturnFee()));
        return problemSalesBusiness.foreignOrderDiscard(batchReturnDto);
    }

    /**
     * @author hyl
     * @date 14:00 2018/4/10
     * @description 国外弃件
     */
    @RequestMapping(value = "/returnForeign", method = RequestMethod.POST)
    public DataGrid returnForeign(@RequestBody ProblemDto problemDto) {
        return problemSalesBusiness.returnForeign(problemDto);
    }

    /**
     * @author hyl
     * @date 14:00 2018/4/10F
     * @description 财务模块获取查询的基本信息
     */
    @RequestMapping(value = "/allProblemInfo", method = RequestMethod.GET)
    public DataGrid allProblemInfo() {
        return problemSalesBusiness.allProblemInfo(SessionUtils.getWarehouseId(), SessionUtils.getOrganizationId());
    }

    /**
     * 获取操作和财务问题件，并进行假分页
     *
     * @param problemDto
     * @return
     */
    @RequestMapping(value = "/getAllProblem", method = RequestMethod.GET)
    public DataGrid<ProblemDto> getAllProblem(ProblemDto problemDto) {
        problemDto.setWarehouseId(SessionUtils.getWarehouseId());
        return problemSalesBusiness.getAllProblem(problemDto);
    }

    /**
     * 获取操作和财务问题件，并进行假分页
     *
     * @param problemDto
     * @return
     */
    @RequestMapping(value = "/exportProblem", method = RequestMethod.GET)
    @ApiOperation(value = "导出", notes = "by linyun")
    public void exportProblem(HttpServletResponse response, ProblemDto problemDto) {
        problemDto.setWarehouseId(SessionUtils.getWarehouseId());

        List<ProblemDto> problemRequestVOS = problemSalesBusiness.exportAllProblem(problemDto).getRows();

        List<Order> orders = orderService.findByIdIn(
                problemRequestVOS.stream().map(ProblemDto::getOrdeOrderId).collect(Collectors.toList()));
        List<ExportProblemVO> exportProblemVOS = new ArrayList<>();

        problemRequestVOS.forEach(p -> {

            Order order = orders.stream().filter(o -> o.getId().equals(p.getOrdeOrderId())).findFirst().get();

            ExportProblemVO exportProblemVO = new ExportProblemVO();
            BeanUtils.copyProperties(p, exportProblemVO);
            exportProblemVO.setDestinationName(order.getDestinationName());
            exportProblemVO.setInsideNumber(order.getInsideNumber());
            exportProblemVO.setTakeDeliveryTime(order.getTakeDeliveryTime());
            exportProblemVO.setActualWeight(order.getActualWeight());
            exportProblemVOS.add(exportProblemVO);
        });

        String[] columnNames = {"订单号","问题类型", "问题描述", " 客户编号", " 客户名称", "销售名称", "目的地", "产品名称", "重量", "入库时间"};
        String fileName = "ProblemList_" + System.currentTimeMillis();
        ExportExcelWrapper<ExportProblemVO> exportExcelWrapper = new ExportExcelWrapper<>();
        exportExcelWrapper.exportExcel(fileName, fileName, columnNames, exportProblemVOS, response, ExportExcelUtil.EXCEL_FILE_2003);
    }


    /**
     * @author LinYun
     * @date 14:00 2018/4/10F
     * @description 财务模块获取查询的基本信息
     */
    @RequestMapping(value = "/financeProblemInfo", method = RequestMethod.GET)
    public DataGrid get() {

        return problemSalesBusiness.finaProblemInfo(SessionUtils.getWarehouseId(), SessionUtils.getOrganizationId());
    }

    /**
     * @author LinYun
     * @date 14:00 2018/4/10    update By yuchao 财务
     * @description 财务模块查询国内问题件
     */
    @RequestMapping(value = "/findFinaProblem", method = RequestMethod.POST)
    @ApiOperation(value = "财务模块查询国内问题件", notes = "create by linyun  / update by yuchao")
    public DataGrid findFinaProblem(@RequestBody ProblemDto problemDto) {

        if (StringUtils.isEmpty(problemDto.getProblemType())) {
            //获取财务问题件问题类型
            List<FinaProblemInfoVO.ProblemType> problemTypes = problemSalesBusiness.getFinaProblemTypes();
            List<String> problemNames = problemTypes.stream().map(problemType -> problemType.getValue()).collect(Collectors.toList());
            problemDto.setProblemTypeList(problemNames);
        }

        problemDto.setOrganizationId(SessionUtils.getOrganizationId());
//        problemDto.setOrganizationId(SessionUtils.getOrganizationId());
//        分公司id 如果没有传入分公司id 那么从session获取 yuchao
        if (StringUtils.isEmpty(problemDto.getWarehouseId())) {
            problemDto.setWarehouseId(SessionUtils.getWarehouseId());
        }
        if (problemDto.isCustomCorrelation()) {
            FinanceCustomerCorrelation customerCorrelation = new FinanceCustomerCorrelation();
            customerCorrelation.setFinanceId(SessionUtils.getUserId());
            List<String> customerIds = new ArrayList<>();
            List<String> customerCodes = new ArrayList<>();
            List<FinanceCustomerCorrelation> correlations = financeCustomerCorrelationServic.list(customerCorrelation);
            customerIds.add("0");
            for (FinanceCustomerCorrelation customerCorrelation1 : correlations) {
                customerIds.add(customerCorrelation1.getCustomerId());
            }
            Map<String, User> map = userService.mapByIds(customerIds);
            for (FinanceCustomerCorrelation customerCorrelation1 : correlations) {
                if (map.get(customerCorrelation1.getCustomerId()) != null) {
                    customerCodes.add(map.get(customerCorrelation1.getCustomerId()).getCustomerCode());
                }
            }
            problemDto.setCustomerCodes(customerCodes);
        }
        return problemSalesBusiness.findFinaProblem(problemDto);
    }


}
