from collections import defaultdict
from datetime import datetime, timedelta
from typing import Any, Dict, List, Tuple

from app.schemas.schedule_schema import LocalRescheduleRequest, TaskScheduleDTO


DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def parse_time(value: str) -> datetime:
    if isinstance(value, datetime):
        return value
    return datetime.strptime(value, DATETIME_FORMAT)


def format_time(value: datetime) -> str:
    return value.strftime(DATETIME_FORMAT)


def build_order_sequence_rule(request: LocalRescheduleRequest) -> List[int]:
    order_ids = []
    if request.insertTasks:
        order_ids.append(request.insertOrder.orderId)
    adjustable_ids = sorted({task.orderId for task in request.adjustableTasks})
    for order_id in adjustable_ids:
        if order_id not in order_ids:
            order_ids.append(order_id)
    return order_ids


def decode_local_reschedule(
    request: LocalRescheduleRequest,
    order_sequence: List[int],
) -> Tuple[List[TaskScheduleDTO], Dict[str, Any], List[str]]:
    warnings: List[str] = []
    task_schedules: List[TaskScheduleDTO] = []

    equipments_by_group = defaultdict(list)
    for equipment in request.equipments:
        equipments_by_group[equipment.equipmentGroupId].append(equipment)
    for equipments in equipments_by_group.values():
        equipments.sort(key=lambda item: item.equipmentCode)

    equipment_available_time: Dict[int, datetime] = {
        equipment.equipmentId: request.scheduleStartTime for equipment in request.equipments
    }
    order_ready_time: Dict[int, datetime] = {}

    original_finish_by_order: Dict[int, datetime] = {}
    for task in request.frozenTasks:
        start = parse_time(task.plannedStartTime)
        end = parse_time(task.plannedEndTime)
        task_schedules.append(task.copy(update={
            "isFrozen": "Y",
            "isInserted": "N",
            "isChanged": "N",
        }))
        equipment_available_time[task.equipmentId] = max(
            equipment_available_time.get(task.equipmentId, request.scheduleStartTime),
            end,
        )
        order_ready_time[task.orderId] = max(order_ready_time.get(task.orderId, start), end)

    tasks_by_order: Dict[int, List[Dict[str, Any]]] = defaultdict(list)
    for task in sorted(request.insertTasks, key=lambda item: item.processSeq):
        tasks_by_order[task.orderId].append({
            "order_id": task.orderId,
            "order_code": task.orderCode,
            "product_id": task.productId,
            "product_code": task.productCode,
            "process_seq": task.processSeq,
            "process_code": task.processCode,
            "process_name": task.processName,
            "equipment_group_id": task.equipmentGroupId,
            "duration": task.standardDuration,
            "release_time": request.insertOrder.releaseTime,
            "is_inserted": "Y",
            "source_task_id": None,
            "original_start_time": None,
            "original_end_time": None,
            "original_equipment_id": None,
        })

    for task in sorted(request.adjustableTasks, key=lambda item: (item.orderId, item.processSeq)):
        original_start = task.originalStartTime or task.plannedStartTime
        original_end = task.originalEndTime or task.plannedEndTime
        original_equipment_id = task.originalEquipmentId or task.equipmentId
        tasks_by_order[task.orderId].append({
            "order_id": task.orderId,
            "order_code": task.orderCode,
            "product_id": task.productId,
            "product_code": task.productCode,
            "process_seq": task.processSeq,
            "process_code": task.processCode,
            "process_name": task.processName,
            "equipment_group_id": task.equipmentGroupId,
            "duration": task.duration,
            "release_time": parse_time(original_start),
            "is_inserted": "N",
            "source_task_id": task.sourceTaskId,
            "original_start_time": original_start,
            "original_end_time": original_end,
            "original_equipment_id": original_equipment_id,
        })
        original_finish_by_order[task.orderId] = max(
            original_finish_by_order.get(task.orderId, parse_time(original_end)),
            parse_time(original_end),
        )

    seen = set()
    normalized_sequence = []
    for order_id in order_sequence:
        if order_id in tasks_by_order and order_id not in seen:
            normalized_sequence.append(order_id)
            seen.add(order_id)
    for order_id in sorted(tasks_by_order):
        if order_id not in seen:
            normalized_sequence.append(order_id)

    for order_id in normalized_sequence:
        for task in sorted(tasks_by_order[order_id], key=lambda item: item["process_seq"]):
            candidates = equipments_by_group.get(task["equipment_group_id"], [])
            if not candidates:
                warnings.append(f"Task {task['order_code']}-{task['process_seq']} skipped: equipment group has no equipment.")
                continue

            ready_time = max(
                request.scheduleStartTime,
                task["release_time"],
                order_ready_time.get(task["order_id"], request.scheduleStartTime),
            )
            selected = min(
                candidates,
                key=lambda item: (
                    max(equipment_available_time.get(item.equipmentId, request.scheduleStartTime), ready_time),
                    item.equipmentCode,
                ),
            )
            planned_start = max(equipment_available_time.get(selected.equipmentId, request.scheduleStartTime), ready_time)
            planned_end = planned_start + timedelta(minutes=task["duration"])
            is_changed = "Y"
            if task["is_inserted"] == "N":
                is_changed = "N" if (
                    task["original_start_time"] == format_time(planned_start)
                    and task["original_end_time"] == format_time(planned_end)
                    and task["original_equipment_id"] == selected.equipmentId
                ) else "Y"

            task_schedules.append(TaskScheduleDTO(
                orderId=task["order_id"],
                orderCode=task["order_code"],
                productId=task["product_id"],
                productCode=task["product_code"],
                processSeq=task["process_seq"],
                processCode=task["process_code"],
                processName=task["process_name"],
                equipmentGroupId=task["equipment_group_id"],
                equipmentId=selected.equipmentId,
                equipmentCode=selected.equipmentCode,
                plannedStartTime=format_time(planned_start),
                plannedEndTime=format_time(planned_end),
                duration=task["duration"],
                isFrozen="N",
                isInserted=task["is_inserted"],
                isChanged=is_changed,
                sourceTaskId=task["source_task_id"],
                originalStartTime=task["original_start_time"],
                originalEndTime=task["original_end_time"],
                originalEquipmentId=task["original_equipment_id"],
            ))

            equipment_available_time[selected.equipmentId] = planned_end
            order_ready_time[task["order_id"]] = planned_end

    kpi = calculate_reschedule_kpi(request, task_schedules, original_finish_by_order)
    task_schedules.sort(key=lambda item: (item.equipmentId, item.plannedStartTime, item.processSeq))
    return task_schedules, kpi, warnings


