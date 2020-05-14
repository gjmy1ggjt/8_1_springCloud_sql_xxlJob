package com.yangshan.eship.sales.controller;

import com.yangshan.eship.author.dto.account.RoleDto;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.dto.ReimbursementListRequestDto;
import com.yangshan.eship.sales.dto.ReimbursementListResponeseDto;
import com.yangshan.eship.sales.dto.ReimbursementOperaRequestDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.Reimbursement;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.sales.ReimbursementProcessServiceI;
import com.yangshan.eship.sales.service.sales.ReimbursementServiceI;
import io.swagger.annotations.Api;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author yuchao
 * @Description
 * @Date Administrator 2019/12/16
 * @Param
 */
@RestController
@RequestMapping(Version.VERSION + "/reimbursement")
@Api(value = "ReimbursementController", tags = "费用报销")
public class ReimbursementController {

    @Autowired
    private ReimbursementServiceI reimbursementServiceI;

    @Autowired
    private ReimbursementProcessServiceI reimbursementProcessServiceI;

    @Autowired
    private CustomerAssignServiceI customerAssignServiceI;

    @RequestMapping(value = "/save", method = RequestMethod.POST)
    public DataGrid<String> save(@RequestBody Reimbursement reimbursement) {

        DataGrid<String> dataGrid = new DataGrid<String>();

        String userId = SessionUtils.getUserId();

//        销售 选择 该销售经理的审核人
        List<CustomerAssign> listCustomerAssign = customerAssignServiceI.getByOrganizationIdAndSalesStaffId(SessionUtils.getOrganizationId(), userId);

        if (listCustomerAssign.isEmpty()) {

            dataGrid.setFlag(false);

            dataGrid.setMsg("该销售没有设置销售经理！");

            return dataGrid;
        }

        CustomerAssign customerAssign = listCustomerAssign.get(0);

        reimbursement.setReviewer(customerAssign.getCustomerName());

        reimbursement.setReviewerId(customerAssign.getSalesManagerId());

        reimbursement.setReviewerCode(User.UserRoleCode.sale_manager.name());

        reimbursement.setOrganizationId(SessionUtils.getOrganizationId());

        reimbursement.setSubmitter(SessionUtils.getUserName());

        reimbursement.setSubmitterId(userId);

        DataGrid<Reimbursement> dataGridDb = reimbursementServiceI.saveOrUpdateReimbursement(reimbursement);

//        Reimbursement reimbursementDb = dataGridDb.getObj();
//
//        ReimbursementProcess reimbursementProcess = new ReimbursementProcess();
//
//        reimbursementProcess.setReimbursementId(reimbursementDb.getId());
//
//        reimbursementProcess.setUserId(SessionUtils.getUserId());
//
//        reimbursementProcess.setUserName(SessionUtils.getUserName());
//
//        reimbursementProcess.setOrganizationId(SessionUtils.getOrganizationId());
//
//        reimbursementProcessServiceI.save(reimbursementProcess);

        dataGrid.setFlag(dataGridDb.isFlag());

        dataGrid.setMsg(dataGridDb.getMsg());

        return dataGrid;
    }

    //    操作
    @RequestMapping(value = "/operat", method = RequestMethod.POST)
    public DataGrid<String> saveOrUpdate(@RequestBody ReimbursementOperaRequestDto requestDto) {

//        List<RoleDto> listRoleDto = SessionUtils.getCustomer().getRoles();

        List<String> listCode = new ArrayList<>();

        listCode.add(SessionUtils.getCurrentRoleCode());

        requestDto.setListRoleCode(listCode);
//        requestDto.setListRoleCode(listRoleDto.stream().map(roleDto -> roleDto.getCode()).collect(Collectors.toList()));

        requestDto.setUserId(SessionUtils.getUserId());

        requestDto.setUserName(SessionUtils.getUserName());

        return reimbursementServiceI.operat(requestDto);
    }

    @RequestMapping(value = "/sumbiter", method = RequestMethod.GET)
    public DataGrid sumbiter() {

        return reimbursementServiceI.getSubmit(SessionUtils.getOrganizationId());
    }


//   上传发票 通用上传图片接口 /author/common/uploadImg POST


    @RequestMapping(value = "/list", method = RequestMethod.POST)
    public DataGrid<ReimbursementListResponeseDto> list(@RequestBody ReimbursementListRequestDto requestDto) {

        requestDto.setUserId(SessionUtils.getUserId());

        requestDto.setOrganizationId(SessionUtils.getOrganizationId());

        requestDto.setRoleCode(SessionUtils.getCurrentRoleCode());

        return reimbursementServiceI.list(requestDto);

    }


}
