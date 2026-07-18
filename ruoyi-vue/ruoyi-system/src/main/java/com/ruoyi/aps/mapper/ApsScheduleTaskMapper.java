package com.ruoyi.aps.mapper;

import java.util.List;
import java.util.Date;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.aps.domain.ApsScheduleTask;

/**
 * 调度任务Mapper接口
 *
 * @author aps
 */
public interface ApsScheduleTaskMapper
{
    public ApsScheduleTask selectApsScheduleTaskById(Long taskId);

    public List<ApsScheduleTask> selectApsScheduleTaskList(ApsScheduleTask apsScheduleTask);

    public List<ApsScheduleTask> selectApsScheduleTaskListByPlanId(Long planId);

    public List<ApsScheduleTask> selectAffectedTasks(@Param("planId") Long planId,
            @Param("impactWindowStart") Date impactWindowStart,
            @Param("impactWindowEnd") Date impactWindowEnd,
            @Param("equipmentGroupIds") List<Long> equipmentGroupIds);

    public int insertApsScheduleTask(ApsScheduleTask apsScheduleTask);

    public int updateApsScheduleTask(ApsScheduleTask apsScheduleTask);

    public int softDeleteApsScheduleTaskById(@Param("taskId") Long taskId,
            @Param("operatorName") String operatorName);

    public int softDeleteApsScheduleTaskByIds(@Param("taskIds") Long[] taskIds,
            @Param("operatorName") String operatorName);

    public int softDeleteApsScheduleTasksByPlanId(@Param("planId") Long planId,
            @Param("operatorName") String operatorName);
}
