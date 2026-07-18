package com.ruoyi.aps.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.aps.client.PythonScheduleClient;
import com.ruoyi.aps.client.dto.EquipmentDTO;
import com.ruoyi.aps.client.dto.EquipmentGroupDTO;
import com.ruoyi.aps.client.dto.InitialScheduleRequest;
import com.ruoyi.aps.client.dto.InitialScheduleResponse;
import com.ruoyi.aps.client.dto.OrderDTO;
import com.ruoyi.aps.client.dto.RouteOperationDTO;
import com.ruoyi.aps.client.dto.ScheduleTaskDTO;
import com.ruoyi.aps.domain.ApsEquipment;
import com.ruoyi.aps.domain.ApsEquipmentGroup;
import com.ruoyi.aps.domain.ApsInsertEvent;
import com.ruoyi.aps.domain.ApsOrder;
import com.ruoyi.aps.domain.ApsRouteOperation;
import com.ruoyi.aps.domain.ApsSchedulePlan;
import com.ruoyi.aps.domain.ApsScheduleTask;
import com.ruoyi.aps.mapper.ApsEquipmentGroupMapper;
import com.ruoyi.aps.mapper.ApsEquipmentMapper;
import com.ruoyi.aps.mapper.ApsInsertEventMapper;
import com.ruoyi.aps.mapper.ApsOrderMapper;
import com.ruoyi.aps.mapper.ApsRouteOperationMapper;
import com.ruoyi.aps.mapper.ApsSchedulePlanMapper;
import com.ruoyi.aps.mapper.ApsScheduleTaskMapper;
import com.ruoyi.aps.service.IApsSchedulePlanService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;

@Service
public class ApsSchedulePlanServiceImpl implements IApsSchedulePlanService
{
    private static final String DATE_TIME_FORMAT = DateUtils.YYYY_MM_DD_HH_MM_SS;

    @Autowired
    private ApsSchedulePlanMapper apsSchedulePlanMapper;

    @Autowired
    private ApsOrderMapper apsOrderMapper;

    @Autowired
    private ApsRouteOperationMapper apsRouteOperationMapper;

    @Autowired
    private ApsEquipmentGroupMapper apsEquipmentGroupMapper;

    @Autowired
    private ApsEquipmentMapper apsEquipmentMapper;

    @Autowired
    private ApsScheduleTaskMapper apsScheduleTaskMapper;

    @Autowired
    private PythonScheduleClient pythonScheduleClient;

    @Autowired
    private ApsInsertEventMapper apsInsertEventMapper;

    @Override
    public ApsSchedulePlan selectApsSchedulePlanById(Long planId)
    {
        return apsSchedulePlanMapper.selectApsSchedulePlanById(planId);
    }

    @Override
    public List<ApsSchedulePlan> selectApsSchedulePlanList(ApsSchedulePlan apsSchedulePlan)
    {
        return apsSchedulePlanMapper.selectApsSchedulePlanList(apsSchedulePlan);
    }

    @Override
    public int insertApsSchedulePlan(ApsSchedulePlan apsSchedulePlan)
    {
        apsSchedulePlan.setDelFlag("0");
        return apsSchedulePlanMapper.insertApsSchedulePlan(apsSchedulePlan);
    }

