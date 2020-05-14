package com.yangshan.eship.sales.business;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yangshan.eship.author.dto.account.Customer;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.author.service.syst.RabbitMessageServiceI;
import com.yangshan.eship.business.CommonWeighingBusiness;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.json.ResponseJsonData;
import com.yangshan.eship.common.rabbit.RabbitMessage;
import com.yangshan.eship.common.rabbit.RabbitMessageActionsConstant;
import com.yangshan.eship.common.rabbit.RabbitMessageQueueKeyConstant;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.ArrayUtil;
import com.yangshan.eship.constants.ErrorCodeConstant;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.finance.dto.fina.FinaProblemStatus;
import com.yangshan.eship.finance.dto.fina.FinaProblemType;
import com.yangshan.eship.finance.dto.fina.FinaReturnGoodsDto;
import com.yangshan.eship.finance.dto.fina.FinaReturnGoodsType;
import com.yangshan.eship.finance.entity.fina.FinanceOrder;
import com.yangshan.eship.finance.entity.fina.FinanceOrderMessage;
import com.yangshan.eship.finance.entity.fina.Problem;
import com.yangshan.eship.finance.service.fina.DunningTaskServiceI;
import com.yangshan.eship.finance.service.fina.FinaProblemServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderItemServiceI;
import com.yangshan.eship.finance.service.fina.FinanceOrderServiceI;
import com.yangshan.eship.order.dto.orde.*;
import com.yangshan.eship.order.entity.orde.Box;
import com.yangshan.eship.order.entity.orde.OperateHistory;
import com.yangshan.eship.order.entity.orde.OperateProblem;
import com.yangshan.eship.order.entity.orde.Order;
import com.yangshan.eship.order.service.orde.*;
import com.yangshan.eship.product.entity.surc.SurchargeType;
import com.yangshan.eship.product.service.SurchargeServiceI;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import com.yangshan.eship.sales.vo.FinaProblemInfoVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: LinYun
 * @Description:问题件统计
 * @Date: 9:58 2017/10/25
 * Modified By:
 */
@Service
public class ProblemSalesBusiness {

    private static final Logger logger = LoggerFactory.getLogger(ProblemSalesBusiness.class);

    @Autowired
    private OperateProblemServiceI operateProblemService;
    @Autowired
    private LogisticsProblemServiceI logisticsProblemService;

    @Autowired
    private FinaProblemServiceI finaProblemService;

    @Autowired
    private OrderSearchServiceI orderSearchService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private SurchargeServiceI surchargeService;

    @Autowired
    private OrderServiceI orderService;

    @Autowired
    private BoxServiceI boxService;

    @Autowired
    private FinanceOrderServiceI financeOrderService;

    @Autowired
    private FinanceOrderItemServiceI financeOrderItemService;

    @Autowired
    private DunningTaskServiceI dunningTaskService;

    @Autowired
    private OrgWarehouseServiceI orgWarehouseServiceI;

    @Autowired
    private CommonWeighingBusiness commonWeighingBusiness;

    @Autowired
    private RabbitMessageServiceI rabbitMessageService;

    /**
     * 获取国内问题件统计列表
     *
     * @param problemDto
     * @return
     */
    public DataGrid<ProblemCountListDto> findCountList(ProblemDto problemDto) {
        DataGrid<ProblemCountListDto> grid = new DataGrid<>();
        DataGrid<ProblemCountListDto> operateProblemGrid = operateProblemService.findCount(problemDto);
        DataGrid<ProblemCountListDto> finaProblemGrid = finaProblemService.findCount(problemDto);
        grid = mergeList(operateProblemGrid, finaProblemGrid);
        return grid;
    }


    /**
     * 合并DataGrid<T>
     *
     * @param problemCountListDtoDataGridA
     * @param problemCountListDtoDataGridB
     * @return
     */
    private DataGrid<ProblemCountListDto> mergeList(DataGrid<ProblemCountListDto> problemCountListDtoDataGridA,
                                                    DataGrid<ProblemCountListDto> problemCountListDtoDataGridB) {
        DataGrid<ProblemCountListDto> grid = new DataGrid<>();
        List<ProblemCountListDto> problemCountListDtolist = problemCountListDtoDataGridA.getRows();
        //1、把客户信息完全一样的问题件统计数据合并
        for (ProblemCountListDto bDto : problemCountListDtoDataGridB.getRows()) {
            boolean flag = false;
            for (ProblemCountListDto aDto : problemCountListDtolist) {
                // 判断用户代码、用户公司简称、销售名称和客服名称是否相同，相同这把标识为置为true，累加问题件数量
                if (aDto.getCustomerCode().compareTo(bDto.getCustomerCode()) == 0 && aDto.getSimpleCompanyName().compareTo(bDto
                        .getSimpleCompanyName()) == 0 && aDto.getCustomerSalesName().compareTo(bDto.getCustomerSalesName()) ==
                        0 && aDto
                        .getCustomerServiceName().compareTo(bDto.getCustomerServiceName()) == 0) {
                    flag = true;
                    aDto.setCountNum(aDto.getCountNum() + bDto.getCountNum());
                }
            }
            // 标记为False 表示没有相同的客户信息，把该记录添加到合并集合中
            if (!flag) {
                problemCountListDtolist.add(bDto);
            }
        }
        //2、再次合并问题件数据，只判断客户Code
        problemCountListDtolist = distinctByCustomerCode(problemCountListDtolist);
        grid.setRows(problemCountListDtolist);
        grid.setTotal(problemCountListDtolist.size());
        return grid;
    }

    /**
     * 获取操作和财务问题件，并进行假分页
     *
     * @param problemDto
     * @return
     */
    public DataGrid<ProblemDto> findAll(ProblemDto problemDto) {
        DataGrid<ProblemDto> grid = new DataGrid<>();
        //  插入标识，排除已完成问题件
        problemDto.getOtherInfo().put("flag", "FINISH");
        // 获取查询的分页信息，并把查询条件的分页制空，查询出全部结果，再做假分页
        PagingDto pagingDto = problemDto.getPagingDto();
        problemDto.setPagingDto(new PagingDto());
        DataGrid<ProblemDto> operateGrid = operateProblemService.findAll(problemDto);
        DataGrid<ProblemDto> finaGrid = finaProblemService.findAll(problemDto);
        List<ProblemDto> pList = operateGrid.getRows();
        pList.addAll(finaGrid.getRows());
        //  截取指点的页码数据
        if (pagingDto != null && pagingDto.getPageSize() != null) {
            logger.info("findAll: PageNo={},PageSize={},pListSize={}", pagingDto.getPageNo(), pagingDto.getPageSize(), pList.size());
            // list截取开始的位置，（页码-1）*每页数量
            int startIndex = (pagingDto.getPageNo() - 1) * pagingDto.getPageSize();
            // list总数-开始位置，剩余的记录大于每页数量，截取每页数量；小于则截取剩余数量
            int subSize = (pList.size() - startIndex) > pagingDto.getPageSize()
                    ? pagingDto.getPageSize() : (pList.size() - startIndex);
            grid.setRows(pList.subList(startIndex, startIndex + subSize));
        } else {
            grid.setRows(pList);
        }
        grid.setTotal(pList.size());
        //@Author:hyl @Date:2019/7/30 @Description: 新增需求:
        // 问题件查询，把订单的国家，重量，尺寸，派送商查出来，以当前数据库为准
        // #http://task.17feia.com/zentao/task-view-1581.html
        // 解决方案: 从订单中查询出以上数据添加到新字段返回
        setProblemDto(grid);
        return grid;
    }

