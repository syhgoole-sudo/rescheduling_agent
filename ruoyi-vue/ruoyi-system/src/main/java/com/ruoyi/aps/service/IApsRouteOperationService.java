package com.ruoyi.aps.service;

import java.util.List;
import com.ruoyi.aps.domain.ApsRouteOperation;

/**
 * 工艺路线工序Service接口
 *
 * @author aps
 */
public interface IApsRouteOperationService
{
    public ApsRouteOperation selectApsRouteOperationById(Long routeOperationId);

    public List<ApsRouteOperation> selectApsRouteOperationList(ApsRouteOperation apsRouteOperation);

    public int insertApsRouteOperation(ApsRouteOperation apsRouteOperation);

    public int updateApsRouteOperation(ApsRouteOperation apsRouteOperation);

    public int deleteApsRouteOperationByIds(Long[] routeOperationIds);

    public int deleteApsRouteOperationById(Long routeOperationId);
}
