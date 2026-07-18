package com.ruoyi.aps.client.dto;

import java.util.List;
import java.util.Map;

public class LocalRescheduleResponse
{
    private String requestId;
    private Boolean success;
    private String message;
    private String algorithmName;
    private Integer randomSeed;
    private String strategyType;
    private List<ScheduleTaskDTO> taskSchedules;
    private Map<String, Object> kpi;
    private List<String> warnings;
    private Long runtimeMs;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    public Integer getRandomSeed() { return randomSeed; }
    public void setRandomSeed(Integer randomSeed) { this.randomSeed = randomSeed; }
    public String getStrategyType() { return strategyType; }
    public void setStrategyType(String strategyType) { this.strategyType = strategyType; }
    public List<ScheduleTaskDTO> getTaskSchedules() { return taskSchedules; }
    public void setTaskSchedules(List<ScheduleTaskDTO> taskSchedules) { this.taskSchedules = taskSchedules; }
    public Map<String, Object> getKpi() { return kpi; }
    public void setKpi(Map<String, Object> kpi) { this.kpi = kpi; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public Long getRuntimeMs() { return runtimeMs; }
    public void setRuntimeMs(Long runtimeMs) { this.runtimeMs = runtimeMs; }
}
