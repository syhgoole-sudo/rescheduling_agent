from collections import defaultdict
from datetime import datetime
from typing import Any, Dict, List, Tuple

from app.algorithms.schedule_decoder import decode_local_reschedule, parse_time
from app.schemas.schedule_schema import LocalRescheduleRequest, TaskScheduleDTO


SUPPORTED_DISPATCHING_RULES = ("FIFO", "EDD", "SPT")


def local_reschedule_by_dispatching_rule(
    request: LocalRescheduleRequest,
    rule: str,
) -> Tuple[List[TaskScheduleDTO], Dict[str, Any], List[str]]:
    """Run local rescheduling with a classical order-level dispatching rule."""
    normalized_rule = rule.upper()
    order_sequence = build_order_sequence(request, normalized_rule)
    tasks, kpi, warnings = decode_local_reschedule(request, order_sequence)
    kpi["dispatchingRule"] = normalized_rule
    return tasks, kpi, warnings


def build_order_sequence(request: LocalRescheduleRequest, rule: str) -> List[int]:
    normalized_rule = rule.upper()
    if normalized_rule not in SUPPORTED_DISPATCHING_RULES:
        raise ValueError(
            f"Unsupported dispatching rule: {rule}. "
            f"Expected one of {', '.join(SUPPORTED_DISPATCHING_RULES)}."
        )

    order_features = _collect_order_features(request)
    if normalized_rule == "FIFO":
        key = lambda item: (item["release_time"], item["due_time"], item["order_id"])
    elif normalized_rule == "EDD":
        key = lambda item: (item["due_time"], item["release_time"], item["order_id"])
    else:
        key = lambda item: (item["processing_time"], item["due_time"], item["order_id"])
    return [item["order_id"] for item in sorted(order_features.values(), key=key)]


def _collect_order_features(request: LocalRescheduleRequest) -> Dict[int, Dict[str, Any]]:
    features: Dict[int, Dict[str, Any]] = {}
    if request.insertTasks:
        features[request.insertOrder.orderId] = {
            "order_id": request.insertOrder.orderId,
            "release_time": request.insertOrder.releaseTime,
            "due_time": request.insertOrder.dueTime,
            "processing_time": sum(task.standardDuration for task in request.insertTasks),
        }

    adjustable_by_order = defaultdict(list)
    for task in request.adjustableTasks:
        adjustable_by_order[task.orderId].append(task)

    for order_id, tasks in adjustable_by_order.items():
        release_times = [
            parse_time(task.originalStartTime or task.plannedStartTime)
            for task in tasks
        ]
        due_times = [
            parse_time(task.dueTime)
            for task in tasks
            if task.dueTime is not None
        ]
        features[order_id] = {
            "order_id": order_id,
            "release_time": min(release_times),
            "due_time": min(due_times) if due_times else datetime.max,
            "processing_time": sum(task.duration for task in tasks),
        }
    return features
