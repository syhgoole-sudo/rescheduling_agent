from collections import defaultdict
from datetime import datetime, timedelta
from typing import Any, Dict, List, Tuple

from app.algorithms.kpi_calculator import calculate_local_reschedule_kpi
from app.schemas.schedule_schema import InitialScheduleRequest, LocalRescheduleRequest, TaskScheduleDTO


DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def _format_time(value: datetime) -> str:
    return value.strftime(DATETIME_FORMAT)


def schedule_by_rule(request: InitialScheduleRequest) -> Tuple[List[TaskScheduleDTO], Dict[str, Any], List[str]]:
    warnings: List[str] = []
    tasks: List[TaskScheduleDTO] = []

    normal_orders = [
        order for order in request.orders
        if order.orderType == "NORMAL" and order.orderStatus == "NEW"
    ]
    normal_orders.sort(key=lambda item: (item.priorityLevel, item.dueTime, item.releaseTime))

    routes_by_product = defaultdict(list)
    for operation in request.routeOperations:
        routes_by_product[operation.productId].append(operation)
    for operations in routes_by_product.values():
        operations.sort(key=lambda item: item.processSeq)

    equipments_by_group = defaultdict(list)
    for equipment in request.equipments:
        equipments_by_group[equipment.equipmentGroupId].append(equipment)
    for equipments in equipments_by_group.values():
        equipments.sort(key=lambda item: item.equipmentCode)

    equipment_available_time: Dict[int, datetime] = {
        equipment.equipmentId: request.scheduleStartTime for equipment in request.equipments
    }
    order_finish_time: Dict[int, datetime] = {}

    for order in normal_orders:
        operations = routes_by_product.get(order.productId, [])
        if not operations:
            warnings.append(f"Order {order.orderCode} skipped: route operations not found.")
            continue
        missing_group = next(
            (operation.equipmentGroupId for operation in operations if not equipments_by_group.get(operation.equipmentGroupId)),
            None,
        )
        if missing_group is not None:
            warnings.append(f"Order {order.orderCode} skipped: equipment group {missing_group} has no equipment.")
            continue

        order_ready_time = max(request.scheduleStartTime, order.releaseTime)
        for operation in operations:
            candidates = equipments_by_group[operation.equipmentGroupId]
            selected = min(
                candidates,
                key=lambda item: (
                    max(equipment_available_time[item.equipmentId], order_ready_time),
                    item.equipmentCode,
                ),
            )
            planned_start = max(equipment_available_time[selected.equipmentId], order_ready_time)
            planned_end = planned_start + timedelta(minutes=operation.standardDuration)

            tasks.append(TaskScheduleDTO(
                orderId=order.orderId,
                orderCode=order.orderCode,
                productId=order.productId,
                productCode=order.productCode,
                processSeq=operation.processSeq,
                processCode=operation.processCode,
                processName=operation.processName,
                equipmentGroupId=operation.equipmentGroupId,
                equipmentId=selected.equipmentId,
                equipmentCode=selected.equipmentCode,
                plannedStartTime=_format_time(planned_start),
                plannedEndTime=_format_time(planned_end),
                duration=operation.standardDuration,
            ))

            equipment_available_time[selected.equipmentId] = planned_end
            order_ready_time = planned_end
        order_finish_time[order.orderId] = order_ready_time

    scheduled_orders = [order for order in normal_orders if order.orderId in order_finish_time]
    makespan_minutes = 0
    if tasks:
        max_end = max(datetime.strptime(task.plannedEndTime, DATETIME_FORMAT) for task in tasks)
        makespan_minutes = int((max_end - request.scheduleStartTime).total_seconds() // 60)

    delay_minutes = []
    due_time_by_order = {order.orderId: order.dueTime for order in scheduled_orders}
    for order_id, finish_time in order_finish_time.items():
        delay = max(0, int((finish_time - due_time_by_order[order_id]).total_seconds() // 60))
        delay_minutes.append(delay)

    kpi = {
        "makespan": makespan_minutes,
        "delayOrderCount": sum(1 for item in delay_minutes if item > 0),
        "totalDelayMinutes": sum(delay_minutes),
        "maxDelayMinutes": max(delay_minutes) if delay_minutes else 0,
        "scheduledOrderCount": len(scheduled_orders),
        "scheduledTaskCount": len(tasks),
    }
    return tasks, kpi, warnings


def local_reschedule_by_rule(request: LocalRescheduleRequest) -> Tuple[List[TaskScheduleDTO], Dict[str, Any], List[str]]:
    warnings: List[str] = []
    task_schedules: List[TaskScheduleDTO] = []

    equipments_by_group = defaultdict(list)
    equipment_code_by_id: Dict[int, str] = {}
    for equipment in request.equipments:
        equipments_by_group[equipment.equipmentGroupId].append(equipment)
        equipment_code_by_id[equipment.equipmentId] = equipment.equipmentCode
    for equipments in equipments_by_group.values():
        equipments.sort(key=lambda item: item.equipmentCode)

    equipment_available_time: Dict[int, datetime] = {
        equipment.equipmentId: request.scheduleStartTime for equipment in request.equipments
    }
    order_ready_time: Dict[int, datetime] = {}

    for task in request.frozenTasks:
        start = _parse_task_time(task.plannedStartTime)
        end = _parse_task_time(task.plannedEndTime)
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

    pending_tasks = []
    for task in sorted(request.insertTasks, key=lambda item: item.processSeq):
        pending_tasks.append({
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
            "priority": 0,
            "is_inserted": "Y",
            "is_changed": "Y",
            "source_task_id": None,
            "original_start_time": None,
            "original_end_time": None,
            "original_equipment_id": None,
            "due_time": request.insertOrder.dueTime,
        })

    adjustable_sorted = sorted(
        request.adjustableTasks,
        key=lambda item: (_parse_task_time(item.plannedStartTime), item.orderCode, item.processSeq),
    )
    for task in adjustable_sorted:
        pending_tasks.append({
            "order_id": task.orderId,
            "order_code": task.orderCode,
            "product_id": task.productId,
            "product_code": task.productCode,
            "process_seq": task.processSeq,
            "process_code": task.processCode,
            "process_name": task.processName,
            "equipment_group_id": task.equipmentGroupId,
            "duration": task.duration,
            "release_time": _parse_task_time(task.originalStartTime or task.plannedStartTime),
            "priority": 1,
            "is_inserted": "N",
            "is_changed": "Y",
            "source_task_id": task.sourceTaskId,
            "original_start_time": task.originalStartTime or task.plannedStartTime,
            "original_end_time": task.originalEndTime or task.plannedEndTime,
            "original_equipment_id": task.originalEquipmentId or task.equipmentId,
            "due_time": task.dueTime,
        })

    pending_tasks.sort(key=lambda item: (item["priority"], item["order_id"], item["process_seq"]))
    for task in pending_tasks:
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
            equipmentCode=equipment_code_by_id.get(selected.equipmentId, selected.equipmentCode),
            plannedStartTime=_format_time(planned_start),
            plannedEndTime=_format_time(planned_end),
            duration=task["duration"],
            isFrozen="N",
            isInserted=task["is_inserted"],
            isChanged=task["is_changed"],
            sourceTaskId=task["source_task_id"],
            originalStartTime=task["original_start_time"],
            originalEndTime=task["original_end_time"],
            originalEquipmentId=task["original_equipment_id"],
            dueTime=_format_time(task["due_time"]) if isinstance(task["due_time"], datetime) else task["due_time"],
        ))

        equipment_available_time[selected.equipmentId] = planned_end
        order_ready_time[task["order_id"]] = planned_end

    kpi = calculate_local_reschedule_kpi(request, task_schedules)
    task_schedules.sort(key=lambda item: (item.equipmentId, item.plannedStartTime, item.processSeq))
    return task_schedules, kpi, warnings


def _parse_task_time(value: str) -> datetime:
    if isinstance(value, datetime):
        return value
    return datetime.strptime(value, DATETIME_FORMAT)
