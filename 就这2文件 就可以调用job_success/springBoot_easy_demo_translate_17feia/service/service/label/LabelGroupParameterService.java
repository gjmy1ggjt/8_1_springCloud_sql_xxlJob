package com.yangshan.eship.sales.service.label;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.yangshan.eship.sales.dto.LabelGroupType;
import com.yangshan.eship.sales.entity.label.LabelGroupParameter;
import com.yangshan.eship.sales.repository.label.LabelGroupParameterDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: LinYun
 * @Description:
 * @date: 15:16 2018/3/10
 * Modified By:
 */
@Service
@Transactional
public class LabelGroupParameterService implements LabelGroupParameterServiceI {
    @Autowired
    private LabelGroupParameterDao labelGroupParameterDao;

    @Override
    public List<LabelGroupParameter> findByGroupType(LabelGroupType groupType) {
        List<LabelGroupParameter> list = labelGroupParameterDao.findByGroupType(groupType);
        List<LabelGroupParameter> returnList = new ArrayList<>();
        if(list != null){
            for(LabelGroupParameter parameterDb : list){
                LabelGroupParameter parameter = new LabelGroupParameter();
                BeanUtils.copyProperties(parameterDb,parameter);
                returnList.add(parameter);
            }
        }
        return returnList;
    }
}