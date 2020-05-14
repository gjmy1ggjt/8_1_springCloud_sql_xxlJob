package com.yangshan.eship.sales.service.sales;

import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.jpa.PagingUtils;
import com.yangshan.eship.common.utils.DataUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import com.yangshan.eship.constants.ErrorCodeConstant;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.sales.dto.AccountApplicationDto;
import com.yangshan.eship.sales.entity.sale.AccountApplication;
import com.yangshan.eship.sales.entity.sale.AccountStatus;
import com.yangshan.eship.sales.entity.sale.AccountType;
import com.yangshan.eship.sales.repository.sale.AccountApplicationDao;
import com.yangshan.eship.sales.repository.sale.SalesStaffAssignDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/** 
* @author HuKai 
* @date 2017年11月29日 下午4:27:20 
* 类说明 
*/
@Service
@Transactional
public class AccountApplicationService implements AccountApplicationServiceI {

	private Logger logger = LoggerFactory.getLogger(AccountApplicationService.class);
	
	@Autowired
	private AccountApplicationDao accountApplicationDao;

	@Autowired
	private SalesStaffAssignDao salesStaffAssignDao;

	@Autowired
	private UserServiceI userService;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public DataGrid<String> commitAccountInfo(AccountApplicationDto accountApplicationDto) {
		DataGrid<String> grid = new DataGrid<String>();
		
		if (StringUtils.isNotBlank(accountApplicationDto.getId())) {
			//修改
			AccountApplication application = accountApplicationDao.findOne(accountApplicationDto.getId());
			
			if (StringUtils.isNotBlank(accountApplicationDto.getAccountStartTime())) {
				try {
					application.setAccountStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(accountApplicationDto.getAccountStartTime() + " 00:00:00"));
				} catch (ParseException e) {
					logger.error(e.getMessage(), e);
				}
			}

			if (StringUtils.isNotBlank(accountApplicationDto.getAccountEndTime())) {
				try {
					application.setAccountEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(accountApplicationDto.getAccountEndTime() + " 23:59:59"));
				} catch (ParseException e) {
					logger.error(e.getMessage(), e);
				}
			}

			if (StringUtils.isNotBlank(accountApplicationDto.getAccountType())) {
				application.setAccountType(AccountType.valueOf(accountApplicationDto.getAccountType()));
			}

			//账期天数
			if (accountApplicationDto.getPaymentDays() != 0) {
				application.setPaymentDays(accountApplicationDto.getPaymentDays());
			}
			
			if (StringUtils.isNotBlank(accountApplicationDto.getAccountStatus())) {
				application.setAccountStatus(AccountStatus.valueOf(accountApplicationDto.getAccountStatus()));
			}
			
			if (accountApplicationDto.getLineOfCredit() != null) {
				application.setLineOfCredit(accountApplicationDto.getLineOfCredit());
			}

			//出货要求
			application.setDeliveryRequirementsType(accountApplicationDto.getDeliveryRequirementsType());
			application.setRequirementsNumber(accountApplicationDto.getRequirementsNumber());

			accountApplicationDao.save(application);
			
