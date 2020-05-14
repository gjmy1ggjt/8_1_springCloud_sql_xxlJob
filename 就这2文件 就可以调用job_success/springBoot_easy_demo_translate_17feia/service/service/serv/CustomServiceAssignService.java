package com.yangshan.eship.sales.service.serv;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.jpa.PagingUtils;
import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.sales.dto.CustomServiceSearchRequestDto;
import com.yangshan.eship.sales.dto.CustomServiceSearchResponseDto;
import com.yangshan.eship.sales.dto.StaffSearchRequestDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import com.yangshan.eship.sales.repository.cust.CustomerAssignDao;
import com.yangshan.eship.sales.repository.cust.DataPermissionsDao;
import com.yangshan.eship.sales.repository.serv.CustomServiceAssignDao;
import com.yangshan.eship.sales.repository.serv.CustomServiceZoneItemDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.MessageFormat;
import java.util.*;


@Service
@Transactional
public class CustomServiceAssignService implements CustomServiceAssignServiceI {

	private final static Logger logger = LoggerFactory.getLogger(CustomServiceAssignService.class);

    @Autowired
    private CustomServiceAssignDao customServiceAssignDao;

    @Autowired
    private CustomServiceZoneItemDao customServiceZoneItemDao;

    @Autowired
    private CustomerAssignDao customerAssignDao;

    @Autowired
	private UserServiceI userService;

    @Autowired
    private DataPermissionsDao dataPermissionsDao;
	
	@PersistenceContext
	private EntityManager entityManager;
    
    @Override
    public DataGrid<CustomServiceAssign> list(CustomServiceAssign assign) {
        return customServiceAssignDao.list(assign);
    }

    @Override
    public void update(List<String> ids, String serviceStaffZoneId) {
    	ids = QueryPlanCacheWrapper.wrapper(ids,"CustomServiceAssignService.update.ids");
        customServiceAssignDao.update(ids,serviceStaffZoneId);
    }

	@Override
	public CustomServiceAssign findById(String id) {
		return customServiceAssignDao.findOne(id);
	}

	@Override
	public List<CustomServiceAssign> listByIds(List<String> serviceIds) {
    	serviceIds = QueryPlanCacheWrapper.wrapper(serviceIds,"CustomServiceAssignService.listByIds.serviceIds");
		return customServiceAssignDao.listByIds(serviceIds);
	}

	@Override
	public DataGrid<CustomServiceAssign> findByOrganizationIdAndCustomServiceManagerId(CustomServiceAssign customServiceAsgin,
			PagingDto pagingDto) {
		
		Map<String, Object> parmasMap = new HashMap<>();
		parmasMap.put("organizationId", customServiceAsgin.getOrganizationId());
		parmasMap.put("customServiceManagerId", customServiceAsgin.getCustomServiceManagerId());
		return PagingUtils.exeHql(entityManager, "from CustomServiceAssign csz where csz.organizationId=:organizationId and csz.customServiceManagerId=:customServiceManagerId",
				parmasMap, pagingDto, "csz");

	}

	@Override
	public void customServiceAsginDel(List<String> customServiceAssginIds) {
		for(String id :customServiceAssginIds){
			customServiceAssignDao.delete(id);
		}
	}

	@Override
	public void customServiceAsginSave(List<CustomServiceAssign> customServiceAssigns,String organizationId,String userId) {

		for(CustomServiceAssign customServiceAsgin :customServiceAssigns){
			List<CustomServiceAssign> customServiceAsginDb = customServiceAssignDao.findByCustomServiceStaffId(customServiceAsgin.getCustomServiceStaffId());
			
			customServiceAsgin.setOrganizationId(organizationId);
			
			if(customServiceAsginDb.size()>0){
				throw new EshipException(MessageFormat.format("该客服人员已经分配给了`{0}`", customServiceAsginDb.get(0).getCustomServiceManagerName()));
			}
			customServiceAsgin.setCreatedDate(new Date());
//			customServiceAsgin.setServiceStaffZoneId("空间ID");
			customServiceAssignDao.save(customServiceAsgin);
		}
	}

	@Override
	public List<CustomServiceAssign> customServiceAsginListAll(CustomServiceAssign customServiceAsgin) {
		return customServiceAssignDao.findByOrganizationId(customServiceAsgin.getOrganizationId());
	}

	@Override
	public DataGrid<User> listMyCustomServices(String userId) {
		List<CustomServiceAssign> customServiceAsginDb = customServiceAssignDao.findByCustomServiceManagerId(userId);

		Set<String> userIds = new HashSet<>();
		for (CustomServiceAssign customServiceAssign : customServiceAsginDb) {
			userIds.add(customServiceAssign.getCustomServiceStaffId());
		}

		if (userIds.size() > 0) {
			Map<String,Integer> countMap = Maps.newHashMap();
			List<Object[]> targetCountList = dataPermissionsDao.countUserTarget(User.UserRoleCode.custom_service.name(), new ArrayList<>(userIds));
			if(targetCountList != null && targetCountList.size() > 0){
				for(int i=0; i<targetCountList.size();i++){
					Object[] targetCount = targetCountList.get(i);
					String key = (String)targetCount[0];
					Object value = targetCount[1];
					countMap.put(key,Long.valueOf(String.valueOf(value)).intValue());
				}
			}

			List<User> users = userService.listByIds(new ArrayList<>(userIds));
			if(users != null && !users.isEmpty()){
				users.forEach(user -> {
					if(countMap.containsKey(user.getId())){
						user.setTargetCount(countMap.get(user.getId()));
					}
				});
			}

			return new DataGrid<>(true, users);
		}

		return new DataGrid<>();
	}

