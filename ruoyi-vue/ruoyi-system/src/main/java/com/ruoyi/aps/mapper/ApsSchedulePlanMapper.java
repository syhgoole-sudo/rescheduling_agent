package com.ruoyi.aps.mapper;

import java.util.List;
import com.ruoyi.aps.domain.ApsSchedulePlan;

/**
 * 调度方案Mapper接口
 *
 * @author aps
 */
public interface ApsSchedulePlanMapper
{
    public ApsSchedulePlan selectApsSchedulePlanById(Long planId);

    public List<ApsSchedulePlan> selectApsSchedulePlanList(ApsSchedulePlan apsSchedulePlan);

    public ApsSchedulePlan selectActiveInitialPlan();

    public int insertApsSchedulePlan(ApsSchedulePlan apsSchedulePlan);

    public int updateApsSchedulePlan(ApsSchedulePlan apsSchedulePlan);

    public int deleteApsSchedulePlanById(Long planId);

    public int deleteApsSchedulePlanByIds(Long[] planIds);

    public int archiveActiveInitialPlans();
}
