package com.ruoyi.aps.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.aps.domain.ApsSchedulePlan;

/**
 * 调度方案Service接口
 *
 * @author aps
 */
public interface IApsSchedulePlanService
{
    public ApsSchedulePlan selectApsSchedulePlanById(Long planId);

    public List<ApsSchedulePlan> selectApsSchedulePlanList(ApsSchedulePlan apsSchedulePlan);

    public int insertApsSchedulePlan(ApsSchedulePlan apsSchedulePlan);

    public int updateApsSchedulePlan(ApsSchedulePlan apsSchedulePlan);

    public int deleteApsSchedulePlanByIds(Long[] planIds, String operatorName);

    public int deleteApsSchedulePlanById(Long planId, String operatorName);

    public Map<String, Object> generateInitialSchedule(String username);

    public Map<String, Object> compareReschedulePlan(Long newPlanId);

    public Map<String, Object> confirmReschedulePlan(Long planId, String operatorName);

    public Map<String, Object> rejectReschedulePlan(Long planId, String operatorName);
}