			grid.setFlag(true);
			grid.setMsg("信息修改成功!");
		} else {
			//添加
			String customerId = accountApplicationDto.getCustomerId();
			List<AccountApplication> applications = accountApplicationDao.findByCustomerId(customerId);

			if (!applications.isEmpty()) {
				//1.判断是否含有新申请状态下的订单, 有则不让申请
				//2.判断是否和最新一条账期类型相同, 有则不让申请
				this.judgeApplyAble(applications, accountApplicationDto);
			}

			AccountApplication application = new AccountApplication();
			BeanUtils.copyProperties(accountApplicationDto, application);

			//@Author: Kevin 2018-10-17 11:43
			//@Descreption: 获取原账期并设置原账期信息
			if (!applications.isEmpty()) {
				AccountApplication originalAccountApplication = applications.get(0);
				application.setOriginalAccountType(originalAccountApplication.getAccountType());
				application.setOriginalLineOfCredit(originalAccountApplication.getLineOfCredit());
				application.setOriginalPaymentDays(originalAccountApplication.getPaymentDays());
			} else {
				//@Author: Kevin 2018-10-19 15:18
				//@Descreption: 客户没有申请过账期信息
				application.setOriginalAccountType(AccountType.NO_ACCOUNT);
				application.setOriginalLineOfCredit(DataUtils.toBigDecimal(0f));
				application.setOriginalPaymentDays(0);
			}

			if (StringUtils.isNotBlank(accountApplicationDto.getAccountStartTime())) {
				try {
					application.setAccountStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(accountApplicationDto.getAccountStartTime() + " 00:00:00"));
				} catch (ParseException e) {
					logger.error(e.getMessage(), e);
				}
			}

			if (StringUtils.isNotBlank(accountApplicationDto.getAccountEndTime())) {
				try {
					application.setAccountEndTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(accountApplicationDto.getAccountEndTime() + " 23:59:59"));
				} catch (ParseException e) {
					logger.error(e.getMessage(), e);
				}
			}

			if (StringUtils.isNotBlank(accountApplicationDto.getAccountType())) {
				application.setAccountType(AccountType.valueOf(accountApplicationDto.getAccountType()));
			}

			//账期天数
			if (accountApplicationDto.getPaymentDays() != 0) {
				application.setPaymentDays(accountApplicationDto.getPaymentDays());
			}

			//处理人
			if (StringUtils.isNotBlank(accountApplicationDto.getHandlePersonId())) {
				application.setHandlePersonId(accountApplicationDto.getHandlePersonId());
			}

			application.setApplyDate(new Date());

			application.setAccountStatus(AccountStatus.APPLYED);

			//清除驳回理由信息
			application.setTurnDownReason(null);

			//出货要求
			application.setDeliveryRequirementsType(accountApplicationDto.getDeliveryRequirementsType());
			application.setRequirementsNumber(accountApplicationDto.getRequirementsNumber());

			accountApplicationDao.save(application);

			grid.setFlag(true);
			grid.setMsg("申请信息添加成功, 请等待审核!");
		}
		
		return grid;
	}

	@Override
	public DataGrid<AccountApplicationDto> getAll(AccountApplicationDto applicationDto) {
		DataGrid<AccountApplication> grid = new DataGrid<AccountApplication>();
		DataGrid<AccountApplicationDto> dataGrid = new DataGrid<AccountApplicationDto>();
		Map<String, Object> qParams = new HashMap<>();
		StringBuilder hql = new StringBuilder(" from AccountApplication a where 1=1 ");

		/**
		* @Author: HuKai
		* @Date: 2018/1/18 16:44
		* @Description: 根据客户编号查询, 首先查出符合条件的userIds
		*/
		if (StringUtils.isNotBlank(applicationDto.getCustomerCode())) {
			if (applicationDto.getUserIds().isEmpty()) {
				return new DataGrid<AccountApplicationDto>(true, new ArrayList<AccountApplicationDto>());
			} else {
				hql.append(" and a.customerId in (:userIds)");
				List<String> userIds = QueryPlanCacheWrapper.wrapper(applicationDto.getUserIds(),"AccountApplicationService.getAll.userIds");
				qParams.put("userIds", userIds);
			}
		}


		if (StringUtils.isNotBlank(applicationDto.getAccountType())) {
			hql.append(" and a.accountType = :accountType");
			qParams.put("accountType", AccountType.valueOf(applicationDto.getAccountType()));
		}

		hql.append(" and a.organizationId = :organizationId");
		qParams.put("organizationId", applicationDto.getOrganizationId());

		if (StringUtils.isNotBlank(applicationDto.getWarehouseId())) {
			hql.append(" and a.warehouseId = :warehouseId");
			qParams.put("warehouseId", applicationDto.getWarehouseId());
		}

		if (StringUtils.isNotBlank(applicationDto.getAccountStatus())) {
			hql.append(" and a.accountStatus = :accountStatus");
			qParams.put("accountStatus", AccountStatus.valueOf(applicationDto.getAccountStatus()));
		}

		if (applicationDto.getProccesd() != null) {
			if (applicationDto.getProccesd().booleanValue()) {
				//已处理
				if (StringUtils.isNotBlank(applicationDto.getAccountStatus())) {
					hql.append(" and a.accountStatus = :accountStatus");
					qParams.put("accountStatus", AccountStatus.valueOf(applicationDto.getAccountStatus()));
				} else {
					hql.append(" and (a.accountStatus = :accountStatus1 or a.accountStatus = :accountStatus2)");
					qParams.put("accountStatus1", AccountStatus.PASS_REVIEW);
					qParams.put("accountStatus2", AccountStatus.FAILED_REVIEW);
				}
			} else {
				hql.append(" and a.accountStatus = :accountStatus");
				qParams.put("accountStatus", AccountStatus.APPLYED);
			}
		}

		if (StringUtils.isNotBlank(applicationDto.getCustomerId())) {
			hql.append(" and a.customerId = :customerId");
			qParams.put("customerId", applicationDto.getCustomerId());
		}

		if (StringUtils.isNotBlank(applicationDto.getSalesStaffId())) {
			hql.append(" and a.salesStaffId = :salesStaffId");
			qParams.put("salesStaffId", applicationDto.getSalesStaffId());
		}

		//申请时间
		if (StringUtils.isNotBlank(applicationDto.getStartCreatedDate()) && StringUtils.isNotBlank(applicationDto.getEndCreatedDate())) {
			Date startTime = null;
			Date endTime = null;

			try {
				startTime = DateUtil.parse(DateUtil.SECOND_DF, applicationDto.getStartCreatedDate());
				endTime = DateUtil.parse(DateUtil.SECOND_DF, applicationDto.getEndCreatedDate());

				hql.append(" and (a.createdDate between :startTime and :endTime)");
				qParams.put("startTime", startTime);
				qParams.put("endTime", endTime);
			} catch (ParseException e) {
				logger.error("时间转换出错");
			}
		}

		/*处理人*/
		if (StringUtils.isNotBlank(applicationDto.getHandlePersonId())) {
			hql.append(" and a.handlePersonId = :handlePersonId");
			qParams.put("handlePersonId", applicationDto.getHandlePersonId());
		}

		//按照申请日期降序排列
		PagingDto pagingDto = applicationDto.getPagingDto();
		if(pagingDto != null) {
			pagingDto.setOrderBy(new String[]{"applyDate"});
			pagingDto.setOrder(new String[]{"desc"});
		}
		PagingUtils.exeHql(entityManager, grid, hql.toString(), qParams, pagingDto,"a");

		List<AccountApplicationDto> dtos = new ArrayList<AccountApplicationDto>();
		if (!grid.getRows().isEmpty()) {
			Set<String> userIds = new HashSet<>();

			for (AccountApplication application : grid.getRows()) {
				userIds.add(application.getCustomerId());
				userIds.add(application.getSalesStaffId());
				if (StringUtils.isNotBlank(application.getHandlePersonId())) {
					userIds.add(application.getHandlePersonId());
				}
			}

			Map<String, User> userMap = userService.mapByIds(new ArrayList<String>(userIds));

			for (AccountApplication application : grid.getRows()) {
				AccountApplicationDto dto = new AccountApplicationDto();
				BeanUtils.copyProperties(application, dto);

				dto.setAccountType(application.getAccountType().name());
				dto.setAccountTypeStr(application.getAccountType().getDesc());

				dto.setAccountStatus(application.getAccountStatus().name());
				dto.setAccountStatusStr(application.getAccountStatus().getDesc());

				if (application.getOriginalAccountType() != null) {
					dto.setOriginalAccountType(application.getOriginalAccountType().getDesc());
				}

				//申请人
				if (StringUtils.isNotBlank(application.getSalesStaffId())) {
					dto.setSaleStaffName(userMap.get(application.getSalesStaffId()).getName());
				}

				//客户名称&公司简称&客户编号
				if (StringUtils.isNotBlank(application.getCustomerId())) {
					User currentUser = userMap.get(application.getCustomerId());

					if (currentUser != null) {
						dto.setCustomerName(currentUser.getName());
						dto.setSimpleCompanyName(currentUser.getSimpleCompanyName());
						dto.setCustomerCode(currentUser.getCustomerCode());
					}
				}

				if (application.getAccountStartTime() != null) {
					dto.setAccountStartTime(DateUtil.formatThird(application.getAccountStartTime()));
				}

				if (application.getAccountEndTime() != null) {
					dto.setAccountEndTime(DateUtil.formatThird(application.getAccountEndTime()));
				}

				if (application.getApplyDate() != null) {
					dto.setApplyDate(DateUtil.formatThird(application.getApplyDate()));
				}

				if (StringUtils.isNotBlank(application.getHandlePersonId())) {
					User user = userMap.get(application.getHandlePersonId());
					if(user != null) {
						dto.setHandlePersonName(user.getName());
					}
				}

				dtos.add(dto);
			}
		}

		dataGrid.setRows(dtos);
		dataGrid.setTotal(grid.getTotal());
		dataGrid.setFlag(grid.isFlag());
		return dataGrid;

	}

	@Override
	public List<AccountApplication> findByUserId(String userId) {
		return accountApplicationDao.findByCustomerId(userId);
	}

	@Override
	public DataGrid<String> deleteAccountInfo(String id) {
		DataGrid<String> grid = new DataGrid<>();

		try {
			accountApplicationDao.delete(id);
			grid.setMsg("删除成功!");
			grid.setFlag(true);
		} catch (Exception e) {
			grid.setMsg("删除失败!");
			logger.error(e.getMessage(), e);
		}
		return grid;
	}

	@Override
	public DataGrid turnDownApply(AccountApplicationDto applicationDto) {
		try {
			AccountApplication application = accountApplicationDao.findOne(applicationDto.getId());

			application.setAccountStatus(AccountStatus.FAILED_REVIEW);
			application.setTurnDownReason(applicationDto.getTurnDownReason());
			application.setHandleTime(new Date());

			accountApplicationDao.save(application);
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new EshipException(ErrorCodeConstant.TURN_DOWN_ERROR);
		}

		return new DataGrid();
	}

	@Override
	public AccountApplication applySuccess(String id, AccountStatus accountStatus) {
		AccountApplication application = new AccountApplication();
		try {
			application = accountApplicationDao.findOne(id);

			application.setAccountStatus(accountStatus);
			application.setHandleTime(new Date());

			application = accountApplicationDao.save(application);

		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			throw new EshipException(ErrorCodeConstant.OPERATE_FAIL);
		}

		return application;
	}

	@Override
	public AccountApplication findOne(String applyId) {

		return accountApplicationDao.findOne(applyId);
	}

	@Override
	public void judgeApplyAble(List<AccountApplication> applications, AccountApplicationDto applicationDto) {
		//1.判断是否含有新申请状态下的订单, 有则不让申请
		for (AccountApplication application : applications) {
			if (AccountStatus.APPLYED.equals(application.getAccountStatus())) {
				throw new EshipException(ErrorCodeConstant.APPLYED_ACCOUNT_EXIST);
			}
		}

		//2.判断是否和最新一条账期信息相同, 有则不让申请
		AccountType accountType = applications.get(0).getAccountType();

		if (AccountType.NO_ACCOUNT.equals(accountType)) {
			if (accountType.name().equals(applicationDto.getAccountType())) {
				throw new EshipException(ErrorCodeConstant.SAME_ACCOUNT_TYPE, false, accountType.getDesc());
			}
		} else {
			if (accountType.name().equals(applicationDto.getAccountType())
					&& applicationDto.getLineOfCredit().equals(applications.get(0).getLineOfCredit())
					&& (applicationDto.getPaymentDays().equals(applications.get(0).getPaymentDays()))) {
				throw new EshipException(ErrorCodeConstant.SAME_ACCOUNT_TYPE, false, accountType.getDesc());
			}
		}
	}
}
 