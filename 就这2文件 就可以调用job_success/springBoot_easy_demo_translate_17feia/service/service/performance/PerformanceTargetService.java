package com.yangshan.eship.sales.service.performance;

import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.entity.BaseEntity;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.sales.dto.PerformanceTargetRequestVO;
import com.yangshan.eship.sales.dto.PerformanceTargetResponseVO;
import com.yangshan.eship.sales.entity.performance.PerformanceTarget;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.repository.sale.PerformanceTargetDao;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author: Yifan
 * @Date: 2019/12/12 15:06
 * @Description:
 */
@Service
@Transactional
public class PerformanceTargetService implements PerformanceTargetServiceI {
    @Autowired
    private PerformanceTargetDao performanceTargetDao;
    @Autowired
    private SalesStaffAssignServiceI salesStaffAssignServiceI;
    @Autowired
    private UserServiceI userServiceI;

    @Override
    public List<SalesStaffAssign> getSales(String userId) {
        List<SalesStaffAssign> salesStaffAssigns = salesStaffAssignServiceI.findInSaleManagerIds(Arrays.asList(userId));

        return salesStaffAssigns;
    }

    @Override
    public String save(PerformanceTarget performanceTarget) {
        if (StringUtils.isNotBlank(performanceTarget.getId())) {
            PerformanceTarget performanceTargetDB = performanceTargetDao.findOne(performanceTarget.getId());
            BeanUtils.copyProperties(performanceTarget, performanceTargetDB, BaseEntity.toIgnoreProperties());

            performanceTargetDao.save(performanceTargetDB);
        } else {
            Long count = this.countBySalesIdAndCreatedDate(performanceTarget.getSalesId());
            if (count > 0) {
                return "该销售在本年已设置销售目标，无法新增";
            }
            performanceTargetDao.save(performanceTarget);
        }

        return "";
    }

    @Override
    public PerformanceTargetResponseVO getById(String id) {
        PerformanceTarget performanceTarget = performanceTargetDao.getOne(id);
        PerformanceTargetResponseVO performanceTargetResponseVO = new PerformanceTargetResponseVO();
        BeanUtils.copyProperties(performanceTarget, performanceTargetResponseVO);

        List<SalesStaffAssign> salesStaffAssignList = this.getSales(SessionUtils.getUserId());
        Map<String, String> salesStaffAssignMap = salesStaffAssignList.stream().collect(Collectors.toMap(SalesStaffAssign::getSalesStaffId, s -> s.getSalesStaffName()));
        performanceTargetResponseVO.setSalesName(salesStaffAssignMap.get(performanceTargetResponseVO.getSalesId()));

        return performanceTargetResponseVO;
    }

    @Override
    public void deleteById(String id) {
        performanceTargetDao.delete(id);
    }

    @Override
    public DataGrid<PerformanceTargetResponseVO> list(PerformanceTargetRequestVO performanceTargetRequestVO) {
        DataGrid<PerformanceTargetResponseVO> performanceTargetResponseVODataGrid = new DataGrid<PerformanceTargetResponseVO>();

        //所有销售
        SalesStaffAssign salesStaffAssign = new SalesStaffAssign();
        salesStaffAssign.setPagingDto(performanceTargetRequestVO.getPagingDto());
        salesStaffAssign.setSalesManagerId(performanceTargetRequestVO.getSalesManagerId());
        if (StringUtils.isNotBlank(performanceTargetRequestVO.getSalesId())) {
            salesStaffAssign.setSalesStaffId(performanceTargetRequestVO.getSalesId());
        }
        DataGrid<SalesStaffAssign> salesStaffAssignDataGrid = salesStaffAssignServiceI.list(salesStaffAssign);
        List<SalesStaffAssign> salesStaffAssignList = salesStaffAssignDataGrid.getRows();
        List<String> salesIdList = salesStaffAssignList.stream().map(SalesStaffAssign::getSalesStaffId).collect(Collectors.toList());
        Map<String, User> userMap = userServiceI.mapByIds(salesIdList);

        List<PerformanceTarget> performanceTargetList = performanceTargetDao.findBySalesIdIn(salesIdList);
        Map<String, PerformanceTarget> performanceTargetMap = performanceTargetList.stream().collect(Collectors.toMap(PerformanceTarget::getSalesId, s -> s));

        List<PerformanceTargetResponseVO> performanceTargetResponseVOList = new ArrayList<PerformanceTargetResponseVO>();
        userMap.forEach((userId, user) -> {
            PerformanceTargetResponseVO performanceTargetResponseVO = new PerformanceTargetResponseVO();
            performanceTargetResponseVO.setSalesId(userId);
            performanceTargetResponseVO.setSalesName(user.getName());
            performanceTargetResponseVO.setLoginId(user.getLoginId());
            performanceTargetResponseVO.setNickName(user.getNickName());
            performanceTargetResponseVO.setPhone(user.getPhone());
            performanceTargetResponseVO.setEmail(user.getEmail());
            performanceTargetResponseVO.setWechat(user.getWechat());
            performanceTargetResponseVO.setQq(user.getQq());

            if (performanceTargetMap.containsKey(userId)) {
                PerformanceTarget performanceTarget = performanceTargetMap.get(userId);
                BeanUtils.copyProperties(performanceTarget, performanceTargetResponseVO);
            }

            performanceTargetResponseVOList.add(performanceTargetResponseVO);
        });

        performanceTargetResponseVODataGrid.setRows(performanceTargetResponseVOList);
        performanceTargetResponseVODataGrid.setTotal(salesStaffAssignDataGrid.getTotal());

        return performanceTargetResponseVODataGrid;
    }

    @Override
    public Long countBySalesIdAndCreatedDate(String salesId) {
        return performanceTargetDao.countBySalesIdAndCreatedDate(salesId, DateUtil.format(new Date(), "yyyy"));
    }
}