package com.ruoyi.aps.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.aps.domain.ApsSchedulePlan;
import com.ruoyi.aps.domain.ApsScheduleTask;
import com.ruoyi.aps.mapper.ApsSchedulePlanMapper;
import com.ruoyi.aps.mapper.ApsScheduleTaskMapper;
import com.ruoyi.aps.service.IApsScheduleTaskService;
import com.ruoyi.common.utils.DateUtils;

@Service
public class ApsScheduleTaskServiceImpl implements IApsScheduleTaskService
{
    @Autowired
    private ApsScheduleTaskMapper apsScheduleTaskMapper;

    @Autowired
    private ApsSchedulePlanMapper apsSchedulePlanMapper;

    @Override
    public ApsScheduleTask selectApsScheduleTaskById(Long taskId)
    {
        return apsScheduleTaskMapper.selectApsScheduleTaskById(taskId);
    }

    @Override
    public List<ApsScheduleTask> selectApsScheduleTaskList(ApsScheduleTask apsScheduleTask)
    {
        return apsScheduleTaskMapper.selectApsScheduleTaskList(apsScheduleTask);
    }

    @Override
    public List<ApsScheduleTask> selectApsScheduleTaskListByPlanId(Long planId)
    {
        return apsScheduleTaskMapper.selectApsScheduleTaskListByPlanId(planId);
    }

    @Override
    public Map<String, Object> selectApsScheduleTaskGantt(Long planId)
    {
        List<ApsScheduleTask> tasks = selectApsScheduleTaskListByPlanId(planId);
        Map<String, Object> result = buildGanttData(planId, tasks);
        return result;
    }

    @Override
    public Map<String, Object> getGanttCompareData(Long newPlanId)
    {
        ApsSchedulePlan newPlan = apsSchedulePlanMapper.selectApsSchedulePlanById(newPlanId);
        if (newPlan == null)
        {
            throw new IllegalStateException("新方案不存在");
        }
        if (!"RESCHEDULE".equals(newPlan.getPlanType()))
        {
            throw new IllegalStateException("只能对 RESCHEDULE 方案进行甘特图对比");
        }
        if (newPlan.getSourcePlanId() == null)
        {
            throw new IllegalStateException("重调度方案缺少 source_plan_id");
        }
        ApsSchedulePlan sourcePlan = apsSchedulePlanMapper.selectApsSchedulePlanById(newPlan.getSourcePlanId());
        if (sourcePlan == null)
        {
            throw new IllegalStateException("原方案不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourcePlan", toPlanInfo(sourcePlan));
        result.put("newPlan", toPlanInfo(newPlan));
        result.put("sourceGantt", buildGanttData(sourcePlan.getPlanId(), selectApsScheduleTaskListByPlanId(sourcePlan.getPlanId())));
        result.put("newGantt", buildGanttData(newPlan.getPlanId(), selectApsScheduleTaskListByPlanId(newPlan.getPlanId())));
        return result;
    }

    private Map<String, Object> buildGanttData(Long planId, List<ApsScheduleTask> tasks)
    {
        Map<Long, Map<String, Object>> equipmentMap = new LinkedHashMap<>();
        for (ApsScheduleTask task : tasks)
        {
            Long equipmentId = task.getEquipmentId();
            Map<String, Object> equipment = equipmentMap.computeIfAbsent(equipmentId, key -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("equipmentId", task.getEquipmentId());
                item.put("equipmentCode", task.getEquipmentCode());
                item.put("equipmentName", task.getEquipmentCode());
                item.put("tasks", new ArrayList<Map<String, Object>>());
                return item;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> equipmentTasks = (List<Map<String, Object>>) equipment.get("tasks");
            equipmentTasks.add(toGanttTask(task));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", planId);
        result.put("equipments", new ArrayList<>(equipmentMap.values()));
        return result;
    }

    private Map<String, Object> toPlanInfo(ApsSchedulePlan plan)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("planId", plan.getPlanId());
        item.put("planCode", plan.getPlanCode());
        item.put("planName", plan.getPlanName());
        item.put("planType", plan.getPlanType());
        item.put("planStatus", plan.getPlanStatus());
        return item;
    }

    @Override
    public int insertApsScheduleTask(ApsScheduleTask apsScheduleTask)
    {
        return apsScheduleTaskMapper.insertApsScheduleTask(apsScheduleTask);
    }

    @Override
    public int updateApsScheduleTask(ApsScheduleTask apsScheduleTask)
    {
        return apsScheduleTaskMapper.updateApsScheduleTask(apsScheduleTask);
    }

    @Override
    public int deleteApsScheduleTaskByIds(Long[] taskIds)
    {
        return apsScheduleTaskMapper.deleteApsScheduleTaskByIds(taskIds);
    }

    @Override
    public int deleteApsScheduleTaskById(Long taskId)
    {
        return apsScheduleTaskMapper.deleteApsScheduleTaskById(taskId);
    }

    private Map<String, Object> toGanttTask(ApsScheduleTask task)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("taskId", task.getTaskId());
        item.put("sourceTaskId", task.getSourceTaskId());
        item.put("orderId", task.getOrderId());
        item.put("orderCode", task.getOrderCode());
        item.put("productCode", task.getProductCode());
        item.put("processSeq", task.getProcessSeq());
        item.put("processCode", task.getProcessCode());
        item.put("processName", task.getProcessName());
        item.put("equipmentCode", task.getEquipmentCode());
        item.put("plannedStartTime", formatDate(task.getPlannedStartTime()));
        item.put("plannedEndTime", formatDate(task.getPlannedEndTime()));
        item.put("duration", task.getDuration());
        item.put("isInserted", task.getIsInserted());
        item.put("isFrozen", task.getIsFrozen());
        item.put("isChanged", task.getIsChanged());
        return item;
    }

    private String formatDate(java.util.Date date)
    {
        return date == null ? null : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, date);
    }
}
