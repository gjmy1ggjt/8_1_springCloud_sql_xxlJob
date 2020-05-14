package com.yangshan.eship.sales.service.cust;

import org.springframework.stereotype.Service;
import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.sales.entity.cust.Agent;
import com.yangshan.eship.sales.repository.cust.AgentDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;


/**
 * Created by Hukai
 * 2018/1/2 17:06
 */
@Service
@Transactional
public class AgentService implements AgentServiceI {

    @Autowired
    private AgentDao agentDao;

    @Override
    public DataGrid<Agent> findByOrganizationId(String organizationId) {

        return new DataGrid<Agent>(true, agentDao.findByOrganizationId(organizationId));
    }

    @Override
    public Agent findById(String id) {
        return agentDao.findOne(id);
    }
}