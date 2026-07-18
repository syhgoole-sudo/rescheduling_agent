package com.ruoyi.aps.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 调度方案对象 aps_schedule_plan
 *
 * @author aps
 */
public class ApsSchedulePlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 调度方案ID */
    @Excel(name = "调度方案ID")
    private Long planId;

    /** 调度方案编码 */
    @Excel(name = "调度方案编码")
    private String planCode;

    /** 调度方案名称 */
    @Excel(name = "调度方案名称")
    private String planName;

    /** 方案类型：INITIAL初始方案，RESCHEDULE重调度方案 */
    @Excel(name = "方案类型：INITIAL初始方案，RESCHEDULE重调度方案")
    private String planType;

    /** 方案状态：PENDING待确认，ACTIVE当前有效，HISTORY历史，REJECTED拒绝，FAILED失败 */
    @Excel(name = "方案状态：PENDING待确认，ACTIVE当前有效，HISTORY历史，REJECTED拒绝，FAILED失败")
    private String planStatus;

    /** 来源方案ID，重调度方案对应的原方案 */
    @Excel(name = "来源方案ID，重调度方案对应的原方案")
    private Long sourcePlanId;

    /** 插单事件ID */
    @Excel(name = "插单事件ID")
    private Long eventId;

    /** 算法名称 */
    @Excel(name = "算法名称")
    private String algorithmName;

    /** 策略类型 */
    @Excel(name = "策略类型")
    private String strategyType;

    /** 调度开始时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "调度开始时间")
    private Date scheduleStartTime;

    /** 调度结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "调度结束时间")
    private Date scheduleEndTime;

    /** KPI结果JSON */
    private String kpiJson;

    /** AI解释文本 */
    private String aiExplanation;

    /** 是否当前有效：Y是，N否 */
    @Excel(name = "是否当前有效：Y是，N否")
    private String activeFlag;

    /** 删除标志：0存在，2删除 */
    @Excel(name = "删除标志：0存在，2删除")
    private String delFlag;

    /** 查询上下文中的产品ID，不表示方案归属于该产品 */
    private Long productId;

    /** 方案全部产品任务总数 */
    private Long totalTaskCount;

    /** 查询上下文中当前产品的任务数 */
    private Long currentProductTaskCount;

    /** 方案任务涉及的产品数 */
    private Long participatingProductCount;

    /** 方案范围固定为全局调度 */
    private String planScope;

    public void setPlanId(Long planId)
    {
        this.planId = planId;
    }

    public Long getPlanId()
    {
        return planId;
    }

    public void setPlanCode(String planCode)
    {
        this.planCode = planCode;
    }

    public String getPlanCode()
    {
        return planCode;
    }

    public void setPlanName(String planName)
    {
        this.planName = planName;
    }

    public String getPlanName()
    {
        return planName;
    }

    public void setPlanType(String planType)
    {
        this.planType = planType;
    }

    public String getPlanType()
    {
        return planType;
    }

    public void setPlanStatus(String planStatus)
    {
        this.planStatus = planStatus;
    }

    public String getPlanStatus()
    {
        return planStatus;
    }

    public void setSourcePlanId(Long sourcePlanId)
    {
        this.sourcePlanId = sourcePlanId;
    }

    public Long getSourcePlanId()
    {
        return sourcePlanId;
    }

    public void setEventId(Long eventId)
    {
        this.eventId = eventId;
    }

    public Long getEventId()
    {
        return eventId;
    }

    public void setAlgorithmName(String algorithmName)
    {
        this.algorithmName = algorithmName;
    }

    public String getAlgorithmName()
    {
        return algorithmName;
    }

    public void setStrategyType(String strategyType)
    {
        this.strategyType = strategyType;
    }

    public String getStrategyType()
    {
        return strategyType;
    }

    public void setScheduleStartTime(Date scheduleStartTime)
    {
        this.scheduleStartTime = scheduleStartTime;
    }

    public Date getScheduleStartTime()
    {
        return scheduleStartTime;
    }

    public void setScheduleEndTime(Date scheduleEndTime)
    {
        this.scheduleEndTime = scheduleEndTime;
    }

    public Date getScheduleEndTime()
    {
        return scheduleEndTime;
    }

    public void setKpiJson(String kpiJson)
    {
        this.kpiJson = kpiJson;
    }

    public String getKpiJson()
    {
        return kpiJson;
    }

    public void setAiExplanation(String aiExplanation)
    {
        this.aiExplanation = aiExplanation;
    }

    public String getAiExplanation()
    {
        return aiExplanation;
    }

    public void setActiveFlag(String activeFlag)
    {
        this.activeFlag = activeFlag;
    }

    public String getActiveFlag()
    {
        return activeFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getTotalTaskCount()
    {
        return totalTaskCount;
    }

    public void setTotalTaskCount(Long totalTaskCount)
    {
        this.totalTaskCount = totalTaskCount;
    }

    public Long getCurrentProductTaskCount()
    {
        return currentProductTaskCount;
    }

    public void setCurrentProductTaskCount(Long currentProductTaskCount)
    {
        this.currentProductTaskCount = currentProductTaskCount;
    }

    public Long getParticipatingProductCount()
    {
        return participatingProductCount;
    }

    public void setParticipatingProductCount(Long participatingProductCount)
    {
        this.participatingProductCount = participatingProductCount;
    }

    public String getPlanScope()
    {
        return planScope;
    }

    public void setPlanScope(String planScope)
    {
        this.planScope = planScope;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("planId", getPlanId())
            .append("planCode", getPlanCode())
            .append("planName", getPlanName())
            .append("planType", getPlanType())
            .append("planStatus", getPlanStatus())
            .append("sourcePlanId", getSourcePlanId())
            .append("eventId", getEventId())
            .append("algorithmName", getAlgorithmName())
            .append("strategyType", getStrategyType())
            .append("scheduleStartTime", getScheduleStartTime())
            .append("scheduleEndTime", getScheduleEndTime())
            .append("kpiJson", getKpiJson())
            .append("aiExplanation", getAiExplanation())
            .append("activeFlag", getActiveFlag())
            .append("delFlag", getDelFlag())
            .append("productId", getProductId())
            .append("totalTaskCount", getTotalTaskCount())
            .append("currentProductTaskCount", getCurrentProductTaskCount())
            .append("participatingProductCount", getParticipatingProductCount())
            .append("planScope", getPlanScope())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
