package com.ruoyi.aps.mapper;

import java.util.List;
import com.ruoyi.aps.domain.ApsRouteOperation;

/**
 * 工艺路线工序Mapper接口
 *
 * @author aps
 */
public interface ApsRouteOperationMapper
{
    public ApsRouteOperation selectApsRouteOperationById(Long routeOperationId);

    public List<ApsRouteOperation> selectApsRouteOperationList(ApsRouteOperation apsRouteOperation);

    public int insertApsRouteOperation(ApsRouteOperation apsRouteOperation);

    public int updateApsRouteOperation(ApsRouteOperation apsRouteOperation);

    public int deleteApsRouteOperationById(Long routeOperationId);

    public int deleteApsRouteOperationByIds(Long[] routeOperationIds);
}
