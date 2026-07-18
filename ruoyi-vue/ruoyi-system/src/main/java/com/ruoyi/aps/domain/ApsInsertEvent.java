package com.ruoyi.aps.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 插单事件对象 aps_insert_event
 *
 * @author aps
 */
public class ApsInsertEvent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 插单事件ID */
    @Excel(name = "插单事件ID")
    private Long eventId;

    /** 插单事件编码 */
    @Excel(name = "插单事件编码")
    private String eventCode;

    /** 插单订单ID */
    @Excel(name = "插单订单ID")
    private Long insertOrderId;

    /** 插单发生时的原有效方案ID */
    @Excel(name = "插单发生时的原有效方案ID")
    private Long sourcePlanId;

    /** 重调度生成的新方案ID */
    @Excel(name = "重调度生成的新方案ID")
    private Long newPlanId;

    /** 插单 Lot 编码，仅用于列表上下文展示 */
    private String insertOrderCode;

    /** 插单 Lot 释放时间，仅用于列表上下文展示 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date releaseTime;

    /** 原方案编码，仅用于列表上下文展示 */
    private String sourcePlanCode;

    /** 新方案编码，仅用于列表上下文展示 */
    private String newPlanCode;

    /** 插单 Hot Lot 所属产品ID，作为事件查询上下文 */
    private Long triggerProductId;

    /** 插单 Hot Lot 所属产品编码 */
    private String triggerProductCode;

    /** 事件发生时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "事件发生时间")
    private Date eventTime;

    /** 事件状态：NEW新建，ANALYZED已分析，RESCHEDULED已重调度，CONFIRMED已采用，REJECTED已拒绝 */
    @Excel(name = "事件状态：NEW新建，ANALYZED已分析，RESCHEDULED已重调度，CONFIRMED已采用，REJECTED已拒绝")
    private String eventStatus;

    /** 影响范围分析结果JSON */
    private String impactJson;

    /** 重调度策略类型 */
    @Excel(name = "重调度策略类型")
    private String strategyType;

    /** 插单原因 */
    @Excel(name = "插单原因")
    private String eventReason;

    /** 删除标志：0存在，2删除 */
    @Excel(name = "删除标志：0存在，2删除")
    private String delFlag;

    public void setEventId(Long eventId)
    {
        this.eventId = eventId;
    }

    public Long getEventId()
    {
        return eventId;
    }

    public void setEventCode(String eventCode)
    {
        this.eventCode = eventCode;
    }

    public String getEventCode()
    {
        return eventCode;
    }

    public void setInsertOrderId(Long insertOrderId)
    {
        this.insertOrderId = insertOrderId;
    }

    public Long getInsertOrderId()
    {
        return insertOrderId;
    }

    public void setSourcePlanId(Long sourcePlanId)
    {
        this.sourcePlanId = sourcePlanId;
    }

    public Long getSourcePlanId()
    {
        return sourcePlanId;
    }

    public void setNewPlanId(Long newPlanId)
    {
        this.newPlanId = newPlanId;
    }

    public Long getNewPlanId()
    {
        return newPlanId;
    }

    public String getInsertOrderCode()
    {
        return insertOrderCode;
    }

    public void setInsertOrderCode(String insertOrderCode)
    {
        this.insertOrderCode = insertOrderCode;
    }

    public Date getReleaseTime()
    {
        return releaseTime;
    }

    public void setReleaseTime(Date releaseTime)
    {
        this.releaseTime = releaseTime;
    }

    public String getSourcePlanCode()
    {
        return sourcePlanCode;
    }

    public void setSourcePlanCode(String sourcePlanCode)
    {
        this.sourcePlanCode = sourcePlanCode;
    }

    public String getNewPlanCode()
    {
        return newPlanCode;
    }

    public void setNewPlanCode(String newPlanCode)
    {
        this.newPlanCode = newPlanCode;
    }

    public Long getTriggerProductId()
    {
        return triggerProductId;
    }

    public void setTriggerProductId(Long triggerProductId)
    {
        this.triggerProductId = triggerProductId;
    }

    public String getTriggerProductCode()
    {
        return triggerProductCode;
    }

    public void setTriggerProductCode(String triggerProductCode)
    {
        this.triggerProductCode = triggerProductCode;
    }

    public void setEventTime(Date eventTime)
    {
        this.eventTime = eventTime;
    }

    public Date getEventTime()
    {
        return eventTime;
    }

    public void setEventStatus(String eventStatus)
    {
        this.eventStatus = eventStatus;
    }

    public String getEventStatus()
    {
        return eventStatus;
    }

    public void setImpactJson(String impactJson)
    {
        this.impactJson = impactJson;
    }

    public String getImpactJson()
    {
        return impactJson;
    }

    public void setStrategyType(String strategyType)
    {
        this.strategyType = strategyType;
    }

    public String getStrategyType()
    {
        return strategyType;
    }

    public void setEventReason(String eventReason)
    {
        this.eventReason = eventReason;
    }

    public String getEventReason()
    {
        return eventReason;
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
            .append("eventId", getEventId())
            .append("eventCode", getEventCode())
            .append("insertOrderId", getInsertOrderId())
            .append("sourcePlanId", getSourcePlanId())
            .append("newPlanId", getNewPlanId())
            .append("insertOrderCode", getInsertOrderCode())
            .append("releaseTime", getReleaseTime())
            .append("sourcePlanCode", getSourcePlanCode())
            .append("newPlanCode", getNewPlanCode())
            .append("triggerProductId", getTriggerProductId())
            .append("triggerProductCode", getTriggerProductCode())
            .append("eventTime", getEventTime())
            .append("eventStatus", getEventStatus())
            .append("impactJson", getImpactJson())
            .append("strategyType", getStrategyType())
            .append("eventReason", getEventReason())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
