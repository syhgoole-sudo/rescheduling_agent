package com.ruoyi.aps.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.aps.domain.ApsInsertEvent;

/**
 * 插单事件Service接口
 *
 * @author aps
 */
public interface IApsInsertEventService
{
    public ApsInsertEvent selectApsInsertEventById(Long eventId);

    public List<ApsInsertEvent> selectApsInsertEventList(ApsInsertEvent apsInsertEvent);

    public int insertApsInsertEvent(ApsInsertEvent apsInsertEvent);

    public int updateApsInsertEvent(ApsInsertEvent apsInsertEvent);

    public int deleteApsInsertEventByIds(Long[] eventIds);

    public int deleteApsInsertEventById(Long eventId);

    public Map<String, Object> createAndAnalyzeInsertEvent(Long insertOrderId, String operatorName);

    public Map<String, Object> recommendStrategy(Long eventId, String operatorName);

    public Map<String, Object> generateLocalReschedule(Long eventId, String operatorName);

    public Map<String, Object> generateLocalReschedule(Long eventId, String operatorName, String algorithmType);

    public Map<String, Object> generateExplanationReport(Long eventId, String operatorName);

    public Map<String, Object> generateAiExplanationReport(Long eventId, String operatorName);
}
