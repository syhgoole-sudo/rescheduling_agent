package com.ruoyi.aps.service.impl;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ruoyi.aps.domain.ApsEquipmentGroup;
import com.ruoyi.aps.domain.ApsInsertEvent;
import com.ruoyi.aps.domain.ApsOrder;
import com.ruoyi.aps.domain.ApsSchedulePlan;
import com.ruoyi.aps.domain.ApsScheduleTask;
import com.ruoyi.aps.mapper.ApsEquipmentGroupMapper;
import com.ruoyi.aps.mapper.ApsInsertEventMapper;
import com.ruoyi.aps.mapper.ApsOrderMapper;
import com.ruoyi.aps.mapper.ApsSchedulePlanMapper;
import com.ruoyi.aps.mapper.ApsScheduleTaskMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;

@ExtendWith(MockitoExtension.class)
class ApsSchedulePlanServiceImplTest
{
    private static final Long PLAN_ID = 101L;
    private static final String OPERATOR = "tester";

    @Mock
    private ApsSchedulePlanMapper planMapper;

    @Mock
    private ApsScheduleTaskMapper taskMapper;

    @Mock
    private ApsInsertEventMapper insertEventMapper;

    @Mock
    private ApsOrderMapper orderMapper;

    @Mock
    private ApsEquipmentGroupMapper equipmentGroupMapper;

    @InjectMocks
    private ApsSchedulePlanServiceImpl service;

    private ApsSchedulePlan plan;

    @BeforeEach
    void setUp()
    {
        plan = new ApsSchedulePlan();
        plan.setPlanId(PLAN_ID);
        plan.setPlanStatus("PENDING");
        plan.setActiveFlag("N");
    }

    @Test
    void deleteUnreferencedPlanSoftDeletesPlanThenTasks()
    {
        when(planMapper.selectApsSchedulePlanById(PLAN_ID)).thenReturn(plan);
        when(planMapper.softDeleteApsSchedulePlanById(PLAN_ID, OPERATOR)).thenReturn(1);

        int result = service.deleteApsSchedulePlanById(PLAN_ID, OPERATOR);

        assertEquals(1, result);
        InOrder order = inOrder(planMapper, taskMapper);
        order.verify(planMapper).softDeleteApsSchedulePlanById(PLAN_ID, OPERATOR);
        order.verify(taskMapper).softDeleteApsScheduleTasksByPlanId(PLAN_ID, OPERATOR);
    }

    @Test
    void activePlanCannotBeDeleted()
    {
        plan.setActiveFlag("Y");
        when(planMapper.selectApsSchedulePlanById(PLAN_ID)).thenReturn(plan);

        assertThrows(ServiceException.class, () -> service.deleteApsSchedulePlanById(PLAN_ID, OPERATOR));
        verify(planMapper, never()).softDeleteApsSchedulePlanById(PLAN_ID, OPERATOR);
    }

    @Test
    void confirmedPlanCannotBeDeleted()
    {
        plan.setPlanStatus("CONFIRMED");
        when(planMapper.selectApsSchedulePlanById(PLAN_ID)).thenReturn(plan);

        assertThrows(ServiceException.class, () -> service.deleteApsSchedulePlanById(PLAN_ID, OPERATOR));
        verify(planMapper, never()).softDeleteApsSchedulePlanById(PLAN_ID, OPERATOR);
    }

