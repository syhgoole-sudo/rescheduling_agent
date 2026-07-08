from collections import defaultdict
from datetime import datetime, timedelta
from typing import Any, Dict, List, Tuple

from app.algorithms.kpi_calculator import calculate_local_reschedule_kpi
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
            "due_time": request.insertOrder.dueTime,
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
            "due_time": task.dueTime,
        })

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
                dueTime=format_time(task["due_time"]) if isinstance(task["due_time"], datetime) else task["due_time"],
            ))

            equipment_available_time[selected.equipmentId] = planned_end
            order_ready_time[task["order_id"]] = planned_end

    kpi = calculate_local_reschedule_kpi(request, task_schedules)
    task_schedules.sort(key=lambda item: (item.equipmentId, item.plannedStartTime, item.processSeq))
    return task_schedules, kpi, warnings
