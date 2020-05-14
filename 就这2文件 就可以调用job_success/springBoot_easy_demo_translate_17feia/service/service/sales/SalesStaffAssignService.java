package com.yangshan.eship.sales.service.sales;

import com.yangshan.eship.common.utils.QueryPlanCacheWrapper;
import org.springframework.stereotype.Service;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.jpa.PagingUtils;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.sales.dto.SalesStaffAssignDto;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.repository.sale.SalesStaffAssignDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SalesStaffAssignService implements SalesStaffAssignServiceI {

    @Autowired
    private SalesStaffAssignDao salesStaffAssignDao;

	@PersistenceContext
	private EntityManager entityManager;

    @Override
    public DataGrid<SalesStaffAssign> list(SalesStaffAssign salesStaffAssign) {

        return salesStaffAssignDao.list(salesStaffAssign);
    }
    
    @Override
	public DataGrid<SalesStaffAssign> findByOrganizationIdAndSalesManagerId(SalesStaffAssign salesStaffAsgin,
			PagingDto pagingDto) {
		Map<String, Object> parmasMap = new HashMap<>();
		parmasMap.put("organizationId", salesStaffAsgin.getOrganizationId());
		parmasMap.put("salesManagerId", salesStaffAsgin.getSalesManagerId());
		return PagingUtils.exeHql(entityManager,
				"from SalesStaffAssign ssa where ssa.organizationId=:organizationId and ssa.salesManagerId=:salesManagerId",
				parmasMap, pagingDto, "ssa");
	}

	@Override
	public void salesStaffasginSave(List<SalesStaffAssign> salesStaffAsgins,String organizationId,String userId) {
		for(SalesStaffAssign salesStaffAsgin : salesStaffAsgins){
			
			List<SalesStaffAssign> salesStaffAsginsDb = salesStaffAssignDao.findBySalesStaffId(salesStaffAsgin.getSalesStaffId());
			if(!salesStaffAsginsDb.isEmpty()){
				throw new EshipException(MessageFormat.format("该销售人员已经分配给了`{0}`", salesStaffAsginsDb.get(0).getSalesManagerName()));
			}
			
			salesStaffAsgin.setCreatedDate(new Date());
			
			salesStaffAsgin.setOrganizationId(organizationId);
			
			salesStaffAssignDao.save(salesStaffAsgin);
		}
	}

	@Override
	public void salesStaffasginDel(List<String> salesStaffAsginIds) {
		for(String id : salesStaffAsginIds){
			salesStaffAssignDao.delete(id);	
		}
	}

	@Override
	public List<SalesStaffAssign> customServiceAsginListAll(SalesStaffAssign salesStaffAsgin) {
		 return salesStaffAssignDao.findByOrganizationId(salesStaffAsgin.getOrganizationId());
	}

	@Override
	public List<SalesStaffAssign> findBySalesStaffId(String salesStaffId) {

		return salesStaffAssignDao.findBySalesStaffId(salesStaffId);
	}

	@Override
	public List<SalesStaffAssign> findInSaleManagerIds(List<String> saleManagerIds) {
		saleManagerIds = QueryPlanCacheWrapper.wrapper(saleManagerIds,"SalesStaffAssignService.findInSaleManagerIds.saleManagerIds");
		return salesStaffAssignDao.findInSaleManagerIds(saleManagerIds);
	}

	@Override
	public List<SalesStaffAssign> findBySalaManagerAndSalesStaffName(List<String> saleManagerIds, String saleName) {
		saleManagerIds = QueryPlanCacheWrapper.wrapper(saleManagerIds,"SalesStaffAssignService.findBySalaManagerAndSalesStaffName.saleManagerIds");
		return salesStaffAssignDao.findBySalaManagerAndSalesStaffName(saleManagerIds, saleName);
	}

	@Override
	public List<SalesStaffAssign> findBySalesStaffIdIn(List<String> saleIds) {
    	saleIds = QueryPlanCacheWrapper.wrapper(saleIds,"SalesStaffAssignService.findBySalesStaffIdIn.saleIds");
		return salesStaffAssignDao.findBySalesStaffIdIn(saleIds);
	}
}