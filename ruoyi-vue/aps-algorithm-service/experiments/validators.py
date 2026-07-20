from collections import defaultdict
from datetime import datetime
from typing import List

from app.schemas.schedule_schema import LocalRescheduleRequest, TaskScheduleDTO


DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def validate_local_schedule(
    request: LocalRescheduleRequest,
    tasks: List[TaskScheduleDTO],
) -> List[str]:
    errors = []
    errors.extend(_validate_task_count(request, tasks))
    errors.extend(_validate_equipment_overlap(tasks))
    errors.extend(_validate_process_precedence(tasks))
    errors.extend(_validate_frozen_tasks(request, tasks))
    errors.extend(_validate_equipment_qualification(request, tasks))
    return errors


def _parse_time(value: str) -> datetime:
    return datetime.strptime(value, DATETIME_FORMAT)


def _validate_task_count(request, tasks):
    expected = len(request.frozenTasks) + len(request.adjustableTasks) + len(request.insertTasks)
    if len(tasks) != expected:
        return [f"task count mismatch: expected {expected}, got {len(tasks)}"]
    return []


def _validate_equipment_overlap(tasks):
    errors = []
    by_equipment = defaultdict(list)
    for task in tasks:
        by_equipment[task.equipmentId].append(task)
    for equipment_id, equipment_tasks in by_equipment.items():
        ordered = sorted(equipment_tasks, key=lambda item: _parse_time(item.plannedStartTime))
        for previous, current in zip(ordered, ordered[1:]):
            if _parse_time(previous.plannedEndTime) > _parse_time(current.plannedStartTime):
                errors.append(
                    f"equipment {equipment_id} overlap: {previous.orderCode} and {current.orderCode}"
                )
    return errors


def _validate_process_precedence(tasks):
    errors = []
    by_order = defaultdict(list)
    for task in tasks:
        by_order[task.orderId].append(task)
    for order_id, order_tasks in by_order.items():
        ordered = sorted(order_tasks, key=lambda item: item.processSeq)
        for previous, current in zip(ordered, ordered[1:]):
            if _parse_time(previous.plannedEndTime) > _parse_time(current.plannedStartTime):
                errors.append(
                    f"order {order_id} precedence violation: {previous.processSeq} -> {current.processSeq}"
                )
    return errors


def _validate_frozen_tasks(request, tasks):
    errors = []
    result_by_source_id = {
        task.sourceTaskId: task for task in tasks if task.sourceTaskId is not None
    }
    for frozen in request.frozenTasks:
        result = result_by_source_id.get(frozen.sourceTaskId)
        if result is None:
            errors.append(f"frozen task {frozen.sourceTaskId} is missing")
        elif (
            result.plannedStartTime != frozen.plannedStartTime
            or result.plannedEndTime != frozen.plannedEndTime
            or result.equipmentId != frozen.equipmentId
        ):
            errors.append(f"frozen task {frozen.sourceTaskId} was changed")
    return errors


def _validate_equipment_qualification(request, tasks):
    group_by_equipment = {
        equipment.equipmentId: equipment.equipmentGroupId
        for equipment in request.equipments
    }
    return [
        f"task {task.orderCode}-{task.processSeq} assigned to an unqualified equipment"
        for task in tasks
        if group_by_equipment.get(task.equipmentId) != task.equipmentGroupId
    ]
