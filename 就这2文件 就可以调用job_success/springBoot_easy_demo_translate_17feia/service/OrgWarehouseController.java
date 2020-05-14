package com.yangshan.eship.product.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.account.OrganizationServiceI;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.sales.controller.Version;
import com.yangshan.eship.sales.entity.ware.OrgWarehouse;
import com.yangshan.eship.sales.service.ware.OrgWarehouseServiceI;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * @author: LinYun
 * @Description:
 * @Date: 11:44 2017/10/25
 * Modified By:
 */
@RestController
@RequestMapping("/" + Version.VERSION+ "/warehouse")
@Api(value = "OrgWarehouseController", tags = "分公司管理")
public class OrgWarehouseController {
    private static final Logger logger = LoggerFactory.getLogger(OrgWarehouseController.class);

    @Autowired
    private OrgWarehouseServiceI orgWarehouseService;

    @Autowired
    private RegionServiceI regionService;

    @Autowired
	private OrganizationServiceI organizationService;
    
    @RequestMapping(value = "/list")
    public DataGrid<OrgWarehouse> findCountList(PagingDto pagingDto){
    	String organizationId = SessionUtils.getOrganizationId();
    	
    	DataGrid<OrgWarehouse> dataGrid = orgWarehouseService.list(organizationId,pagingDto);
    			dataGrid.setFlag(true);
        return dataGrid;
    }

	@PostMapping(value = "/listWarehouses")
	@ApiOperation(value = "获取分公司列表", notes = "by 胡凯")
	public DataGrid<OrgWarehouse> listWarehouses(@RequestBody PagingDto pagingDto){
		String organizationId = SessionUtils.getOrganizationId();

		DataGrid<OrgWarehouse> dataGrid = orgWarehouseService.list(organizationId,pagingDto);
		dataGrid.setFlag(true);
		return dataGrid;
	}

    @PostMapping
	@ApiOperation(value = "新增或修改分公司", notes = "by 胡凯")
    public DataGrid addOrUpdate(@RequestBody OrgWarehouse orgWarehouse) {
		if (org.apache.commons.lang3.StringUtils.isNotBlank(orgWarehouse.getId())) {
			return orgWarehouseService.updateWarehouse(orgWarehouse);
		} else {
			String organizationId = SessionUtils.getOrganizationId();
			orgWarehouse.setOrganizationId(organizationId);
			return orgWarehouseService.addWarehouse(orgWarehouse);
		}
	}

    @RequestMapping(value = "/edit")
    public DataGrid<OrgWarehouse> edit(OrgWarehouse orgWarehouse){
    	
    	DataGrid<OrgWarehouse> dataGrid = new DataGrid<>();
    	if(StringUtils.hasText(orgWarehouse.getId())){
    		orgWarehouseService.update(orgWarehouse);
    	}else{
    		String organizationId = SessionUtils.getOrganizationId();
    		orgWarehouse.setOrganizationId(organizationId);
    		orgWarehouse.setCreatedDate(new Date());
    		orgWarehouseService.save(orgWarehouse);
    	}
    	dataGrid.setFlag(true);
    	
        return dataGrid;
    }
    
    @RequestMapping(value = "/delete/{id}", method = RequestMethod.POST)
    @ApiOperation(value = "删除分公司", notes = "by 胡凯")
    public DataGrid<OrgWarehouse> delete(@PathVariable("id") String id){
    	
    	DataGrid<OrgWarehouse> dataGrid = new DataGrid<>();
    	if(StringUtils.hasText(id)){
    		orgWarehouseService.delete(id);
    	}
    	dataGrid.setFlag(true);
        return dataGrid;
    }
    
    @RequestMapping(value = "/isRegion")
    public DataGrid<Object> isRegion(){
    	
    	DataGrid<Object> dataGrid = new DataGrid<>();
    	dataGrid.setObj(regionService.findOrigins());
    	dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * 针对仓库新增时候去掉当前已经存在的地址
     * @return
     */
    @RequestMapping(value = "/distinctIsRegion", method = RequestMethod.POST)
	@ApiOperation(value = "针对仓库新增时候去掉当前已经存在的地址", notes = "by Hukai. 新增时不需要传orginNo, 修改时传当前的")
	public DataGrid<Object> distinctIsRegion(@RequestBody(required = false) String orginNo) {

		DataGrid<Object> dataGrid = new DataGrid<>();
		List<Region> regions = regionService.findOrigins();

		List<OrgWarehouse> orgWarehouses = orgWarehouseService.list(SessionUtils.getOrganizationId(), null).getRows();
		List<String> orginNos = Lists.newArrayList();
		for (OrgWarehouse orgWarehouse : orgWarehouses) {
			orginNos.add(orgWarehouse.getOriginNo());
		}
		ListIterator<Region> listIterator = regions.listIterator();
		while (listIterator.hasNext()) {
			Region region = listIterator.next();
			if(StringUtils.hasText(orginNo) && region.getRegionCode().toLowerCase().trim().equalsIgnoreCase(orginNo.toLowerCase().trim())){
				continue;
			}
			if (orginNos.contains(region.getRegionCode())) {
				listIterator.remove();
			}
		}
		dataGrid.setObj(regions);
		dataGrid.setFlag(true);
		return dataGrid;
	}
    
    /**
     * 查询客服所在地区的仓库
     * @param
     * @author Kee.Li
     * @date 2017/12/26 14:51
     */
    @RequestMapping(value = "/listCsOrgWarehouses")
    public DataGrid<OrgWarehouse> findOrgWarehouse(){

        String organizationId = SessionUtils.getOrganizationId();
        String customerServiceId = SessionUtils.getUserId();

        DataGrid<OrgWarehouse> dataGrid = new DataGrid<>();
        List<OrgWarehouse> orgWarehouses = orgWarehouseService.findCustomerServiceOrgWarehouses(organizationId,customerServiceId);
        dataGrid.setTotal(orgWarehouses == null ? 0:orgWarehouses.size());
        dataGrid.setFlag(true);
        dataGrid.setRows(orgWarehouses);
        return dataGrid;
    }
}
