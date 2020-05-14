package com.yangshan.eship.sales.business;

import org.springframework.beans.factory.annotation.Autowired;
import com.yangshan.eship.author.entity.syst.LogisticsSupplierItem;
import com.yangshan.eship.author.service.syst.LogisticsSupplierItemServiceI;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.label.BusinessType;
import com.yangshan.eship.common.label.ShipmentDeliveryAddress;
import com.yangshan.eship.common.label.ShipmentRequest;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.EshipBeanUtils;
import com.yangshan.eship.common.utils.JsonUtils;
import com.yangshan.eship.order.dto.labelex.*;
import com.yangshan.eship.order.dto.orde.AddressType;
import com.yangshan.eship.order.entity.cust.CustomerAddress;
import com.yangshan.eship.order.entity.exlabel.ExLabelWorkingTable;
import com.yangshan.eship.order.entity.orde.Goods;
import com.yangshan.eship.order.entity.orde.GoodsType;
import com.yangshan.eship.order.service.labelex.LabelHandleServiceI;
import com.yangshan.eship.order.service.labelex.PdfPrintServiceI;
import com.yangshan.eship.order.service.orde.CustomerAddressServiceI;
import com.yangshan.eship.order.service.orde.GoodsServiceI;
import com.yangshan.eship.order.service.orde.GoodsTypeServiceI;
import com.yangshan.eship.order.service.orde.OrderServiceI;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ExtendedAreaSuchargeServiceI;
import com.yangshan.eship.product.service.ProductServiceI;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @Author: hyl
 * @Date: 2018/3/7 17:58
 * @Description:
 */
@Service
public class LabelHandleBusiness {

    @Autowired
    private LabelHandleServiceI labelHandleService;
    @Autowired
    private LogisticsSupplierItemServiceI logisticsSupplierItemService;
    @Autowired
    private ProductServiceI productService;
    @Autowired
    private PdfPrintServiceI pdfPrintService;
    @Autowired
    private GoodsServiceI goodsService;
    @Autowired
    private GoodsTypeServiceI goodsTypeService;
    @Autowired
    private OrderServiceI orderService;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    CustomerAddressServiceI customerAddressService;
    @Autowired
    private ExtendedAreaSuchargeServiceI extendedAreaSuchargeService;

    @Value("${labelEx.validateUrl}")
    private String labelExValidateUrl;

    private void setDto(LabelHandleDto labelHandleDto, java.lang.Object object, ShipmentDeliveryAddress shipmentDeliveryAddress) {

        if (object.getClass().equals(ImporterAddressDto.class)) {
            ImporterAddressDto importerAddressDto = (ImporterAddressDto) object;
            String importerAddress = JsonUtils.beanToJson(importerAddressDto);
            importerAddressDto.setImporterAddress(importerAddress);
            labelHandleDto.setImporterAddressDto(importerAddressDto);
            shipmentDeliveryAddress.setImporterAddress(importerAddress);
        }
        if (object.getClass().equals(JapanShipperAddressDto.class)) {
            JapanShipperAddressDto japanShipperAddressDto = (JapanShipperAddressDto) object;
            String returnAddress = JsonUtils.beanToJson(japanShipperAddressDto);
            japanShipperAddressDto.setReturnAddress(returnAddress);
            labelHandleDto.setJapanShipperAddressDto(japanShipperAddressDto);
            shipmentDeliveryAddress.setReturnAddress(returnAddress);
        }
    }


