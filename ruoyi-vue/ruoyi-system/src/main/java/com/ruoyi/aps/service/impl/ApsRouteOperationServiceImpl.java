package com.ruoyi.aps.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.aps.mapper.ApsRouteOperationMapper;
import com.ruoyi.aps.domain.ApsRouteOperation;
import com.ruoyi.aps.service.IApsRouteOperationService;

/**
 * 工艺路线工序Service业务层处理
 *
 * @author aps
 */
@Service
public class ApsRouteOperationServiceImpl implements IApsRouteOperationService
{
    @Autowired
    private ApsRouteOperationMapper apsRouteOperationMapper;

    @Override
    public ApsRouteOperation selectApsRouteOperationById(Long routeOperationId)
    {
        return apsRouteOperationMapper.selectApsRouteOperationById(routeOperationId);
    }

    @Override
    public List<ApsRouteOperation> selectApsRouteOperationList(ApsRouteOperation apsRouteOperation)
    {
        return apsRouteOperationMapper.selectApsRouteOperationList(apsRouteOperation);
    }

    @Override
    public int insertApsRouteOperation(ApsRouteOperation apsRouteOperation)
    {
        return apsRouteOperationMapper.insertApsRouteOperation(apsRouteOperation);
    }

    @Override
    public int updateApsRouteOperation(ApsRouteOperation apsRouteOperation)
    {
        return apsRouteOperationMapper.updateApsRouteOperation(apsRouteOperation);
    }

    @Override
    public int deleteApsRouteOperationByIds(Long[] routeOperationIds)
    {
        return apsRouteOperationMapper.deleteApsRouteOperationByIds(routeOperationIds);
    }

    @Override
    public int deleteApsRouteOperationById(Long routeOperationId)
    {
        return apsRouteOperationMapper.deleteApsRouteOperationById(routeOperationId);
    }
}
