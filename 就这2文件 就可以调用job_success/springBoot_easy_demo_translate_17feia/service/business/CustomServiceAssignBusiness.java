package com.yangshan.eship.sales.business;

import com.yangshan.eship.business.CommonCustomerSearchBusiness;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.finance.entity.cust.Wallet;
import com.yangshan.eship.finance.service.cust.WalletServiceI;
import com.yangshan.eship.sales.dto.*;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceZone;
import com.yangshan.eship.sales.service.cust.CustomerAssignServiceI;
import com.yangshan.eship.sales.service.serv.CustomServiceAssignServiceI;
import com.yangshan.eship.sales.service.serv.CustomServiceZoneServiceI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CustomServiceAssignBusiness {

    private static final Logger logger = LoggerFactory.getLogger(CustomServiceAssignBusiness.class);

    @Value("${rest.exportData.customerSearchUrl}")
    private String customerSearchUrl;

    @Autowired
    private CustomServiceAssignServiceI customServiceAssignService;

    @Autowired
    private UserServiceI userService;

    @Autowired
    private CustomServiceZoneServiceI customServiceZoneService;

    @Autowired
    private CustomerAssignServiceI customerAssignService;

    @Autowired
    private CommonCustomerSearchBusiness commonCustomerSearchBusiness;

    public DataGrid<CustomServiceAssignDto> list(CustomServiceAssign assign) {

        DataGrid<CustomServiceAssign> dataGrid = customServiceAssignService.list(assign);
        DataGrid<CustomServiceAssignDto> resultGrid = new DataGrid<>();
        resultGrid.setTotal(dataGrid.getTotal());
        resultGrid.setFlag(true);

        if (dataGrid == null || dataGrid.getTotal() < 1) {
            return resultGrid;
        }

        //获取当前记录中的客服id
        Set<String> userIds = new HashSet<>();
        Set<String> zoneIds = new HashSet<>();
        for (CustomServiceAssign csa : dataGrid.getRows()) {
            userIds.add(csa.getCustomServiceStaffId());
            if (StringUtils.isNotBlank(csa.getServiceStaffZoneId())) {
                zoneIds.add(csa.getServiceStaffZoneId());
            }
        }
        //查询客服详情
        Map<String, User> userMap = userService.mapByIds(new ArrayList<>(userIds));

        Map<String, String> zoneMap = new HashMap<>();
        if (!zoneIds.isEmpty()) {
            //查询所在区域
            List<CustomServiceZone> zones = customServiceZoneService.listByIds(new ArrayList<>(zoneIds));
            if (zones != null && !zones.isEmpty()) {
                for (CustomServiceZone zone : zones) {
                    zoneMap.put(zone.getId(), zone.getZoneName());
                }
            }
        }


        for (CustomServiceAssign csa : dataGrid.getRows()) {

            CustomServiceAssignDto dto = new CustomServiceAssignDto();
            BeanUtils.copyProperties(csa, dto);

            //设置用户基本信息
            User customService = userMap.get(csa.getCustomServiceStaffId());
            if (customService == null) {
                logger.error("## 客服id=" + csa.getCustomServiceStaffId() + "没有查询到用户信息");
            } else {
                dto.setCustomServiceStaffName(customService.getNickName());
                dto.setEmail(customService.getEmail());
                dto.setPhone(customService.getPhone());
                dto.setQq(customService.getQq());
                dto.setWechat(customService.getWechat());
                dto.setName(customService.getName());
                dto.setLoginId(customService.getLoginId());
                dto.setSimpleCompanyName(customService.getSimpleCompanyName());
            }

            //设置客服区域信息
            if (StringUtils.isNotBlank(csa.getServiceStaffZoneId())) {
                String zoneName = zoneMap.get(csa.getServiceStaffZoneId());
                if (StringUtils.isNotBlank(zoneName)) {
                    dto.setZoneName(zoneName);
                } else {
                    logger.error("## 客服语言区域id=" + csa.getServiceStaffZoneId() + "不存在");
                }
            }

            resultGrid.getRows().add(dto);
        }

        return resultGrid;
    }

    /**
     * 更新客服的区域
     *
     * @param ids                 主键id
     * @param serviceStaffZoneId 区域id，取消设置为null
     * @return
     * @author: Kee.Li
     * @date: 2017/11/8 13:39
     */
    public void update(List<String> ids, String serviceStaffZoneId) {
        customServiceAssignService.update(ids, serviceStaffZoneId);
    }

    public DataGrid<CustomServiceSalseResponseDto> listSalesData() {
        List<CustomServiceSalseResponseDto> customServiceSalseResponseDtos = new ArrayList<>();

        List<String> salesStaffIdList = customerAssignService.listWarehouseSalesStaffs(SessionUtils.getWarehouseId());
        if (salesStaffIdList.isEmpty()) {
            return new DataGrid<>(true, new ArrayList<>());
        }

        Map<String, User> userMap = userService.mapByIds(salesStaffIdList);
        userMap.forEach((userId, sales) -> {
            CustomServiceSalseResponseDto customServiceSalseResponseDto = new CustomServiceSalseResponseDto();
            customServiceSalseResponseDto.setSalesStaffId(userId);
            customServiceSalseResponseDto.setSalesStaffName(sales.getName());

            customServiceSalseResponseDtos.add(customServiceSalseResponseDto);
        });

        return new DataGrid<>(true, customServiceSalseResponseDtos);
    }

    public DataGrid<StaffInfoStaticsDto> listCustomServiceManagers(String warehouseId) {
        //获取分公司下所有客服经理
        List<User> customServiceManagers = userService.findCustomerByCodeAndWarehouseId(User.UserRoleCode.custom_service_manager.name(), warehouseId);
        Map<String, String> customServiceManagerMap = customServiceManagers.stream().collect(Collectors.toMap(User::getId, User::getName));

        List<String> customServiceManagerIds = customServiceManagers.stream().map(User::getId).collect(Collectors.toList());
        //获取客服经理下的客服数量
        List<CustomServiceAssign> customServiceAssignList = customServiceAssignService.getCustomServiceCustomerStatics(customServiceManagerIds);
        Map<String, Long> customServiceAssignMap = customServiceAssignList.stream().collect(Collectors.toMap(CustomServiceAssign::getCustomServiceManagerId, CustomServiceAssign::getCustomServiceTotal));

        List<StaffInfoStaticsDto> staffInfoStaticsDtoList = new ArrayList<>();
        customServiceManagerMap.forEach((customServiceManagerId, customServiceManagerName) -> {
            StaffInfoStaticsDto staffInfoStaticsDto = new StaffInfoStaticsDto();
            staffInfoStaticsDto.setId(customServiceManagerId);
            staffInfoStaticsDto.setName(customServiceManagerName);
            staffInfoStaticsDto.setStaffCount(0);
            if (customServiceAssignMap.containsKey(customServiceManagerId)) {
                staffInfoStaticsDto.setStaffCount(customServiceAssignMap.get(customServiceManagerId).intValue());
            }

            staffInfoStaticsDtoList.add(staffInfoStaticsDto);
        });

        return new DataGrid<>(true, staffInfoStaticsDtoList);
    }

    public DataGrid<CustomServiceInfoDto> listCustomServices(StaffSearchRequestDto staffSearchRequestDto) {
        List<CustomServiceInfoDto> customServiceInfoDtos = new ArrayList<>();

        DataGrid<CustomServiceAssign> customServiceAssignList = customServiceAssignService.listCustomServices(staffSearchRequestDto);

        //处理数据
        if (!customServiceAssignList.getRows().isEmpty()) {
            List<String> userIds = customServiceAssignList.getRows().stream().map(CustomServiceAssign::getCustomServiceStaffId).collect(Collectors.toList());
            Map<String, User> customServiceAssignMap = userService.mapByIds(userIds);

            customServiceAssignList.getRows().stream().forEach(customServiceAssign -> {
                CustomServiceInfoDto customServiceInfoDto = new CustomServiceInfoDto();
                customServiceInfoDto.setId(customServiceAssign.getId());
                User customServiceUser = customServiceAssignMap.get(customServiceAssign.getCustomServiceStaffId());
                customServiceInfoDto.setCustomServiceManagerName(customServiceAssign.getCustomServiceManagerName());

                if (customServiceUser != null) {
                    customServiceInfoDto.setCreatedDate(customServiceUser.getCreatedDate());
                    customServiceInfoDto.setEmail(customServiceUser.getEmail());
                    customServiceInfoDto.setLoginId(customServiceUser.getLoginId());
                    customServiceInfoDto.setName(customServiceUser.getName());
                    customServiceInfoDto.setNickName(customServiceUser.getNickName());
                    customServiceInfoDto.setPhoneNo(customServiceUser.getPhone());
                    customServiceInfoDto.setQq(customServiceUser.getQq());
                    customServiceInfoDto.setWechat(customServiceUser.getWechat());
                }

                customServiceInfoDtos.add(customServiceInfoDto);
            });
        }

        return new DataGrid<>(true, customServiceInfoDtos);
    }

    public DataGrid distributionCustomServices(String customManagerId, List<String> customServiceIds) {
        Set<String> userIdSet = new HashSet<>();
        userIdSet.add(customManagerId);
        customServiceIds.stream().forEach(customServiceAssignId -> {
            userIdSet.add(customServiceAssignId);
        });

        Map<String, User> staffMap = userService.mapByIds(new ArrayList<>(userIdSet));

        List<CustomServiceAssign> customServiceAssignList = new ArrayList<>();
        User customerManagerStaff = staffMap.get(customManagerId);
        customServiceIds.stream().forEach(customServiceAssignId -> {
            CustomServiceAssign customServiceAssign = new CustomServiceAssign();

            User customerServiceStaff = staffMap.get(customServiceAssignId);
            customServiceAssign.setCustomServiceStaffId(customerServiceStaff.getId());
            customServiceAssign.setCustomServiceStaffName(customerServiceStaff.getName());
            customServiceAssign.setCustomServiceManagerId(customerManagerStaff.getId());
            customServiceAssign.setCustomServiceManagerName(customerManagerStaff.getName());
            customServiceAssign.setWarehouseId(SessionUtils.getWarehouseId());

            customServiceAssignList.add(customServiceAssign);
        });

        customServiceAssignService.save(customServiceAssignList);

        //同步customerAssign表中客服客服经理绑定关系
        customerAssignService.updateCustomerAssignCustomManagerId(customServiceIds, customManagerId);

        return new DataGrid(true, "客服分配成功!");
    }

    public DataGrid cancelDistributionCustomServices(List<String> customServiceIds) {
        List<CustomServiceAssign> customServiceAssignList = customServiceAssignService.findByCustomServiceStaffIdIn(customServiceIds);

        //直接删除
        customServiceAssignService.delete(customServiceAssignList);

        //同步customerAssign表中客服客服经理绑定关系
        customerAssignService.updateCustomerAssignCustomManagerId(customServiceIds, null);

        return new DataGrid(true, "客服取消分配成功!");
    }

    public DataGrid<CustomerInfoResponseDto> listSalesCustomerAssign(CustomerInfoRequestDto customerInfoRequestDto) {
        customerInfoRequestDto.setCustomerSearchUrl(customerSearchUrl);

        //查找未分配销售的客户时候, 要把销售id设为null
        if (customerInfoRequestDto.getDistributionSaleState() != null && !customerInfoRequestDto.getDistributionSaleState()) {
            customerInfoRequestDto.setSalesStaffId(null);
        }

        return commonCustomerSearchBusiness.searchCustomerData(customerInfoRequestDto);
    }
}
