package com.ruoyi.aps.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.aps.mapper.ApsOrderMapper;
import com.ruoyi.aps.domain.ApsOrder;
import com.ruoyi.aps.service.IApsOrderService;

/**
 * 订单Service业务层处理
 *
 * @author aps
 */
@Service
public class ApsOrderServiceImpl implements IApsOrderService
{
    @Autowired
    private ApsOrderMapper apsOrderMapper;

    @Override
    public ApsOrder selectApsOrderById(Long orderId)
    {
        return apsOrderMapper.selectApsOrderById(orderId);
    }

    @Override
    public List<ApsOrder> selectApsOrderList(ApsOrder apsOrder)
    {
        return apsOrderMapper.selectApsOrderList(apsOrder);
    }

    @Override
    public int insertApsOrder(ApsOrder apsOrder)
    {
        return apsOrderMapper.insertApsOrder(apsOrder);
    }

    @Override
    public int updateApsOrder(ApsOrder apsOrder)
    {
        return apsOrderMapper.updateApsOrder(apsOrder);
    }

    @Override
    public int deleteApsOrderByIds(Long[] orderIds)
    {
        return apsOrderMapper.deleteApsOrderByIds(orderIds);
    }

    @Override
    public int deleteApsOrderById(Long orderId)
    {
        return apsOrderMapper.deleteApsOrderById(orderId);
    }
}
