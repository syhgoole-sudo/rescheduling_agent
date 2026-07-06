package com.ruoyi.aps.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 调度任务对象 aps_schedule_task
 *
 * @author aps
 */
public class ApsScheduleTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 调度任务ID */
    @Excel(name = "调度任务ID")
    private Long taskId;

    /** 调度方案ID */
    @Excel(name = "调度方案ID")
    private Long planId;

    /** 订单ID */
    @Excel(name = "订单ID")
    private Long orderId;

    /** 订单编码 */
    @Excel(name = "订单编码")
    private String orderCode;

    /** 产品ID */
    @Excel(name = "产品ID")
    private Long productId;

    /** 产品编码 */
    @Excel(name = "产品编码")
    private String productCode;

    /** 工序顺序号 */
    @Excel(name = "工序顺序号")
    private Integer processSeq;

    /** 工序编码 */
    @Excel(name = "工序编码")
    private String processCode;

    /** 工序名称 */
    @Excel(name = "工序名称")
    private String processName;

    /** 设备组ID */
    @Excel(name = "设备组ID")
    private Long equipmentGroupId;

    /** 设备ID */
    @Excel(name = "设备ID")
    private Long equipmentId;

    /** 设备编码 */
    @Excel(name = "设备编码")
    private String equipmentCode;

    /** 计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "计划开始时间")
    private Date plannedStartTime;

    /** 计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "计划结束时间")
    private Date plannedEndTime;

    /** 加工时长，单位分钟 */
    @Excel(name = "加工时长，单位分钟")
    private Integer duration;

    /** 任务状态：PLANNED已计划，FROZEN冻结 */
    @Excel(name = "任务状态：PLANNED已计划，FROZEN冻结")
    private String taskStatus;

    /** 是否冻结：Y是，N否 */
    @Excel(name = "是否冻结：Y是，N否")
    private String isFrozen;

    /** 是否插单任务：Y是，N否 */
    @Excel(name = "是否插单任务：Y是，N否")
    private String isInserted;

    /** 相对来源方案是否变更：Y是，N否 */
    @Excel(name = "相对来源方案是否变更：Y是，N否")
    private String isChanged;

    /** 来源任务ID，用于重调度追溯 */
    @Excel(name = "来源任务ID，用于重调度追溯")
    private Long sourceTaskId;

    /** 原计划开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "原计划开始时间")
    private Date originalStartTime;

    /** 原计划结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "原计划结束时间")
    private Date originalEndTime;

    /** 原设备ID */
    @Excel(name = "原设备ID")
    private Long originalEquipmentId;

    /** 删除标志：0存在，2删除 */
    @Excel(name = "删除标志：0存在，2删除")
    private String delFlag;

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public Long getPlanId()
    {
        return planId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderCode(String orderCode)
    {
        this.orderCode = orderCode;
    }

    public String getOrderCode()
    {
        return orderCode;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductCode(String productCode)
    {
        this.productCode = productCode;
    }

    public String getProductCode()
    {
        return productCode;
    }

    public void setProcessSeq(Integer processSeq)
    {
        this.processSeq = processSeq;
    }

    public Integer getProcessSeq()
    {
        return processSeq;
    }

    public void setProcessCode(String processCode)
    {
        this.processCode = processCode;
    }

    public String getProcessCode()
    {
        return processCode;
    }

    public void setProcessName(String processName)
    {
        this.processName = processName;
    }

    public String getProcessName()
    {
        return processName;
    }

    public void setEquipmentGroupId(Long equipmentGroupId)
    {
        this.equipmentGroupId = equipmentGroupId;
    }

    public Long getEquipmentGroupId()
    {
        return equipmentGroupId;
    }

    public void setEquipmentId(Long equipmentId)
    {
        this.equipmentId = equipmentId;
    }

    public Long getEquipmentId()
    {
        return equipmentId;
    }

    public void setEquipmentCode(String equipmentCode)
    {
        this.equipmentCode = equipmentCode;
    }

    public String getEquipmentCode()
    {
        return equipmentCode;
    }

    public void setPlannedStartTime(Date plannedStartTime)
    {
        this.plannedStartTime = plannedStartTime;
    }

    public Date getPlannedStartTime()
    {
        return plannedStartTime;
    }

    public void setPlannedEndTime(Date plannedEndTime)
    {
        this.plannedEndTime = plannedEndTime;
    }

    public Date getPlannedEndTime()
    {
        return plannedEndTime;
    }

    public void setDuration(Integer duration)
    {
        this.duration = duration;
    }

    public Integer getDuration()
    {
        return duration;
    }

    public void setTaskStatus(String taskStatus)
    {
        this.taskStatus = taskStatus;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public void setIsFrozen(String isFrozen)
    {
        this.isFrozen = isFrozen;
    }

    public String getIsFrozen()
    {
        return isFrozen;
    }

    public void setIsInserted(String isInserted)
    {
        this.isInserted = isInserted;
    }

    public String getIsInserted()
    {
        return isInserted;
    }

    public void setIsChanged(String isChanged)
    {
        this.isChanged = isChanged;
    }

    public String getIsChanged()
    {
        return isChanged;
    }

    public void setSourceTaskId(Long sourceTaskId)
    {
        this.sourceTaskId = sourceTaskId;
    }

    public Long getSourceTaskId()
    {
        return sourceTaskId;
    }

    public void setOriginalStartTime(Date originalStartTime)
    {
        this.originalStartTime = originalStartTime;
    }

    public Date getOriginalStartTime()
    {
        return originalStartTime;
    }

    public void setOriginalEndTime(Date originalEndTime)
    {
        this.originalEndTime = originalEndTime;
    }

    public Date getOriginalEndTime()
    {
        return originalEndTime;
    }

    public void setOriginalEquipmentId(Long originalEquipmentId)
    {
        this.originalEquipmentId = originalEquipmentId;
    }

    public Long getOriginalEquipmentId()
    {
        return originalEquipmentId;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("taskId", getTaskId())
            .append("planId", getPlanId())
            .append("orderId", getOrderId())
            .append("orderCode", getOrderCode())
            .append("productId", getProductId())
            .append("productCode", getProductCode())
            .append("processSeq", getProcessSeq())
            .append("processCode", getProcessCode())
            .append("processName", getProcessName())
            .append("equipmentGroupId", getEquipmentGroupId())
            .append("equipmentId", getEquipmentId())
            .append("equipmentCode", getEquipmentCode())
            .append("plannedStartTime", getPlannedStartTime())
            .append("plannedEndTime", getPlannedEndTime())
            .append("duration", getDuration())
            .append("taskStatus", getTaskStatus())
            .append("isFrozen", getIsFrozen())
            .append("isInserted", getIsInserted())
            .append("isChanged", getIsChanged())
            .append("sourceTaskId", getSourceTaskId())
            .append("originalStartTime", getOriginalStartTime())
            .append("originalEndTime", getOriginalEndTime())
            .append("originalEquipmentId", getOriginalEquipmentId())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
