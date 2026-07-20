import random
from datetime import datetime, timedelta
from typing import Dict, List, Tuple

from app.algorithms.rule_scheduler import schedule_by_rule
from app.schemas.schedule_schema import (
    EquipmentDTO,
    InitialScheduleRequest,
    InsertTaskDTO,
    LocalRescheduleRequest,
    OrderDTO,
    RouteOperationDTO,
    TaskScheduleDTO,
)


DATETIME_FORMAT = "%Y-%m-%d %H:%M:%S"


def generate_local_reschedule_instance(
    order_count: int,
    seed: int,
    ga_population_size: int = 30,
    ga_generations: int = 50,
) -> LocalRescheduleRequest:
    if order_count < 2:
        raise ValueError("order_count must be at least 2")

    rng = random.Random(seed)
    schedule_start = datetime(2026, 1, 1, 8, 0, 0)
    routes = _build_routes(rng)
    equipments = _build_equipments()
    orders = _build_normal_orders(order_count, schedule_start, routes, rng)
    initial_request = InitialScheduleRequest(
        requestId=f"INITIAL-{order_count}-{seed}",
        scheduleStartTime=schedule_start,
        orders=orders,
        routeOperations=[operation for route in routes.values() for operation in route],
        equipments=equipments,
    )
    initial_tasks, initial_kpi, warnings = schedule_by_rule(initial_request)
    if warnings or len(initial_tasks) == 0:
        raise RuntimeError(f"Cannot generate initial schedule: {warnings}")

    insert_product_id = 2
    insert_route = routes[insert_product_id]
    insert_release = schedule_start + timedelta(
        minutes=max(30, int(initial_kpi["makespan"] * 0.30))
    )
    insert_duration = sum(operation.standardDuration for operation in insert_route)
    insert_order = OrderDTO(
        orderId=order_count + 10_000,
        orderCode=f"HOT-{order_count}-{seed}",
        orderType="INSERT",
        productId=insert_product_id,
        productCode=f"P-{insert_product_id}",
        quantity=1,
        priorityLevel=1,
        releaseTime=insert_release,
        dueTime=insert_release + timedelta(minutes=int(insert_duration * 1.5)),
        orderStatus="NEW",
    )

    due_by_order = {order.orderId: order.dueTime for order in orders}
    source_tasks = _attach_source_information(initial_tasks, due_by_order)
    affected_groups = {operation.equipmentGroupId for operation in insert_route}
    frozen_tasks, adjustable_tasks = _partition_tasks(
        source_tasks,
        insert_release,
        affected_groups,
    )

    return LocalRescheduleRequest(
        requestId=f"BENCHMARK-{order_count}-{seed}",
        eventId=seed,
        sourcePlanId=seed,
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
        equipments=equipments,
        routeOperations=insert_route,
        strategyConfig={
            "algorithmType": "GA",
            "randomSeed": seed,
            "populationSize": ga_population_size,
            "generations": ga_generations,
            "crossoverRate": 0.8,
            "mutationRate": 0.15,
        },
    )


def _build_routes(rng: random.Random) -> Dict[int, List[RouteOperationDTO]]:
    route_groups = {
        1: [1, 2, 3],
        2: [1, 2, 1, 3],
        3: [2, 3, 1],
    }
    process_codes = {1: "PHOTO", 2: "ETCH", 3: "INSPECT"}
    routes: Dict[int, List[RouteOperationDTO]] = {}
    for product_id, groups in route_groups.items():
        operations = []
        for process_seq, group_id in enumerate(groups, start=1):
            process_code = process_codes[group_id]
            operations.append(RouteOperationDTO(
                routeOperationId=product_id * 100 + process_seq,
                productId=product_id,
                productCode=f"P-{product_id}",
                processSeq=process_seq,
                processCode=f"{process_code}-{process_seq}",
                processName=process_code.title(),
                equipmentGroupId=group_id,
                standardDuration=rng.randint(20, 55),
            ))
        routes[product_id] = operations
    return routes


def _build_equipments() -> List[EquipmentDTO]:
    group_codes = {1: "PHOTO", 2: "ETCH", 3: "INSPECT"}
    equipments = []
    for group_id, group_code in group_codes.items():
        for index in range(1, 3):
            equipments.append(EquipmentDTO(
                equipmentId=group_id * 100 + index,
                equipmentCode=f"{group_code}-{index:02d}",
                equipmentName=f"{group_code.title()} Tool {index:02d}",
                equipmentGroupId=group_id,
                equipmentGroupCode=group_code,
                status="0",
            ))
    return equipments


def _build_normal_orders(
    order_count: int,
    schedule_start: datetime,
    routes: Dict[int, List[RouteOperationDTO]],
    rng: random.Random,
) -> List[OrderDTO]:
    orders = []
    for order_id in range(1, order_count + 1):
        product_id = rng.choice(sorted(routes))
        processing_time = sum(item.standardDuration for item in routes[product_id])
        release_time = schedule_start + timedelta(minutes=rng.randint(0, 180))
        due_slack = int(processing_time * rng.uniform(1.4, 2.8))
        orders.append(OrderDTO(
            orderId=order_id,
            orderCode=f"NORMAL-{order_id:04d}",
            orderType="NORMAL",
            productId=product_id,
            productCode=f"P-{product_id}",
            quantity=1,
            priorityLevel=rng.randint(2, 5),
            releaseTime=release_time,
            dueTime=release_time + timedelta(minutes=due_slack),
            orderStatus="NEW",
        ))
    return orders


def _attach_source_information(
    tasks: List[TaskScheduleDTO],
    due_by_order: Dict[int, datetime],
) -> List[TaskScheduleDTO]:
    source_tasks = []
    for source_task_id, task in enumerate(tasks, start=1):
        source_tasks.append(task.model_copy(update={
            "sourceTaskId": source_task_id,
            "originalStartTime": task.plannedStartTime,
            "originalEndTime": task.plannedEndTime,
            "originalEquipmentId": task.equipmentId,
            "dueTime": due_by_order[task.orderId].strftime(DATETIME_FORMAT),
        }))
    return source_tasks


def _partition_tasks(
    tasks: List[TaskScheduleDTO],
    insert_release: datetime,
    affected_groups: set,
) -> Tuple[List[TaskScheduleDTO], List[TaskScheduleDTO]]:
    frozen_tasks = []
    adjustable_tasks = []
    for task in tasks:
        planned_start = datetime.strptime(task.plannedStartTime, DATETIME_FORMAT)
        if planned_start >= insert_release and task.equipmentGroupId in affected_groups:
            adjustable_tasks.append(task.model_copy(update={"isFrozen": "N"}))
        else:
            frozen_tasks.append(task.model_copy(update={"isFrozen": "Y"}))
    if not adjustable_tasks:
        raise RuntimeError("Generated instance contains no adjustable task")
    return frozen_tasks, adjustable_tasks
