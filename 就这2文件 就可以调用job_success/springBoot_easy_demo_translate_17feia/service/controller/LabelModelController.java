package com.yangshan.eship.sales.controller;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.common.util.SessionUtils;
import com.yangshan.eship.common.utils.DateUtil;
import com.yangshan.eship.json.annotation.JsonFilter;
import com.yangshan.eship.product.entity.prod.Product;
import com.yangshan.eship.product.service.ProductServiceI;
import com.yangshan.eship.sales.dto.LabelGroupParameterDto;
import com.yangshan.eship.sales.dto.LabelModelDto;
import com.yangshan.eship.sales.dto.PrintTargetDto;
import com.yangshan.eship.sales.entity.label.LabelGroupParameter;
import com.yangshan.eship.sales.entity.label.LabelModel;
import com.yangshan.eship.sales.service.label.LabelGroupParameterServiceI;
import com.yangshan.eship.sales.service.label.LabelModelServiceI;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author: LinYun
 * @Description:
 * @date: 15:11 2018/3/9
 * Modified By:
 */
@RestController
@RequestMapping(Version.VERSION + "/labelModel")
public class LabelModelController {
    private static final Logger logger = LoggerFactory.getLogger(LabelModelController.class);
    @Autowired
    private LabelModelServiceI labelModelService;
    @Autowired
    private LabelGroupParameterServiceI labelGroupParameterService;
    @Autowired
    private ProductServiceI productService;

    @Value("${static.file.upload}")
    private String staticFileUpload;

    @Value("${static.file.server}")
    private String staticFileServer;

    /**
     * @author LinYun
     * @date 15:17 2018/3/9
     * @description 查询标签模板列表
     */
    @RequestMapping(method = RequestMethod.POST, value = "/list")
    @JsonFilter(type = LabelModel.class, exclude = "version,createdBy,lastUpdatedBy,lastUpdatedDate")
    public DataGrid<LabelModel> list(@RequestBody LabelModelDto labelModelDto) {

        // 从session中获取组织id
        String organizationId = SessionUtils.getOrganizationId();
        labelModelDto.setOrganizationId(organizationId);

        return labelModelService.findAll(labelModelDto);
    }

    /**
     * @author LinYun
     * @date 16:04 2018/3/9
     * @description 更新默认模板项
     */
    @RequestMapping(value = "/updateDefault", method = RequestMethod.GET)
    public DataGrid updateDefault(String id, Boolean isDefault) {
        DataGrid dataGrid = new DataGrid();
        try {
            //这里的默认是当前模板的默认值，取反则为需要改变的结果
            labelModelService.setDefault(id);

            dataGrid.setFlag(true);
            dataGrid.setMsg("更新成功");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            dataGrid.setMsg("更新异常");
        }

        return dataGrid;
    }

    /**
     * 获取模板类型枚举
     *
     * @return
     */
    @RequestMapping(value = "/getLabelGroupType", method = RequestMethod.GET)
    public Map<String, String> getLabelGroupType() {
        return labelModelService.getLabelGroupType();
    }

