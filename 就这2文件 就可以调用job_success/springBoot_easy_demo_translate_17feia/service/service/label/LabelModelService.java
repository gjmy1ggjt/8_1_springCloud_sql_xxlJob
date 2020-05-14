package com.yangshan.eship.sales.service.label;

import com.google.common.collect.Maps;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.sales.dto.LabelGroupType;
import com.yangshan.eship.sales.dto.LabelModelDto;
import com.yangshan.eship.sales.entity.label.LabelModel;
import com.yangshan.eship.sales.repository.label.LabelModelDao;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;


/**
 * @author: LinYun
 * @Description:
 * @date: 2018/1/30 13:56
 * Modified By:
 */
@Service
@Transactional
public class LabelModelService implements LabelModelServiceI {
    private static final Logger logger = LoggerFactory.getLogger(LabelModelService.class);
    @Autowired
    private LabelModelDao labelModelDao;
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public LabelModel findById(String id) {
        return labelModelDao.findOne(id);
    }

    @Override
    public List<LabelModel> findByGroupTypeAndOrganizationId(LabelGroupType labelGroupType, String organizationId) {
        return labelModelDao.findByGroupTypeAndOrganizationId(labelGroupType, organizationId);
    }

    @Override
    public DataGrid<LabelModel> findAll(LabelModelDto labelModelDto) {
        logger.debug("findAll：OrganizationId={},groupType={},modelName={}", labelModelDto.getOrganizationId(), labelModelDto.getGroupType(), labelModelDto.getModelName());
        DataGrid<LabelModel> dataGrid = new DataGrid<>();
        StringBuilder hsql = new StringBuilder(" from LabelModel lm where 1=1 ");

        Map<String, Object> hqlParam = Maps.newHashMap();
        // 所属组织
        if (StringUtils.isNotBlank(labelModelDto.getOrganizationId())) {
            hsql.append(" and lm.organizationId=:organizationId ");
            hqlParam.put("organizationId", labelModelDto.getOrganizationId());
        }
        // 模板类型
        if (labelModelDto.getGroupType() != null) {
            hsql.append("and lm.groupType = :groupType ");
            hqlParam.put("groupType", labelModelDto.getGroupType());
        }
        // 模板名称
        if (StringUtils.isNotBlank(labelModelDto.getModelName())) {
            hsql.append(" and lm.modelName like :modelName ");
            hqlParam.put("modelName", "%" + labelModelDto.getModelName() + "%");
        }

        hsql.append(" order by lm.createdDate desc ");

        Query query = entityManager.createQuery(hsql.toString());
        Query queryCount = entityManager.createQuery("SELECT COUNT(1) " + hsql.toString());

        for (Map.Entry<String, Object> qParam : hqlParam.entrySet()) {
            query.setParameter(qParam.getKey(), qParam.getValue());
            queryCount.setParameter(qParam.getKey(), qParam.getValue());
        }

        if (labelModelDto.getPagingDto() != null && labelModelDto.getPagingDto().getPageNo() != null) {
            query.setFirstResult((labelModelDto.getPagingDto().getPageNo() - 1) * labelModelDto.getPagingDto().getPageSize());
            query.setMaxResults(labelModelDto.getPagingDto().getPageSize());
        }

        List<LabelModel> list = query.getResultList();
        Long total = (Long) queryCount.getSingleResult();

        for (LabelModel labelModel : list) {
            labelModel.setGroupName(labelModel.getGroupType().getDesc());
        }

        dataGrid.setTotal(total);
        dataGrid.setRows(list);
        logger.debug("findAll：hsql={}，response.size={}", hsql.toString(), total);
        return dataGrid;
    }

    @Override
    public void updateDefault(String id, Boolean isDefault) {
        LabelModel labelModel = labelModelDao.findOne(id);
        if (isDefault) {
            //设置默认前先清除原先的默认项
            //labelModelDao.updateDefault(labelModel.getGroupType(), labelModel.getOrganizationId());
            //设置当前项为默认
            labelModel.setDefault(true);
            labelModelDao.save(labelModel);
        } else {
            //取消默认则直接取消
            labelModel.setDefault(false);
            labelModelDao.save(labelModel);
        }
    }

    @Override
    public Map<String, String> getLabelGroupType() {
        return LabelGroupType.list();
    }

    @Override
    public LabelModel save(LabelModel labelModel) {
        return labelModelDao.save(labelModel);
    }

    @Override
    public void delete(String id) {
        labelModelDao.delete(id);
    }

    @Override
    public void setDefault(String organizationId, String originNo, String id) {
        labelModelDao.updateByOODefault(organizationId, originNo);
        labelModelDao.updateByIdDefault(id);
    }

    /**
     * @Author: Yifan
     * @Date: 2019/7/24 10:22
     * @Description: 设置默认值
     */
    @Override
    public void setDefault(String id) {
        LabelModel labelModel = labelModelDao.findOne(id);
        labelModel.setDefault(!labelModel.getDefault());
        labelModelDao.save(labelModel);

        if (labelModel.getDefault()) {
            //如果设置为是，同组织ID、模块、同出发地的设为否
            labelModelDao.updateToFalse(labelModel.getGroupType(), labelModel.getOrganizationId(), labelModel.getOriginNo(), id);
        }
    }
}