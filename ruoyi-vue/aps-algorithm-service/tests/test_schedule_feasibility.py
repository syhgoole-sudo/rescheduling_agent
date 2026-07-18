from collections import defaultdict
from datetime import datetime


DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def _parse_time(value: str) -> datetime:
    return datetime.strptime(value, DATETIME_FORMAT)


def test_tasks_on_same_equipment_do_not_overlap(schedule_result):
    _, _, tasks, _, _ = schedule_result
    tasks_by_equipment = defaultdict(list)
    for task in tasks:
        tasks_by_equipment[task.equipmentId].append(task)

    for equipment_tasks in tasks_by_equipment.values():
        ordered_tasks = sorted(equipment_tasks, key=lambda item: _parse_time(item.plannedStartTime))
        for previous, current in zip(ordered_tasks, ordered_tasks[1:]):
            assert _parse_time(previous.plannedEndTime) <= _parse_time(current.plannedStartTime)


def test_order_operations_follow_process_sequence(schedule_result):
    _, _, tasks, _, _ = schedule_result
    tasks_by_order = defaultdict(list)
    for task in tasks:
        tasks_by_order[task.orderId].append(task)

    for order_tasks in tasks_by_order.values():
        ordered_tasks = sorted(order_tasks, key=lambda item: item.processSeq)
        for previous, current in zip(ordered_tasks, ordered_tasks[1:]):
            assert previous.processSeq < current.processSeq
            assert _parse_time(previous.plannedEndTime) <= _parse_time(current.plannedStartTime)


def test_insert_order_contains_all_route_operations(schedule_result):
    _, local_request, tasks, _, _ = schedule_result
    insert_order_id = local_request.insertOrder.orderId
    insert_tasks = [task for task in tasks if task.orderId == insert_order_id]
    route_operation_count = sum(
        operation.productId == local_request.insertOrder.productId
        for operation in local_request.routeOperations
    )

    assert insert_tasks
    assert len(insert_tasks) == route_operation_count


def test_frozen_tasks_keep_original_times(schedule_result):
    _, local_request, tasks, _, _ = schedule_result
    result_by_source_id = {
        task.sourceTaskId: task
        for task in tasks
        if task.sourceTaskId is not None
    }

    for source_task in local_request.frozenTasks:
        result_task = result_by_source_id[source_task.sourceTaskId]
        assert result_task.isFrozen == "Y"
        assert result_task.plannedStartTime == source_task.plannedStartTime
        assert result_task.plannedEndTime == source_task.plannedEndTime