	@Override
	public CustomerAssign assignServiceStaff(Map<String,String> serviceAssginMap) {

		String destinationNo = serviceAssginMap.get("destinationNo");
		String userId = serviceAssginMap.get("userId");
		CustomerAssign customerAssign = customerAssignDao.findByCustomerId(userId);
		logger.debug("............customerAssign....{}",JSONObject.toJSONString(customerAssign));
		if(customerAssign!=null && StringUtils.isBlank(customerAssign.getCustomServiceStaffId())){
			String warehouseId = customerAssign.getWarehouseId();
			//查找没有分配的
			List<CustomServiceAssign> customServiceAssigns = customServiceAssignDao.findCustomerSerivceAsignByDestinationCountryNoAndWarehouseId(destinationNo,warehouseId);
			logger.debug("............customerAssignSize....{}",customServiceAssigns.size());
			if(customServiceAssigns.size()>0){
				CustomServiceAssign customServiceAssign = customServiceAssigns.get(new Random().nextInt(customServiceAssigns.size()));

				//获取客服的信息
				String customServiceStaffId = customServiceAssign.getCustomServiceStaffId();
				String customServiceManagerId = customServiceAssign.getCustomServiceManagerId();

				customerAssign.setCustomServiceStaffId(customServiceStaffId);
				customerAssign.setCustomServiceManagerId(customServiceManagerId);

				customerAssign = customerAssignDao.save(customerAssign);

			}else{
				//分组进行查看(查找已经分配了，而且是分配最少的)
				List<Object[]>  groupByInfos = customServiceZoneItemDao.findCustomServiceZoneByDestinationCountryNoAndWarehouseId(destinationNo,warehouseId);
				logger.debug("............groupByInfos....{}",JSONObject.toJSONString(groupByInfos));
				if(groupByInfos.size()>0){
					//最小的
					Object[] firstAssignInfo = groupByInfos.get(groupByInfos.size()-1);
					//获取客服的信息
					String customServiceStaffId = (String) firstAssignInfo[0];
					String customServiceManagerId = (String) firstAssignInfo[2];

					customerAssign.setCustomServiceStaffId(customServiceStaffId);
					customerAssign.setCustomServiceManagerId(customServiceManagerId);

					customerAssign = customerAssignDao.save(customerAssign);
				}
			}
		}
		return customerAssign;
	}

	@Override
	public CustomerAssign findByCustomerId(String userId) {
		return  customerAssignDao.findByCustomerId(userId);
	}

	@Override
	public DataGrid<CustomServiceSearchResponseDto> listCustomService(CustomServiceSearchRequestDto customServiceSearchRequestDto) {
		return customServiceAssignDao.listCustomService(customServiceSearchRequestDto);
	}

	@Override
	public DataGrid distributionCustomService(String customServiceStaffId, List<String> customerIds) {
    	List<CustomerAssign> customerAssignList = customerAssignDao.findByCustomerIdIn(customerIds);

		//获取客服对应的客服经理
		List<CustomServiceAssign> customServiceAsginDb = customServiceAssignDao.findByCustomServiceStaffId(customServiceStaffId);
		String customerManagerId = customServiceAsginDb.get(0).getCustomServiceManagerId();

		customerAssignList.stream().forEach(customerAssign -> {
			customerAssign.setCustomServiceStaffId(customServiceStaffId);
			customerAssign.setCustomServiceManagerId(customServiceAsginDb.isEmpty() ? null : customerManagerId);
		});

		customerAssignDao.save(customerAssignList);

		return new DataGrid(true, "客户分配成功!");
	}

	@Override
	public DataGrid cancelDistribution(List<String> customerIds) {
		List<CustomerAssign> customerAssignList = customerAssignDao.findByCustomerIdIn(customerIds);

		customerAssignList.stream().forEach(customerAssign -> {
			customerAssign.setCustomServiceStaffId(null);
			customerAssign.setCustomServiceManagerId(null);
		});

		customerAssignDao.save(customerAssignList);

		return new DataGrid(true, "取消分配成功!");
	}

	@Override
	public List<CustomServiceAssign> getCustomServiceCustomerStatics(List<String> customServiceManagerIds) {
		return customServiceAssignDao.getCustomServiceCustomerStatics(customServiceManagerIds);
	}

	@Override
	public List<CustomServiceAssign> findByWarehouseIdAndCustomServiceStaffNameContaining(String warehouseId, String name) {
		return customServiceAssignDao.findByWarehouseIdAndCustomServiceStaffNameContaining(warehouseId, name);
	}

	@Override
	public DataGrid<CustomServiceAssign> listCustomServices(StaffSearchRequestDto staffSearchRequestDto) {

		return customServiceAssignDao.listCustomServices(staffSearchRequestDto);
	}

	@Override
	public List<CustomServiceAssign> findByCustomServiceStaffIdIn(List<String> customManagerIds) {
		return customServiceAssignDao.findByCustomServiceStaffIdIn(customManagerIds);
	}

	@Override
	public void save(List<CustomServiceAssign> customServiceAssignList) {
		customServiceAssignDao.save(customServiceAssignList);
	}

	@Override
	public void delete(List<CustomServiceAssign> customServiceAssignList) {
		customServiceAssignDao.delete(customServiceAssignList);
	}
}