    /**
     * @author LinYun
     * @date 10:49 2018/3/16
     * @description 根据模板类型获取类型参数
     */
    @RequestMapping(value = "/getLabelGroupParameter", method = RequestMethod.POST)
    public DataGrid getLabelGroupParameter(@RequestBody LabelModelDto labelModelDto) {
        DataGrid dataGrid = new DataGrid();
        LabelGroupParameterDto labelGroupParameterDto = new LabelGroupParameterDto();
        List<LabelGroupParameter> labelGroupParameterList = Lists.newArrayList();
        // 查询前先清空打印目标列表，以防切换查询时数据重叠了
        labelModelDto.setPrintTargetDtos(Lists.newArrayList());
        if (StringUtils.isNotBlank(labelModelDto.getId())) {
            //修改，根据模板中的内容给对应的参数赋值
            LabelModel labelModel = labelModelService.findById(labelModelDto.getId());
            labelModelDto.setGroupType(labelModel.getGroupType());
            labelGroupParameterDto.setId(labelModel.getId());
            labelGroupParameterDto.setImgPath(labelModel.getBackgroundImageOneUrl());
            labelGroupParameterDto.setShowImgPath(staticFileServer + labelModel.getBackgroundImageOneUrl());
            labelGroupParameterDto.setModelName(labelModel.getModelName());
            labelGroupParameterDto.setOriginNo(labelModel.getOriginNo());
            if (StringUtils.isNotBlank(labelModel.getLabelTarget())) {
                // 显示绑定的是name值
                String[] targetNames = labelModel.getLabelTarget().split(",");
                String[] targetIds = labelModel.getLabelTargetIds().split(",");
                labelGroupParameterDto.setPrintTargetIds(targetIds);
                // 添加已经设置的打印对象列表到list中
                List<PrintTargetDto> printTargetDtos = Lists.newArrayList();
                for (Integer i = 0; i < targetNames.length; i++) {
                    PrintTargetDto printTargetDto = new PrintTargetDto();
                    printTargetDto.setId(targetIds[i]);
                    printTargetDto.setName(targetNames[i]);
                    printTargetDtos.add(printTargetDto);
                }
                labelModelDto.setPrintTargetDtos(printTargetDtos);
            }
            labelGroupParameterList = labelGroupParameterService.findByGroupType(labelModelDto.getGroupType());
            //region 替换模板样式
            String[] singleParameterStyles = labelModel.getStyle().split("}");
            for (String s : singleParameterStyles) {
                String[] single = s.split("\\{");
                try {
                    LabelGroupParameter labelGroupParameter = labelGroupParameterList.stream()
                            .filter(o -> StringUtils.equals(o.getParameterCode(), single[0].replace("#", "")))
                            .findFirst().orElse(null);

                    if (labelGroupParameter != null) {

                        labelGroupParameter.setParameterStyle(single[1]);

                        if (!single[1].contains("display: none;")) {
                            labelGroupParameter.setDefaultDisplay(true);
                        }
                    }

                } catch (Exception e) {
                    logger.error(single[0]);
                    logger.error(e.getMessage(), e);
                }
            }
            //endregion
        }
        //新增
        //region 根据模板类型获取类型参数以及组合样式map
        if (labelGroupParameterList == null || labelGroupParameterList.size() < 1) {
            labelGroupParameterList = labelGroupParameterService.findByGroupType(labelModelDto.getGroupType());
        }
        LabelGroupParameter labelGroupParameterWide = null;
        LabelGroupParameter labelGroupParameterHigh = null;
        for (LabelGroupParameter labelGroupParameter : labelGroupParameterList) {
            if (StringUtils.equals("canvasWide", labelGroupParameter.getParameterCode())) {
                // 获取画布宽度
                labelGroupParameterDto.setCanvasWide(labelGroupParameter.getParameterStyle());
                labelGroupParameterWide = labelGroupParameter;
            } else if (StringUtils.equals("canvasHigh", labelGroupParameter.getParameterCode())) {
                // 获取画布高度
                labelGroupParameterDto.setCanvasHigh(labelGroupParameter.getParameterStyle());
                labelGroupParameterHigh = labelGroupParameter;
            } else {
                labelGroupParameter.setParameterStyle(labelGroupParameter.getParameterStyle() + "backgroundColor: transparent;");
                // 分割样式字符串，组合成map
                String[] styleS = labelGroupParameter.getParameterStyle().split(";");
                Map<String, String> styleMap = Maps.newHashMap();
                for (String styleValues : styleS) {
                    String[] values = styleValues.split(":");
//                    if (StringUtils.equals(values[0], "color")) {
//                        values[1] = values[1].replace("#fff", "#9E2927");
//                    }
                    styleMap.put(values[0].replace("/r/n", "").replace(" ", ""), values[1].replace("/r/n", "").replace(" ", ""));
                }
                labelGroupParameter.setStyleMap(styleMap);
            }
        }
        labelGroupParameterList.remove(labelGroupParameterWide);
        labelGroupParameterList.remove(labelGroupParameterHigh);

        labelGroupParameterDto.setCanvasStyle(String.format("width: %s; height: %s;", labelGroupParameterDto.getCanvasWide(), labelGroupParameterDto.getCanvasHigh()));
        labelGroupParameterDto.setLabelGroupParameters(labelGroupParameterList);
        //endregion
        labelGroupParameterDto.setGroupType(labelModelDto.getGroupType());
        labelGroupParameterDto.setPrintTargetDtos(getPrintTargets(labelModelDto));
        dataGrid.setObj(labelGroupParameterDto);
        dataGrid.setFlag(true);
        return dataGrid;
    }

