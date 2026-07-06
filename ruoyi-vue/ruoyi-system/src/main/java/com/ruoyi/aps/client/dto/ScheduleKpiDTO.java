package com.ruoyi.aps.client.dto;

public class ScheduleKpiDTO
{
    private Integer makespan;
    private Integer delayOrderCount;
    private Integer totalDelayMinutes;
    private Integer maxDelayMinutes;
    private Integer scheduledOrderCount;
    private Integer scheduledTaskCount;

    public Integer getMakespan() { return makespan; }
    public void setMakespan(Integer makespan) { this.makespan = makespan; }
    public Integer getDelayOrderCount() { return delayOrderCount; }
    public void setDelayOrderCount(Integer delayOrderCount) { this.delayOrderCount = delayOrderCount; }
    public Integer getTotalDelayMinutes() { return totalDelayMinutes; }
    public void setTotalDelayMinutes(Integer totalDelayMinutes) { this.totalDelayMinutes = totalDelayMinutes; }
    public Integer getMaxDelayMinutes() { return maxDelayMinutes; }
    public void setMaxDelayMinutes(Integer maxDelayMinutes) { this.maxDelayMinutes = maxDelayMinutes; }
    public Integer getScheduledOrderCount() { return scheduledOrderCount; }
    public void setScheduledOrderCount(Integer scheduledOrderCount) { this.scheduledOrderCount = scheduledOrderCount; }
    public Integer getScheduledTaskCount() { return scheduledTaskCount; }
    public void setScheduledTaskCount(Integer scheduledTaskCount) { this.scheduledTaskCount = scheduledTaskCount; }
}
