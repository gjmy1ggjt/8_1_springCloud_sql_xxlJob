package com.yangshan.eship.product.controller;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.dto.account.RoleDto;
import com.yangshan.eship.author.dto.account.UserDto;
import com.yangshan.eship.author.service.account.RoleServiceI;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.bean.PagingDto;
import com.yangshan.eship.common.json.ResponseJsonData;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.exception.EshipException;
import com.yangshan.eship.sales.controller.Version;
import com.yangshan.eship.sales.entity.sale.SalesStaffAssign;
import com.yangshan.eship.sales.entity.serv.CustomServiceAssign;
import com.yangshan.eship.sales.service.sales.DepartmentServiceI;
import com.yangshan.eship.sales.service.sales.SalesStaffAssignServiceI;
import com.yangshan.eship.sales.service.serv.CustomServiceAssignServiceI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 出发地、中转地、目的地
 *
 * @author: Kee.Li
 * @date: 2017/10/24 9:36
 */
@RestController
@RequestMapping("/" + Version.VERSION+ "/staff")
public class StaffController {

	private static Logger logger = LoggerFactory.getLogger(StaffController.class);

	@Autowired
	private UserServiceI userService;

	@Autowired
	private RoleServiceI roleService;

	@Autowired
	private CustomServiceAssignServiceI customServiceAsginService;

	@Autowired
	private SalesStaffAssignServiceI salesStaffAsginService;

	@Autowired
	private DepartmentServiceI departmentService;

	@RequestMapping("/user/save")
	@ResponseBody
	public ResponseJsonData edit(UserDto userDto) {
		ResponseJsonData jsonResult = new ResponseJsonData();
		try {

			String organizationId = SessionUtils.getOrganizationId();

			userDto.getOtherInfo().put("organizationId", organizationId);
			userDto.getOtherInfo().put("loginUserId",SessionUtils.getUserId());
			userDto.getOtherInfo().put("warehouseId",SessionUtils.getWarehouseId());
			userDto.setCreatedDate(new Date());
			userService.staffEdit(userDto);

			//修改组织关系中的姓名
			departmentService.updateDepartmentByUser(userDto);

			jsonResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			jsonResult.setFlag(false);
			if (e instanceof EshipException) {
				jsonResult.setMsg(e.getMessage());
			} else {
				jsonResult.setMsg("保存失败");
			}
		}

		return jsonResult;
	}

