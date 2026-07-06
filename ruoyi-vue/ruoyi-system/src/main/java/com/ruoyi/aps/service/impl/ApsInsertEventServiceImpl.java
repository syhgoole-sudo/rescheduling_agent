package com.ruoyi.aps.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.aps.client.PythonScheduleClient;
import com.ruoyi.aps.client.dto.EquipmentDTO;
import com.ruoyi.aps.client.dto.LocalRescheduleRequest;
import com.ruoyi.aps.client.dto.LocalRescheduleResponse;
import com.ruoyi.aps.client.dto.OrderDTO;
import com.ruoyi.aps.client.dto.RouteOperationDTO;
import com.ruoyi.aps.client.dto.ScheduleTaskDTO;
import com.ruoyi.aps.domain.ApsEquipment;
import com.ruoyi.aps.domain.ApsInsertEvent;
import com.ruoyi.aps.domain.ApsOrder;
import com.ruoyi.aps.domain.ApsRouteOperation;
import com.ruoyi.aps.domain.ApsSchedulePlan;
import com.ruoyi.aps.domain.ApsScheduleTask;
import com.ruoyi.aps.mapper.ApsEquipmentMapper;
import com.ruoyi.aps.mapper.ApsInsertEventMapper;
import com.ruoyi.aps.mapper.ApsOrderMapper;
import com.ruoyi.aps.mapper.ApsRouteOperationMapper;
import com.ruoyi.aps.mapper.ApsSchedulePlanMapper;
import com.ruoyi.aps.mapper.ApsScheduleTaskMapper;
import com.ruoyi.aps.service.IApsInsertEventService;
import com.ruoyi.aps.service.IApsSchedulePlanService;
import com.ruoyi.common.utils.DateUtils;

@Service
public class ApsInsertEventServiceImpl implements IApsInsertEventService
{
    @Autowired
    private ApsInsertEventMapper apsInsertEventMapper;

    @Autowired
    private ApsOrderMapper apsOrderMapper;

    @Autowired
    private ApsRouteOperationMapper apsRouteOperationMapper;

    @Autowired
    private ApsSchedulePlanMapper apsSchedulePlanMapper;

    @Autowired
    private ApsScheduleTaskMapper apsScheduleTaskMapper;

    @Autowired
    private ApsEquipmentMapper apsEquipmentMapper;

    @Autowired
    private PythonScheduleClient pythonScheduleClient;

    @Autowired
    private IApsSchedulePlanService apsSchedulePlanService;

    @Override
    public ApsInsertEvent selectApsInsertEventById(Long eventId)
    {
        return apsInsertEventMapper.selectApsInsertEventById(eventId);
    }

    @Override
    public List<ApsInsertEvent> selectApsInsertEventList(ApsInsertEvent apsInsertEvent)
    {
        return apsInsertEventMapper.selectApsInsertEventList(apsInsertEvent);
    }

    @Override
    public int insertApsInsertEvent(ApsInsertEvent apsInsertEvent)
    {
        return apsInsertEventMapper.insertApsInsertEvent(apsInsertEvent);
    }

    @Override
    public int updateApsInsertEvent(ApsInsertEvent apsInsertEvent)
    {
        return apsInsertEventMapper.updateApsInsertEvent(apsInsertEvent);
    }

    @Override
    public int deleteApsInsertEventByIds(Long[] eventIds)
    {
        return apsInsertEventMapper.deleteApsInsertEventByIds(eventIds);
    }

