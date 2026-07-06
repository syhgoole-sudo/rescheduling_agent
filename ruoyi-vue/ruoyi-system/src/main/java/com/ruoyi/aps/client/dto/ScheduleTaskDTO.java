package com.ruoyi.aps.client.dto;

public class ScheduleTaskDTO
{
    private Long orderId;
    private String orderCode;
    private Long productId;
    private String productCode;
    private Integer processSeq;
    private String processCode;
    private String processName;
    private Long equipmentGroupId;
    private Long equipmentId;
    private String equipmentCode;
    private String plannedStartTime;
    private String plannedEndTime;
    private Integer duration;
    private String isFrozen;
    private String isInserted;
    private String isChanged;
    private Long sourceTaskId;
    private String originalStartTime;
    private String originalEndTime;
    private Long originalEquipmentId;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public Integer getProcessSeq() { return processSeq; }
    public void setProcessSeq(Integer processSeq) { this.processSeq = processSeq; }
    public String getProcessCode() { return processCode; }
    public void setProcessCode(String processCode) { this.processCode = processCode; }
    public String getProcessName() { return processName; }
    public void setProcessName(String processName) { this.processName = processName; }
    public Long getEquipmentGroupId() { return equipmentGroupId; }
    public void setEquipmentGroupId(Long equipmentGroupId) { this.equipmentGroupId = equipmentGroupId; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getPlannedStartTime() { return plannedStartTime; }
    public void setPlannedStartTime(String plannedStartTime) { this.plannedStartTime = plannedStartTime; }
    public String getPlannedEndTime() { return plannedEndTime; }
    public void setPlannedEndTime(String plannedEndTime) { this.plannedEndTime = plannedEndTime; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getIsFrozen() { return isFrozen; }
    public void setIsFrozen(String isFrozen) { this.isFrozen = isFrozen; }
    public String getIsInserted() { return isInserted; }
    public void setIsInserted(String isInserted) { this.isInserted = isInserted; }
    public String getIsChanged() { return isChanged; }
    public void setIsChanged(String isChanged) { this.isChanged = isChanged; }
    public Long getSourceTaskId() { return sourceTaskId; }
    public void setSourceTaskId(Long sourceTaskId) { this.sourceTaskId = sourceTaskId; }
    public String getOriginalStartTime() { return originalStartTime; }
    public void setOriginalStartTime(String originalStartTime) { this.originalStartTime = originalStartTime; }
    public String getOriginalEndTime() { return originalEndTime; }
    public void setOriginalEndTime(String originalEndTime) { this.originalEndTime = originalEndTime; }
    public Long getOriginalEquipmentId() { return originalEquipmentId; }
    public void setOriginalEquipmentId(Long originalEquipmentId) { this.originalEquipmentId = originalEquipmentId; }
}