    @Test
    void sourcePlanReferencedByInsertEventCannotBeDeleted()
    {
        when(planMapper.selectApsSchedulePlanById(PLAN_ID)).thenReturn(plan);
        when(insertEventMapper.countBySourcePlanId(PLAN_ID)).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.deleteApsSchedulePlanById(PLAN_ID, OPERATOR));
        verify(planMapper, never()).softDeleteApsSchedulePlanById(PLAN_ID, OPERATOR);
    }

    @Test
    void newPlanReferencedByInsertEventCannotBeDeleted()
    {
        when(planMapper.selectApsSchedulePlanById(PLAN_ID)).thenReturn(plan);
        when(insertEventMapper.countByNewPlanId(PLAN_ID)).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.deleteApsSchedulePlanById(PLAN_ID, OPERATOR));
        verify(planMapper, never()).softDeleteApsSchedulePlanById(PLAN_ID, OPERATOR);
    }

    @Test
    void historyPlanRemainsQueryable()
    {
        plan.setPlanStatus("HISTORY");
        when(planMapper.selectApsSchedulePlanById(PLAN_ID)).thenReturn(plan);

        assertSame(plan, service.selectApsSchedulePlanById(PLAN_ID));
    }

    @Test
    @SuppressWarnings("unchecked")
    void compareBuildsLotTaskAndDecisionEvidence()
    {
        ApsSchedulePlan sourcePlan = schedulePlan(100L, "INITIAL", "HISTORY", null, null);
        ApsSchedulePlan newPlan = schedulePlan(PLAN_ID, "RESCHEDULE", "PENDING", 100L, 10L);
        ApsInsertEvent event = new ApsInsertEvent();
        event.setEventId(10L);
        event.setInsertOrderId(99L);

        ApsScheduleTask originalChanged = task(11L, 1L, "LOT-001", 1, 10L, 100L,
                "EQ-100", "2026-07-18 08:00:00", "2026-07-18 09:00:00");
        ApsScheduleTask originalFrozen = task(21L, 2L, "LOT-002", 1, 20L, 200L,
                "EQ-200", "2026-07-18 08:00:00", "2026-07-18 08:30:00");

        ApsScheduleTask shifted = task(101L, 1L, "LOT-001", 1, 10L, 101L,
                "EQ-101", "2026-07-18 08:30:00", "2026-07-18 09:30:00");
        shifted.setSourceTaskId(11L);
        shifted.setIsChanged("Y");
        ApsScheduleTask frozen = task(201L, 2L, "LOT-002", 1, 20L, 200L,
                "EQ-200", "2026-07-18 08:00:00", "2026-07-18 08:30:00");
        frozen.setSourceTaskId(21L);
        frozen.setIsFrozen("Y");
        ApsScheduleTask inserted = task(999L, 99L, "HOT-099", 1, 30L, 300L,
                "EQ-300", "2026-07-18 09:30:00", "2026-07-18 10:30:00");
        inserted.setIsInserted("Y");
        ApsScheduleTask softDeleted = task(888L, 88L, "DELETED", 1, 40L, 400L,
                "EQ-400", "2026-07-18 09:00:00", "2026-07-18 11:00:00");
        softDeleted.setDelFlag("2");

        ApsOrder normalOrder = order(1L, "LOT-001", "NORMAL", 2, "2026-07-18 12:00:00");
        ApsOrder frozenOrder = order(2L, "LOT-002", "NORMAL", 3, "2026-07-18 12:00:00");
        ApsOrder insertOrder = order(99L, "HOT-099", "INSERT", 1, "2026-07-18 11:00:00");

        when(planMapper.selectApsSchedulePlanById(PLAN_ID)).thenReturn(newPlan);
        when(planMapper.selectApsSchedulePlanById(100L)).thenReturn(sourcePlan);
        when(taskMapper.selectApsScheduleTaskListByPlanId(100L))
                .thenReturn(Arrays.asList(originalChanged, originalFrozen));
        when(taskMapper.selectApsScheduleTaskListByPlanId(PLAN_ID))
                .thenReturn(Arrays.asList(shifted, frozen, inserted, softDeleted));
        when(insertEventMapper.selectApsInsertEventById(10L)).thenReturn(event);
        when(orderMapper.selectApsOrderById(1L)).thenReturn(normalOrder);
        when(orderMapper.selectApsOrderById(2L)).thenReturn(frozenOrder);
        when(orderMapper.selectApsOrderById(99L)).thenReturn(insertOrder);
        when(equipmentGroupMapper.selectApsEquipmentGroupById(10L)).thenReturn(equipmentGroup(10L, "TG-10"));
        when(equipmentGroupMapper.selectApsEquipmentGroupById(30L)).thenReturn(equipmentGroup(30L, "TG-30"));

        Map<String, Object> result = service.compareReschedulePlan(PLAN_ID);
        List<Map<String, Object>> affectedOrders = (List<Map<String, Object>>) result.get("affectedOrderDetails");
        List<Map<String, Object>> changedTasks = (List<Map<String, Object>>) result.get("changedTaskDetails");
        Map<String, Object> evidence = (Map<String, Object>) result.get("decisionEvidence");
        Map<String, Object> summary = (Map<String, Object>) result.get("summary");

        Map<String, Object> hotLot = findByLong(affectedOrders, "orderId", 99L);
        assertEquals("INSERTED", hotLot.get("impactType"));
        assertEquals(0L, hotLot.get("trueDelayMinutes"));
        Map<String, Object> delayedLot = findByLong(affectedOrders, "orderId", 1L);
        assertEquals(30L, delayedLot.get("stabilityDelayMinutes"));

        Map<String, Object> insertedTask = findByLong(changedTasks, "taskId", 999L);
        assertEquals("INSERTED", insertedTask.get("changeType"));
        Map<String, Object> shiftedTask = findByLong(changedTasks, "taskId", 101L);
        assertEquals("TIME_AND_EQUIPMENT_CHANGED", shiftedTask.get("changeType"));
        assertEquals(30L, shiftedTask.get("startShiftMinutes"));
        Map<String, Object> frozenTask = findByLong(changedTasks, "taskId", 201L);
        assertEquals("FROZEN", frozenTask.get("changeType"));
        assertEquals(0L, frozenTask.get("startShiftMinutes"));
        assertEquals(frozenTask.get("originalStartTime"), frozenTask.get("newStartTime"));

        assertEquals(3, summary.get("newTaskCount"));
        assertTrue(changedTasks.stream().noneMatch(item -> Long.valueOf(888L).equals(item.get("taskId"))));
        assertEquals(Boolean.TRUE, evidence.get("insertOrderOnTime"));
        assertEquals(2, evidence.get("affectedOrderCount"));
        assertEquals(2, evidence.get("affectedEquipmentGroupCount"));
        assertEquals("CAUTION", evidence.get("recommendationLevel"));
    }

    private ApsSchedulePlan schedulePlan(Long id, String type, String status, Long sourcePlanId, Long eventId)
    {
        ApsSchedulePlan result = new ApsSchedulePlan();
        result.setPlanId(id);
        result.setPlanCode("PLAN-" + id);
        result.setPlanType(type);
        result.setPlanStatus(status);
        result.setSourcePlanId(sourcePlanId);
        result.setEventId(eventId);
        return result;
    }

    private ApsScheduleTask task(Long taskId, Long orderId, String orderCode, Integer processSeq,
            Long groupId, Long equipmentId, String equipmentCode, String start, String end)
    {
        ApsScheduleTask result = new ApsScheduleTask();
        result.setTaskId(taskId);
        result.setOrderId(orderId);
        result.setOrderCode(orderCode);
        result.setProcessSeq(processSeq);
        result.setProcessCode("OP-" + processSeq);
        result.setEquipmentGroupId(groupId);
        result.setEquipmentId(equipmentId);
        result.setEquipmentCode(equipmentCode);
        result.setPlannedStartTime(date(start));
        result.setPlannedEndTime(date(end));
        result.setIsInserted("N");
        result.setIsChanged("N");
        result.setIsFrozen("N");
        result.setDelFlag("0");
        return result;
    }

    private ApsOrder order(Long orderId, String code, String type, Integer priority, String dueTime)
    {
        ApsOrder result = new ApsOrder();
        result.setOrderId(orderId);
        result.setOrderCode(code);
        result.setOrderType(type);
        result.setPriorityLevel(priority);
        result.setDueTime(date(dueTime));
        return result;
    }

    private ApsEquipmentGroup equipmentGroup(Long id, String code)
    {
        ApsEquipmentGroup group = new ApsEquipmentGroup();
        group.setEquipmentGroupId(id);
        group.setEquipmentGroupCode(code);
        group.setEquipmentGroupName(code);
        return group;
    }

    private Date date(String value)
    {
        return DateUtils.parseDate(value);
    }

    private Map<String, Object> findByLong(List<Map<String, Object>> items, String key, Long value)
    {
        return items.stream()
                .filter(item -> value.equals(item.get(key)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing item for " + key + "=" + value));
    }
}
