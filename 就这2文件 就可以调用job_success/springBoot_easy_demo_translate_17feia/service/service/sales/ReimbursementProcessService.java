package com.yangshan.eship.sales.service.sales;

import com.yangshan.eship.common.bean.DataGrid;
import com.yangshan.eship.sales.dto.ReimbursementOperaRequestDto;
import com.yangshan.eship.sales.entity.sale.Reimbursement;
import com.yangshan.eship.sales.entity.sale.ReimbursementProcess;
import com.yangshan.eship.sales.repository.sale.ReimbursementProcessDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author yuchao
 * @Description 
 * @Date Administrator 2019/12/16
 * @Param 
 */
@Service
@Slf4j
@Transactional
public class ReimbursementProcessService implements ReimbursementProcessServiceI{

    @Autowired
    private ReimbursementProcessDao reimbursementProcessDao;


    @Override
    public void save(ReimbursementProcess reimbursementProcess) {
        reimbursementProcessDao.save(reimbursementProcess);
    }


}