    /**
     * 问题件新加字段
     * @param grid
     */
    private void setProblemDto(DataGrid<ProblemDto> grid) {
        List<ProblemDto> problemDtos = grid.getRows();
        List<String> orderIds = problemDtos.stream().map(ProblemDto::getOrdeOrderId).collect(Collectors.toList());
        List<Order> orders = orderService.findByIdIn(orderIds);
        List<Box> boxes = boxService.findByOrderIds(orderIds);

        problemDtos.forEach(p -> {

            Order order = orders.stream().filter(o -> o.getId().equals(p.getOrdeOrderId())).findFirst().get();
            p.setActualWeight(order.getActualWeight());
            p.setDeliveryCompany(order.getDeliveryCompany());
            p.setDestinationName(order.getDestinationName());

            List<String> sizes = new ArrayList<>();
            List<Box> boxList = boxes.stream().filter(b -> b.getoId().equals(p.getOrdeOrderId())).collect(Collectors.toList());
            boxList.forEach(box -> sizes.add(box.getOperateLength() + "x" + box.getOperateWidth() + "x" + box.getOperateHeight()));
            p.setBoxSizeList(sizes);

        });
    }

    /**
     * 获取问题件类型Map
     *
     * @return
     */
    public Map<String, String> getProblemTypeList() {
        Map<String, String> problemType = OperateProblemType.list();
        problemType.putAll(surchargeService.getSurchargeProblemType(SurchargeType.SIZELIMIT, SessionUtils.getOrganizationId()));
        problemType.putAll(surchargeService.getSurchargeProblemType(SurchargeType.SURCHARGE, SessionUtils.getOrganizationId()));
        problemType.putAll(finaProblemService.getFinaProblemTypeList());
        return problemType;
    }


    /**
     * 问题件更新
     *
     * @param problemDto
     * @return
     */
    public ResponseJsonData updateProblem(ProblemDto problemDto) {
        problemDto.setOperatorId(SessionUtils.getUserId());
        ResponseJsonData jsonResult = new ResponseJsonData();
        String problemSource = problemDto.getProblemSource();
        try {
            if (problemSource.equals(ProblemSource.OPERATE.name())) {
                operateProblemService.update(problemDto);

                //region 判断该订单的所有问题件是否都已经完结 start
                List<OperateProblem> operateProblems =
                        operateProblemService.findByInsideNumberAndProblemStatusNot(problemDto.getInsideNumber(), OperateProblemStatus.FINISH.name());

                //region 判断该订单的所有问题件是否都已经完结 start
                List<Problem> problems = finaProblemService.findByInsideNumberAndProblemStatusNot(problemDto
                        .getInsideNumber(), FinaProblemStatus.FINISH.name());

                if (!problems.isEmpty()) {
                    logger.info("## 完结问题件修改订单状态为物流商已收货，财务问题件个数："+ problems.size());
                    orderService.updateStatus(problemDto.getOrdeOrderId(), OrderStatus.SIGN);
                }

                if (operateProblems.isEmpty()) {
//                    订单状态改变 update by yuchao bagin
//                    orderService.updateStatus(problemDto.getOrdeOrderId(), OrderStatus.WAIT_DELIVE);
//                    订单状态改变 update by yuchao end

                    RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.OPERATOR_PROBLEM_FINISH, problemDto.getOrdeOrderId(), SessionUtils.getUserId());
                    rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.ORDER_SERVICE_QUEUEKEY,rabbitMessage);
                }
//
                //endregion 判断该订单的所有问题件是否都已经完结 end
            } else if (problemSource.equals(ProblemSource.LOGISTICS.name())) {
                logisticsProblemService.update(problemDto);
            } else if (problemSource.equals(ProblemSource.FINA.name())) {
                problemDto.setOperatorName(SessionUtils.getCustomer().getName());
                problemDto.setOperatorId(SessionUtils.getUserId());
                finaProblemService.update(problemDto);

                //region 判断该订单的所有问题件是否都已经完结 start
                List<Problem> problems = finaProblemService.findByInsideNumberAndProblemStatusNot(problemDto
                        .getInsideNumber(), FinaProblemStatus.FINISH.name());

                //region 判断该订单的所有问题件是否都已经完结 start
                List<OperateProblem> operateProblems =
                        operateProblemService.findByInsideNumberAndProblemStatusNot(problemDto.getInsideNumber(), OperateProblemStatus.FINISH.name());

//                if (!problems.isEmpty() || !operateProblems.isEmpty()) {
//                    logger.info("## 完结财务问题件，存在操作问题件，状态改为待发货，单号：" + problemDto.getInsideNumber());
//                    orderService.updateStatus(problemDto.getOrdeOrderId(), OrderStatus.SIGN);
//                    return jsonResult;
//                }
                if (!problems.isEmpty() || !operateProblems.isEmpty()) {
                    logger.info("## 当前订单存在其他问题件" + problemDto.getInsideNumber());
                    jsonResult.setMsg("完结当前财务问题件成功");
                    return jsonResult;
                }


                //更新订单财务状态
                //Kevintodo 基于状态更新, 加日志
                Order order = orderService.findById(problemDto.getOrdeOrderId());
                if (OrderStatus.CREATED.name().equals(order.getStatus().name()) || OrderStatus.SIGN.name().equals(order.getStatus().name()) || OrderStatus.WEIGH_PASS.name().equals(order.getStatus().name())) {
                    orderService.updateStatus(problemDto.getOrdeOrderId(), OrderStatus.WAIT_DELIVE);
                    OperateHistory operateHistory = new OperateHistory();
                    operateHistory.setOrderId(problemDto.getOrdeOrderId());
                    operateHistory.setOperateUserId(SessionUtils.getUserId());
                    operateHistory.setOperateActivity("问题件完结");
                    operateHistory.setOperateTime(new Date());
                    operateHistory.setOperateDesc("订单的状态已改为: " + OrderStatus.WAIT_DELIVE.getDesc());
                    RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.ADD_ORDER_LOG, problemDto.getOrdeOrderId(), operateHistory);
                    rabbitMessageService.sendAddOrderLogMessage(rabbitMessage);

                }
            }
