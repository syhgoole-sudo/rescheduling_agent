package com.ruoyi.aps.mapper;

import java.util.List;
import com.ruoyi.aps.domain.ApsInsertEvent;

/**
 * 插单事件Mapper接口
 *
 * @author aps
 */
public interface ApsInsertEventMapper
{
    public ApsInsertEvent selectApsInsertEventById(Long eventId);

    public List<ApsInsertEvent> selectApsInsertEventList(ApsInsertEvent apsInsertEvent);

    public int insertApsInsertEvent(ApsInsertEvent apsInsertEvent);

    public int updateApsInsertEvent(ApsInsertEvent apsInsertEvent);

    public int countBySourcePlanId(Long sourcePlanId);

    public int countByNewPlanId(Long newPlanId);

    public int deleteApsInsertEventById(Long eventId);

    public int deleteApsInsertEventByIds(Long[] eventIds);
}
