package com.yangshan.eship.sales.service.label;

import org.springframework.stereotype.Service;
import com.yangshan.eship.sales.dto.LabelGroupType;
import com.yangshan.eship.sales.entity.label.LabelModelMap;
import com.yangshan.eship.sales.repository.label.LabelModelMapDao;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * @author: LinYun
 * @Description:
 * @date: 2018/1/30 13:57
 * Modified By:
 */
@Service
@Transactional
public class LabelModelMapService implements LabelModelMapServiceI {

    @Autowired
    private LabelModelMapDao labelModelMapDao;

    @Override
    public List<LabelModelMap> findByOrganizationIdAndGroupType(String organizationId, LabelGroupType labelGroupType) {
        return labelModelMapDao.findByOrganizationIdAndGroupType(organizationId, labelGroupType);
    }
}