	@RequestMapping("/user/delete")
    @ResponseBody
    public ResponseJsonData delete(String id) {
        ResponseJsonData jsonResult = new ResponseJsonData();
		//删除组织架构关系
		departmentService.deleteDepartmentByUser(id);
        try {
            userService.delete(id);
            jsonResult.setFlag(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            jsonResult.setFlag(false);
        }

        return jsonResult;
    }
	
    @RequestMapping("/roles/Assign")
    @ResponseBody
    public ResponseJsonData Assign(@RequestBody RoleUsrDto roleUsrDto) {
        ResponseJsonData jsonResult = new ResponseJsonData();
        try {
            userService.Assign(roleUsrDto.getUsers(),roleUsrDto.getRole(),roleUsrDto.getWarehouseId(),roleUsrDto.getDestinationId());
            jsonResult.setFlag(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            jsonResult.setFlag(false);
        }

        return jsonResult;
    }
	
    @RequestMapping("/roles/unAssign")
    @ResponseBody
    public ResponseJsonData unAssign(@RequestBody RoleUsrDto roleUsrDto) {
        ResponseJsonData jsonResult = new ResponseJsonData();
        try {
            userService.unAssign(roleUsrDto.getUsers(),roleUsrDto.getRole());
            jsonResult.setFlag(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            jsonResult.setFlag(false);
        }

        return jsonResult;
    }
    
	@RequestMapping(value = "/roles")
	@ResponseBody
	public DataGrid<RoleDto> findAll(RoleDto roleDto) {
		roleDto.setPagingDto(new PagingDto());
		DataGrid<RoleDto> roleDtos = roleService.findAll(roleDto);
		roleDtos.setFlag(true);
		return roleDtos;
	}

	/**
	 * 用户管理页面多条件查询
	 */
	@RequestMapping("/user/list")
	@ResponseBody
	public DataGrid<UserDto> userList(UserDto userDto, String role) {

		DataGrid<UserDto> listResult = new DataGrid<UserDto>();// ,
																// page,pageSize,organizationId,start,end,country
		try {

			// 加入其它信息
			if (StringUtils.isNotBlank(role)) {
				userDto.getOtherInfo().put("role", role);
			}

			String organizationId = SessionUtils.getOrganizationId();

			userDto.getOtherInfo().put("organizationId", organizationId);

			userDto.setWarehouseId(SessionUtils.getWarehouseId());
			userDto.setId(SessionUtils.getUserId());

			listResult = userService.staffList(userDto);
			listResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return listResult;
	}

	/**
	 * 用户管理页面多条件查询
	 */
	@RequestMapping("/user/listByRoleCode")
	@ResponseBody
	public DataGrid<UserDto> listByRoleCode(String roleCode) {

		DataGrid<UserDto> listResult = new DataGrid<UserDto>();// ,
																// page,pageSize,organizationId,start,end,country
		try {

			// 加入其它信息
			if (StringUtils.isNotBlank(roleCode)) {

				String organizationId = SessionUtils.getOrganizationId();

				listResult = userService.listUserDtoByRoleCode(roleCode, organizationId);

			}

			listResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return listResult;
	}

	/**
	 * 查询客服经理经理下面的客服
	 */
	@RequestMapping("/customServiceAsgin/manager/{customServiceManagerId}")
	@ResponseBody
	public DataGrid<CustomServiceAssign> customServiceAsginManagerlist(CustomServiceAssign customServiceAsgin,
			PagingDto pagingDto) {

		DataGrid<CustomServiceAssign> listResult = new DataGrid<CustomServiceAssign>();// ,
		try {

			// 加入其它信息
			if (StringUtils.isNotBlank(customServiceAsgin.getCustomServiceManagerId())) {

				String organizationId = SessionUtils.getOrganizationId();
				customServiceAsgin.setOrganizationId(organizationId);
				listResult = customServiceAsginService.findByOrganizationIdAndCustomServiceManagerId(customServiceAsgin,
						pagingDto);
			}

			listResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return listResult;
	}

	@RequestMapping("/customServiceAsgin/save")
	@ResponseBody
	public DataGrid<Object> customServiceAsginSave(@RequestBody List<CustomServiceAssign> customServiceAsgin) {

		DataGrid<Object> listResult = new DataGrid<Object>();// ,
		try {

			// 加入其它信息
			if (customServiceAsgin!=null&& !customServiceAsgin.isEmpty()){
					
					String organizationId = SessionUtils.getOrganizationId();
					String userId = SessionUtils.getUserId();
					String warehouseId = SessionUtils.getWarehouseId();
					customServiceAsgin.stream().forEach(assign -> {
						assign.setWarehouseId(warehouseId);
					});
					customServiceAsginService.customServiceAsginSave(customServiceAsgin,organizationId,userId);
			}

			listResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			listResult.setFlag(false);
			if (e instanceof EshipException) {
				listResult.setMsg(e.getMessage());
			} else {
				listResult.setMsg("分配失败");
			}
		}
		return listResult;
	}


	@RequestMapping("/customServiceAsgin/listAll")
	@ResponseBody
	public DataGrid<CustomServiceAssign> customServiceAsginListAll(CustomServiceAssign customServiceAsgin) {

		DataGrid<CustomServiceAssign> listResult = new DataGrid<CustomServiceAssign>();// ,
		String organizationId = SessionUtils.getOrganizationId();
		customServiceAsgin.setOrganizationId(organizationId);
		listResult.setRows(customServiceAsginService.customServiceAsginListAll(customServiceAsgin));
		listResult.setFlag(true);
		return listResult;
	}
	
	@RequestMapping("/customServiceAsgin/del")
	@ResponseBody
	public DataGrid<CustomServiceAssign> customServiceAsginDel(@RequestBody List<String> customServiceAssginIds) {

		DataGrid<CustomServiceAssign> listResult = new DataGrid<CustomServiceAssign>();// ,
		try {

			// 加入其它信息
			if (customServiceAssginIds!=null&&customServiceAssginIds.size()>0) {
				customServiceAsginService.customServiceAsginDel(customServiceAssginIds);
			}

			listResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return listResult;
	}

	/**
	 * 查询销售经理下面的销售
	 */
	@RequestMapping("/salesStaffasgin/manager/{salesManagerId}")
	@ResponseBody
	public DataGrid<SalesStaffAssign> salesStaffAsginList(SalesStaffAssign salesStaffAsgin, PagingDto pagingDto) {

		DataGrid<SalesStaffAssign> listResult = new DataGrid<SalesStaffAssign>();// ,
		try {

			// 加入其它信息
			if (StringUtils.isNotBlank(salesStaffAsgin.getSalesManagerId())) {

				String organizationId = SessionUtils.getOrganizationId();
				salesStaffAsgin.setOrganizationId(organizationId);
				listResult = salesStaffAsginService.findByOrganizationIdAndSalesManagerId(salesStaffAsgin, pagingDto);
			}

			listResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return listResult;
	}

	@RequestMapping("/salesStaffasgin/listAll")
	@ResponseBody
	public DataGrid<SalesStaffAssign> salesStaffasginListAll(SalesStaffAssign salesStaffAsgin) {

		DataGrid<SalesStaffAssign> listResult = new DataGrid<SalesStaffAssign>();// ,
		String organizationId = SessionUtils.getOrganizationId();
		salesStaffAsgin.setOrganizationId(organizationId);
		listResult.setRows(salesStaffAsginService.customServiceAsginListAll(salesStaffAsgin));
		listResult.setFlag(true);
		return listResult;
	}
	
	@RequestMapping("/salesStaffasgin/save")
	@ResponseBody
	public DataGrid<Object> salesStaffasginSave(@RequestBody List<SalesStaffAssign> salesStaffAsgins) {

		DataGrid<Object> listResult = new DataGrid<Object>();// ,
		try {

			// 加入其它信息
			if (salesStaffAsgins!=null&&salesStaffAsgins.size()>0) {

				String organizationId = SessionUtils.getOrganizationId();
				String userId = SessionUtils.getUserId();
				salesStaffAsginService.salesStaffasginSave(salesStaffAsgins,organizationId,userId);
			}

			listResult.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return listResult;
	}

	@RequestMapping("/salesStaffasgin/del")
	@ResponseBody
	public DataGrid<Object> salesStaffasginDel(@RequestBody List<String> salesStaffAsginIds) {

		DataGrid<Object> result = new DataGrid<Object>();// ,
		try {

			// 加入其它信息
			if (salesStaffAsginIds!=null&&salesStaffAsginIds.size()>0) {

				salesStaffAsginService.salesStaffasginDel(salesStaffAsginIds);
			}

			result.setFlag(true);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return result;
	}
	
	public static class RoleUsrDto{
		private String[] users;
		private String role;
		private String warehouseId;
		private String destinationId;
		public String[] getUsers() {
			return users;
		}
		public void setUsers(String[] users) {
			this.users = users;
		}
		public String getRole() {
			return role;
		}
		public void setRole(String role) {
			this.role = role;
		}
		public String getWarehouseId() {
			return warehouseId;
		}
		public void setWarehouseId(String warehouseId) {
			this.warehouseId = warehouseId;
		}
		public String getDestinationId() {
			return destinationId;
		}
		public void setDestinationId(String destinationId) {
			this.destinationId = destinationId;
		}
	}

}
