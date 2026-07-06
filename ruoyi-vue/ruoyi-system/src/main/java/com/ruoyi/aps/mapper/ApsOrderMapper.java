package com.ruoyi.aps.mapper;

import java.util.List;
import com.ruoyi.aps.domain.ApsOrder;

/**
 * 订单Mapper接口
 *
 * @author aps
 */
public interface ApsOrderMapper
{
    public ApsOrder selectApsOrderById(Long orderId);

    public List<ApsOrder> selectApsOrderList(ApsOrder apsOrder);

    public int insertApsOrder(ApsOrder apsOrder);

    public int updateApsOrder(ApsOrder apsOrder);

    public int deleteApsOrderById(Long orderId);

    public int deleteApsOrderByIds(Long[] orderIds);

    public int updateOrdersScheduledByIds(Long[] orderIds);
}