def calculate_reschedule_kpi(
    request: LocalRescheduleRequest,
    tasks: List[TaskScheduleDTO],
    original_finish_by_order: Dict[int, datetime] = None,
) -> Dict[str, Any]:
    original_finish_by_order = original_finish_by_order or {}
    if not tasks:
        return {
            "makespan": 0,
            "delayOrderCount": 0,
            "totalDelayMinutes": 0,
            "maxDelayMinutes": 0,
            "changedTaskCount": 0,
            "changedTaskRatio": 0,
            "insertOrderFinishTime": None,
            "insertOrderDelayMinutes": 0,
            "scheduledTaskCount": 0,
        }

    max_end = max(parse_time(task.plannedEndTime) for task in tasks)
    makespan = int((max_end - request.scheduleStartTime).total_seconds() // 60)
    changed_count = sum(1 for task in tasks if task.isChanged == "Y")

    finish_by_order: Dict[int, datetime] = {}
    for task in tasks:
        finish_by_order[task.orderId] = max(
            finish_by_order.get(task.orderId, request.scheduleStartTime),
            parse_time(task.plannedEndTime),
        )

    delay_minutes: List[int] = []
    insert_finish_time = finish_by_order.get(request.insertOrder.orderId)
    for order_id, finish_time in finish_by_order.items():
        if order_id == request.insertOrder.orderId:
            due_time = request.insertOrder.dueTime
        elif order_id in original_finish_by_order:
            due_time = original_finish_by_order[order_id]
        else:
            due_time = finish_time
        delay_minutes.append(max(0, int((finish_time - due_time).total_seconds() // 60)))

    insert_delay = 0
    if insert_finish_time is not None:
        insert_delay = max(0, int((insert_finish_time - request.insertOrder.dueTime).total_seconds() // 60))

    return {
        "makespan": makespan,
        "delayOrderCount": sum(1 for item in delay_minutes if item > 0),
        "totalDelayMinutes": sum(delay_minutes),
        "maxDelayMinutes": max(delay_minutes) if delay_minutes else 0,
        "changedTaskCount": changed_count,
        "changedTaskRatio": round(changed_count / len(tasks), 4),
        "insertOrderFinishTime": format_time(insert_finish_time) if insert_finish_time else None,
        "insertOrderDelayMinutes": insert_delay,
        "scheduledTaskCount": len(tasks),
    }
