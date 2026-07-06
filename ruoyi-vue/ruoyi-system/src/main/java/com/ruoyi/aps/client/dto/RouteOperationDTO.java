package com.ruoyi.aps.client.dto;

public class RouteOperationDTO
{
    private Long routeOperationId;
    private Long productId;
    private String productCode;
    private Integer processSeq;
    private String processCode;
    private String processName;
    private Long equipmentGroupId;
    private Integer standardDuration;

    public Long getRouteOperationId() { return routeOperationId; }
    public void setRouteOperationId(Long routeOperationId) { this.routeOperationId = routeOperationId; }
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
    public Integer getStandardDuration() { return standardDuration; }
    public void setStandardDuration(Integer standardDuration) { this.standardDuration = standardDuration; }
}