//
            jsonResult.setFlag(true);

        } catch (Exception e) {
            logger.error("## 问题件更新报错", e);
            jsonResult.setFlag(false);
            jsonResult.setMsg("操作失败！");
        }

        return jsonResult;
    }

    /**
     * 国外问题件统计
     *
     * @param problemDto
     * @return
     */
    public DataGrid<ProblemCountListDto> findForeignCountList(ProblemDto problemDto) {

        DataGrid<ProblemCountListDto> grid = logisticsProblemService.findCount(problemDto);
        grid.setRows(distinctByCustomerCode(grid.getRows()));
        grid.setTotal(grid.getRows().size());
        return grid;
    }

    /**
     * 国外问题详细列表
     *
     * @param problemDto
     * @return
     */
    public DataGrid<ProblemDto> findForeignAll(ProblemDto problemDto) {
        /**  插入标识，排除已完成问题件 **/
        problemDto.getOtherInfo().put("flag", "FINISH");
        return logisticsProblemService.findAll(problemDto);
    }

    /**
     * 获取国外问题件类型Map
     *
     * @return
     */
    public Map getForeignProblemTypeList() {
        Map problemType = LogisticsProblemType.list();
        return problemType;
    }

    /**
     * @author LinYun
     * @date 9:56 2018/4/11
     * @description 检查订单状态
     */
    public DataGrid checkOrderStatus(ProblemDto problemDto) {
        DataGrid dataGrid = new DataGrid();
        if (!problemDto.getInsideNumber().toUpperCase().startsWith("FEI")) {
            dataGrid.setFlag(false);
            dataGrid.setMsg(String.format("%s:填写了错误的订单号", problemDto.getInsideNumber()));
            return dataGrid;
        }
        Order order = orderService.findByInsideNumber(problemDto.getInsideNumber());
        if (order == null) {
            dataGrid.setFlag(false);
            dataGrid.setMsg(String.format("%s:订单不存在", problemDto.getInsideNumber()));
            return dataGrid;
        }
        if (!(order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.COMPLETED)) {
            dataGrid.setFlag(false);
            dataGrid.setMsg(String.format("%s:订单不是物流商已发货及之后状态，不能生成国外问题件", problemDto.getInsideNumber()));
            return dataGrid;
        }
        //客服只能添加同一仓库下的订单问题件
        if (!order.getWarehouseId().equals(SessionUtils.getWarehouseId())) {
            dataGrid.setFlag(false);
            dataGrid.setMsg(String.format("%s:不是该仓库的订单", problemDto.getInsideNumber()));
            return dataGrid;
        }
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * excel文件转换为问题件list
     *
     * @param fileInputStream
     * @return
     * @throws Exception
     */
    public List<ProblemDto> importExcel(InputStream fileInputStream) throws Exception {

        Workbook wb = WorkbookFactory.create(fileInputStream);
        Sheet sheet = wb.getSheet("问题件");
        if (sheet == null) {

            sheet = wb.getSheetAt(0);
        }
        // 标题，也是要导入的字段
        Row rowHead = sheet.getRow(0);
        // 行数
        int rowCount = sheet.getPhysicalNumberOfRows();
        // 列数
        int columnCount = rowHead.getLastCellNum();
        // 头部
        List<String> headNames = new ArrayList<>();

        for (int i = 0; i < columnCount; i++) {
            String headName = rowHead.getCell(i).getStringCellValue();
            headNames.add(headName);
        }
        //将excel行数据转为对象
        List<ProblemDto> problemDtos = new ArrayList<>();
        Class problemDtoClass = ProblemDto.class;
        for (int j = 1; j < rowCount; j++) {
            Row row = sheet.getRow(j);
            if (isBlankRow(row)) {
                continue;
            }

            ProblemDto problemDto = (ProblemDto) problemDtoClass.newInstance();

            for (int k = 0; k < columnCount; k++) {

                String columeValue = transferCellType(row.getCell(k));
                switch (headNames.get(k)) {
                    case "订单号":
                        problemDto.setInsideNumber(columeValue);
                        break;
                    case "问题名称":
                        String problemType = "";
                        switch (columeValue) {
                            case "地址不符":
                                problemType = LogisticsProblemType.ADDRESS_IS_NOT.name();
                                break;
                            case "收件人不在":
                                problemType = LogisticsProblemType.ABSENCE_OF_ADDRESSEE.name();
                                break;
                            case "自提":
                                problemType = LogisticsProblemType.PICK_UP_BY_CUSTOMER.name();
                                break;
                            case "客户拒签":
                                problemType = LogisticsProblemType.CUSTOMER_REFUSED.name();
                                break;
                        }
                        problemDto.setProblemName(columeValue);
                        problemDto.setProblemType(problemType);
                        break;
                    case "备注":
                        problemDto.setContent(columeValue);
                        break;
                }
            }
            problemDto.setProblemSource(ProblemSource.LOGISTICS.name());
            problemDtos.add(problemDto);
        }
        return problemDtos;
    }

    /**
     * @Description: 判断行是否为空
     * @param:
     * @Date: 14:13 2017/9/6
     */
    private static boolean isBlankRow(Row row) {
        boolean blank = true;

        Iterator<Cell> cellItr = row.iterator();
        while (cellItr.hasNext()) {
            Cell c = cellItr.next();
            if (c.getCellType() != HSSFCell.CELL_TYPE_BLANK) {
                blank = false;
                break;
            }
        }
        return blank;
    }


    private static String transferCellType(Cell cell) {
        if (cell == null) {
            return null;
        }
        cell.setCellType(Cell.CELL_TYPE_STRING);
        return cell.getStringCellValue();
    }

    /**
     * 获取重派订单信息
     *
     * @param problemId
     * @param ordeId
     * @return
     */
    public OrderResendDto getOrderResend(String problemId, String ordeId) {
        OrderResendDto orderResendDto = new OrderResendDto();
        DataGrid<OrderDetailDto> orderDetailDtoDG = orderSearchService.getById(ordeId);
        if (orderDetailDtoDG == null || orderDetailDtoDG.getObj() == null) {
            throw new EshipException(ErrorCodeConstant.ORDER_NOT_EXIST);
        }
        OrderDetailDto orderDetailDto = orderDetailDtoDG.getObj();
        orderResendDto.setProblemId(problemId);
        orderResendDto.setParentId(orderDetailDto.getId());
        orderResendDto.setInsideNumber(orderDetailDto.getInsideNumber() + "-CP");
        orderResendDto.setDeliveryNumber(orderDetailDto.getDeliveryNumber());
        orderResendDto.setConsignee(orderDetailDto.getConsignee());
        orderResendDto.setCompanyName(orderDetailDto.getCompanyName());
        orderResendDto.setCountry(orderDetailDto.getCountry());
        orderResendDto.setProvince(orderDetailDto.getProvince());
        orderResendDto.setCity(orderDetailDto.getCity());
        orderResendDto.setAddress(orderDetailDto.getAddress());
        orderResendDto.setPostcode(orderDetailDto.getPostcode());
        orderResendDto.setPhoneNo(orderDetailDto.getPhoneNo());
        orderResendDto.setEmail(orderDetailDto.getEmail());

        return orderResendDto;
    }

    /**
     * 获取重派国外订单信息
     *
     * @param ordeId
     * @return
     */
    public OrderResendDto getForeignOrderResend(String ordeId) {
        OrderResendDto orderResendDto = new OrderResendDto();
        DataGrid<OrderDetailDto> orderDetailDtoDG = orderSearchService.getById(ordeId);
        if (orderDetailDtoDG == null || orderDetailDtoDG.getObj() == null) {
            throw new EshipException(ErrorCodeConstant.ORDER_NOT_EXIST);
        }
        OrderDetailDto orderDetailDto = orderDetailDtoDG.getObj();
        //orderResendDto.setProblemId(problemId);
        orderResendDto.setParentId(orderDetailDto.getId());
        String insideNumber = orderDetailDto.getInsideNumber();

        if (StringUtils.countMatches(insideNumber, "CP-") > 1) {
            throw new EshipException(ErrorCodeConstant.OPERATE_ERROR);
        }
        Integer index = insideNumber.indexOf("-");
        StringBuilder newInsideNumber = new StringBuilder(insideNumber.substring(index + 1));
        Integer num = 1;
        if (insideNumber.contains("-")) {
            if (insideNumber.startsWith("CP")) {
                num = Integer.parseInt(insideNumber.substring(2, index)) + 1;
            } else {
                newInsideNumber = new StringBuilder(insideNumber.substring(0, index));
                num = Integer.parseInt(insideNumber.substring(insideNumber.length() - 1)) + 1;
            }
        }
        newInsideNumber.append("-").append("CP").append(num);
        orderResendDto.setInsideNumber(newInsideNumber.toString());

        orderResendDto.setDeliveryNumber(orderDetailDto.getDeliveryNumber());
        orderResendDto.setConsignee(orderDetailDto.getConsignee());
        orderResendDto.setCompanyName(orderDetailDto.getCompanyName());
        orderResendDto.setCountry(orderDetailDto.getCountry());
        orderResendDto.setProvince(orderDetailDto.getProvince());
        orderResendDto.setCity(orderDetailDto.getCity());
        orderResendDto.setAddress(orderDetailDto.getAddress());
        orderResendDto.setPostcode(orderDetailDto.getPostcode());
        orderResendDto.setPhoneNo(orderDetailDto.getPhoneNo());
        orderResendDto.setEmail(orderDetailDto.getEmail());

        return orderResendDto;
    }

    /**
     * 保存重派订单
     *
     * @param orderResendDto
     * @return
     */
    public DataGrid saveOrderResend(OrderResendDto orderResendDto) {
        DataGrid dataGrid = new DataGrid();
        User user = new User();
        user.setId(SessionUtils.getUserId());
        user.setName(SessionUtils.getCustomer().getName());

        logisticsProblemService.deleteRepeat(orderResendDto.getProblemId());

        DataGrid<Order> orderDataGrid = orderService.orderResend(orderResendDto);
        if (!orderDataGrid.isFlag() || orderDataGrid.getObj() == null) {
            dataGrid.setFlag(false);
            dataGrid.setMsg("重派订单生成失败！");
            return dataGrid;
        }
        // 生成财务账单
        List<String> orderIdsList = Lists.newArrayList();
        orderIdsList.add(orderDataGrid.getObj().getId());
        FinaReturnGoodsDto finaReturnGoodsDto = new FinaReturnGoodsDto();
        finaReturnGoodsDto.setFinaReturnGoodsType(FinaReturnGoodsType.REDELIVERY);
        finaReturnGoodsDto.setTotalAmount(orderResendDto.getResendFee());
        finaReturnGoodsDto.setSaleCostPrice(orderResendDto.getSaleCostPrice());
        finaReturnGoodsDto.setBranchCostPrice(orderResendDto.getBranchCostPrice());
        finaReturnGoodsDto.setOperationDirectorPrice(orderResendDto.getOperationDirectorPrice());
        List<String> financeOrderIds = commonWeighingBusiness.createFinanceOrder(orderIdsList, finaReturnGoodsDto, user);
        ProblemDto problemDto = new ProblemDto();
        problemDto.setId(orderResendDto.getProblemId());
        logisticsProblemService.update(problemDto);

        if (financeOrderIds != null && !financeOrderIds.isEmpty()) {
            financeOrderIds.forEach(financeOrderId -> {
                FinanceOrder financeOrder = financeOrderService.findOne(financeOrderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(financeOrder.getCustomerId());
                //写财务消息记录表
                StringBuilder mess = new StringBuilder("国外问题件重派,重派费用 " + orderResendDto.getResendFee());

                writeFinanceMessage(customerAssign.getSimpleCompanyName(), financeOrder.getFinaceOrderNo(), orderResendDto.getInsideNumber(), mess.toString(), financeOrder.getCustomerId());
            });
        } else {
            for (String orderId : orderIdsList) {
                OrderInfoDto orderInfoDto = orderService.getOrderInfo(orderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(orderInfoDto.getUserId());
                //写财务消息记录表
                StringBuilder mess = new StringBuilder("国外问题件重派,重派费用 " + orderResendDto.getResendFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), "", orderInfoDto.getInsideNumber(), mess.toString(), orderInfoDto.getUserId());
            }
        }
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * 获取操作问题件
     *
     * @param problemDto
     * @return
     */
    public DataGrid<ProblemDto> findOperateAll(ProblemDto problemDto) {
        /**  插入标识，排除已完成问题件 **/
        problemDto.getOtherInfo().put("flag", "FINISH");
        return operateProblemService.findAll(problemDto);
    }

    /**
     * 获取财务问题件
     *
     * @param problemDto
     * @return
     */
    public DataGrid<ProblemDto> findFinanceAll(ProblemDto problemDto) {
        /**  插入标识，排除已完成问题件 **/
        problemDto.getOtherInfo().put("flag", "FINISH");
        return finaProblemService.findAll(problemDto);
    }

    /**
     * @author LinYun
     * @date 16:35 2018/4/8
     * @description 国内问题件批量退回
     */
    public DataGrid batchReturnDomestic(BatchReturnDto batchReturnDto) {
        DataGrid dataGrid = new DataGrid();
        List<String> problemIdList = new ArrayList<>();
        User user = new User();
        user.setId(SessionUtils.getUserId());
        user.setName(SessionUtils.getCustomer().getName());
        List<String> insideNumberList = ArrayUtil.conver2array(batchReturnDto.getInsideNumbers(), ",");
        List<String> orderIds = ArrayUtil.conver2array(batchReturnDto.getOrderIds(), ",");
        //1.对每一票订单判断是否有financeOrder，有则退款并取消或者直接取消
        financeOrderService.cancelOrder(ArrayUtil.conver2array(batchReturnDto.getOrderIds(), ","));
        //2.生成financeOrder 并 3. 扣款，若失败则生成催款任务
        FinaReturnGoodsDto finaReturnGoodsDto = new FinaReturnGoodsDto();
        finaReturnGoodsDto.setFinaReturnGoodsType(FinaReturnGoodsType.DOMESTIC);
        finaReturnGoodsDto.setTotalAmount(batchReturnDto.getReturnFee());
        //生成财务账单 如果金额为零不生成
        List<String> financeOrderIds = commonWeighingBusiness.createFinanceOrder(orderIds, finaReturnGoodsDto, user);
        //4.更改订单状态
        orderService.updateReturnExpressNumber(insideNumberList, OrderStatus.RETURN, batchReturnDto.getExpressNumber(), batchReturnDto.getDescription());

        for (String orderId : orderIds) {
            logger.info("================================> 开始发送ERP产品下的订单 {} 修改订单状态的消息...", orderId);
            rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.ESHIP_API_QUEUEKEY,new RabbitMessage(RabbitMessageActionsConstant.ORDER_UPDATE_RETURN_DOMESTIC_STATUS, orderId));
        }

        //5.更新问题件状态,包含操作和财务
        if (StringUtils.isBlank(batchReturnDto.getProblemIds())) {
            List<OperateProblem> operateProblemList = operateProblemService.getOperateProblem(orderIds.get(0));
            List<Problem> problemList = finaProblemService.getOperateProblem(orderIds.get(0));

            List<String> operateProblemIds = new ArrayList<>();
            List<String> problemIds = new ArrayList<>();
            if (operateProblemList != null && !operateProblemList.isEmpty()) {
                operateProblemList.forEach(operateProblem -> {
                    if (!operateProblem.getProblemStatus().equals("FINISH")) {
                        operateProblemIds.add(operateProblem.getId());
                    }
                });
                problemIdList.addAll(operateProblemIds);
            }
            if (problemList != null && !problemList.isEmpty()) {
                problemList.forEach(problem -> {
                    if (!problem.getProblemStatus().equals("FINISH")) {
                        problemIds.add(problem.getId());
                    }
                });
                problemIdList.addAll(problemIds);
            }
        } else {
            problemIdList = ArrayUtil.conver2array(batchReturnDto.getProblemIds(), ",");
        }

        operateProblemService.batchFinishProblem(problemIdList);
        finaProblemService.batchFinishProblem(problemIdList);

        if (financeOrderIds != null && !financeOrderIds.isEmpty()) {
            financeOrderIds.forEach(financeOrderId -> {
                FinanceOrder financeOrder = financeOrderService.findOne(financeOrderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(financeOrder.getCustomerId());
                StringBuffer insideNumbers = new StringBuffer();
                for (String str : insideNumberList) {
                    insideNumbers.append(str + ",");
                }
                //写财务消息记录表
                StringBuilder mess = new StringBuilder("国内问题件退回,退回费用 " + batchReturnDto.getReturnFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), financeOrder.getFinaceOrderNo(), insideNumbers.toString(), mess.toString(), financeOrder.getCustomerId());
            });
        } else {
            for (String orderId : orderIds) {
                OrderInfoDto orderInfoDto = orderService.getOrderInfo(orderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(orderInfoDto.getUserId());
                //写财务消息记录表
                StringBuilder mess = new StringBuilder("国内问题件退回,退回费用 " + batchReturnDto.getReturnFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), "", batchReturnDto.getInsideNumbers(), mess.toString(), orderInfoDto.getUserId());

                //如果是代理产品下的订单需要同步状态
                if (StringUtils.isNotBlank(orderInfoDto.getCallbackUrl())) {
                    logger.info("================================> 开始发送通过代理产品下的订单 {} 修改订单状态的消息...", orderInfoDto.getInsideNumber());

                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("insideNumber", orderInfoDto.getReferenceNo());
                    messageMap.put("orderStatus", OrderStatus.RETURN);
                    messageMap.put("lastUpdatedDate", new Date());
                    messageMap.put("proxyToken", orderInfoDto.getProxyToken());

                    RabbitMessage msg = new RabbitMessage(RabbitMessageActionsConstant.UPDATE_PROXY_ORDER_STATUS, orderInfoDto.getId(), messageMap);
                    rabbitMessageService.sendUpdateProxyOrderStatusMessage(msg);
                }
            }
        }

//        FinanceOrderMessage operateHistory = new FinanceOrderMessage();
//        operateHistory.setOperateActivity("");
//        operateHistory.setOrderId(orderId);
//        operateHistory.setOperateUserId(SessionUtils.getUserId());
//        operateHistory.setOperateTime(new Date());
//        logger.debug("#订单重派成功 发送消息  #原订单号: {} #新订单号:{}", orderId, insideNumber);
//        RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.ADD_ORDER_LOG, orderId, operateHistory);
//        rabbitMessageService.sendMessage(rabbitMessage);


        //3.扣款，若失败则生成催款任务
//        try {
//            financeOrderService.feeDeduction(financeOrder.getId(), batchReturnDto.getDescription());
//        } catch (EshipException ee) {
//            DunningTask dunningTask = new DunningTask();
//            dunningTask.setCustomerId(financeOrder.getCustomerId());
//            dunningTask.setDunningAmount(financeOrder.getTotalPrice());
//            dunningTask.setFinanceOrderNo(financeOrder.getFinaceOrderNo());
//            dunningTaskService.addDunningTask(dunningTask);
//        }
        dataGrid.setFlag(true);
        dataGrid.setMsg("操作成功");
        return dataGrid;
    }

    /**
     * TuShiDing 写财务消息记录表
     *
     * @param simpleCompanyName  公司简称
     * @param financeOrderNoDesc 涉及财务流水号
     * @param insideNumber       订单号
     * @param operateDesc        涉及的财务操作描述
     */
    public void writeFinanceMessage(String simpleCompanyName, String financeOrderNoDesc, String insideNumber, String operateDesc, String customerId) {
        User user = userService.findOne(customerId);
        Customer opertor = SessionUtils.getCustomer();
        RabbitMessage rabbitMessage = new RabbitMessage(RabbitMessageActionsConstant.ADD_FINANCE_MESSAGE, SessionUtils.getUserId(),
                new FinanceOrderMessage(user.getCustomerCode(), simpleCompanyName, opertor.getName(), opertor.getPhone(),
                        financeOrderNoDesc, insideNumber, operateDesc, false,SessionUtils.getOrganizationId()));
        rabbitMessageService.sendMessage(RabbitMessageQueueKeyConstant.FINANCE_SERVICE_QUEUEKEY,rabbitMessage);
    }


    /**
     * @author LinYun
     * @date 13:59 2018/4/10
     * @description 国外弃件
     */
    public DataGrid returnForeign(ProblemDto problemDto) {
        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        User user = new User();
        user.setId(SessionUtils.getUserId());
        user.setName(SessionUtils.getCustomer().getName());

        logisticsProblemService.deleteRepeat(problemDto.getId());

        if (problemDto.getReturnFee() > 0) {
            // 费用大于0表示需要收费，则生成账单
            List<String> orderIds = Lists.newArrayList();
            orderIds.add(problemDto.getOrdeOrderId());
            FinaReturnGoodsDto finaReturnGoodsDto = new FinaReturnGoodsDto();
            finaReturnGoodsDto.setFinaReturnGoodsType(FinaReturnGoodsType.ABROAD);
            finaReturnGoodsDto.setOperationDirectorPrice(problemDto.getReturnFeeCost());
            finaReturnGoodsDto.setBranchCostPrice(problemDto.getReturnFeeCost());
            finaReturnGoodsDto.setSaleCostPrice(problemDto.getReturnFeeCost());
            finaReturnGoodsDto.setTotalAmount(problemDto.getReturnFee());
            List<String> financeOrderIds = commonWeighingBusiness.createFinanceOrder(orderIds, finaReturnGoodsDto, user);
            if (financeOrderIds.isEmpty()) {
                dataGrid.setFlag(false);
                dataGrid.setMsg("财务账单生成失败！");
            }
            //写财务消息记录表
            if (!financeOrderIds.isEmpty()) {

                financeOrderIds.forEach(financeOrderId -> {
                    FinanceOrder financeOrder = financeOrderService.findOne(financeOrderId);
                    CustomerAssign customerAssign = customerAssignService.findByCustomerId(financeOrder.getCustomerId());

                    //写财务消息记录表
                    StringBuilder mess = new StringBuilder("国内问题件弃件 ,弃件费用 " + problemDto.getReturnFee());
                    writeFinanceMessage(customerAssign.getSimpleCompanyName(), financeOrder.getFinaceOrderNo(), problemDto.getInsideNumber(), mess.toString(), financeOrder.getCustomerId());
                });
            }
        }
        if (dataGrid.isFlag()) {
            //国外弃件
            orderService.updateStatus(problemDto.getOrdeOrderId(), OrderStatus.GIVE_UP);

            // 2.完结问题件
            updateProblem(problemDto);
        }
        return dataGrid;
    }

    /**
     * @author LinYun
     * @date 17:31 2018/5/14
     * @description 自有VAT问题件完结
     */
    public DataGrid finishFBAProblem(ProblemDto problemDto) {
        //HYLTODO VAT问题件需要检查
        DataGrid dataGrid = new DataGrid();

        // 2.完结问题件
        ResponseJsonData jsonResult = updateProblem(problemDto);
        dataGrid.setFlag(jsonResult.isFlag());
        dataGrid.setMsg(jsonResult.getMsg());

        return dataGrid;
    }

    /*public static void main(String[] args) {

        List<ProblemCountListDto> problemCountListDtolist = Lists.newArrayList();
        //region 模拟数据
        ProblemCountListDto problemCountListDtoa = new ProblemCountListDto();
        problemCountListDtoa.setCountNum(6 + 1);
        problemCountListDtoa.setCustomerId("idF04523");
        problemCountListDtoa.setCustomerCode("F045233");
        problemCountListDtoa.setCustomerSalesName("张尧");
        problemCountListDtoa.setCustomerServiceName("朱丽");
        problemCountListDtoa.setSimpleCompanyName("公司ss");
        problemCountListDtolist.add(problemCountListDtoa);
        for (int i = 0; i < 5; i++) {
            ProblemCountListDto problemCountListDto = new ProblemCountListDto();
            problemCountListDto.setCountNum(i + 1);
            problemCountListDto.setCustomerCode("F04523" + i);
            problemCountListDto.setCustomerSalesName("张尧");
            problemCountListDto.setCustomerServiceName("朱丽");
            problemCountListDto.setSimpleCompanyName("公司" + i);
            problemCountListDtolist.add(problemCountListDto);
        }
        ProblemCountListDto problemCountListDto = new ProblemCountListDto();
        problemCountListDto.setCountNum(6 + 1);
        problemCountListDto.setCustomerId("idF04523");
        problemCountListDto.setCustomerCode("F045230");
        problemCountListDto.setCustomerSalesName("张尧");
        problemCountListDto.setCustomerServiceName("朱丽");
        problemCountListDto.setSimpleCompanyName("公司s0");
        problemCountListDtolist.add(problemCountListDto);
//        for (ProblemCountListDto dto : problemCountListDtolist) {
//            System.out.println(JsonUtils.beanToJson(dto));
//        }
        //endregion
        Long startTime = System.currentTimeMillis();
        //region Description
        List<ProblemCountListDto> countList = problemCountListDtolist;
        Map<String, ProblemCountListDto> restulMap = Maps.newHashMap();
        for (ProblemCountListDto countListDto : countList) {
            ProblemCountListDto aDto = restulMap.get(countListDto.getCustomerCode());
            if (aDto != null) {
                // 存在map中则更新统计结果
                if (StringUtils.isEmpty(aDto.getSimpleCompanyName())) {
                    aDto.setSimpleCompanyName(countListDto.getSimpleCompanyName());
                }
                aDto.setCountNum(aDto.getCountNum() + countListDto.getCountNum());
                restulMap.put(aDto.getCustomerCode(), aDto);
            } else {
                // 不存在则直接加入到map中
                restulMap.put(countListDto.getCustomerCode(), countListDto);
            }
        }

        List<ProblemCountListDto> rtnList = Lists.newArrayList();
        Set<String> keySet = restulMap.keySet();
        //循环map把ProblemCountListDto添加到list中
        for (String s : keySet) {
            rtnList.add(restulMap.get(s));
        }
        //endregion
        System.out.println("合并所用时间：" + String.valueOf(System.currentTimeMillis() - startTime));
        System.out.println("根据用户code合并后");
        for (ProblemCountListDto dto : rtnList) {
            System.out.println(JsonUtils.beanToJson(dto));
        }

    }*/

    /**
     * @author LinYun
     * @date 13:53 2018/5/26
     * @description 根据用户code合并统计结果
     */
    public List<ProblemCountListDto> distinctByCustomerCode(List<ProblemCountListDto> countList) {
        //region Description
        Map<String, ProblemCountListDto> restulMap = Maps.newHashMap();
        for (ProblemCountListDto countListDto : countList) {
            ProblemCountListDto aDto = restulMap.get(countListDto.getCustomerCode());
            if (aDto != null) {
                // 存在map中则更新统计结果
                if (StringUtils.isEmpty(aDto.getSimpleCompanyName())) {
                    aDto.setSimpleCompanyName(countListDto.getSimpleCompanyName());
                }
                aDto.setCountNum(aDto.getCountNum() + countListDto.getCountNum());
                restulMap.put(aDto.getCustomerCode(), aDto);
            } else {
                // 不存在则直接加入到map中
                restulMap.put(countListDto.getCustomerCode(), countListDto);
            }
        }

        List<ProblemCountListDto> rtnList = Lists.newArrayList();
        Set<String> keySet = restulMap.keySet();
        //循环map把ProblemCountListDto添加到list中
        for (String s : keySet) {
            rtnList.add(restulMap.get(s));
        }
        //endregion

        return rtnList;
    }

    public DataGrid finaProblemInfo(String warehouseId, String organizationId) {
        FinaProblemInfoVO finaProblemInfoVO = new FinaProblemInfoVO();
        List<FinaProblemInfoVO.Customer> customers = new ArrayList<>();
        List<FinaProblemInfoVO.ProblemStatus> problemStatusList = new ArrayList<>();

        //查询所有财务问题件
        List<Problem> problems =
                finaProblemService.findAllByProblemStatus(FinaProblemStatus.CUSTOMER_SERVICE.name(), warehouseId, organizationId);

        List<Problem> finaProblems = new ArrayList<>();
        for (Problem problem : problems) {
            if (FinaProblemType.CURRENT_AMOUNT_IS_OVERRUNS.name().equals(problem.getProblemType())
                    || FinaProblemType.PAYMENT_DAYS_IS_INVALID.name().equals(problem.getProblemType())
                    || FinaProblemType.TEMP_LINE_OF_CREDIT_BALANCE_IS_OVERRUNS.name().equals(problem.getProblemType())
                    || FinaProblemType.TAX_LINE_OF_CREDIT_BALANCE_IS_OVERRUNS.name().equals(problem.getProblemType())
                    || FinaProblemType.TAX_CURRENT_AMOUNT_IS_OVERRUNS.name().equals(problem.getProblemType())) {
                finaProblems.add(problem);
            }
        }

        List<String> problemUserIds = finaProblems.stream()
                .filter(p -> p.getProblemStatus().equals(FinaProblemStatus.CUSTOMER_SERVICE.name()))
                .map(problem -> problem.getUserId())
                .collect(Collectors.toList());

        //查找问题件用户列表
        getCustomer(warehouseId, customers, problemUserIds);

        //获取财务问题件问题类型
        List<FinaProblemInfoVO.ProblemType> problemTypes = getFinaProblemTypes();
        //设置问题件状态
        getFinaProblemStatus(problemStatusList);
        finaProblemInfoVO.setCustomers(customers);
        finaProblemInfoVO.setProblemTypes(problemTypes);
        finaProblemInfoVO.setProblemStatusList(problemStatusList);

        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        dataGrid.setObj(finaProblemInfoVO);
        return dataGrid;
    }

    public List<FinaProblemInfoVO.ProblemType> getFinaProblemTypes() {
        List<FinaProblemInfoVO.ProblemType> problemTypes = new ArrayList<>();
        //查找问题件财务问题件枚举类型
        Map<String, String> finaProblemTypeMap = FinaProblemType.list();
        finaProblemTypeMap.forEach((k, v) -> {
            FinaProblemInfoVO.ProblemType problemType = new FinaProblemInfoVO.ProblemType();
            problemType.setLabel(v);
            problemType.setValue(k);
            if (FinaProblemType.CURRENT_AMOUNT_IS_OVERRUNS.name().equals(k)
                    || FinaProblemType.PAYMENT_DAYS_IS_INVALID.name().equals(k)
                    || FinaProblemType.TEMP_LINE_OF_CREDIT_BALANCE_IS_OVERRUNS.name().equals(k)
                    || FinaProblemType.TAX_LINE_OF_CREDIT_BALANCE_IS_OVERRUNS.name().equals(k)
                    || FinaProblemType.TAX_CURRENT_AMOUNT_IS_OVERRUNS.name().equals(k)) {
                problemTypes.add(problemType);
            }
        });
        return problemTypes;
    }

    public DataGrid findFinaProblem(ProblemDto problemDto) {
//        yuchao  添加 分公司名称

        String warehouseId = problemDto.getWarehouseId();

        OrgWarehouse orgWarehouse = orgWarehouseServiceI.findOne(warehouseId);

        problemDto.setWarehouseName(orgWarehouse.getName());

//        yuchao
        DataGrid<ProblemDto> finaGrid = finaProblemService.findAll(problemDto);

        return finaGrid;
    }

    public DataGrid exportAllProblem(ProblemDto problemDto) {
        DataGrid<ProblemDto> grid = new DataGrid<>();
        problemDto.setPagingDto(new PagingDto());

        DataGrid<ProblemDto> operateGrid = operateProblemService.findAll(problemDto);
        DataGrid<ProblemDto> finaGrid = finaProblemService.findAll(problemDto);
        List<ProblemDto> pList = operateGrid.getRows();
        pList.addAll(finaGrid.getRows());
        grid.setRows(pList);
        grid.setTotal(pList.size());
        return grid;
    }


    public DataGrid getAllProblem(ProblemDto problemDto) {
        DataGrid<ProblemDto> grid = new DataGrid<>();
        // 获取查询的分页信息，并把查询条件的分页制空，查询出全部结果，再做假分页
        PagingDto pagingDto = problemDto.getPagingDto();
        problemDto.setPagingDto(new PagingDto());
        DataGrid<ProblemDto> operateGrid = operateProblemService.findAll(problemDto);
        DataGrid<ProblemDto> finaGrid = finaProblemService.findAll(problemDto);
        List<ProblemDto> pList = operateGrid.getRows();
        pList.addAll(finaGrid.getRows());
        //  截取指点的页码数据
        if (pagingDto != null && pagingDto.getPageSize() != null) {
            logger.info("getAllProblem: PageNo={},PageSize={},pListSize={}", pagingDto.getPageNo(), pagingDto.getPageSize(), pList.size());
            // list截取开始的位置，（页码-1）*每页数量
            int startIndex = (pagingDto.getPageNo() - 1) * pagingDto.getPageSize();
            // list总数-开始位置，剩余的记录大于每页数量，截取每页数量；小于则截取剩余数量
            int subSize = (pList.size() - startIndex) > pagingDto.getPageSize()
                    ? pagingDto.getPageSize() : (pList.size() - startIndex);
            grid.setRows(pList.subList(startIndex, startIndex + subSize));
        } else {
            grid.setRows(pList);
        }
        grid.setTotal(pList.size());

        if(!pList.isEmpty()){
            setProblemDto(grid);
        }
        return grid;
    }


    /**
     * 国外订单退回
     *
     * @param batchReturnDto
     * @return
     */
    public DataGrid foreignOrderReturn(BatchReturnDto batchReturnDto) {
        logger.debug("国外订单退回，参数batchReturnDto={}", JSONObject.toJSONString(batchReturnDto));
        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        List<String> insideNumberList = ArrayUtil.conver2array(batchReturnDto.getInsideNumbers(), ",");
        List<String> orderIds = ArrayUtil.conver2array(batchReturnDto.getOrderIds(), ",");
        User user = new User();
        user.setId(SessionUtils.getUserId());
        user.setName(SessionUtils.getCustomer().getName());

        //修改订单状态
        orderService.updateOrderStatus(insideNumberList, OrderStatus.FOREIGN_RETURN, batchReturnDto.getDescription());
        //修改是否是国外退回件  将国外操作备注加到客服备注中
        orderService.updateForeignReturnOrderFlag(batchReturnDto.getOrderIds(), true, batchReturnDto.getDescription());

        //创建财务明细
        FinaReturnGoodsDto finaReturnGoodsDto = new FinaReturnGoodsDto();
        finaReturnGoodsDto.setFinaReturnGoodsType(FinaReturnGoodsType.ABROAD);
        finaReturnGoodsDto.setTotalAmount(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setOperationDirectorPrice(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setBranchCostPrice(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setSaleCostPrice(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setDescription(batchReturnDto.getDescription()); //备注
        List<String> financeOrderIds = commonWeighingBusiness.createFinanceOrder(orderIds, finaReturnGoodsDto, user);

        //通知财务
        if (financeOrderIds != null && !financeOrderIds.isEmpty()) {
            financeOrderIds.forEach(financeOrderId -> {
                FinanceOrder financeOrder = financeOrderService.findOne(financeOrderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(financeOrder.getCustomerId());
                StringBuffer insideNumbers = new StringBuffer();
                for (String str : insideNumberList) {
                    insideNumbers.append(str + ",");
                }
                StringBuilder mess = new StringBuilder("国外件退回,退回费用 " + batchReturnDto.getReturnFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), financeOrder.getFinaceOrderNo(), insideNumbers.toString(), mess.toString(), financeOrder.getCustomerId());
            });
        } else {
            for (String orderId : orderIds) {
                OrderInfoDto orderInfoDto = orderService.getOrderInfo(orderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(orderInfoDto.getUserId());
                StringBuilder mess = new StringBuilder("国外件退回,退回费用 " + batchReturnDto.getReturnFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), "", batchReturnDto.getInsideNumbers(), mess.toString(), orderInfoDto.getUserId());

                if (StringUtils.isNotBlank(orderInfoDto.getCallbackUrl())) {
                    logger.info("================================> 开始发送通过代理产品下的订单 {} 修改订单状态的消息...", orderInfoDto.getInsideNumber());

                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("insideNumber", orderInfoDto.getReferenceNo());
                    messageMap.put("orderStatus", OrderStatus.FOREIGN_RETURN);
                    messageMap.put("lastUpdatedDate", new Date());
                    messageMap.put("proxyToken", orderInfoDto.getProxyToken());

                    RabbitMessage msg = new RabbitMessage(RabbitMessageActionsConstant.UPDATE_PROXY_ORDER_STATUS, orderInfoDto.getId(), messageMap);
                    rabbitMessageService.sendUpdateProxyOrderStatusMessage(msg);
                }
            }
        }
        //记录操作日志
        rabbitMessageService.sendAddOrderLogMessage(new RabbitMessage(RabbitMessageActionsConstant.ADD_ORDER_LOG, batchReturnDto.getOrderIds(),
                new OperateHistory(batchReturnDto.getOrderIds(), user.getId(), "国外订单退回", "订单被国外退回", new Date())));

        dataGrid.setFlag(true);
        dataGrid.setMsg("操作成功");
        return dataGrid;
    }

    /**
     * 国外订单重派
     *
     * @param orderResendDto
     * @return
     */
    public DataGrid foreignOrderResend(OrderResendDto orderResendDto) {
        logger.debug("国外订单重派，参数orderResendDto={}", JSONObject.toJSONString(orderResendDto));
        DataGrid dataGrid = new DataGrid();
        User user = new User();
        user.setId(SessionUtils.getUserId());
        user.setName(SessionUtils.getCustomer().getName());

        orderService.updateStatus(orderResendDto.getParentId(), OrderStatus.COMPLETED);

        DataGrid<Order> orderDataGrid = orderService.orderResend(orderResendDto);
        if (!orderDataGrid.isFlag() || orderDataGrid.getObj() == null) {
            dataGrid.setFlag(false);
            dataGrid.setMsg("重派订单生成失败！");
            return dataGrid;
        }

        List<String> orderIdsList = Lists.newArrayList();
        orderIdsList.add(orderDataGrid.getObj().getId());
        FinaReturnGoodsDto finaReturnGoodsDto = new FinaReturnGoodsDto();
        finaReturnGoodsDto.setFinaReturnGoodsType(FinaReturnGoodsType.REDELIVERY);
        finaReturnGoodsDto.setTotalAmount(orderResendDto.getResendFee());
        finaReturnGoodsDto.setSaleCostPrice(orderResendDto.getResendFee());
        finaReturnGoodsDto.setBranchCostPrice(orderResendDto.getResendFee());
        finaReturnGoodsDto.setOperationDirectorPrice(orderResendDto.getResendFee());
        finaReturnGoodsDto.setDescription(orderResendDto.getContent());
        List<String> financeOrderIds = commonWeighingBusiness.createFinanceOrder(orderIdsList, finaReturnGoodsDto, user);

        if (financeOrderIds != null && !financeOrderIds.isEmpty()) {
            financeOrderIds.forEach(financeOrderId -> {
                FinanceOrder financeOrder = financeOrderService.findOne(financeOrderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(financeOrder.getCustomerId());
                StringBuilder mess = new StringBuilder("国外件重派,重派费用 " + orderResendDto.getResendFee());

                writeFinanceMessage(customerAssign.getSimpleCompanyName(), financeOrder.getFinaceOrderNo(), orderResendDto.getInsideNumber(), mess.toString(), financeOrder.getCustomerId());
            });
        } else {
            for (String orderId : orderIdsList) {
                OrderInfoDto orderInfoDto = orderService.getOrderInfo(orderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(orderInfoDto.getUserId());
                StringBuilder mess = new StringBuilder("国外件重派,重派费用 " + orderResendDto.getResendFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), "", orderInfoDto.getInsideNumber(), mess.toString(), orderInfoDto.getUserId());
            }
        }
        //记录原订单操作日志
        rabbitMessageService.sendAddOrderLogMessage(new RabbitMessage(RabbitMessageActionsConstant.ADD_ORDER_LOG, orderResendDto.getParentId(),
                new OperateHistory(orderResendDto.getParentId(), user.getId(), "国外订单重派", "国外订单重派，订单状态变更为已签收，新订单内单号为" + orderDataGrid.getObj().getInsideNumber(), new Date())));
        //记录新订单操作日志
        rabbitMessageService.sendAddOrderLogMessage(new RabbitMessage(RabbitMessageActionsConstant.ADD_ORDER_LOG, orderDataGrid.getObj().getId(),
                new OperateHistory(orderDataGrid.getObj().getId(), user.getId(), "国外订单重派", "通过国外订单重派新增订单", new Date())));

        dataGrid.setFlag(true);
        dataGrid.setMsg("操作成功");
        return dataGrid;
    }

    /**
     * 国外订单弃件
     *
     * @param batchReturnDto
     * @return
     */
    public DataGrid foreignOrderDiscard(BatchReturnDto batchReturnDto) {
        logger.debug("国外订单弃件，参数batchReturnDto={}", JSONObject.toJSONString(batchReturnDto));
        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        User user = new User();
        user.setId(SessionUtils.getUserId());
        user.setName(SessionUtils.getCustomer().getName());

        //修改订单状态
        orderService.updateStatus(batchReturnDto.getOrderIds(), OrderStatus.GIVE_UP);
        //将国外操作备注加到国外备注中
        orderService.updateForeignContent(batchReturnDto.getOrderIds(), batchReturnDto.getDescription());

        List<String> orderIds = Lists.newArrayList();
        orderIds.add(batchReturnDto.getOrderIds());
        FinaReturnGoodsDto finaReturnGoodsDto = new FinaReturnGoodsDto();
        finaReturnGoodsDto.setFinaReturnGoodsType(FinaReturnGoodsType.DISCARD);
        finaReturnGoodsDto.setTotalAmount(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setOperationDirectorPrice(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setBranchCostPrice(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setSaleCostPrice(batchReturnDto.getReturnFee());
        finaReturnGoodsDto.setDescription(batchReturnDto.getDescription());
        List<String> financeOrderIds = commonWeighingBusiness.createFinanceOrder(orderIds, finaReturnGoodsDto, user);

        if (financeOrderIds != null && !financeOrderIds.isEmpty()) {
            financeOrderIds.forEach(financeOrderId -> {
                FinanceOrder financeOrder = financeOrderService.findOne(financeOrderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(financeOrder.getCustomerId());

                StringBuilder mess = new StringBuilder("国外订单弃件,弃件费用 " + batchReturnDto.getReturnFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), financeOrder.getFinaceOrderNo(), batchReturnDto.getInsideNumbers(), mess.toString(), financeOrder.getCustomerId());
            });
        } else {
            for (String orderId : orderIds) {
                OrderInfoDto orderInfoDto = orderService.getOrderInfo(orderId);
                CustomerAssign customerAssign = customerAssignService.findByCustomerId(orderInfoDto.getUserId());

                StringBuilder mess = new StringBuilder("国外订单弃件,弃件费用 " + batchReturnDto.getReturnFee());
                writeFinanceMessage(customerAssign.getSimpleCompanyName(), "", orderInfoDto.getInsideNumber(), mess.toString(), orderInfoDto.getUserId());
            }
        }
        rabbitMessageService.sendAddOrderLogMessage(new RabbitMessage(RabbitMessageActionsConstant.ADD_ORDER_LOG, batchReturnDto.getOrderIds(),
                new OperateHistory(batchReturnDto.getOrderIds(), user.getId(), "国外订单弃件", "订单被弃件", new Date())));

        dataGrid.setFlag(true);
        dataGrid.setMsg("操作成功");
        return dataGrid;
    }

    public DataGrid allProblemInfo(String warehouseId, String organizationId) {
        FinaProblemInfoVO finaProblemInfoVO = new FinaProblemInfoVO();
        List<FinaProblemInfoVO.Customer> customers = new ArrayList<>();
        List<FinaProblemInfoVO.ProblemStatus> problemStatusList = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();

        //查询所有财务问题件
        List<Problem> finaProblems =
                finaProblemService.findAllByProblemStatus(FinaProblemStatus.CUSTOMER_SERVICE.name(), warehouseId, organizationId);
        //查询所有财务问题件
        List<OperateProblem> operateProblems =
                operateProblemService.findAllByProblemStatus(FinaProblemStatus.CUSTOMER_SERVICE.name());

        for (OperateProblem operateProblem : operateProblems) {
            Problem problem = new Problem();
            BeanUtils.copyProperties(operateProblem, problem);
            problems.add(problem);
        }


        problems.addAll(finaProblems);

        List<String> problemUserIds = problems.stream()
                .filter(p -> p.getProblemStatus().equals(FinaProblemStatus.CUSTOMER_SERVICE.name()))
                .map(problem -> problem.getUserId())
                .collect(Collectors.toList());

        getCustomer(warehouseId, customers, problemUserIds);
        getFinaProblemStatus(problemStatusList);

        finaProblemInfoVO.setCustomers(customers);
        finaProblemInfoVO.setProblemStatusList(problemStatusList);

        DataGrid dataGrid = new DataGrid();
        dataGrid.setFlag(true);
        dataGrid.setObj(finaProblemInfoVO);
        return dataGrid;
    }

    private void getFinaProblemStatus(List<FinaProblemInfoVO.ProblemStatus> problemStatusList) {
        //设置问题件状态
        Map<String, String> finaProblemStatusMap = FinaProblemStatus.list();
        finaProblemStatusMap.forEach((k, v) -> {
            FinaProblemInfoVO.ProblemStatus problemStatus = new FinaProblemInfoVO.ProblemStatus();
            problemStatus.setLabel(v);
            problemStatus.setValue(k);
            problemStatusList.add(problemStatus);
        });
    }

    private void getCustomer(String warehouseId, List<FinaProblemInfoVO.Customer> customers, List<String> problemUserIds) {
        if (problemUserIds != null && !problemUserIds.isEmpty()) {
            //查找问题件用户列表
            Map<String, User> mapUser = userService.mapByIds(problemUserIds);
            mapUser.forEach((k, v) -> {
                if (v.getWarehouseId().equals(warehouseId)) {

                    FinaProblemInfoVO.Customer customer = new FinaProblemInfoVO.Customer();
                    String name = "";
                    if (StringUtils.isNotBlank(v.getName())) {
                        name = v.getName();
                    } else {
                        if (StringUtils.isNotBlank(v.getNickName())) {
                            name = v.getNickName();
                        }
                    }
                    if (StringUtils.isNotBlank(v.getSimpleCompanyName())) {
                        name = v.getSimpleCompanyName();
                    }
                    customer.setLabel(v.getCustomerCode() + "-" + name);
                    customer.setValue(k);
                    customers.add(customer);
                }
            });
        }
    }

}

