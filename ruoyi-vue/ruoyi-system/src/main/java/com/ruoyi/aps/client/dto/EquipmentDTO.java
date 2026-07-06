package com.ruoyi.aps.client.dto;

public class EquipmentDTO
{
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long equipmentGroupId;
    private String equipmentGroupCode;
    private String status;

    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public String getEquipmentCode() { return equipmentCode; }
    public void setEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public Long getEquipmentGroupId() { return equipmentGroupId; }
    public void setEquipmentGroupId(Long equipmentGroupId) { this.equipmentGroupId = equipmentGroupId; }
    public String getEquipmentGroupCode() { return equipmentGroupCode; }
    public void setEquipmentGroupCode(String equipmentGroupCode) { this.equipmentGroupCode = equipmentGroupCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
