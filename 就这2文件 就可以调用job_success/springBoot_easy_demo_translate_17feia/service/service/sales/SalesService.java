package com.yangshan.eship.sales.service.sales;

import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.utils.EshipBeanUtils;
import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import com.yangshan.eship.sales.dto.CustomerAssignDto;
import com.yangshan.eship.sales.dto.StaffSearchRequestDto;
import com.yangshan.eship.sales.entity.cust.CustomerAssign;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.repository.cust.CustomerAssignDao;
import com.yangshan.eship.sales.repository.sale.SalesServiceDao;
import com.yangshan.eship.sales.repository.sale.SalesStaffAssignDao;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 
* @author HuKai 
* @date 2017年11月15日 下午2:50:07 
* 类说明 
*/
@Service
@Transactional
public class SalesService implements SalesServiceI {

	@Autowired
	private SalesServiceDao salesServiceDao;
	
	@PersistenceContext
    private EntityManager entityManager;

	@Autowired
	private SalesStaffAssignDao salesStaffAssignDao;

	@Autowired
	private CustomerAssignDao customerAssignDao;
	
	@Override
	public DataGrid<CustomerAssignDto> getMyCustomers(CustomerAssign customerAssign) {
		DataGrid<CustomerAssignDto> grid = new DataGrid<>();
		
		Map<String, Object> qParams = new HashMap<>();
		StringBuilder hql = new StringBuilder("from CustomerAssign c where 1=1 ");

		/*公司简称或客户编号*/
		if (customerAssign != null && StringUtils.isNotBlank(customerAssign.getSimpleCompanyName())) {
			hql.append("and (c.simpleCompanyName like :simpleCompanyName or c.customerCode like :simpleCompanyName )");
			qParams.put("simpleCompanyName", "%" + customerAssign.getSimpleCompanyName() +"%");
		}

		if (customerAssign != null && StringUtils.isNotBlank(customerAssign.getCustomerName())) {
			hql.append("and c.customerName like :customerName ");
			qParams.put("customerName", "%" + customerAssign.getCustomerName() +"%");
		}

		if (customerAssign != null && StringUtils.isNotBlank(customerAssign.getSalesStaffId())) {
			hql.append("and c.salesStaffId = :salesStaffId ");
			qParams.put("salesStaffId", customerAssign.getSalesStaffId());
		}

		if (customerAssign != null && StringUtils.isNotBlank(customerAssign.getCustomServiceStaffId())) {
			hql.append("and c.customServiceStaffId = :customServiceStaffId ");
			qParams.put("customServiceStaffId", customerAssign.getCustomServiceStaffId());
		}
		//仓库id
		if (customerAssign != null && StringUtils.isNotBlank(customerAssign.getWarehouseId())) {
			hql.append("and c.warehouseId = :warehouseId ");
			qParams.put("warehouseId", customerAssign.getWarehouseId());
		}
//		权限客户 yuchao
		if (customerAssign != null && customerAssign.getListCustomerId() != null && customerAssign.getListCustomerId().size() > 0) {
			hql.append("and c.customerId in (:listCustomerId) ");
			List<String> listCustomerId = QueryPlanCacheWrapper.wrapper(customerAssign.getListCustomerId(),"SalesService.getMyCustomers.listCustomerId");
			qParams.put("listCustomerId", listCustomerId);
		}

		if (customerAssign != null && customerAssign.getCustomerIds() != null && !customerAssign.getCustomerIds().isEmpty()) {
			hql.append("and c.customerId in (:customerIds) ");
			List<String> customerIds = QueryPlanCacheWrapper.wrapper(customerAssign.getCustomerIds(),"SalesService.getMyCustomers.customerIds");
			qParams.put("customerIds", customerIds);
		}

		hql.append("order by c.createdDate desc ");

		Query query = entityManager.createQuery(hql.toString());
		Query queryCount = entityManager.createQuery("SELECT COUNT(1) " + hql.toString());

		for (Map.Entry<String, Object> qParam : qParams.entrySet()) {
			query.setParameter(qParam.getKey(), qParam.getValue());
			queryCount.setParameter(qParam.getKey(), qParam.getValue());
		}
		
		if (customerAssign != null && customerAssign.getPagingDto() != null) {
			query.setFirstResult((customerAssign.getPagingDto().getPageNo()-1) * customerAssign.getPagingDto().getPageSize());
			query.setMaxResults(customerAssign.getPagingDto().getPageSize());
		}
		
		@SuppressWarnings("unchecked")
		List<CustomerAssign> list = query.getResultList();

		Long total = (Long) queryCount.getSingleResult();

		List<CustomerAssignDto> dtos = EshipBeanUtils.copyPojos2Dtos(list, CustomerAssignDto.class);
		
		grid.setRows(dtos);
		grid.setTotal(total);
		grid.setFlag(true);
		
		return grid;
	}