    /**
     * 获取问题标签处理订单信息对象
     *
     * @Author: hyl
     * @Date: 2018/3/8 10:01
     * @Description:
     */
    public DataGrid<LabelHandleDto> getOrder(String insideNumber) {
        DataGrid<LabelHandleDto> dataGrid = new DataGrid<>();
        ShipmentRequest shipmentRequest = new ShipmentRequest();
        ShipmentDeliveryAddress shipmentDeliveryAddress = new ShipmentDeliveryAddress();

        //获取问题标签处理订单信息对象
        LabelHandleDto labelHandleDto = labelHandleService.getLabelHandleDto(insideNumber);

        //获取派送商信息
//        LogisticsSupplierItem logisticsSupplierItem =
//                logisticsSupplierItemService.findOne(labelHandleDto.getDeliveryCompanyId());
        LabelHandleDeliveryAddressDto labelHandleDeliveryAddressDto = labelHandleDto.getLabelHandleDeliveryAddressDto();
        labelHandleDto.setJapanShipperAddressDto(new JapanShipperAddressDto());
        labelHandleDto.setImporterAddressDto(new ImporterAddressDto());
        //日本派送商
        if (labelHandleDto.getLabelHandleDeliveryAddressDto().getCountry().equals("JAPAN")) {

            //获取日本发货人地址
            CustomerAddress customerAddress =
                    customerAddressService.findByUserIdAndAddressTypeEqualsAndJapanShipperTrue(labelHandleDto.getUserId(), AddressType.SHIPPER);
            if (customerAddress == null) {
                customerAddress = customerAddressService.findByAddressTypeEqualsAndUserIdIsNullAndJapanShipperTrue(AddressType.SHIPPER);
            }
            if (customerAddress != null) {
                JapanShipperAddressDto japanShipperAddressDto = new JapanShipperAddressDto();
                BeanUtils.copyProperties(customerAddress, japanShipperAddressDto);
                setDto(labelHandleDto, japanShipperAddressDto, shipmentDeliveryAddress);
            } else {
                labelHandleDto.setApiErrorMsg("该用户尚未设置有日本发货人地址!");
            }

            //获取所有进口商地址
            List<CustomerAddress> customerAddressList =
                    customerAddressService.findByAddressType(AddressType.IMPORTER);
            if (customerAddressList != null && !customerAddressList.isEmpty()) {
                ImporterAddressDto importerAddressDto = new ImporterAddressDto();
                customerAddress =
                        customerAddressList.get((int) (Math.random() * customerAddressList.size()));
                BeanUtils.copyProperties(customerAddress, importerAddressDto);
                setDto(labelHandleDto, importerAddressDto, shipmentDeliveryAddress);
            }

            //如果是FBA件
            if (BusinessType.FBA.equals(labelHandleDto.getBusinessType())) {

                List<CustomerAddress> customerAddressFbaList =
                        customerAddressService.findFbaByCountryCode("JP");
                //取出邮编数字
                String postCode = Pattern.compile("[^0-9]")
                        .matcher(labelHandleDeliveryAddressDto.getPostcode()).replaceAll("").trim();

                List<CustomerAddress> customerAddressFbas = new ArrayList<>();
                for (CustomerAddress customerAddressFba : customerAddressFbaList) {
                    //取出邮编数字
                    String code = Pattern.compile("[^0-9]")
                            .matcher(customerAddressFba.getPostcode()).replaceAll("").trim();

                    if (postCode.equals(code)) {
                        //设置FBA地址
                        EshipBeanUtils.copyExclude(customerAddressFba, labelHandleDeliveryAddressDto);
                        customerAddressFbas.add(customerAddressFba);
                    }
                }
                if (customerAddressFbas.isEmpty()) {
                    shipmentRequest.setErrorMessage("匹配不到对应的FBA地址，请检查邮编地址是否正确！");
                }

                //获取进口商地址
                List<CustomerAddress> fbaCustomerAddressList =
                        customerAddressService.findByUserIdAndAddressType(labelHandleDto.getUserId(), AddressType.IMPORTER);
                if (fbaCustomerAddressList != null && !fbaCustomerAddressList.isEmpty()) {
                    ImporterAddressDto importerAddressDto = new ImporterAddressDto();
                    BeanUtils.copyProperties(fbaCustomerAddressList.get(0), importerAddressDto);
                    setDto(labelHandleDto, importerAddressDto, shipmentDeliveryAddress);
                }
            }
        }

        if (StringUtils.isNotBlank(labelHandleDto.getProdProductId())) {
            //获取产品
            Product product =
                    productService.getInstruction(labelHandleDto.getProdProductId());
            //设置产品名称
            labelHandleDto.setProductName(product.getName());
            labelHandleDto.setReportPriceLimit(product.getReportPriceLimit());
        } else {
            //设置产品名称
            labelHandleDto.setProductName("小包打大包");
        }
        dataGrid.setFlag(labelHandleDto != null);
        dataGrid.setObj(labelHandleDto);

        return dataGrid;
    }


