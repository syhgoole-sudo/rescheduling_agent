package com.ruoyi.aps.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.aps.domain.ApsScheduleTask;

/**
 * 调度任务Service接口
 *
 * @author aps
 */
public interface IApsScheduleTaskService
{
    public ApsScheduleTask selectApsScheduleTaskById(Long taskId);

    public List<ApsScheduleTask> selectApsScheduleTaskList(ApsScheduleTask apsScheduleTask);

    public List<ApsScheduleTask> selectApsScheduleTaskListByPlanId(Long planId);

    public Map<String, Object> selectApsScheduleTaskGantt(Long planId);

    public Map<String, Object> getGanttCompareData(Long newPlanId);

    public int insertApsScheduleTask(ApsScheduleTask apsScheduleTask);

    public int updateApsScheduleTask(ApsScheduleTask apsScheduleTask);

    public int deleteApsScheduleTaskByIds(Long[] taskIds, String operatorName);

    public int deleteApsScheduleTaskById(Long taskId, String operatorName);
}
