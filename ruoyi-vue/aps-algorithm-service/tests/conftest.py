from datetime import datetime

import pytest

from app.algorithms.ga_scheduler import local_reschedule_by_ga
from app.algorithms.rule_scheduler import local_reschedule_by_rule
from app.schemas.schedule_schema import (
    EquipmentDTO,
    InsertTaskDTO,
    LocalRescheduleRequest,
    OrderDTO,
    RouteOperationDTO,
    TaskScheduleDTO,
)


def build_local_reschedule_request(random_seed: int = 42) -> LocalRescheduleRequest:
    schedule_start = datetime(2026, 1, 1, 8, 0, 0)
    insert_order = OrderDTO(
        orderId=99,
        orderCode="HOT-LOT-99",
        orderType="INSERT",
        productId=2,
        productCode="HOT-PRODUCT",
        quantity=1,
        priorityLevel=1,
        releaseTime=datetime(2026, 1, 1, 8, 15, 0),
        dueTime=datetime(2026, 1, 1, 10, 0, 0),
        orderStatus="NEW",
    )
    insert_route = [
        RouteOperationDTO(
            productId=2,
            productCode="HOT-PRODUCT",
            processSeq=1,
            processCode="PHOTO",
            processName="Photo",
            equipmentGroupId=1,
            standardDuration=20,
        ),
        RouteOperationDTO(
            productId=2,
            productCode="HOT-PRODUCT",
            processSeq=2,
            processCode="ETCH",
            processName="Etch",
            equipmentGroupId=2,
            standardDuration=25,
        ),
    ]
    frozen_tasks = [
        TaskScheduleDTO(
            orderId=10,
            orderCode="NORMAL-10",
            productId=1,
            productCode="NORMAL-PRODUCT",
            processSeq=1,
            processCode="PHOTO",
            processName="Photo",
            equipmentGroupId=1,
            equipmentId=101,
            equipmentCode="PHOTO-01",
            plannedStartTime="2026-01-01 08:00:00",
            plannedEndTime="2026-01-01 08:30:00",
            duration=30,
            isFrozen="Y",
            isInserted="N",
            isChanged="N",
            sourceTaskId=1001,
            originalStartTime="2026-01-01 08:00:00",
            originalEndTime="2026-01-01 08:30:00",
            originalEquipmentId=101,
            dueTime="2026-01-01 12:00:00",
        )
    ]
    adjustable_tasks = [
        _source_task(20, 2001, 1, 1, 101, "PHOTO-01", "08:30:00", "09:00:00", 30),
        _source_task(20, 2002, 2, 2, 201, "ETCH-01", "09:00:00", "09:30:00", 30),
        _source_task(30, 3001, 1, 1, 101, "PHOTO-01", "09:00:00", "09:25:00", 25),
        _source_task(30, 3002, 2, 2, 201, "ETCH-01", "09:30:00", "10:00:00", 30),
    ]
    return LocalRescheduleRequest(
        requestId=f"TEST-GA-{random_seed}",
        eventId=1,
        sourcePlanId=1,
        scheduleStartTime=schedule_start,
        insertOrder=insert_order,
        frozenTasks=frozen_tasks,
        adjustableTasks=adjustable_tasks,
        insertTasks=[
            InsertTaskDTO(
                orderId=insert_order.orderId,
                orderCode=insert_order.orderCode,
                productId=operation.productId,
                productCode=insert_order.productCode,
                processSeq=operation.processSeq,
                processCode=operation.processCode,
                processName=operation.processName,
                equipmentGroupId=operation.equipmentGroupId,
                standardDuration=operation.standardDuration,
            )
            for operation in insert_route
        ],
        equipments=[
            EquipmentDTO(
                equipmentId=101,
                equipmentCode="PHOTO-01",
                equipmentName="Photo Tool 01",
                equipmentGroupId=1,
                equipmentGroupCode="PHOTO",
                status="0",
            ),
            EquipmentDTO(
                equipmentId=201,
                equipmentCode="ETCH-01",
                equipmentName="Etch Tool 01",
                equipmentGroupId=2,
                equipmentGroupCode="ETCH",
                status="0",
            ),
        ],
        routeOperations=insert_route,
        strategyConfig={
            "algorithmType": "GA",
            "randomSeed": random_seed,
            "populationSize": 12,
            "generations": 10,
            "crossoverRate": 0.8,
            "mutationRate": 0.2,
        },
    )


def _source_task(
    order_id: int,
    source_task_id: int,
    process_seq: int,
    equipment_group_id: int,
    equipment_id: int,
    equipment_code: str,
    start_time: str,
    end_time: str,
    duration: int,
) -> TaskScheduleDTO:
    planned_start = f"2026-01-01 {start_time}"
    planned_end = f"2026-01-01 {end_time}"
    return TaskScheduleDTO(
        orderId=order_id,
        orderCode=f"NORMAL-{order_id}",
        productId=1,
        productCode="NORMAL-PRODUCT",
        processSeq=process_seq,
        processCode="PHOTO" if equipment_group_id == 1 else "ETCH",
        processName="Photo" if equipment_group_id == 1 else "Etch",
        equipmentGroupId=equipment_group_id,
        equipmentId=equipment_id,
        equipmentCode=equipment_code,
        plannedStartTime=planned_start,
        plannedEndTime=planned_end,
        duration=duration,
        isFrozen="N",
        isInserted="N",
        isChanged="N",
        sourceTaskId=source_task_id,
        originalStartTime=planned_start,
        originalEndTime=planned_end,
        originalEquipmentId=equipment_id,
        dueTime="2026-01-01 12:00:00",
    )


@pytest.fixture(scope="session")
def local_request_factory():
    return build_local_reschedule_request


@pytest.fixture(scope="module", params=("RULE", "GA"))
def schedule_result(request, local_request_factory):
    local_request = local_request_factory(42)
    if request.param == "GA":
        tasks, kpi, warnings = local_reschedule_by_ga(local_request)
    else:
        tasks, kpi, warnings = local_reschedule_by_rule(local_request)
    return request.param, local_request, tasks, kpi, warnings
