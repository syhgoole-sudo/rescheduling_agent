from datetime import datetime
from typing import Any, Dict, List, Optional

from app.schemas.schedule_schema import LocalRescheduleRequest, TaskScheduleDTO


DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def parse_time(value: Any) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value
    return datetime.strptime(value, DATETIME_FORMAT)


def format_time(value: Optional[datetime]) -> Optional[str]:
    return value.strftime(DATETIME_FORMAT) if value else None


def minutes_between(start: datetime, end: datetime) -> int:
    return int((end - start).total_seconds() // 60)


def calculate_local_reschedule_kpi(
    request: LocalRescheduleRequest,
    tasks: List[TaskScheduleDTO],
) -> Dict[str, Any]:
    if not tasks:
        return _empty_kpi()

    finish_by_order = aggregate_finish_by_order(tasks, request.scheduleStartTime)
    due_by_order = aggregate_due_by_order(request, tasks)
    original_finish_by_order = aggregate_original_finish_by_order(tasks)

    max_end = max(parse_time(task.plannedEndTime) for task in tasks if task.plannedEndTime)
    makespan = minutes_between(request.scheduleStartTime, max_end) if max_end else 0
    changed_count = sum(1 for task in tasks if task.isChanged == "Y")
    inserted_count = sum(1 for task in tasks if task.isInserted == "Y")
    frozen_count = sum(1 for task in tasks if task.isFrozen == "Y")

    true_delays: List[int] = []
    for order_id, finish_time in finish_by_order.items():
        due_time = due_by_order.get(order_id)
        if due_time is None:
            continue
        true_delays.append(max(0, minutes_between(due_time, finish_time)))

    stability_delays: List[int] = []
    for order_id, finish_time in finish_by_order.items():
        original_finish_time = original_finish_by_order.get(order_id)
        if original_finish_time is None:
            # New insert lots and tasks without originalFinishTime do not participate
            # in stabilityDelay because there is no baseline plan finish time.
            continue
        stability_delays.append(max(0, minutes_between(original_finish_time, finish_time)))

    insert_finish_time = finish_by_order.get(request.insertOrder.orderId)
    insert_true_delay = 0
    if insert_finish_time is not None and request.insertOrder.dueTime is not None:
        insert_true_delay = max(0, minutes_between(request.insertOrder.dueTime, insert_finish_time))

    true_delay = {
        "trueDelayOrderCount": sum(1 for item in true_delays if item > 0),
        "trueTotalDelayMinutes": sum(true_delays),
        "trueMaxDelayMinutes": max(true_delays) if true_delays else 0,
        "insertOrderTrueDelayMinutes": insert_true_delay,
    }
    stability_delay = {
        "stabilityDelayOrderCount": sum(1 for item in stability_delays if item > 0),
        "stabilityTotalDelayMinutes": sum(stability_delays),
        "stabilityMaxDelayMinutes": max(stability_delays) if stability_delays else 0,
        "averageStabilityDelayMinutes": round(sum(stability_delays) / len(stability_delays), 4) if stability_delays else 0,
    }

    kpi = {
        "makespan": makespan,
        "delayOrderCount": true_delay["trueDelayOrderCount"],
        "totalDelayMinutes": true_delay["trueTotalDelayMinutes"],
        "maxDelayMinutes": true_delay["trueMaxDelayMinutes"],
        "changedTaskCount": changed_count,
        "changedTaskRatio": round(changed_count / len(tasks), 4),
        "insertedTaskCount": inserted_count,
        "frozenTaskCount": frozen_count,
        "insertOrderFinishTime": format_time(insert_finish_time),
        "insertOrderDelayMinutes": insert_true_delay,
        "insertOrderTrueDelayMinutes": insert_true_delay,
        "scheduledTaskCount": len(tasks),
        "trueDelay": true_delay,
        "stabilityDelay": stability_delay,
    }
    return kpi


def aggregate_finish_by_order(tasks: List[TaskScheduleDTO], default_time: datetime) -> Dict[int, datetime]:
    finish_by_order: Dict[int, datetime] = {}
    for task in tasks:
        finish_time = parse_time(task.plannedEndTime)
        if finish_time is None:
            continue
        finish_by_order[task.orderId] = max(finish_by_order.get(task.orderId, default_time), finish_time)
    return finish_by_order


def aggregate_due_by_order(
    request: LocalRescheduleRequest,
    tasks: List[TaskScheduleDTO],
) -> Dict[int, datetime]:
    due_by_order: Dict[int, datetime] = {}
    if request.insertOrder and request.insertOrder.dueTime is not None:
        due_by_order[request.insertOrder.orderId] = request.insertOrder.dueTime
    for task in tasks:
        due_time = parse_time(getattr(task, "dueTime", None))
        if due_time is not None:
            due_by_order[task.orderId] = due_time
    return due_by_order


def aggregate_original_finish_by_order(tasks: List[TaskScheduleDTO]) -> Dict[int, datetime]:
    original_finish_by_order: Dict[int, datetime] = {}
    for task in tasks:
        original_finish_time = parse_time(task.originalEndTime)
        if original_finish_time is None:
            continue
        current = original_finish_by_order.get(task.orderId)
        if current is None or original_finish_time > current:
            original_finish_by_order[task.orderId] = original_finish_time
    return original_finish_by_order


def _empty_kpi() -> Dict[str, Any]:
    return {
        "makespan": 0,
        "delayOrderCount": 0,
        "totalDelayMinutes": 0,
        "maxDelayMinutes": 0,
        "changedTaskCount": 0,
        "changedTaskRatio": 0,
        "insertedTaskCount": 0,
        "frozenTaskCount": 0,
        "insertOrderFinishTime": None,
        "insertOrderDelayMinutes": 0,
        "insertOrderTrueDelayMinutes": 0,
        "scheduledTaskCount": 0,
        "trueDelay": {
            "trueDelayOrderCount": 0,
            "trueTotalDelayMinutes": 0,
            "trueMaxDelayMinutes": 0,
            "insertOrderTrueDelayMinutes": 0,
        },
        "stabilityDelay": {
            "stabilityDelayOrderCount": 0,
            "stabilityTotalDelayMinutes": 0,
            "stabilityMaxDelayMinutes": 0,
            "averageStabilityDelayMinutes": 0,
        },
    }