    /**
     * @author LinYun
     * @date 11:00 2018/3/16
     * @description 获取打印目标，并过滤已经选择的
     */
    @RequestMapping(value = "/getPrintTargets", method = RequestMethod.POST)
    public DataGrid<PrintTargetDto> getPrintTargetsController(@RequestBody LabelModelDto labelModelDto) {
        labelModelDto.setPrintTargetDtos(new ArrayList<>());
        List<PrintTargetDto> list = getPrintTargets(labelModelDto);
        DataGrid<PrintTargetDto> dtoDataGrid = new DataGrid<>();
        dtoDataGrid.setRows(list);
        dtoDataGrid.setFlag(true);
        return dtoDataGrid;
    }

    /**
     * @author LinYun
     * @date 11:00 2018/3/16
     * @description 获取打印目标，并过滤已经选择的
     */
    public List<PrintTargetDto> getPrintTargets(LabelModelDto labelModelDto) {
        List<PrintTargetDto> printTargetDtos = labelModelDto.getPrintTargetDtos();

        String organizationId = SessionUtils.getOrganizationId();
        // 查询已添加的模板
        labelModelDto.setOrganizationId(organizationId);
        List<LabelModel> labelModelList = labelModelService.findByGroupTypeAndOrganizationId(labelModelDto.getGroupType(), organizationId);
        // 添加当前模板类型的打印目标到list中
        List<String> targetNames = Lists.newArrayList();
        //targetNames.add(targetName);
        for (LabelModel labelModel : labelModelList) {
            if (StringUtils.isNotBlank(labelModel.getLabelTargetIds())) {
                for (String id : labelModel.getLabelTargetIds().split(",")) {
                    targetNames.add(id);
                }
            }
        }
        // 根据不同的模板类型获取不同的打印目标
        switch (labelModelDto.getGroupType()) {
            case BAG_LABEL:
                break;
            case INSIDE_LABEL:
                // 查询产品信息
                List<Product> productList = productService.findAllOnlineProduct(organizationId);
                //查询产品关联的出发地
                List<String> productIds = new ArrayList<>();
                for (Product product : productList) {
                    productIds.add(product.getId());
                }

                for (Product product : productList) {
                    if (!targetNames.contains(product.getId())) {
                        if (labelModelDto.getOriginNo() == null || "".equals(labelModelDto.getOriginNo()) || product.getOriginNo().contains(labelModelDto.getOriginNo())) {
                            PrintTargetDto printTargetDto = new PrintTargetDto();
                            printTargetDto.setId(product.getId());
                            printTargetDto.setName(product.getName());
                            printTargetDtos.add(printTargetDto);
                        }
                    }
                }
                break;
            case DELIVERY_LABEL:
                break;
            case DECLARATION_FORM:
                break;
        }
        return printTargetDtos;
    }