	@Override
	public SalesStaffAssign findById(String salesStaffId) {
		return salesServiceDao.findOne(salesStaffId);
	}

	@Override
	public List<SalesStaffAssign> listByIds(List<String> userIds) {
		userIds = QueryPlanCacheWrapper.wrapper(userIds,"SalesService.listByIds.userIds");
		return salesServiceDao.listByIds(userIds);
	}

	@Override
	public List<SalesStaffAssign> getByStaffName(String staffName) {
		return salesStaffAssignDao.findBySalesStaffName(staffName);
	}

	/**
	* @Author: HuKai
	* @Date: 2018/2/2 17:37
	* @Description: 查询各个销售下的客户数量, 升序排列 ==> 客户注册 分配销售使用
	*/
	@Override
	public List<Object[]> getSaleCusNumbers(List<String> saleStaffIds) {

		return salesStaffAssignDao.getSaleCusNumbers(saleStaffIds);

	}

	@Override
	public List<SalesStaffAssign> getSalesManagerSalesStatics(List<String> salesManagerIds) {
		return salesStaffAssignDao.getSalesManagerSalesStatics(salesManagerIds);
	}

	@Override
	public DataGrid<SalesStaffAssign> listSales(StaffSearchRequestDto staffSearchRequestDto) {
		return salesStaffAssignDao.listSales(staffSearchRequestDto);
	}

	@Override
	public List<SalesStaffAssign> findBySalesStaffIdIn(List<String> salesStaffIds) {
		return salesStaffAssignDao.findBySalesStaffIdIn(salesStaffIds);
	}

	@Override
	public void save(List<SalesStaffAssign> salesAssignList) {
		salesStaffAssignDao.save(salesAssignList);
	}

	@Override
	public DataGrid distributionSalesCustomer(String salesStaffId, List<String> customerIds) {
		List<CustomerAssign> customerAssignList = customerAssignDao.findByCustomerIdIn(customerIds);

		//获取客户对应销售
		List<SalesStaffAssign> salesStaffAssignList = salesStaffAssignDao.findBySalesStaffId(salesStaffId);
		String salesId = salesStaffAssignList.get(0).getSalesStaffId();
		String salesManagerId = salesStaffAssignList.get(0).getSalesManagerId();

		customerAssignList.stream().forEach(customerAssign -> {
			customerAssign.setSalesStaffId(salesId);
			customerAssign.setSalesManagerId(salesManagerId);
		});

		customerAssignDao.save(customerAssignList);

		return new DataGrid(true, "客户分配成功!");
	}

	@Override
	public DataGrid cancelDistribution(List<String> customerIds) {
		//获取客户对应销售
		List<CustomerAssign> customerAssignList = customerAssignDao.findByCustomerIdIn(customerIds);

		customerAssignList.stream().forEach(customerAssign -> {
			customerAssign.setSalesStaffId(null);
			customerAssign.setSalesManagerId(null);
		});

		customerAssignDao.save(customerAssignList);

		return new DataGrid(true, "取消分配成功!");
	}

	@Override
	public List<SalesStaffAssign> findByIdIn(List<String> ids) {
		return salesServiceDao.findByIdIn(ids);
	}

	@Override
	public void delete(List<SalesStaffAssign> salesStaffAssignList) {
		salesServiceDao.delete(salesStaffAssignList);
	}

	@Override
	public List<SalesStaffAssign> findBySalesManagerId(String salesManagerId) {
		return salesServiceDao.findBySalesManagerId(salesManagerId);
	}
}