    /**
     * 更新订单信息
     *
     * @Author: hyl
     * @Date: 2018/3/9 14:42
     * @Description:
     */
    public DataGrid<LabelHandleDto> update(LabelHandleDto labelHandleDto) {
        return labelHandleService.update(labelHandleDto);
    }

    /**
     * 获取标签
     *
     * @Author: hyl
     * @Date: 2018/3/9 14:42
     * @Description:
     */

    public ExLabelWorkingTable syncGeneratePdf(String orderId, String importerAddress) {
        pdfPrintService.syncGeneratePdf(orderId, importerAddress);
        return pdfPrintService.findByOrderId(orderId);
    }

    /**
     * 更新发件人地址
     *
     * @Author: hyl
     * @Date: 2018/3/9 14:42
     * @Description:
     */
    public JapanShipperAddressDto updateJapanShipperAddress(JapanShipperAddressDto japanShipperAddressDto) {
        return customerAddressService.updateJapanAddress(japanShipperAddressDto);
    }

    public LabelHandleGoodsDto updateGoods(LabelHandleGoodsDto labelHandleGoodsDto) {
        String id = labelHandleGoodsDto.getId();
        String name = labelHandleGoodsDto.getName();
        String nameEn = labelHandleGoodsDto.getNameEn();
        Goods goods = goodsService.update(id, name, nameEn);
        GoodsType goodsType = new GoodsType();
        goodsType.setOrganizationId(SessionUtils.getOrganizationId());
        goodsType.setKeyword(name);
        goodsType.setName(name);
        goodsType.setKeywordEn(nameEn);
        goodsTypeService.addGoodsType(goodsType, false);
        BeanUtils.copyProperties(goods, labelHandleGoodsDto);
        return labelHandleGoodsDto;
    }

    public ImporterAddressDto updateImporterAddress(ImporterAddressDto importerAddressDto) {
        return customerAddressService.updateImporterAddress(importerAddressDto);
    }

    public DataGrid<Object> updateLogisticsSupplier(LabelHandleDto labelHandleDto) {
        return labelHandleService.updateLogisticsSupplier(labelHandleDto);
    }

    public Map<String, List<LogisticsSupplierItem>> remoteSurchargeCategory(String insideNumber, String postCode) {
        //获取问题标签处理订单信息对象
        LabelHandleDto labelHandleDto =
                labelHandleService.getLabelHandleDto(insideNumber);

        LabelHandleDeliveryAddressDto labelHandleDeliveryAddressDto = labelHandleDto.getLabelHandleDeliveryAddressDto();

        List<LogisticsSupplierItem> logisticsSupplierItemList =
                logisticsSupplierItemService.findByOrganizationIdAndIsAvailable(SessionUtils.getOrganizationId(), true);

        postCode = StringUtils.isNotBlank(postCode) ? StringUtils.trim(postCode) : labelHandleDeliveryAddressDto.getPostcode();

        //根据产品获取可用的物流渠道
        Map<String, List<LogisticsSupplierItem>> remoteSurchargeCategory = extendedAreaSuchargeService.remoteSurchargeCategory(logisticsSupplierItemList, labelHandleDeliveryAddressDto.getCountryCode(), postCode, labelHandleDto.getOrganizationId());

        //设置派送商信息列表
        return remoteSurchargeCategory;
    }
}
