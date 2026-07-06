package com.ruoyi.aps.client.dto;

import java.util.List;
import java.util.Map;

public class LocalRescheduleRequest
{
    private String requestId;
    private Long eventId;
    private Long sourcePlanId;
    private String scheduleStartTime;
    private OrderDTO insertOrder;
    private List<ScheduleTaskDTO> frozenTasks;
    private List<ScheduleTaskDTO> adjustableTasks;
    private List<Map<String, Object>> insertTasks;
    private List<EquipmentDTO> equipments;
    private List<RouteOperationDTO> routeOperations;
    private Map<String, Object> strategyConfig;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public Long getSourcePlanId() { return sourcePlanId; }
    public void setSourcePlanId(Long sourcePlanId) { this.sourcePlanId = sourcePlanId; }
    public String getScheduleStartTime() { return scheduleStartTime; }
    public void setScheduleStartTime(String scheduleStartTime) { this.scheduleStartTime = scheduleStartTime; }
    public OrderDTO getInsertOrder() { return insertOrder; }
    public void setInsertOrder(OrderDTO insertOrder) { this.insertOrder = insertOrder; }
    public List<ScheduleTaskDTO> getFrozenTasks() { return frozenTasks; }
    public void setFrozenTasks(List<ScheduleTaskDTO> frozenTasks) { this.frozenTasks = frozenTasks; }
    public List<ScheduleTaskDTO> getAdjustableTasks() { return adjustableTasks; }
    public void setAdjustableTasks(List<ScheduleTaskDTO> adjustableTasks) { this.adjustableTasks = adjustableTasks; }
    public List<Map<String, Object>> getInsertTasks() { return insertTasks; }
    public void setInsertTasks(List<Map<String, Object>> insertTasks) { this.insertTasks = insertTasks; }
    public List<EquipmentDTO> getEquipments() { return equipments; }
    public void setEquipments(List<EquipmentDTO> equipments) { this.equipments = equipments; }
    public List<RouteOperationDTO> getRouteOperations() { return routeOperations; }
    public void setRouteOperations(List<RouteOperationDTO> routeOperations) { this.routeOperations = routeOperations; }
    public Map<String, Object> getStrategyConfig() { return strategyConfig; }
    public void setStrategyConfig(Map<String, Object> strategyConfig) { this.strategyConfig = strategyConfig; }
}