    /**
     * @author LinYun
     * @date 9:50 2018/3/14
     * @description 采用file.Transto 来保存上传的文件
     */
    @RequestMapping(value = "/import")
    public DataGrid importFile(@RequestParam("file") CommonsMultipartFile file) {
        DataGrid dataGrid = new DataGrid<>();

        try {
            String imgDir = "label-internal/" + SessionUtils.getOrganizationId() + "/" + DateUtil.getFilePrefix() + "/";

            //region 判断文件夹是否存在，不存在则创建
            File dir = new File(staticFileUpload + imgDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            //endregion
            // 获取文件后缀
            String suffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String imgName = UUID.randomUUID().toString().replace("-", "") + suffix;

            File newFile = new File(staticFileUpload + imgDir + imgName);
            //通过CommonsMultipartFile的方法直接写文件（注意这个时候）
            file.transferTo(newFile);

            dataGrid.setFlag(true);
            String downloadPath = "/" + imgDir + imgName;
            dataGrid.setMsg(downloadPath);

            dataGrid.setObj(staticFileServer + downloadPath);

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            dataGrid.setFlag(false);
            dataGrid.setMsg(e.toString());
        }

        return dataGrid;
    }

    /**
     * @author LinYun
     * @date 13:40 2018/3/16
     * @description 保存标签模板
     */
    @RequestMapping(value = "/labelModeSave", method = RequestMethod.POST)
    public DataGrid labelModeSave(@RequestBody LabelGroupParameterDto labelGroupParameterDto) {
        DataGrid dataGrid = new DataGrid();
        LabelModel labelModel = new LabelModel();
        if (StringUtils.isNotBlank(labelGroupParameterDto.getId())) {
            labelModel = labelModelService.findById(labelGroupParameterDto.getId());
        }
        labelModel.setOrganizationId(SessionUtils.getOrganizationId());
        labelModel.setBackgroundImageOneUrl(labelGroupParameterDto.getImgPath());
        labelModel.setGroupType(labelGroupParameterDto.getGroupType());
        labelModel.setModelName(labelGroupParameterDto.getModelName());
        labelModel.setOriginNo(labelGroupParameterDto.getOriginNo());
        List<LabelGroupParameter> labelGroupParameterList = labelGroupParameterDto.getLabelGroupParameters();
        StringBuffer styleString = new StringBuffer();
        for (LabelGroupParameter labelGroupParameter : labelGroupParameterList) {
            String displayValue = "";
            if (!labelGroupParameter.getDefaultDisplay()) {
                displayValue = "display: none;";
            }
            String parameterStyle = labelGroupParameter.getParameterStyle();
            for (String styleAttribute : labelGroupParameter.getStyleMap().keySet()) {
                if (StringUtils.equals(styleAttribute, "backgroundColor") || StringUtils.equals(styleAttribute, "display")) {
                    // 背景色和是否显示直接删除
                    parameterStyle = parameterStyle.replaceAll(String.format("(.*)%s.*?;(.*)", styleAttribute), "$1$2");
                } else {
                    //根据样式attribute替换对应的值
                    parameterStyle = parameterStyle.replaceAll(String.format("(.*%s:).*?(;.*)", styleAttribute), String.format("$1 %s$2", labelGroupParameter.getStyleMap().get(styleAttribute)));
                }
            }
            // 添加到样式字符串中，根据显示要求添加上对应的display属性
            styleString.append(String.format("#%s{%s}", labelGroupParameter.getParameterCode(), parameterStyle + displayValue));
        }
        labelModel.setStyle(styleString.toString());
        //先清空打印目标
        labelModel.setLabelTarget("");
        labelModel.setLabelTargetIds("");
        for (PrintTargetDto targetDto : labelGroupParameterDto.getPrintTargetDtos()) {
            // 遍历添加打印目标，有内容时加上","分割
            if (StringUtils.isBlank(labelModel.getLabelTargetIds())) {
                labelModel.setLabelTarget(targetDto.getName());
                labelModel.setLabelTargetIds(targetDto.getId());
            } else {
                labelModel.setLabelTarget(labelModel.getLabelTarget() + "," + targetDto.getName());
                labelModel.setLabelTargetIds(labelModel.getLabelTargetIds() + "," + targetDto.getId());
            }
        }

        labelModel = labelModelService.save(labelModel);
        if (StringUtils.isEmpty(labelGroupParameterDto.getId())) {
            labelModelService.setDefault(labelModel.getOrganizationId(), labelModel.getOriginNo(), labelModel.getId());
        }
        dataGrid.setFlag(true);
        dataGrid.setMsg("保存成功");
        return dataGrid;
    }

    /**
     * @author LinYun
     * @date 15:14 2018/3/16
     * @description 根据主键id删除一条记录
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public DataGrid delete(@PathVariable("id") String id) {
        DataGrid dataGrid = new DataGrid();
        try {
            LabelModel labelModel = labelModelService.findById(id);
            if (labelModel.getDefault()) {
                dataGrid.setFlag(false);
                dataGrid.setMsg("不能删除默认标签");
            } else if (StringUtils.isNotEmpty(labelModel.getLabelTargetIds())) {
                dataGrid.setFlag(false);
                dataGrid.setMsg("请先删除该标签关联的物流渠道!");
            } else {
                labelModelService.delete(id);
                dataGrid.setFlag(true);
                dataGrid.setMsg("删除成功");
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            dataGrid.setFlag(false);
            dataGrid.setMsg("删除异常");
        }
        return dataGrid;
    }

    /**
     * @author LinYun
     * @date 13:32 2018/3/17
     * @description 复制模板
     */
    @RequestMapping(value = "/clone/{id}", method = RequestMethod.POST)
    public DataGrid clone(@PathVariable("id") String id) {
        DataGrid dataGrid = new DataGrid();
        LabelModel labelModelDb = labelModelService.findById(id);
        LabelModel labelModel = new LabelModel();
        BeanUtils.copyProperties(labelModelDb,labelModel);
        labelModel.setId("");
        labelModel.setModelName(labelModel.getModelName() + "-clone");
        labelModel.setCreatedDate(DateTime.now().toDate());
        labelModel.setLastUpdatedDate(DateTime.now().toDate());
        labelModel.setDefault(false);
        try {
            labelModelService.save(labelModel);
            dataGrid.setFlag(true);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            dataGrid.setFlag(false);
            dataGrid.setMsg("复制失败！");
        }
        return dataGrid;
    }
}
