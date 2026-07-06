package com.ruoyi.aps.service;

import java.util.List;
import com.ruoyi.aps.domain.ApsOrder;

/**
 * 订单Service接口
 *
 * @author aps
 */
public interface IApsOrderService
{
    public ApsOrder selectApsOrderById(Long orderId);

    public List<ApsOrder> selectApsOrderList(ApsOrder apsOrder);

    public int insertApsOrder(ApsOrder apsOrder);

    public int updateApsOrder(ApsOrder apsOrder);

    public int deleteApsOrderByIds(Long[] orderIds);

    public int deleteApsOrderById(Long orderId);
}
