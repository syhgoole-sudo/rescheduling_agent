package com.ruoyi.aps.client.dto;

import java.util.List;
import java.util.Map;

public class InitialScheduleRequest
{
    private String requestId;
    private String scheduleStartTime;
    private List<OrderDTO> orders;
    private List<RouteOperationDTO> routeOperations;
    private List<EquipmentGroupDTO> equipmentGroups;
    private List<EquipmentDTO> equipments;
    private Map<String, Object> algorithmConfig;

    public String getRequestId()
    {
        return requestId;
    }

    public void setRequestId(String requestId)
    {
        this.requestId = requestId;
    }

    public String getScheduleStartTime()
    {
        return scheduleStartTime;
    }

    public void setScheduleStartTime(String scheduleStartTime)
    {
        this.scheduleStartTime = scheduleStartTime;
    }

    public List<OrderDTO> getOrders()
    {
        return orders;
    }

    public void setOrders(List<OrderDTO> orders)
    {
        this.orders = orders;
    }

    public List<RouteOperationDTO> getRouteOperations()
    {
        return routeOperations;
    }

    public void setRouteOperations(List<RouteOperationDTO> routeOperations)
    {
        this.routeOperations = routeOperations;
    }

    public List<EquipmentGroupDTO> getEquipmentGroups()
    {
        return equipmentGroups;
    }

    public void setEquipmentGroups(List<EquipmentGroupDTO> equipmentGroups)
    {
        this.equipmentGroups = equipmentGroups;
    }

    public List<EquipmentDTO> getEquipments()
    {
        return equipments;
    }

    public void setEquipments(List<EquipmentDTO> equipments)
    {
        this.equipments = equipments;
    }

    public Map<String, Object> getAlgorithmConfig()
    {
        return algorithmConfig;
    }

    public void setAlgorithmConfig(Map<String, Object> algorithmConfig)
    {
        this.algorithmConfig = algorithmConfig;
    }
}
