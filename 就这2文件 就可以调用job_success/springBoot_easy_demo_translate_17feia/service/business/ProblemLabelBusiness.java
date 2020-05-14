package com.yangshan.eship.sales.business;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.account.User;
import com.yangshan.eship.author.entity.syst.Region;
import com.yangshan.eship.author.service.account.UserServiceI;
import com.yangshan.eship.author.service.syst.RegionServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.order.dto.labelex.ExLabelWorkingTableDto;
import com.yangshan.eship.order.dto.labelex.ProblemLabelSearchDataDto;
import com.yangshan.eship.order.service.labelex.ProblemLabelServiceI;
import com.yangshan.eship.order.service.orde.OrderServiceI;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: hyl
 * @Date: 2018/3/6 14:07
 * @Description:
 */
@Service
public class ProblemLabelBusiness {

    @Autowired
    private ProblemLabelServiceI problemLabelService;
    @Autowired
    private UserServiceI userService;
    @Autowired
    private OrderServiceI orderService;
    @Autowired
    private ProductServiceI productService;
    @Autowired
    private RegionServiceI regionService;

    /**
     * 获取问题标签列表
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:42
     * @Description:
     */
    public DataGrid<ExLabelWorkingTableDto> getProblemLabelList(ProblemLabelSearchDataDto problemLabelSearchDataDto) {

        List<String> productIds = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        //获取问题标签list，订单idsList
        DataGrid<ExLabelWorkingTableDto> exLabelWorkingTableDtoDataGrid =
                problemLabelService.getProblemLabelList(problemLabelSearchDataDto);
        //查询结果为空
        if (exLabelWorkingTableDtoDataGrid.getRows().size() == 0) {
            return exLabelWorkingTableDtoDataGrid;
        }

        List<ExLabelWorkingTableDto> exLabelWorkingTableDtoList = exLabelWorkingTableDtoDataGrid.getRows();

        //获取用户ids,产品ids
        exLabelWorkingTableDtoList.stream().forEach(exLabelWorkingTableDto -> {
            if (!StringUtils.isEmpty(exLabelWorkingTableDto.getProdProductId())) {
                productIds.add(exLabelWorkingTableDto.getProdProductId());
            } else {
                exLabelWorkingTableDto.setProductName("小包打大包");
            }
            userIds.add(exLabelWorkingTableDto.getUserId());
            //格式化下单时间
            exLabelWorkingTableDto.setCreatedDateStr(
                    format.format(exLabelWorkingTableDto.getCreatedDate())
            );
            if(exLabelWorkingTableDto.getTakeDeliveryTime() != null){
                exLabelWorkingTableDto.setTakeDeliveryTimeStr(format.format(exLabelWorkingTableDto.getTakeDeliveryTime()));
            }
            //格式化标签获取失败时间
            exLabelWorkingTableDto.setLabelCreatedDateStr((
                    format.format(exLabelWorkingTableDto.getLabelCreatedDate())
            ));
        });

        //设置产品名称
        setProductName(productIds, exLabelWorkingTableDtoList);

        //设置客户代码
        setCustomerCode(userIds, exLabelWorkingTableDtoList);

        return exLabelWorkingTableDtoDataGrid;
    }

    /**
     * 设置产品名称
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:15
     * @Description:
     */
    private void setProductName(List<String> ids, List<ExLabelWorkingTableDto> exLabelWorkingTableDtoList) {
        if(ids.isEmpty())return;
        //获取产品list
        List<Product> productList = productService.listByProductIds(ids);
        //循环产品对象List设置产品名称属性
        productList.forEach(product -> {
            exLabelWorkingTableDtoList.forEach(exLabelWorkingTableDto -> {
                //如果产品Id一致
                if (product.getId().equals(exLabelWorkingTableDto.getProdProductId())) {
                    //设置产品名称
                    exLabelWorkingTableDto.setProductName(product.getName());
                }
            });
        });
    }

    /**
     * 设置客户代码
     *
     * @Author: hyl
     * @Date: 2018/3/6 15:15
     * @Description:
     */
    private void setCustomerCode(List<String> ids, List<ExLabelWorkingTableDto> exLabelWorkingTableDtoList) {
        //获取用户list
        List<User> userList = userService.listByIds(ids);
        //循环用户对象List设置用户代码属性
        userList.forEach(user -> {
            exLabelWorkingTableDtoList.forEach(exLabelWorkingTableDto -> {
                //如果用户Id一致
                if (exLabelWorkingTableDto.getUserId().equals(user.getId())) {
                    //设置用户代码
                    exLabelWorkingTableDto.setCustomerCode(user.getCustomerCode());
                }
            });
        });
    }

    public DataGrid<Region> getCountryList() {
        DataGrid dataGrid = new DataGrid();
        List<Region> regionList = regionService.findDestinations();
        dataGrid.setRows(regionList);
        dataGrid.setFlag(regionList != null);
        return dataGrid;
    }
}