    @Override
    public int updateApsSchedulePlan(ApsSchedulePlan apsSchedulePlan)
    {
        return apsSchedulePlanMapper.updateApsSchedulePlan(apsSchedulePlan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteApsSchedulePlanByIds(Long[] planIds, String operatorName)
    {
        if (planIds == null || planIds.length == 0)
        {
            throw new ServiceException("请选择要删除的调度方案");
        }
        for (Long planId : planIds)
        {
            validatePlanCanBeDeleted(planId);
        }

        int deletedCount = 0;
        for (Long planId : planIds)
        {
            int planRows = apsSchedulePlanMapper.softDeleteApsSchedulePlanById(planId, operatorName);
            if (planRows != 1)
            {
                throw new ServiceException("调度方案删除失败，方案可能已被删除或状态已变化");
            }
            apsScheduleTaskMapper.softDeleteApsScheduleTasksByPlanId(planId, operatorName);
            deletedCount += planRows;
        }
        return deletedCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteApsSchedulePlanById(Long planId, String operatorName)
    {
        validatePlanCanBeDeleted(planId);
        int planRows = apsSchedulePlanMapper.softDeleteApsSchedulePlanById(planId, operatorName);
        if (planRows != 1)
        {
            throw new ServiceException("调度方案删除失败，方案可能已被删除或状态已变化");
        }
        apsScheduleTaskMapper.softDeleteApsScheduleTasksByPlanId(planId, operatorName);
        return planRows;
    }

    private void validatePlanCanBeDeleted(Long planId)
    {
        if (planId == null)
        {
            throw new ServiceException("调度方案ID不能为空");
        }
        ApsSchedulePlan plan = apsSchedulePlanMapper.selectApsSchedulePlanById(planId);
        if (plan == null)
        {
            throw new ServiceException("调度方案不存在或已删除");
        }
        String planStatus = plan.getPlanStatus();
        if ("Y".equalsIgnoreCase(plan.getActiveFlag()))
        {
            throw new ServiceException("当前激活方案禁止删除");
        }
        if ("ACTIVE".equalsIgnoreCase(planStatus) || "CONFIRMED".equalsIgnoreCase(planStatus))
        {
            throw new ServiceException("ACTIVE 或 CONFIRMED 方案禁止删除");
        }
        if ("HISTORY".equalsIgnoreCase(planStatus))
        {
            throw new ServiceException("HISTORY 方案属于历史追溯记录，禁止删除");
        }
        if (plan.getEventId() != null || apsInsertEventMapper.countBySourcePlanId(planId) > 0
                || apsInsertEventMapper.countByNewPlanId(planId) > 0)
        {
            throw new ServiceException("该方案已关联插单事件，禁止删除");
        }
        if (apsSchedulePlanMapper.countReschedulePlansBySourcePlanId(planId) > 0)
        {
            throw new ServiceException("该方案已关联重调度方案，禁止删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateInitialSchedule(String username)
    {
        List<ApsOrder> orders = selectPendingNormalOrders();
        if (orders.isEmpty())
        {
            throw new IllegalStateException("没有可调度的普通 NEW 订单");
        }

        List<ApsRouteOperation> routeOperations = selectEnabledRouteOperations();
        List<ApsEquipmentGroup> equipmentGroups = selectEnabledEquipmentGroups();
        List<ApsEquipment> equipments = selectEnabledEquipments();
        if (routeOperations.isEmpty() || equipments.isEmpty())
        {
            throw new IllegalStateException("工艺路线或设备资源为空，无法生成初始调度");
        }

        Date scheduleStartTime = orders.stream()
                .map(ApsOrder::getReleaseTime)
                .filter(item -> item != null)
                .min(Date::compareTo)
                .orElse(DateUtils.getNowDate());
        InitialScheduleRequest request = buildInitialScheduleRequest(
                orders, routeOperations, equipmentGroups, equipments, scheduleStartTime);
        InitialScheduleResponse response = pythonScheduleClient.generateInitialSchedule(request);
        if (!Boolean.TRUE.equals(response.getSuccess()))
        {
            throw new IllegalStateException(response.getMessage());
        }
        List<ScheduleTaskDTO> taskSchedules = response.getTaskSchedules();
        if (taskSchedules == null || taskSchedules.isEmpty())
        {
            throw new IllegalStateException("Python 调度服务未返回任务明细");
        }

        apsSchedulePlanMapper.archiveActiveInitialPlans();

        ApsSchedulePlan plan = buildSchedulePlan(response, scheduleStartTime, username);
        apsSchedulePlanMapper.insertApsSchedulePlan(plan);

        for (ScheduleTaskDTO taskDTO : taskSchedules)
        {
            apsScheduleTaskMapper.insertApsScheduleTask(buildScheduleTask(plan.getPlanId(), taskDTO, username));
        }
        Set<Long> scheduledOrderIds = taskSchedules.stream()
                .map(ScheduleTaskDTO::getOrderId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        apsOrderMapper.updateOrdersScheduledByIds(scheduledOrderIds.toArray(new Long[0]));

        Map<String, Object> result = new HashMap<>();
        result.put("planId", plan.getPlanId());
        result.put("planCode", plan.getPlanCode());
        result.put("taskCount", taskSchedules.size());
        result.put("kpi", response.getKpi());
        result.put("warnings", response.getWarnings());
        return result;
    }

    @Override
    public Map<String, Object> compareReschedulePlan(Long newPlanId)
    {
        ApsSchedulePlan newPlan = apsSchedulePlanMapper.selectApsSchedulePlanById(newPlanId);
        if (newPlan == null)
        {
            throw new IllegalStateException("重调度方案不存在");
        }
        if (!"RESCHEDULE".equals(newPlan.getPlanType()))
        {
            throw new IllegalStateException("只能对 RESCHEDULE 方案进行对比");
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

        List<ApsScheduleTask> originalTasks = activeTasks(
                apsScheduleTaskMapper.selectApsScheduleTaskListByPlanId(sourcePlan.getPlanId()));
        List<ApsScheduleTask> newTasks = activeTasks(
                apsScheduleTaskMapper.selectApsScheduleTaskListByPlanId(newPlan.getPlanId()));
        Map<String, Object> originalKpi = calculatePlanKpi(originalTasks);
        Map<String, Object> newKpi = calculatePlanKpi(newTasks);
        Map<Long, Date> originalFinishByOrder = calculateOrderFinishTimes(originalTasks);
        Map<Long, Date> newFinishByOrder = calculateOrderFinishTimes(newTasks);

        ApsInsertEvent event = newPlan.getEventId() == null ? null : apsInsertEventMapper.selectApsInsertEventById(newPlan.getEventId());
        ApsOrder insertOrder = event == null || event.getInsertOrderId() == null ? null : apsOrderMapper.selectApsOrderById(event.getInsertOrderId());
        Date insertFinishTime = insertOrder == null ? null : newFinishByOrder.get(insertOrder.getOrderId());
        long insertOrderTrueDelayMinutes = insertOrder == null || insertFinishTime == null
                ? 0L : Math.max(0L, minutesBetween(insertOrder.getDueTime(), insertFinishTime));

        long originalTrueDelayOrderCount = getLong(originalKpi, "delayOrderCount");
        long newTrueDelayOrderCount = getLong(newKpi, "delayOrderCount");
        long originalTrueTotalDelayMinutes = getLong(originalKpi, "totalDelayMinutes");
        long newTrueTotalDelayMinutes = getLong(newKpi, "totalDelayMinutes");
        long originalTrueMaxDelayMinutes = getLong(originalKpi, "maxDelayMinutes");
        long newTrueMaxDelayMinutes = getLong(newKpi, "maxDelayMinutes");
        Date originalMakespan = (Date) originalKpi.get("makespan");
        Date newMakespan = (Date) newKpi.get("makespan");

        long changedTaskCount = newTasks.stream().filter(task -> "Y".equals(task.getIsChanged())).count();
        long insertedTaskCount = newTasks.stream().filter(task -> "Y".equals(task.getIsInserted())).count();
        long frozenTaskCount = newTasks.stream().filter(task -> "Y".equals(task.getIsFrozen())).count();
        double changedTaskRatio = newTasks.isEmpty() ? 0D : round4((double) changedTaskCount / newTasks.size());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("originalTaskCount", originalTasks.size());
        summary.put("newTaskCount", newTasks.size());
        summary.put("changedTaskCount", changedTaskCount);
        summary.put("changedTaskRatio", changedTaskRatio);
        summary.put("insertedTaskCount", insertedTaskCount);
        summary.put("frozenTaskCount", frozenTaskCount);

        Map<String, Object> trueDelayCompare = new LinkedHashMap<>();
        trueDelayCompare.put("originalTrueDelayOrderCount", originalTrueDelayOrderCount);
        trueDelayCompare.put("newTrueDelayOrderCount", newTrueDelayOrderCount);
        trueDelayCompare.put("trueDelayOrderCountDiff", newTrueDelayOrderCount - originalTrueDelayOrderCount);
        trueDelayCompare.put("originalTrueTotalDelayMinutes", originalTrueTotalDelayMinutes);
        trueDelayCompare.put("newTrueTotalDelayMinutes", newTrueTotalDelayMinutes);
        trueDelayCompare.put("trueTotalDelayMinutesDiff", newTrueTotalDelayMinutes - originalTrueTotalDelayMinutes);
        trueDelayCompare.put("originalTrueMaxDelayMinutes", originalTrueMaxDelayMinutes);
        trueDelayCompare.put("newTrueMaxDelayMinutes", newTrueMaxDelayMinutes);
        trueDelayCompare.put("trueMaxDelayMinutesDiff", newTrueMaxDelayMinutes - originalTrueMaxDelayMinutes);
        trueDelayCompare.put("insertOrderTrueDelayMinutes", insertOrderTrueDelayMinutes);

        Map<String, Object> stabilityCompare = calculateStabilityCompare(originalFinishByOrder, newFinishByOrder);

        Map<String, Object> delayCompare = new LinkedHashMap<>();
        delayCompare.put("originalDelayOrderCount", originalTrueDelayOrderCount);
        delayCompare.put("newDelayOrderCount", newTrueDelayOrderCount);
        delayCompare.put("delayOrderCountDiff", newTrueDelayOrderCount - originalTrueDelayOrderCount);
        delayCompare.put("originalTotalDelayMinutes", originalTrueTotalDelayMinutes);
        delayCompare.put("newTotalDelayMinutes", newTrueTotalDelayMinutes);
        delayCompare.put("totalDelayMinutesDiff", newTrueTotalDelayMinutes - originalTrueTotalDelayMinutes);
        delayCompare.put("originalMaxDelayMinutes", originalTrueMaxDelayMinutes);
        delayCompare.put("newMaxDelayMinutes", newTrueMaxDelayMinutes);
        delayCompare.put("maxDelayMinutesDiff", newTrueMaxDelayMinutes - originalTrueMaxDelayMinutes);

        Map<String, Object> makespanCompare = new LinkedHashMap<>();
        makespanCompare.put("originalMakespan", formatDate(originalMakespan));
        makespanCompare.put("newMakespan", formatDate(newMakespan));
        makespanCompare.put("makespanDiffMinutes", originalMakespan == null || newMakespan == null ? 0L : minutesBetween(originalMakespan, newMakespan));

        Map<String, Object> insertOrderCompare = new LinkedHashMap<>();
        insertOrderCompare.put("insertOrderFinishTime", formatDate(insertFinishTime));
        insertOrderCompare.put("insertOrderDelayMinutes", insertOrderTrueDelayMinutes);
        insertOrderCompare.put("insertOrderTrueDelayMinutes", insertOrderTrueDelayMinutes);

        List<Map<String, Object>> changedTaskDetails = buildChangedTaskDetails(originalTasks, newTasks);
        List<Map<String, Object>> affectedOrderDetails = buildAffectedOrderDetails(
                originalTasks, newTasks, originalFinishByOrder, newFinishByOrder,
                changedTaskDetails, insertOrder == null ? null : insertOrder.getOrderId());
        Map<String, Object> decisionEvidence = buildDecisionEvidence(
                affectedOrderDetails, changedTaskDetails, insertFinishTime,
                insertOrderTrueDelayMinutes, newTrueDelayOrderCount - originalTrueDelayOrderCount,
                changedTaskCount, changedTaskRatio, stabilityCompare);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourcePlanId", sourcePlan.getPlanId());
        result.put("newPlanId", newPlan.getPlanId());
        result.put("sourcePlanCode", sourcePlan.getPlanCode());
        result.put("newPlanCode", newPlan.getPlanCode());
        result.put("eventId", newPlan.getEventId());
        result.put("insertOrderCode", insertOrder == null ? null : insertOrder.getOrderCode());
        result.put("summary", summary);
        result.put("trueDelayCompare", trueDelayCompare);
        result.put("stabilityCompare", stabilityCompare);
        result.put("delayCompare", delayCompare);
        result.put("makespanCompare", makespanCompare);
        result.put("insertOrderCompare", insertOrderCompare);
        result.put("affectedOrderDetails", affectedOrderDetails);
        result.put("changedTaskDetails", changedTaskDetails);
        result.put("decisionEvidence", decisionEvidence);
        result.put("conclusion", buildCompareConclusion(insertOrderTrueDelayMinutes,
                newTrueDelayOrderCount - originalTrueDelayOrderCount,
                newTrueTotalDelayMinutes - originalTrueTotalDelayMinutes,
                getLong(stabilityCompare, "stabilityDelayOrderCount"),
                getLong(stabilityCompare, "stabilityTotalDelayMinutes"),
                (Long) makespanCompare.get("makespanDiffMinutes")));
        return result;
    }

    private List<Map<String, Object>> buildChangedTaskDetails(List<ApsScheduleTask> originalTasks,
            List<ApsScheduleTask> newTasks)
    {
        Map<Long, ApsScheduleTask> originalByTaskId = originalTasks.stream()
                .filter(task -> task.getTaskId() != null)
                .collect(Collectors.toMap(ApsScheduleTask::getTaskId, task -> task, (left, right) -> left));
        Map<String, ApsScheduleTask> originalByOrderProcess = originalTasks.stream()
                .filter(task -> taskMatchKey(task) != null)
                .collect(Collectors.toMap(this::taskMatchKey, task -> task, (left, right) -> left));

        List<Map<String, Object>> details = new ArrayList<>();
        for (ApsScheduleTask newTask : newTasks)
        {
            ApsScheduleTask originalTask = newTask.getSourceTaskId() == null
                    ? null : originalByTaskId.get(newTask.getSourceTaskId());
            if (originalTask == null && taskMatchKey(newTask) != null)
            {
                originalTask = originalByOrderProcess.get(taskMatchKey(newTask));
            }

            boolean inserted = "Y".equalsIgnoreCase(newTask.getIsInserted()) || originalTask == null;
            boolean timeChanged = !inserted && (!Objects.equals(originalTask.getPlannedStartTime(), newTask.getPlannedStartTime())
                    || !Objects.equals(originalTask.getPlannedEndTime(), newTask.getPlannedEndTime()));
            boolean equipmentChanged = !inserted
                    && !Objects.equals(originalTask.getEquipmentId(), newTask.getEquipmentId());
            boolean frozen = "Y".equalsIgnoreCase(newTask.getIsFrozen());
            boolean flaggedChanged = "Y".equalsIgnoreCase(newTask.getIsChanged());
            if (!inserted && !timeChanged && !equipmentChanged && !frozen && !flaggedChanged)
            {
                continue;
            }

            String changeType = resolveTaskChangeType(inserted, timeChanged, equipmentChanged, frozen);
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("taskId", newTask.getTaskId());
            detail.put("sourceTaskId", newTask.getSourceTaskId());
            detail.put("orderId", newTask.getOrderId());
            detail.put("orderCode", newTask.getOrderCode());
            detail.put("processSeq", newTask.getProcessSeq());
            detail.put("processCode", newTask.getProcessCode());
            detail.put("equipmentGroupId", newTask.getEquipmentGroupId());
            detail.put("originalEquipmentGroupId", originalTask == null ? null : originalTask.getEquipmentGroupId());
            detail.put("equipmentId", newTask.getEquipmentId());
            detail.put("equipmentCode", newTask.getEquipmentCode());
            detail.put("originalEquipmentId", originalTask == null ? null : originalTask.getEquipmentId());
            detail.put("originalEquipmentCode", originalTask == null ? null : originalTask.getEquipmentCode());
            detail.put("originalStartTime", formatDate(originalTask == null ? null : originalTask.getPlannedStartTime()));
            detail.put("originalEndTime", formatDate(originalTask == null ? null : originalTask.getPlannedEndTime()));
            detail.put("newStartTime", formatDate(newTask.getPlannedStartTime()));
            detail.put("newEndTime", formatDate(newTask.getPlannedEndTime()));
            detail.put("startShiftMinutes", originalTask == null ? null
                    : minutesBetween(originalTask.getPlannedStartTime(), newTask.getPlannedStartTime()));
            detail.put("endShiftMinutes", originalTask == null ? null
                    : minutesBetween(originalTask.getPlannedEndTime(), newTask.getPlannedEndTime()));
            detail.put("isInserted", inserted ? "Y" : valueOrDefault(newTask.getIsInserted(), "N"));
            detail.put("isChanged", valueOrDefault(newTask.getIsChanged(), "N"));
            detail.put("isFrozen", valueOrDefault(newTask.getIsFrozen(), "N"));
            detail.put("changeType", changeType);
            details.add(detail);
        }
        details.sort(Comparator
                .comparing((Map<String, Object> item) -> changeTypeRank((String) item.get("changeType")))
                .thenComparing(item -> (String) item.get("orderCode"), Comparator.nullsLast(String::compareTo))
                .thenComparing(item -> (Integer) item.get("processSeq"), Comparator.nullsLast(Integer::compareTo)));
        return details;
    }

    private List<Map<String, Object>> buildAffectedOrderDetails(List<ApsScheduleTask> originalTasks,
            List<ApsScheduleTask> newTasks, Map<Long, Date> originalFinishByOrder,
            Map<Long, Date> newFinishByOrder, List<Map<String, Object>> changedTaskDetails,
            Long insertOrderId)
    {
        Map<Long, List<Map<String, Object>>> detailByOrder = changedTaskDetails.stream()
                .filter(detail -> detail.get("orderId") != null)
                .collect(Collectors.groupingBy(detail -> (Long) detail.get("orderId")));
        Map<Long, ApsScheduleTask> originalTaskByOrder = firstTaskByOrder(originalTasks);
        Map<Long, ApsScheduleTask> newTaskByOrder = firstTaskByOrder(newTasks);
        Set<Long> orderIds = new LinkedHashSet<>();
        orderIds.addAll(newFinishByOrder.keySet());
        orderIds.addAll(detailByOrder.keySet());

        List<Map<String, Object>> details = new ArrayList<>();
        for (Long orderId : orderIds)
        {
            ApsOrder order = apsOrderMapper.selectApsOrderById(orderId);
            Date originalFinishTime = originalFinishByOrder.get(orderId);
            Date newFinishTime = newFinishByOrder.get(orderId);
            List<Map<String, Object>> taskDetails = detailByOrder.getOrDefault(orderId, new ArrayList<>());
            long insertedTaskCount = taskDetails.stream()
                    .filter(detail -> "Y".equals(detail.get("isInserted"))).count();
            long frozenTaskCount = newTasks.stream()
                    .filter(task -> orderId.equals(task.getOrderId()) && "Y".equalsIgnoreCase(task.getIsFrozen())).count();
            long changedTaskCount = taskDetails.stream()
                    .filter(detail -> !"Y".equals(detail.get("isInserted")) && isActualTaskImpact(detail)).count();
            boolean insertOrder = orderId.equals(insertOrderId)
                    || (order != null && "INSERT".equalsIgnoreCase(order.getOrderType()));

            String impactType;
            if (insertOrder)
            {
                impactType = "INSERTED";
            }
            else if (originalFinishTime != null && newFinishTime != null && newFinishTime.after(originalFinishTime))
            {
                impactType = "DELAYED";
            }
            else if (originalFinishTime != null && newFinishTime != null && newFinishTime.before(originalFinishTime))
            {
                impactType = "ADVANCED";
            }
            else if (changedTaskCount > 0 || insertedTaskCount > 0)
            {
                impactType = "CHANGED";
            }
            else
            {
                impactType = "UNCHANGED";
            }
            if ("UNCHANGED".equals(impactType))
            {
                continue;
            }

            ApsScheduleTask displayTask = newTaskByOrder.getOrDefault(orderId, originalTaskByOrder.get(orderId));
            Date dueTime = order == null ? null : order.getDueTime();
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("orderId", orderId);
            detail.put("orderCode", order == null
                    ? (displayTask == null ? null : displayTask.getOrderCode()) : order.getOrderCode());
            detail.put("priorityLevel", order == null ? null : order.getPriorityLevel());
            detail.put("orderType", order == null ? null : order.getOrderType());
            detail.put("dueTime", formatDate(dueTime));
            detail.put("originalFinishTime", formatDate(originalFinishTime));
            detail.put("newFinishTime", formatDate(newFinishTime));
            detail.put("trueDelayMinutes", dueTime == null || newFinishTime == null
                    ? 0L : Math.max(0L, minutesBetween(dueTime, newFinishTime)));
            detail.put("stabilityDelayMinutes", originalFinishTime == null || newFinishTime == null
                    ? 0L : Math.max(0L, minutesBetween(originalFinishTime, newFinishTime)));
            detail.put("changedTaskCount", changedTaskCount);
            detail.put("insertedTaskCount", insertedTaskCount);
            detail.put("frozenTaskCount", frozenTaskCount);
            detail.put("impactType", impactType);
            details.add(detail);
        }
        details.sort(Comparator
                .comparing((Map<String, Object> item) -> impactTypeRank((String) item.get("impactType")))
                .thenComparing(item -> (String) item.get("orderCode"), Comparator.nullsLast(String::compareTo)));
        return details;
    }

    private Map<String, Object> buildDecisionEvidence(List<Map<String, Object>> affectedOrderDetails,
            List<Map<String, Object>> changedTaskDetails, Date insertFinishTime,
            long insertOrderTrueDelayMinutes, long trueDelayOrderCountDiff,
            long changedTaskCount, double changedTaskRatio, Map<String, Object> stabilityCompare)
    {
        long delayedOrderCount = affectedOrderDetails.stream()
                .filter(detail -> "DELAYED".equals(detail.get("impactType"))).count();
        long advancedOrderCount = affectedOrderDetails.stream()
                .filter(detail -> "ADVANCED".equals(detail.get("impactType"))).count();
        Set<Long> equipmentGroupIds = new LinkedHashSet<>();
        for (Map<String, Object> detail : changedTaskDetails)
        {
            if (!isActualTaskImpact(detail))
            {
                continue;
            }
            addLong(equipmentGroupIds, detail.get("equipmentGroupId"));
            addLong(equipmentGroupIds, detail.get("originalEquipmentGroupId"));
        }
        List<Map<String, Object>> affectedEquipmentGroups = equipmentGroupIds.stream()
                .map(this::toEquipmentGroupEvidence)
                .collect(Collectors.toList());

        long maxStabilityDelayMinutes = getLong(stabilityCompare, "stabilityMaxDelayMinutes");
        long totalStabilityDelayMinutes = getLong(stabilityCompare, "stabilityTotalDelayMinutes");
        boolean insertOrderOnTime = insertFinishTime != null && insertOrderTrueDelayMinutes <= 0L;

        // These thresholds are an explainable operator aid, not an automatic confirmation rule.
        String recommendationLevel;
        String recommendationReason;
        if (insertFinishTime == null)
        {
            recommendationLevel = "CAUTION";
            recommendationReason = "未识别到插单完工时间，需要人工核验任务完整性。";
        }
        else if (insertOrderTrueDelayMinutes > 0L)
        {
            recommendationLevel = "NOT_RECOMMENDED";
            recommendationReason = "插单未按真实交期完成。";
        }
        else if (trueDelayOrderCountDiff <= 0L && changedTaskRatio <= 0.20D
                && maxStabilityDelayMinutes <= 60L)
        {
            recommendationLevel = "RECOMMENDED";
            recommendationReason = "插单按期完成，未新增真实延期订单，且计划扰动处于辅助阈值内。";
        }
        else
        {
            recommendationLevel = "CAUTION";
            recommendationReason = "插单按期完成，但存在新增真实延期或较明显的计划扰动。";
        }

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("insertOrderOnTime", insertOrderOnTime);
        evidence.put("insertOrderTrueDelayMinutes", insertOrderTrueDelayMinutes);
        evidence.put("affectedOrderCount", affectedOrderDetails.size());
        evidence.put("delayedOrderCount", delayedOrderCount);
        evidence.put("advancedOrderCount", advancedOrderCount);
        evidence.put("changedTaskCount", changedTaskCount);
        evidence.put("changedTaskRatio", changedTaskRatio);
        evidence.put("maxStabilityDelayMinutes", maxStabilityDelayMinutes);
        evidence.put("totalStabilityDelayMinutes", totalStabilityDelayMinutes);
        evidence.put("affectedEquipmentGroupCount", affectedEquipmentGroups.size());
        evidence.put("affectedEquipmentGroups", affectedEquipmentGroups);
        evidence.put("recommendationLevel", recommendationLevel);
        evidence.put("recommendationReason", recommendationReason);
        evidence.put("recommendationRule",
                "插单延期则 NOT_RECOMMENDED；插单按期、无新增真实延期、变更比例不超过20%且最大后移不超过60分钟则 RECOMMENDED；其余为 CAUTION。");
        evidence.put("advisoryOnly", true);
        return evidence;
    }

    private Map<Long, ApsScheduleTask> firstTaskByOrder(List<ApsScheduleTask> tasks)
    {
        return tasks.stream()
                .filter(task -> task.getOrderId() != null)
                .collect(Collectors.toMap(ApsScheduleTask::getOrderId, task -> task, (left, right) -> left));
    }

    private List<ApsScheduleTask> activeTasks(List<ApsScheduleTask> tasks)
    {
        if (tasks == null)
        {
            return new ArrayList<>();
        }
        return tasks.stream()
                .filter(task -> task.getDelFlag() == null || "0".equals(task.getDelFlag()))
                .collect(Collectors.toList());
    }

    private String taskMatchKey(ApsScheduleTask task)
    {
        return task.getOrderId() == null || task.getProcessSeq() == null
                ? null : task.getOrderId() + "#" + task.getProcessSeq();
    }

    private String resolveTaskChangeType(boolean inserted, boolean timeChanged,
            boolean equipmentChanged, boolean frozen)
    {
        if (inserted)
        {
            return "INSERTED";
        }
        if (timeChanged && equipmentChanged)
        {
            return "TIME_AND_EQUIPMENT_CHANGED";
        }
        if (equipmentChanged)
        {
            return "EQUIPMENT_CHANGED";
        }
        if (timeChanged)
        {
            return "TIME_SHIFTED";
        }
        return frozen ? "FROZEN" : "UNCHANGED";
    }

    private boolean isActualTaskImpact(Map<String, Object> detail)
    {
        String changeType = (String) detail.get("changeType");
        return "Y".equals(detail.get("isInserted")) || "Y".equals(detail.get("isChanged"))
                || "TIME_SHIFTED".equals(changeType) || "EQUIPMENT_CHANGED".equals(changeType)
                || "TIME_AND_EQUIPMENT_CHANGED".equals(changeType) || "INSERTED".equals(changeType);
    }

    private int changeTypeRank(String changeType)
    {
        if ("INSERTED".equals(changeType))
        {
            return 0;
        }
        if ("TIME_AND_EQUIPMENT_CHANGED".equals(changeType) || "EQUIPMENT_CHANGED".equals(changeType))
        {
            return 1;
        }
        if ("TIME_SHIFTED".equals(changeType))
        {
            return 2;
        }
        return "FROZEN".equals(changeType) ? 3 : 4;
    }

    private int impactTypeRank(String impactType)
    {
        if ("INSERTED".equals(impactType))
        {
            return 0;
        }
        if ("DELAYED".equals(impactType))
        {
            return 1;
        }
        if ("ADVANCED".equals(impactType))
        {
            return 2;
        }
        return 3;
    }

    private Map<String, Object> toEquipmentGroupEvidence(Long equipmentGroupId)
    {
        ApsEquipmentGroup group = apsEquipmentGroupMapper.selectApsEquipmentGroupById(equipmentGroupId);
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("equipmentGroupId", equipmentGroupId);
        item.put("equipmentGroupCode", group == null ? null : group.getEquipmentGroupCode());
        item.put("equipmentGroupName", group == null ? null : group.getEquipmentGroupName());
        return item;
    }

    private void addLong(Set<Long> values, Object value)
    {
        if (value instanceof Number)
        {
            values.add(((Number) value).longValue());
        }
    }

    private String valueOrDefault(String value, String defaultValue)
    {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmReschedulePlan(Long planId, String operatorName)
    {
        ApsSchedulePlan plan = validateReschedulePlan(planId);
        validatePendingReschedulePlan(plan);
        plan.setPlanStatus("CONFIRMED");
        plan.setActiveFlag("Y");
        plan.setUpdateBy(operatorName);
        plan.setRemark(appendRemark(plan.getRemark(), operatorName + " confirmed reschedule plan."));
        apsSchedulePlanMapper.updateApsSchedulePlan(plan);

        if (plan.getSourcePlanId() != null)
        {
            ApsSchedulePlan sourcePlan = apsSchedulePlanMapper.selectApsSchedulePlanById(plan.getSourcePlanId());
            if (sourcePlan != null)
            {
                sourcePlan.setPlanStatus("HISTORY");
                sourcePlan.setActiveFlag("N");
                sourcePlan.setUpdateBy(operatorName);
                sourcePlan.setRemark(appendRemark(sourcePlan.getRemark(), "Archived because reschedule plan " + plan.getPlanId() + " was confirmed."));
                apsSchedulePlanMapper.updateApsSchedulePlan(sourcePlan);
            }
        }

        updateEventStatus(plan, "CONFIRMED", operatorName, "调度员已确认采用重调度方案");
        return buildStatusResult(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rejectReschedulePlan(Long planId, String operatorName)
    {
        ApsSchedulePlan plan = validateReschedulePlan(planId);
        validatePendingReschedulePlan(plan);
        plan.setPlanStatus("REJECTED");
        plan.setActiveFlag("N");
        plan.setUpdateBy(operatorName);
        plan.setRemark(appendRemark(plan.getRemark(), operatorName + " rejected reschedule plan."));
        apsSchedulePlanMapper.updateApsSchedulePlan(plan);

        if (plan.getSourcePlanId() != null)
        {
            ApsSchedulePlan sourcePlan = apsSchedulePlanMapper.selectApsSchedulePlanById(plan.getSourcePlanId());
            if (sourcePlan != null)
            {
                sourcePlan.setPlanStatus("ACTIVE");
                sourcePlan.setActiveFlag("Y");
                sourcePlan.setUpdateBy(operatorName);
                sourcePlan.setRemark(appendRemark(sourcePlan.getRemark(), "Restored because reschedule plan " + plan.getPlanId() + " was rejected."));
                apsSchedulePlanMapper.updateApsSchedulePlan(sourcePlan);
            }
        }

        updateEventStatus(plan, "REJECTED", operatorName, "调度员拒绝采用重调度方案");
        return buildStatusResult(plan);
    }

    private List<ApsOrder> selectPendingNormalOrders()
    {
        ApsOrder query = new ApsOrder();
        query.setOrderType("NORMAL");
        query.setOrderStatus("NEW");
        return apsOrderMapper.selectApsOrderList(query).stream()
                .filter(item -> "0".equals(item.getDelFlag()) || item.getDelFlag() == null)
                .sorted(Comparator.comparing(ApsOrder::getPriorityLevel, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ApsOrder::getDueTime, Comparator.nullsLast(Date::compareTo))
                        .thenComparing(ApsOrder::getReleaseTime, Comparator.nullsLast(Date::compareTo)))
                .collect(Collectors.toList());
    }

    private Map<String, Object> calculatePlanKpi(List<ApsScheduleTask> tasks)
    {
        Map<Long, Date> finishTimeByOrder = calculateOrderFinishTimes(tasks);
        Date makespan = null;
        for (ApsScheduleTask task : tasks)
        {
            Date endTime = task.getPlannedEndTime();
            if (endTime == null)
            {
                continue;
            }
            if (makespan == null || endTime.after(makespan))
            {
                makespan = endTime;
            }
        }

        long delayOrderCount = 0L;
        long totalDelayMinutes = 0L;
        long maxDelayMinutes = 0L;
        for (Map.Entry<Long, Date> entry : finishTimeByOrder.entrySet())
        {
            ApsOrder order = apsOrderMapper.selectApsOrderById(entry.getKey());
            if (order == null || order.getDueTime() == null)
            {
                continue;
            }
            long delayMinutes = Math.max(0L, minutesBetween(order.getDueTime(), entry.getValue()));
            if (delayMinutes > 0)
            {
                delayOrderCount++;
                totalDelayMinutes += delayMinutes;
                maxDelayMinutes = Math.max(maxDelayMinutes, delayMinutes);
            }
        }

        Map<String, Object> kpi = new HashMap<>();
        kpi.put("taskCount", tasks.size());
        kpi.put("makespan", makespan);
        kpi.put("delayOrderCount", delayOrderCount);
        kpi.put("totalDelayMinutes", totalDelayMinutes);
        kpi.put("maxDelayMinutes", maxDelayMinutes);
        return kpi;
    }

    private Map<Long, Date> calculateOrderFinishTimes(List<ApsScheduleTask> tasks)
    {
        Map<Long, Date> finishTimeByOrder = new HashMap<>();
        for (ApsScheduleTask task : tasks)
        {
            Date endTime = task.getPlannedEndTime();
            Long orderId = task.getOrderId();
            if (orderId == null || endTime == null)
            {
                continue;
            }
            Date current = finishTimeByOrder.get(orderId);
            if (current == null || endTime.after(current))
            {
                finishTimeByOrder.put(orderId, endTime);
            }
        }
        return finishTimeByOrder;
    }

    private Map<String, Object> calculateStabilityCompare(Map<Long, Date> originalFinishByOrder,
            Map<Long, Date> newFinishByOrder)
    {
        long stabilityDelayOrderCount = 0L;
        long stabilityTotalDelayMinutes = 0L;
        long stabilityMaxDelayMinutes = 0L;
        long comparableOrderCount = 0L;
        for (Map.Entry<Long, Date> entry : newFinishByOrder.entrySet())
        {
            Date originalFinishTime = originalFinishByOrder.get(entry.getKey());
            if (originalFinishTime == null || entry.getValue() == null)
            {
                continue;
            }
            comparableOrderCount++;
            long stabilityDelayMinutes = Math.max(0L, minutesBetween(originalFinishTime, entry.getValue()));
            if (stabilityDelayMinutes > 0)
            {
                stabilityDelayOrderCount++;
                stabilityTotalDelayMinutes += stabilityDelayMinutes;
                stabilityMaxDelayMinutes = Math.max(stabilityMaxDelayMinutes, stabilityDelayMinutes);
            }
        }
        Map<String, Object> stabilityCompare = new LinkedHashMap<>();
        stabilityCompare.put("stabilityDelayOrderCount", stabilityDelayOrderCount);
        stabilityCompare.put("stabilityTotalDelayMinutes", stabilityTotalDelayMinutes);
        stabilityCompare.put("stabilityMaxDelayMinutes", stabilityMaxDelayMinutes);
        stabilityCompare.put("averageStabilityDelayMinutes",
                comparableOrderCount == 0L ? 0D : round4((double) stabilityTotalDelayMinutes / comparableOrderCount));
        return stabilityCompare;
    }

    private ApsSchedulePlan validateReschedulePlan(Long planId)
    {
        ApsSchedulePlan plan = apsSchedulePlanMapper.selectApsSchedulePlanById(planId);
        if (plan == null)
        {
            throw new IllegalStateException("Schedule plan does not exist.");
        }
        if (!"RESCHEDULE".equals(plan.getPlanType()))
        {
            throw new IllegalStateException("Only RESCHEDULE plan can be confirmed or rejected.");
        }
        return plan;
    }

    private void validatePendingReschedulePlan(ApsSchedulePlan plan)
    {
        if (!"PENDING".equals(plan.getPlanStatus()))
        {
            throw new IllegalStateException("Only PENDING RESCHEDULE plan can be confirmed or rejected.");
        }
    }

    private void updateEventStatus(ApsSchedulePlan plan, String eventStatus, String operatorName, String remark)
    {
        if (plan.getEventId() == null)
        {
            return;
        }
        ApsInsertEvent event = apsInsertEventMapper.selectApsInsertEventById(plan.getEventId());
        if (event == null)
        {
            return;
        }
        event.setEventStatus(eventStatus);
        event.setNewPlanId(plan.getPlanId());
        event.setUpdateBy(operatorName);
        event.setRemark(appendRemark(event.getRemark(), remark));
        apsInsertEventMapper.updateApsInsertEvent(event);
    }

    private Map<String, Object> buildStatusResult(ApsSchedulePlan plan)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("planId", plan.getPlanId());
        result.put("planCode", plan.getPlanCode());
        result.put("planStatus", plan.getPlanStatus());
        result.put("activeFlag", plan.getActiveFlag());
        result.put("sourcePlanId", plan.getSourcePlanId());
        result.put("eventId", plan.getEventId());
        return result;
    }

    private String appendRemark(String oldRemark, String message)
    {
        String timestamp = DateUtils.parseDateToStr(DATE_TIME_FORMAT, DateUtils.getNowDate());
        String item = "[" + timestamp + "] " + message;
        if (oldRemark == null || oldRemark.trim().isEmpty())
        {
            return item;
        }
        return oldRemark + "\n" + item;
    }

    private Date findOrderFinishTime(List<ApsScheduleTask> tasks, Long orderId)
    {
        if (orderId == null)
        {
            return null;
        }
        return tasks.stream()
                .filter(task -> orderId.equals(task.getOrderId()))
                .map(ApsScheduleTask::getPlannedEndTime)
                .filter(item -> item != null)
                .max(Date::compareTo)
                .orElse(null);
    }

    private long minutesBetween(Date start, Date end)
    {
        if (start == null || end == null)
        {
            return 0L;
        }
        return (end.getTime() - start.getTime()) / 60000L;
    }

    private long getLong(Map<String, Object> map, String key)
    {
        Object value = map.get(key);
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private double round4(double value)
    {
        return Math.round(value * 10000D) / 10000D;
    }

    private String formatDate(Date date)
    {
        return date == null ? null : DateUtils.parseDateToStr(DATE_TIME_FORMAT, date);
    }

    private String buildCompareConclusion(long insertOrderTrueDelayMinutes, long trueDelayOrderCountDiff,
            long trueTotalDelayMinutesDiff, long stabilityDelayOrderCount,
            long stabilityTotalDelayMinutes, long makespanDiffMinutes)
    {
        String insertText = insertOrderTrueDelayMinutes <= 0
                ? "新方案保障插单 lot 按真实交期完成" : "插单 lot 相对真实交期仍存在延期";
        String delayText = trueDelayOrderCountDiff < 0 ? "真实延期 lot 数减少"
                : (trueDelayOrderCountDiff > 0 ? "真实延期 lot 数增加" : "真实延期 lot 数不变");
        String totalDelayText = trueTotalDelayMinutesDiff < 0 ? "真实总延期时间减少"
                : (trueTotalDelayMinutesDiff > 0 ? "真实总延期时间增加" : "真实总延期时间不变");
        String stabilityText = stabilityDelayOrderCount > 0
                ? "部分普通 lot 相比原计划发生后移，计划稳定性有所下降，后移总计 " + stabilityTotalDelayMinutes + " 分钟"
                : "普通 lot 未出现相对原计划的后移扰动";
        String makespanText = makespanDiffMinutes > 0 ? "整体完工时间有所增加"
                : (makespanDiffMinutes < 0 ? "整体完工时间有所缩短" : "整体完工时间不变");
        return insertText + "；" + delayText + "，" + totalDelayText + "；" + stabilityText + "；" + makespanText + "。";
    }

    private List<ApsRouteOperation> selectEnabledRouteOperations()
    {
        return apsRouteOperationMapper.selectApsRouteOperationList(new ApsRouteOperation()).stream()
                .filter(item -> ("0".equals(item.getDelFlag()) || item.getDelFlag() == null)
                        && ("0".equals(item.getStatus()) || item.getStatus() == null))
                .collect(Collectors.toList());
    }

    private List<ApsEquipmentGroup> selectEnabledEquipmentGroups()
    {
        return apsEquipmentGroupMapper.selectApsEquipmentGroupList(new ApsEquipmentGroup()).stream()
                .filter(item -> ("0".equals(item.getDelFlag()) || item.getDelFlag() == null)
                        && ("0".equals(item.getStatus()) || item.getStatus() == null))
                .collect(Collectors.toList());
    }

    private List<ApsEquipment> selectEnabledEquipments()
    {
        return apsEquipmentMapper.selectApsEquipmentList(new ApsEquipment()).stream()
                .filter(item -> ("0".equals(item.getDelFlag()) || item.getDelFlag() == null)
                        && ("0".equals(item.getStatus()) || item.getStatus() == null))
                .collect(Collectors.toList());
    }

    private InitialScheduleRequest buildInitialScheduleRequest(List<ApsOrder> orders,
            List<ApsRouteOperation> routeOperations, List<ApsEquipmentGroup> equipmentGroups,
            List<ApsEquipment> equipments, Date scheduleStartTime)
    {
        InitialScheduleRequest request = new InitialScheduleRequest();
        request.setRequestId("INITIAL-" + DateUtils.dateTimeNow());
        request.setScheduleStartTime(DateUtils.parseDateToStr(DATE_TIME_FORMAT, scheduleStartTime));
        request.setOrders(toOrderDTOList(orders));
        request.setRouteOperations(toRouteOperationDTOList(routeOperations));
        request.setEquipmentGroups(toEquipmentGroupDTOList(equipmentGroups));
        request.setEquipments(toEquipmentDTOList(equipments));
        Map<String, Object> algorithmConfig = new HashMap<>();
        algorithmConfig.put("rule", "PRIORITY_DUE_EARLIEST_EQUIPMENT");
        request.setAlgorithmConfig(algorithmConfig);
        return request;
    }

    private ApsSchedulePlan buildSchedulePlan(InitialScheduleResponse response, Date scheduleStartTime, String username)
    {
        Date scheduleEndTime = response.getTaskSchedules().stream()
                .map(ScheduleTaskDTO::getPlannedEndTime)
                .map(DateUtils::parseDate)
                .filter(item -> item != null)
                .max(Date::compareTo)
                .orElse(scheduleStartTime);
        ApsSchedulePlan plan = new ApsSchedulePlan();
        plan.setPlanCode("PLAN-" + DateUtils.dateTimeNow());
        plan.setPlanName("初始调度方案");
        plan.setPlanType("INITIAL");
        plan.setPlanStatus("ACTIVE");
        plan.setAlgorithmName(response.getAlgorithmName());
        plan.setStrategyType("INITIAL_RULE");
        plan.setScheduleStartTime(scheduleStartTime);
        plan.setScheduleEndTime(scheduleEndTime);
        plan.setKpiJson(JSON.toJSONString(response.getKpi()));
        plan.setActiveFlag("Y");
        plan.setDelFlag("0");
        plan.setCreateBy(username);
        return plan;
    }

    private ApsScheduleTask buildScheduleTask(Long planId, ScheduleTaskDTO taskDTO, String username)
    {
        ApsScheduleTask task = new ApsScheduleTask();
        task.setPlanId(planId);
        task.setOrderId(taskDTO.getOrderId());
        task.setOrderCode(taskDTO.getOrderCode());
        task.setProductId(taskDTO.getProductId());
        task.setProductCode(taskDTO.getProductCode());
        task.setProcessSeq(taskDTO.getProcessSeq());
        task.setProcessCode(taskDTO.getProcessCode());
        task.setProcessName(taskDTO.getProcessName());
        task.setEquipmentGroupId(taskDTO.getEquipmentGroupId());
        task.setEquipmentId(taskDTO.getEquipmentId());
        task.setEquipmentCode(taskDTO.getEquipmentCode());
        task.setPlannedStartTime(DateUtils.parseDate(taskDTO.getPlannedStartTime()));
        task.setPlannedEndTime(DateUtils.parseDate(taskDTO.getPlannedEndTime()));
        task.setDuration(taskDTO.getDuration());
        task.setTaskStatus("PLANNED");
        task.setIsFrozen("N");
        task.setIsInserted("N");
        task.setIsChanged("N");
        task.setDelFlag("0");
        task.setCreateBy(username);
        return task;
    }

    private List<OrderDTO> toOrderDTOList(List<ApsOrder> orders)
    {
        List<OrderDTO> dtoList = new ArrayList<>();
        for (ApsOrder order : orders)
        {
            OrderDTO dto = new OrderDTO();
            dto.setOrderId(order.getOrderId());
            dto.setOrderCode(order.getOrderCode());
            dto.setOrderType(order.getOrderType());
            dto.setProductId(order.getProductId());
            dto.setProductCode(order.getProductCode());
            dto.setQuantity(order.getQuantity());
            dto.setPriorityLevel(order.getPriorityLevel());
            dto.setReleaseTime(DateUtils.parseDateToStr(DATE_TIME_FORMAT, order.getReleaseTime()));
            dto.setDueTime(DateUtils.parseDateToStr(DATE_TIME_FORMAT, order.getDueTime()));
            dto.setOrderStatus(order.getOrderStatus());
            dtoList.add(dto);
        }
        return dtoList;
    }

    private List<RouteOperationDTO> toRouteOperationDTOList(List<ApsRouteOperation> routeOperations)
    {
        List<RouteOperationDTO> dtoList = new ArrayList<>();
        for (ApsRouteOperation operation : routeOperations)
        {
            RouteOperationDTO dto = new RouteOperationDTO();
            dto.setRouteOperationId(operation.getRouteOperationId());
            dto.setProductId(operation.getProductId());
            dto.setProductCode(operation.getProductCode());
            dto.setProcessSeq(operation.getProcessSeq());
            dto.setProcessCode(operation.getProcessCode());
            dto.setProcessName(operation.getProcessName());
            dto.setEquipmentGroupId(operation.getEquipmentGroupId());
            dto.setStandardDuration(operation.getStandardDuration());
            dtoList.add(dto);
        }
        return dtoList;
    }

    private List<EquipmentGroupDTO> toEquipmentGroupDTOList(List<ApsEquipmentGroup> equipmentGroups)
    {
        List<EquipmentGroupDTO> dtoList = new ArrayList<>();
        for (ApsEquipmentGroup group : equipmentGroups)
        {
            EquipmentGroupDTO dto = new EquipmentGroupDTO();
            dto.setEquipmentGroupId(group.getEquipmentGroupId());
            dto.setEquipmentGroupCode(group.getEquipmentGroupCode());
            dto.setEquipmentGroupName(group.getEquipmentGroupName());
            dto.setStatus(group.getStatus());
            dtoList.add(dto);
        }
        return dtoList;
    }

    private List<EquipmentDTO> toEquipmentDTOList(List<ApsEquipment> equipments)
    {
        List<EquipmentDTO> dtoList = new ArrayList<>();
        for (ApsEquipment equipment : equipments)
        {
            EquipmentDTO dto = new EquipmentDTO();
            dto.setEquipmentId(equipment.getEquipmentId());
            dto.setEquipmentCode(equipment.getEquipmentCode());
            dto.setEquipmentName(equipment.getEquipmentName());
            dto.setEquipmentGroupId(equipment.getEquipmentGroupId());
            dto.setEquipmentGroupCode(equipment.getEquipmentGroupCode());
            dto.setStatus(equipment.getStatus());
            dtoList.add(dto);
        }
        return dtoList;
    }
}