    @Override
    public int deleteApsInsertEventById(Long eventId)
    {
        return apsInsertEventMapper.deleteApsInsertEventById(eventId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createAndAnalyzeInsertEvent(Long insertOrderId, String operatorName)
    {
        ApsOrder insertOrder = apsOrderMapper.selectApsOrderById(insertOrderId);
        if (insertOrder == null)
        {
            throw new IllegalStateException("插单订单不存在");
        }
        if (!"INSERT".equals(insertOrder.getOrderType()))
        {
            throw new IllegalStateException("订单不是 INSERT 类型");
        }
        ApsSchedulePlan activePlan = apsSchedulePlanMapper.selectActiveInitialPlan();
        if (activePlan == null)
        {
            throw new IllegalStateException("未找到当前 ACTIVE 初始调度方案");
        }

        List<ApsRouteOperation> insertRouteOperations = selectRouteOperations(insertOrder.getProductId());
        Set<Long> equipmentGroupIds = insertRouteOperations.stream()
                .map(ApsRouteOperation::getEquipmentGroupId)
                .filter(item -> item != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ApsScheduleTask> affectedTasks = equipmentGroupIds.isEmpty()
                ? new ArrayList<>()
                : apsScheduleTaskMapper.selectAffectedTasks(activePlan.getPlanId(), insertOrder.getReleaseTime(), new ArrayList<>(equipmentGroupIds));

        Set<Long> affectedTaskIds = affectedTasks.stream()
                .map(ApsScheduleTask::getTaskId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> affectedOrderIds = affectedTasks.stream()
                .map(ApsScheduleTask::getOrderId)
                .filter(item -> item != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> affectedEquipmentIds = affectedTasks.stream()
                .map(ApsScheduleTask::getEquipmentId)
                .filter(item -> item != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String impactLevel = calculateImpactLevel(affectedTasks.size());
        Map<String, Object> impact = buildImpactJson(insertOrder, equipmentGroupIds, affectedEquipmentIds,
                affectedOrderIds, affectedTaskIds, affectedTasks.size(), impactLevel);

        ApsInsertEvent event = new ApsInsertEvent();
        event.setEventCode("IE-" + DateUtils.dateTimeNow());
        event.setInsertOrderId(insertOrderId);
        event.setSourcePlanId(activePlan.getPlanId());
        event.setEventTime(new Date());
        event.setEventStatus("ANALYZED");
        event.setImpactJson(JSON.toJSONString(impact));
        event.setStrategyType("LOCAL_RESCHEDULE_INSERT_ORDER");
        event.setEventReason("插单影响分析");
        event.setDelFlag("0");
        event.setCreateBy(operatorName);
        apsInsertEventMapper.insertApsInsertEvent(event);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", event.getEventId());
        result.put("eventCode", event.getEventCode());
        result.put("sourcePlanId", event.getSourcePlanId());
        result.put("insertOrderId", event.getInsertOrderId());
        result.put("impactLevel", impactLevel);
        result.put("affectedTaskCount", affectedTasks.size());
        result.put("affectedOrderCount", affectedOrderIds.size());
        result.put("affectedEquipmentCount", affectedEquipmentIds.size());
        result.put("impactJson", event.getImpactJson());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recommendStrategy(Long eventId, String operatorName)
    {
        ApsInsertEvent event = apsInsertEventMapper.selectApsInsertEventById(eventId);
        if (event == null)
        {
            throw new IllegalStateException("Insert event does not exist.");
        }
        if (!"ANALYZED".equals(event.getEventStatus()))
        {
            throw new IllegalStateException("Only ANALYZED insert events can recommend strategy.");
        }
        if (event.getSourcePlanId() == null)
        {
            throw new IllegalStateException("Insert event source plan is empty.");
        }
        if (event.getImpactJson() == null || event.getImpactJson().trim().isEmpty())
        {
            throw new IllegalStateException("Insert event impact_json is empty.");
        }

        JSONObject impact = JSON.parseObject(event.getImpactJson());
        String impactLevel = impact.getString("impactLevel");
        Set<Long> affectedTaskIds = parseLongSet(impact.get("affectedTaskIds"));

        List<ApsScheduleTask> planTasks = apsScheduleTaskMapper.selectApsScheduleTaskListByPlanId(event.getSourcePlanId());
        long adjustableTaskCount = planTasks.stream()
                .filter(task -> affectedTaskIds.contains(task.getTaskId()))
                .count();
        long frozenTaskCount = planTasks.stream()
                .filter(task -> !affectedTaskIds.contains(task.getTaskId()))
                .count();

        ApsOrder insertOrder = apsOrderMapper.selectApsOrderById(event.getInsertOrderId());
        if (insertOrder == null)
        {
            throw new IllegalStateException("Insert order does not exist.");
        }
        List<Map<String, Object>> insertTasks = buildInsertTasks(insertOrder);

        Map<String, Object> strategy = buildStrategy(impactLevel, frozenTaskCount,
                adjustableTaskCount, insertTasks.size());
        impact.put("strategy", strategy);

        event.setStrategyType((String) strategy.get("strategyType"));
        event.setImpactJson(JSON.toJSONString(impact));
        event.setRemark((String) strategy.get("recommendedReason"));
        event.setUpdateBy(operatorName);
        apsInsertEventMapper.updateApsInsertEvent(event);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", event.getEventId());
        result.put("strategyType", strategy.get("strategyType"));
        result.put("strategyName", strategy.get("strategyName"));
        result.put("frozenTaskCount", frozenTaskCount);
        result.put("adjustableTaskCount", adjustableTaskCount);
        result.put("insertTaskCount", insertTasks.size());
        result.put("recommendedReason", strategy.get("recommendedReason"));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateLocalReschedule(Long eventId, String operatorName)
    {
        return generateLocalReschedule(eventId, operatorName, "RULE");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateLocalReschedule(Long eventId, String operatorName, String algorithmType)
    {
        ApsInsertEvent event = apsInsertEventMapper.selectApsInsertEventById(eventId);
        if (event == null)
        {
            throw new IllegalStateException("Insert event does not exist.");
        }
        if (!"ANALYZED".equals(event.getEventStatus()) && !"RESCHEDULED".equals(event.getEventStatus()))
        {
            throw new IllegalStateException("Only ANALYZED insert events can generate local reschedule plan.");
        }
        if (event.getSourcePlanId() == null)
        {
            throw new IllegalStateException("Insert event source plan is empty.");
        }
        if (event.getImpactJson() == null || event.getImpactJson().trim().isEmpty())
        {
            throw new IllegalStateException("Insert event impact_json is empty.");
        }

        JSONObject impact = JSON.parseObject(event.getImpactJson());
        JSONObject strategy = impact.getJSONObject("strategy");
        if (strategy == null)
        {
            throw new IllegalStateException("Please recommend strategy before generating local reschedule plan.");
        }
        strategy.put("algorithmType", normalizeAlgorithmType(algorithmType));
        Set<Long> affectedTaskIds = parseLongSet(impact.get("affectedTaskIds"));

        ApsSchedulePlan sourcePlan = apsSchedulePlanMapper.selectApsSchedulePlanById(event.getSourcePlanId());
        if (sourcePlan == null)
        {
            throw new IllegalStateException("Source schedule plan does not exist.");
        }
        ApsOrder insertOrder = apsOrderMapper.selectApsOrderById(event.getInsertOrderId());
        if (insertOrder == null)
        {
            throw new IllegalStateException("Insert order does not exist.");
        }

        List<ApsScheduleTask> sourceTasks = apsScheduleTaskMapper.selectApsScheduleTaskListByPlanId(sourcePlan.getPlanId());
        List<ScheduleTaskDTO> frozenTasks = new ArrayList<>();
        List<ScheduleTaskDTO> adjustableTasks = new ArrayList<>();
        for (ApsScheduleTask task : sourceTasks)
        {
            if (affectedTaskIds.contains(task.getTaskId()))
            {
                adjustableTasks.add(toScheduleTaskDTO(task, false));
            }
            else
            {
                frozenTasks.add(toScheduleTaskDTO(task, true));
            }
        }

        Date scheduleStartTime = insertOrder.getReleaseTime() == null ? DateUtils.getNowDate() : insertOrder.getReleaseTime();
        LocalRescheduleRequest request = new LocalRescheduleRequest();
        request.setRequestId("RESCHEDULE-" + DateUtils.dateTimeNow());
        request.setEventId(event.getEventId());
        request.setSourcePlanId(sourcePlan.getPlanId());
        request.setScheduleStartTime(formatDate(scheduleStartTime));
        request.setInsertOrder(toOrderDTO(insertOrder));
        request.setFrozenTasks(frozenTasks);
        request.setAdjustableTasks(adjustableTasks);
        request.setInsertTasks(buildInsertTasks(insertOrder));
        request.setEquipments(toEquipmentDTOList(selectEnabledEquipments()));
        request.setRouteOperations(toRouteOperationDTOList(selectEnabledRouteOperations()));
        request.setStrategyConfig(strategy);

        LocalRescheduleResponse response = pythonScheduleClient.localReschedule(request);
        if (!Boolean.TRUE.equals(response.getSuccess()))
        {
            throw new IllegalStateException(response.getMessage());
        }
        if (response.getTaskSchedules() == null || response.getTaskSchedules().isEmpty())
        {
            throw new IllegalStateException("Python local reschedule service did not return task schedules.");
        }

        ApsSchedulePlan newPlan = buildReschedulePlan(sourcePlan, event, response, scheduleStartTime, operatorName);
        apsSchedulePlanMapper.insertApsSchedulePlan(newPlan);

        for (ScheduleTaskDTO taskDTO : response.getTaskSchedules())
        {
            apsScheduleTaskMapper.insertApsScheduleTask(buildRescheduleTask(newPlan.getPlanId(), taskDTO, operatorName));
        }

        event.setNewPlanId(newPlan.getPlanId());
        event.setEventStatus("RESCHEDULED");
        event.setStrategyType(response.getStrategyType());
        event.setUpdateBy(operatorName);
        apsInsertEventMapper.updateApsInsertEvent(event);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("newPlanId", newPlan.getPlanId());
        result.put("sourcePlanId", sourcePlan.getPlanId());
        result.put("eventId", event.getEventId());
        result.put("taskCount", response.getTaskSchedules().size());
        result.put("kpi", response.getKpi());
        result.put("warnings", response.getWarnings());
        return result;
    }

    @Override
    public Map<String, Object> generateExplanationReport(Long eventId, String operatorName)
    {
        ApsInsertEvent event = apsInsertEventMapper.selectApsInsertEventById(eventId);
        if (event == null)
        {
            throw new IllegalStateException("Insert event does not exist.");
        }

        ApsOrder insertOrder = event.getInsertOrderId() == null ? null : apsOrderMapper.selectApsOrderById(event.getInsertOrderId());
        ApsSchedulePlan sourcePlan = event.getSourcePlanId() == null ? null : apsSchedulePlanMapper.selectApsSchedulePlanById(event.getSourcePlanId());
        if (event.getNewPlanId() == null)
        {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("eventId", event.getEventId());
            result.put("eventCode", event.getEventCode());
            result.put("sourcePlanId", event.getSourcePlanId());
            result.put("newPlanId", null);
            result.put("reportTitle", "插单重调度解释报告");
            result.put("eventSummary", buildEventSummary(insertOrder, event));
            result.put("impactSummary", "尚未生成重调度方案，无法生成完整解释报告。");
            result.put("strategySummary", event.getStrategyType() == null ? "尚未形成策略建议。" : "当前策略类型为 " + event.getStrategyType() + "。");
            result.put("rescheduleSummary", "尚未生成重调度方案。");
            result.put("kpiSummary", "暂无新旧方案 KPI 对比结果。");
            result.put("benefit", "暂无可评价的重调度收益。");
            result.put("cost", "暂无可评价的重调度代价。");
            result.put("recommendation", "建议先生成局部重调度方案，再查看完整解释报告。");
            result.put("fullReport", "尚未生成重调度方案，无法生成完整解释报告。请先完成策略推荐和局部重调度方案生成。");
            return result;
        }

        ApsSchedulePlan newPlan = apsSchedulePlanMapper.selectApsSchedulePlanById(event.getNewPlanId());
        if (newPlan == null)
        {
            throw new IllegalStateException("New reschedule plan does not exist.");
        }

        JSONObject impact = event.getImpactJson() == null || event.getImpactJson().trim().isEmpty()
                ? new JSONObject() : JSON.parseObject(event.getImpactJson());
        JSONObject strategy = impact.getJSONObject("strategy");
        Map<String, Object> compare = apsSchedulePlanService.compareReschedulePlan(newPlan.getPlanId());

        Map<String, Object> summary = getMap(compare, "summary");
        Map<String, Object> delayCompare = getMap(compare, "delayCompare");
        Map<String, Object> makespanCompare = getMap(compare, "makespanCompare");
        Map<String, Object> insertOrderCompare = getMap(compare, "insertOrderCompare");

        long insertDelayMinutes = getLong(insertOrderCompare, "insertOrderDelayMinutes");
        long originalDelayOrderCount = getLong(delayCompare, "originalDelayOrderCount");
        long newDelayOrderCount = getLong(delayCompare, "newDelayOrderCount");
        long delayOrderCountDiff = getLong(delayCompare, "delayOrderCountDiff");
        long totalDelayMinutesDiff = getLong(delayCompare, "totalDelayMinutesDiff");
        long maxDelayMinutesDiff = getLong(delayCompare, "maxDelayMinutesDiff");
        long makespanDiffMinutes = getLong(makespanCompare, "makespanDiffMinutes");
        double changedTaskRatio = getDouble(summary, "changedTaskRatio");

        String eventSummary = buildEventSummary(insertOrder, event);
        String impactSummary = "系统识别出受影响任务 " + impact.getIntValue("affectedTaskCount")
                + " 个、受影响订单 " + impact.getIntValue("affectedOrderCount")
                + " 个、受影响设备 " + impact.getIntValue("affectedEquipmentCount")
                + " 台，影响等级为 " + defaultText(impact.getString("impactLevel"), "UNKNOWN") + "。";
        String strategyName = strategy == null ? null : strategy.getString("strategyName");
        String strategySummary = "系统采用 " + defaultText(strategyName, event.getStrategyType())
                + " 策略，目标是在优先保障插单交期的同时减少原计划变更。";
        String rescheduleSummary = "新方案任务数为 " + getLong(summary, "newTaskCount")
                + "，其中插单任务 " + getLong(summary, "insertedTaskCount")
                + " 个、冻结任务 " + getLong(summary, "frozenTaskCount")
                + " 个、变更任务 " + getLong(summary, "changedTaskCount")
                + " 个，变更比例为 " + changedTaskRatio + "。";
        String kpiSummary = "KPI 对比显示，延期订单数由 " + originalDelayOrderCount + " 变为 " + newDelayOrderCount
                + "，变化 " + delayOrderCountDiff
                + "；总延期分钟变化 " + totalDelayMinutesDiff
                + "；最大延期分钟变化 " + maxDelayMinutesDiff
                + "；整体完工时间变化 " + makespanDiffMinutes + " 分钟。";
        String benefit = buildBenefit(insertDelayMinutes, delayOrderCountDiff, totalDelayMinutesDiff);
        String cost = buildCost(makespanDiffMinutes, changedTaskRatio, getLong(summary, "changedTaskCount"));
        String recommendation = buildExplanationRecommendation(insertDelayMinutes, newDelayOrderCount,
                originalDelayOrderCount, makespanDiffMinutes, changedTaskRatio);

        String fullReport = eventSummary + "\n\n"
                + impactSummary + "\n\n"
                + strategySummary + "\n\n"
                + rescheduleSummary + "\n\n"
                + kpiSummary + "\n\n"
                + "方案收益：" + benefit + "\n\n"
                + "方案代价：" + cost + "\n\n"
                + "调度建议：" + recommendation;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventId", event.getEventId());
        result.put("eventCode", event.getEventCode());
        result.put("sourcePlanId", event.getSourcePlanId());
        result.put("newPlanId", event.getNewPlanId());
        result.put("sourcePlanCode", sourcePlan == null ? null : sourcePlan.getPlanCode());
        result.put("newPlanCode", newPlan.getPlanCode());
        result.put("reportTitle", "插单重调度解释报告");
        result.put("eventSummary", eventSummary);
        result.put("impactSummary", impactSummary);
        result.put("strategySummary", strategySummary);
        result.put("rescheduleSummary", rescheduleSummary);
        result.put("kpiSummary", kpiSummary);
        result.put("benefit", benefit);
        result.put("cost", cost);
        result.put("recommendation", recommendation);
        result.put("fullReport", fullReport);
        return result;
    }

    @Override
    public Map<String, Object> generateAiExplanationReport(Long eventId, String operatorName)
    {
        ApsInsertEvent event = apsInsertEventMapper.selectApsInsertEventById(eventId);
        if (event == null)
        {
            throw new IllegalStateException("Insert event does not exist.");
        }

        Map<String, Object> fallback = buildAiFallback(eventId, operatorName);
        if (event.getNewPlanId() == null)
        {
            return fallback;
        }

        try
        {
            ApsOrder insertOrder = event.getInsertOrderId() == null ? null : apsOrderMapper.selectApsOrderById(event.getInsertOrderId());
            ApsSchedulePlan sourcePlan = event.getSourcePlanId() == null ? null : apsSchedulePlanMapper.selectApsSchedulePlanById(event.getSourcePlanId());
            ApsSchedulePlan newPlan = event.getNewPlanId() == null ? null : apsSchedulePlanMapper.selectApsSchedulePlanById(event.getNewPlanId());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("event", buildEventContext(event));
            payload.put("insertOrder", buildOrderContext(insertOrder));
            payload.put("impact", event.getImpactJson() == null || event.getImpactJson().trim().isEmpty()
                    ? new LinkedHashMap<>() : JSON.parseObject(event.getImpactJson()));
            payload.put("strategyType", event.getStrategyType());
            payload.put("sourcePlan", buildPlanContext(sourcePlan));
            payload.put("newPlan", buildPlanContext(newPlan));
            payload.put("kpiCompare", apsSchedulePlanService.compareReschedulePlan(event.getNewPlanId()));
            payload.put("semiconductorContext", buildSemiconductorContext());
            payload.put("planStatus", newPlan == null ? null : newPlan.getPlanStatus());
            return pythonScheduleClient.explainReschedule(payload);
        }
        catch (Exception e)
        {
            return fallback;
        }
    }

    private List<ApsRouteOperation> selectRouteOperations(Long productId)
    {
        ApsRouteOperation query = new ApsRouteOperation();
        query.setProductId(productId);
        return apsRouteOperationMapper.selectApsRouteOperationList(query).stream()
                .filter(item -> ("0".equals(item.getDelFlag()) || item.getDelFlag() == null)
                        && ("0".equals(item.getStatus()) || item.getStatus() == null))
                .collect(Collectors.toList());
    }

    private Set<Long> parseLongSet(Object value)
    {
        Set<Long> result = new LinkedHashSet<>();
        if (value instanceof JSONArray)
        {
            JSONArray items = (JSONArray) value;
            for (Object item : items)
            {
                addLongValue(result, item);
            }
        }
        else if (value instanceof List)
        {
            for (Object item : (List<?>) value)
            {
                addLongValue(result, item);
            }
        }
        return result;
    }

    private void addLongValue(Set<Long> result, Object value)
    {
        if (value instanceof Number)
        {
            result.add(((Number) value).longValue());
        }
        else if (value != null)
        {
            result.add(Long.valueOf(value.toString()));
        }
    }

    private List<Map<String, Object>> buildInsertTasks(ApsOrder insertOrder)
    {
        List<Map<String, Object>> insertTasks = new ArrayList<>();
        for (ApsRouteOperation operation : selectRouteOperations(insertOrder.getProductId()))
        {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("orderId", insertOrder.getOrderId());
            task.put("orderCode", insertOrder.getOrderCode());
            task.put("productId", insertOrder.getProductId());
            task.put("productCode", insertOrder.getProductCode());
            task.put("processSeq", operation.getProcessSeq());
            task.put("processCode", operation.getProcessCode());
            task.put("processName", operation.getProcessName());
            task.put("equipmentGroupId", operation.getEquipmentGroupId());
            task.put("standardDuration", operation.getStandardDuration());
            insertTasks.add(task);
        }
        return insertTasks;
    }

    private Map<String, Object> buildStrategy(String impactLevel, long frozenTaskCount,
            long adjustableTaskCount, int insertTaskCount)
    {
        String strategyType;
        String strategyName;
        String recommendedReason;

        if ("LOW".equals(impactLevel))
        {
            strategyType = "RIGHT_SHIFT";
            strategyName = "右移重调度";
            recommendedReason = "当前插单影响等级为 LOW，影响范围较小，可优先保持原加工序列，仅对受影响任务整体后移。";
        }
        else if ("MEDIUM".equals(impactLevel))
        {
            strategyType = "LOCAL_RESCHEDULE";
            strategyName = "局部重调度";
            recommendedReason = "当前插单影响等级为 MEDIUM，建议仅重排受影响任务和插单任务，未受影响任务保持不变。";
        }
        else
        {
            strategyType = "LOCAL_RESCHEDULE_WITH_INSERT_PRIORITY";
            strategyName = "插单优先的局部重调度";
            recommendedReason = "当前插单影响等级为 HIGH，受影响任务较多，但 MVP 阶段采用插单优先的局部重调度，以避免全局重排带来的计划波动。";
        }

        Map<String, Object> strategy = new LinkedHashMap<>();
        strategy.put("strategyType", strategyType);
        strategy.put("strategyName", strategyName);
        strategy.put("freezePolicy", "未受影响任务保持不变，受影响任务进入可调整集合");
        strategy.put("insertPolicy", "插单订单工序加入可调整集合，并按高优先级参与排序");
        strategy.put("objective", "优先保证插单订单交期，同时尽量减少原计划变更");
        strategy.put("frozenTaskCount", frozenTaskCount);
        strategy.put("adjustableTaskCount", adjustableTaskCount);
        strategy.put("insertTaskCount", insertTaskCount);
        strategy.put("recommendedReason", recommendedReason);
        return strategy;
    }

    private List<ApsRouteOperation> selectEnabledRouteOperations()
    {
        return apsRouteOperationMapper.selectApsRouteOperationList(new ApsRouteOperation()).stream()
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

    private ScheduleTaskDTO toScheduleTaskDTO(ApsScheduleTask task, boolean frozen)
    {
        ScheduleTaskDTO dto = new ScheduleTaskDTO();
        dto.setOrderId(task.getOrderId());
        dto.setOrderCode(task.getOrderCode());
        dto.setProductId(task.getProductId());
        dto.setProductCode(task.getProductCode());
        dto.setProcessSeq(task.getProcessSeq());
        dto.setProcessCode(task.getProcessCode());
        dto.setProcessName(task.getProcessName());
        dto.setEquipmentGroupId(task.getEquipmentGroupId());
        dto.setEquipmentId(task.getEquipmentId());
        dto.setEquipmentCode(task.getEquipmentCode());
        dto.setPlannedStartTime(formatDate(task.getPlannedStartTime()));
        dto.setPlannedEndTime(formatDate(task.getPlannedEndTime()));
        dto.setDuration(task.getDuration());
        dto.setIsFrozen(frozen ? "Y" : "N");
        dto.setIsInserted("N");
        dto.setIsChanged(frozen ? "N" : "Y");
        dto.setSourceTaskId(task.getTaskId());
        dto.setOriginalStartTime(formatDate(task.getPlannedStartTime()));
        dto.setOriginalEndTime(formatDate(task.getPlannedEndTime()));
        dto.setOriginalEquipmentId(task.getEquipmentId());
        return dto;
    }

    private OrderDTO toOrderDTO(ApsOrder order)
    {
        OrderDTO dto = new OrderDTO();
        dto.setOrderId(order.getOrderId());
        dto.setOrderCode(order.getOrderCode());
        dto.setOrderType(order.getOrderType());
        dto.setProductId(order.getProductId());
        dto.setProductCode(order.getProductCode());
        dto.setQuantity(order.getQuantity());
        dto.setPriorityLevel(order.getPriorityLevel());
        dto.setReleaseTime(formatDate(order.getReleaseTime()));
        dto.setDueTime(formatDate(order.getDueTime()));
        dto.setOrderStatus(order.getOrderStatus());
        return dto;
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

    private ApsSchedulePlan buildReschedulePlan(ApsSchedulePlan sourcePlan, ApsInsertEvent event,
            LocalRescheduleResponse response, Date scheduleStartTime, String operatorName)
    {
        Date scheduleEndTime = response.getTaskSchedules().stream()
                .map(ScheduleTaskDTO::getPlannedEndTime)
                .map(DateUtils::parseDate)
                .filter(item -> item != null)
                .max(Date::compareTo)
                .orElse(scheduleStartTime);
        ApsSchedulePlan plan = new ApsSchedulePlan();
        plan.setPlanCode("RESCHEDULE-" + DateUtils.dateTimeNow());
        plan.setPlanName("插单局部重调度方案");
        plan.setPlanType("RESCHEDULE");
        plan.setPlanStatus("PENDING");
        plan.setSourcePlanId(sourcePlan.getPlanId());
        plan.setEventId(event.getEventId());
        plan.setAlgorithmName(response.getAlgorithmName());
        plan.setStrategyType(response.getStrategyType());
        plan.setScheduleStartTime(scheduleStartTime);
        plan.setScheduleEndTime(scheduleEndTime);
        plan.setKpiJson(JSON.toJSONString(response.getKpi()));
        plan.setActiveFlag("N");
        plan.setDelFlag("0");
        plan.setCreateBy(operatorName);
        return plan;
    }

    private ApsScheduleTask buildRescheduleTask(Long planId, ScheduleTaskDTO taskDTO, String operatorName)
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
        task.setIsFrozen(defaultFlag(taskDTO.getIsFrozen(), "N"));
        task.setIsInserted(defaultFlag(taskDTO.getIsInserted(), "N"));
        task.setIsChanged(defaultFlag(taskDTO.getIsChanged(), "N"));
        task.setSourceTaskId(taskDTO.getSourceTaskId());
        task.setOriginalStartTime(DateUtils.parseDate(taskDTO.getOriginalStartTime()));
        task.setOriginalEndTime(DateUtils.parseDate(taskDTO.getOriginalEndTime()));
        task.setOriginalEquipmentId(taskDTO.getOriginalEquipmentId());
        task.setDelFlag("0");
        task.setCreateBy(operatorName);
        return task;
    }

    private String defaultFlag(String value, String defaultValue)
    {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private String normalizeAlgorithmType(String algorithmType)
    {
        if (algorithmType == null)
        {
            return "RULE";
        }
        String value = algorithmType.trim().toUpperCase();
        return "GA".equals(value) ? "GA" : "RULE";
    }

    private String calculateImpactLevel(int affectedTaskCount)
    {
        if (affectedTaskCount <= 3)
        {
            return "LOW";
        }
        if (affectedTaskCount <= 8)
        {
            return "MEDIUM";
        }
        return "HIGH";
    }

    private Map<String, Object> buildImpactJson(ApsOrder insertOrder,
            Set<Long> equipmentGroupIds, Set<Long> affectedEquipmentIds,
            Set<Long> affectedOrderIds, Set<Long> affectedTaskIds,
            int affectedTaskCount, String impactLevel)
    {
        Map<String, Object> impact = new LinkedHashMap<>();
        impact.put("impactLevel", impactLevel);
        impact.put("windowStart", formatDate(insertOrder.getReleaseTime()));
        impact.put("windowEnd", formatDate(insertOrder.getDueTime()));
        impact.put("insertOrder", buildInsertOrderJson(insertOrder));
        impact.put("affectedEquipmentGroupIds", new ArrayList<>(equipmentGroupIds));
        impact.put("affectedEquipmentIds", new ArrayList<>(affectedEquipmentIds));
        impact.put("affectedOrderIds", new ArrayList<>(affectedOrderIds));
        impact.put("affectedTaskIds", new ArrayList<>(affectedTaskIds));
        impact.put("affectedTaskCount", affectedTaskCount);
        impact.put("affectedOrderCount", affectedOrderIds.size());
        impact.put("affectedEquipmentCount", affectedEquipmentIds.size());
        impact.put("reason", "插单产品工艺路线需要占用相关设备组，当前方案中这些设备组在插单释放时间之后存在未冻结任务，因此判定为受影响任务。");
        return impact;
    }

    private Map<String, Object> buildInsertOrderJson(ApsOrder insertOrder)
    {
        Map<String, Object> order = new LinkedHashMap<>();
        order.put("orderId", insertOrder.getOrderId());
        order.put("orderCode", insertOrder.getOrderCode());
        order.put("productCode", insertOrder.getProductCode());
        order.put("priorityLevel", insertOrder.getPriorityLevel());
        order.put("releaseTime", formatDate(insertOrder.getReleaseTime()));
        order.put("dueTime", formatDate(insertOrder.getDueTime()));
        return order;
    }

    private Map<String, Object> buildAiFallback(Long eventId, String operatorName)
    {
        Map<String, Object> ruleReport = generateExplanationReport(eventId, operatorName);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("eventSummary", ruleReport.get("eventSummary"));
        result.put("impactAnalysis", ruleReport.get("impactSummary"));
        result.put("strategyExplanation", ruleReport.get("strategySummary"));
        result.put("kpiInterpretation", ruleReport.get("kpiSummary"));
        result.put("riskWarning", ruleReport.get("cost"));
        result.put("recommendation", ruleReport.get("recommendation"));
        result.put("fullReport", ruleReport.get("fullReport"));
        result.put("modelName", "java-fallback-template");
        result.put("fallbackUsed", true);
        return result;
    }

    private Map<String, Object> buildEventContext(ApsInsertEvent event)
    {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("eventId", event.getEventId());
        context.put("eventCode", event.getEventCode());
        context.put("insertOrderId", event.getInsertOrderId());
        context.put("sourcePlanId", event.getSourcePlanId());
        context.put("newPlanId", event.getNewPlanId());
        context.put("eventStatus", event.getEventStatus());
        context.put("eventTime", formatDate(event.getEventTime()));
        context.put("strategyType", event.getStrategyType());
        return context;
    }

    private Map<String, Object> buildOrderContext(ApsOrder order)
    {
        Map<String, Object> context = new LinkedHashMap<>();
        if (order == null)
        {
            return context;
        }
        context.put("orderId", order.getOrderId());
        context.put("orderCode", order.getOrderCode());
        context.put("orderType", order.getOrderType());
        context.put("productId", order.getProductId());
        context.put("productCode", order.getProductCode());
        context.put("quantity", order.getQuantity());
        context.put("priorityLevel", order.getPriorityLevel());
        context.put("releaseTime", formatDate(order.getReleaseTime()));
        context.put("dueTime", formatDate(order.getDueTime()));
        context.put("orderStatus", order.getOrderStatus());
        return context;
    }

    private Map<String, Object> buildPlanContext(ApsSchedulePlan plan)
    {
        Map<String, Object> context = new LinkedHashMap<>();
        if (plan == null)
        {
            return context;
        }
        context.put("planId", plan.getPlanId());
        context.put("planCode", plan.getPlanCode());
        context.put("planName", plan.getPlanName());
        context.put("planType", plan.getPlanType());
        context.put("planStatus", plan.getPlanStatus());
        context.put("sourcePlanId", plan.getSourcePlanId());
        context.put("eventId", plan.getEventId());
        context.put("algorithmName", plan.getAlgorithmName());
        context.put("strategyType", plan.getStrategyType());
        context.put("kpiJson", plan.getKpiJson());
        context.put("activeFlag", plan.getActiveFlag());
        return context;
    }

    private Map<String, Object> buildSemiconductorContext()
    {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("domain", "semiconductor reentrant hot lot local rescheduling");
        context.put("orderMapping", "aps_order is interpreted as lot / wafer lot.");
        context.put("routeMapping", "aps_route_operation is interpreted as route step.");
        context.put("equipmentGroupMapping", "equipment_group is interpreted as tool group.");
        context.put("equipmentMapping", "equipment is interpreted as tool.");
        context.put("taskMapping", "schedule_task is interpreted as lot-step task.");
        context.put("reentrantFeature", "The same product route can visit the same tool group multiple times, e.g. PHOTO-L1, PHOTO-L2 and PHOTO-L3.");
        context.put("agentBoundary", "The model must explain only. It must not generate schedules, task times, equipment assignments, or plan status decisions.");
        return context;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> source, String key)
    {
        Object value = source.get(key);
        return value instanceof Map ? (Map<String, Object>) value : new LinkedHashMap<>();
    }

    private long getLong(Map<String, Object> source, String key)
    {
        Object value = source.get(key);
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    private double getDouble(Map<String, Object> source, String key)
    {
        Object value = source.get(key);
        if (value instanceof Number)
        {
            return ((Number) value).doubleValue();
        }
        return value == null ? 0D : Double.parseDouble(value.toString());
    }

    private String defaultText(String value, String defaultValue)
    {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private String buildEventSummary(ApsOrder insertOrder, ApsInsertEvent event)
    {
        if (insertOrder == null)
        {
            return "本次插单事件 " + event.getEventCode() + " 未找到对应插单订单。";
        }
        return "本次插单事件涉及订单 " + insertOrder.getOrderCode()
                + "，产品 " + insertOrder.getProductCode()
                + "，释放时间 " + formatDate(insertOrder.getReleaseTime())
                + "，交期 " + formatDate(insertOrder.getDueTime())
                + "，优先级 " + insertOrder.getPriorityLevel() + "。";
    }

    private String buildBenefit(long insertDelayMinutes, long delayOrderCountDiff, long totalDelayMinutesDiff)
    {
        List<String> benefits = new ArrayList<>();
        if (insertDelayMinutes == 0)
        {
            benefits.add("插单订单可按期完成");
        }
        if (delayOrderCountDiff <= 0)
        {
            benefits.add("未增加延期订单数");
        }
        if (totalDelayMinutesDiff <= 0)
        {
            benefits.add("未增加总延期时间");
        }
        return benefits.isEmpty() ? "新方案未表现出明显 KPI 收益。" : String.join("，", benefits) + "。";
    }

    private String buildCost(long makespanDiffMinutes, double changedTaskRatio, long changedTaskCount)
    {
        List<String> costs = new ArrayList<>();
        if (makespanDiffMinutes > 0)
        {
            costs.add("整体完工时间增加 " + makespanDiffMinutes + " 分钟");
        }
        if (changedTaskRatio > 0.5D)
        {
            costs.add("变更任务比例为 " + changedTaskRatio + "，计划扰动较大");
        }
        else if (changedTaskCount > 0)
        {
            costs.add("存在 " + changedTaskCount + " 个任务发生调整");
        }
        return costs.isEmpty() ? "当前未识别出明显方案代价。" : String.join("，", costs) + "。";
    }

    private String buildExplanationRecommendation(long insertDelayMinutes, long newDelayOrderCount,
            long originalDelayOrderCount, long makespanDiffMinutes, double changedTaskRatio)
    {
        if (insertDelayMinutes > 0)
        {
            return "谨慎采用。插单订单仍存在延期，建议调度员结合交期承诺和现场资源再确认。";
        }
        if (newDelayOrderCount > originalDelayOrderCount)
        {
            return "谨慎采用。新方案导致更多订单延期，需要评估对原有订单承诺的影响。";
        }
        if (makespanDiffMinutes > 0 || changedTaskRatio > 0.5D)
        {
            return "建议采用，但需关注计划扰动。新方案保障插单订单按期完成，但整体完工时间或任务变更比例有所增加。";
        }
        return "建议采用。新方案保障插单订单按期完成，且未增加延期订单数。";
    }

    private String formatDate(Date date)
    {
        return date == null ? null : DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, date);
    }
}
