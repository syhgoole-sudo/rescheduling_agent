package com.ruoyi.aps.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
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

    public int softDeleteApsSchedulePlanById(@Param("planId") Long planId,
            @Param("operatorName") String operatorName);

    public int countReschedulePlansBySourcePlanId(Long sourcePlanId);

    public int archiveActiveInitialPlans();
}
