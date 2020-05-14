package com.yangshan.eship.sales.service.sales;

import com.google.common.collect.Lists;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.sales.dto.ReimbursementListRequestDto;
import com.yangshan.eship.sales.dto.ReimbursementListResponeseDto;
import com.yangshan.eship.sales.dto.ReimbursementOperaRequestDto;
import com.yangshan.eship.sales.entity.sale.Reimbursement;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.repository.sale.ReimbursementDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author yuchao
 * @Description
 * @Date Administrator 2019/12/16
 * @Param
 */
@Service
@Slf4j
@Transactional
public class ReimbursementService implements ReimbursementServiceI {

    @Autowired
    private ReimbursementDao reimbursementDao;


    @Autowired
    private SalesStaffAssignService salesStaffAssignService;

    @Value("${static.file.server}")
    private String staticFileServer;


    @Override
    public DataGrid<Reimbursement> saveOrUpdateReimbursement(Reimbursement reimbursement) {

        DataGrid<Reimbursement> dataGrid = new DataGrid<>();

        try {
            if (reimbursement.getInvoiceUrl().contains("http")) {

                reimbursement.setInvoiceUrl(reimbursement.getInvoiceUrl());

            } else {

                reimbursement.setInvoiceUrl(staticFileServer + reimbursement.getInvoiceUrl());

            }


            Reimbursement reimbursementDb = reimbursementDao.save(reimbursement);

            dataGrid.setFlag(true);

            dataGrid.setObj(reimbursementDb);

            dataGrid.setMsg("保存成功");

        } catch (Exception e) {

            e.printStackTrace();
            String errorMsg = String.format("保存失败！");
            log.error(errorMsg, e);
            dataGrid.setMsg(errorMsg);

        }

        return dataGrid;
    }

    @Override
    public DataGrid<String> operat(ReimbursementOperaRequestDto requestDto) {

        DataGrid<String> dataGrid = new DataGrid<>();

        String id = requestDto.getId();

        Reimbursement reimbursementDb = reimbursementDao.findOne(id);

        List<String> listRoleCode = requestDto.getListRoleCode();

//        销售经理
        if (listRoleCode.contains(User.UserRoleCode.sale_manager.name())) {
//      审核通过
            if (requestDto.isHasPass()) {

//                如果没有人审核那么 存放当前审核的角色
                reimbursementDb.setReviewerCode(User.UserRoleCode.coo.name());

                reimbursementDb.setReviewer(null);

                reimbursementDb.setReviewerId(requestDto.getUserId());

//          审核驳回
            } else {

                reimbursementDb.setReviewerCode(User.UserRoleCode.sale_manager.name());

                reimbursementDb.setReviewer(requestDto.getUserName());

                reimbursementDb.setReviewerId(requestDto.getUserId());

                reimbursementDb.setReviewed(false);

                reimbursementDb.setReviewOpinion(requestDto.getReason());

            }

//            运营总监
        } else if (listRoleCode.contains(User.UserRoleCode.coo.name())) {

            if (requestDto.isHasPass()) {

                reimbursementDb.setReviewerCode(User.UserRoleCode.finance.name());

                reimbursementDb.setReviewer(null);

            } else {

                reimbursementDb.setReviewerCode(User.UserRoleCode.coo.name());

                reimbursementDb.setReviewer(requestDto.getUserName());

                reimbursementDb.setReviewerId(requestDto.getUserId());

                reimbursementDb.setReviewed(false);

                reimbursementDb.setReviewOpinion(requestDto.getReason());
            }

//            财务
        } else {

            if (requestDto.isHasPass()) {

                reimbursementDb.setReviewerCode(User.UserRoleCode.finance.name());

                reimbursementDb.setReviewer(requestDto.getUserName());

                reimbursementDb.setReviewerId(requestDto.getUserId());

                reimbursementDb.setReviewed(true);

            } else {

                reimbursementDb.setReviewerCode(User.UserRoleCode.finance.name());

                reimbursementDb.setReviewer(requestDto.getUserName());

                reimbursementDb.setReviewerId(requestDto.getUserId());

                reimbursementDb.setReviewed(false);

                reimbursementDb.setReviewOpinion(requestDto.getReason());
            }
        }

        reimbursementDb.setReviewTime(new Date());

        reimbursementDao.save(reimbursementDb);

        dataGrid.setFlag(true);

        return dataGrid;
    }

    @Override
    public DataGrid getSubmit(String orgId) {

        DataGrid dataGrid = new DataGrid();
        dataGrid.setObj(reimbursementDao.findDistinctSubmitter(orgId));

        dataGrid.setFlag(true);

        return dataGrid;
    }

    //    已处理指 审核状态 整体通过或驳回 未处理 只显示到上一级
    @Override
    public DataGrid<ReimbursementListResponeseDto> list(ReimbursementListRequestDto requestDto) {

        String userId = requestDto.getUserId();

        List<String> listRoleCode = requestDto.getListRoleCode();

        String roleCode = requestDto.getRoleCode();

//        已处理
        if (requestDto.isHasSolve()) {

//            只有销售经理 只能看 名下所有销售 ，就是提交人 ；财务和运营总监 可以看分公司所有
            if (roleCode.equals(User.UserRoleCode.sale_manager.name())) {

                List<String> listUserId = new ArrayList<>();

                listUserId.add(userId);

                List<SalesStaffAssign> listSalesStaffAssign = salesStaffAssignService.findInSaleManagerIds(listUserId);

                requestDto.setSubmitterIds(listSalesStaffAssign.stream().map(salesStaffAssign -> salesStaffAssign.getSalesStaffId()).collect(Collectors.toList()));

            } else if (roleCode.equals(User.UserRoleCode.sale.name())) {
//          销售 看自己
                requestDto.setSubmitterIds(Lists.newArrayList(userId));

            } else {

//                财务 和 运营总监 看分公司所有


            }

//      未处理
        } else {

            if (roleCode.equals(User.UserRoleCode.sale_manager.name())) {

//            如果是未处理，那么只显示等待自己处理的
                requestDto.setReviewerId(userId);

            } else if (roleCode.equals(User.UserRoleCode.finance.name())) {

                listRoleCode.add(User.UserRoleCode.finance.name());

                requestDto.setListRoleCode(listRoleCode);

            } else if (roleCode.equals(User.UserRoleCode.coo.name())) {

                listRoleCode.add(User.UserRoleCode.coo.name());

                listRoleCode.add(User.UserRoleCode.finance.name());

                requestDto.setListRoleCode(listRoleCode);

            } else if (roleCode.equals(User.UserRoleCode.sale.name())) {
//          销售 看自己
                List<String> listUserId = new ArrayList<>();

                listUserId.add(userId);

                requestDto.setSubmitterIds(listUserId);
            } else {
//          集团财务。。。不归这方法管
                requestDto.setReviewerId(userId);
            }
        }


        DataGrid<ReimbursementListResponeseDto> dtoDataGrid = reimbursementDao.list(requestDto);

        //            只针对 待处理的数据 销售经理 运营总监 财务 才能进行操作 其他只能看
        if (!requestDto.isHasSolve()) {

            for (ReimbursementListResponeseDto responeseDto : dtoDataGrid.getRows()) {

//                判断该角色是否能操作 未处理的 报销审批
                if (listRoleCode.contains(responeseDto.getReviewerCode())) {

                    responeseDto.setHasOperator(true);

                } else {

                    responeseDto.setHasOperator(false);
                }
            }
        }

        return dtoDataGrid;
    }
}
