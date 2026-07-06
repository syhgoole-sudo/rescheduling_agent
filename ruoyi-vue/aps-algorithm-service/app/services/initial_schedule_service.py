import time

from app.algorithms.rule_scheduler import schedule_by_rule
from app.schemas.schedule_schema import InitialScheduleRequest, InitialScheduleResponse


ALGORITHM_NAME = "INITIAL_RULE_SCHEDULER"


def generate_initial_schedule(request: InitialScheduleRequest) -> InitialScheduleResponse:
    started = time.perf_counter()
    tasks, kpi, warnings = schedule_by_rule(request)
    runtime_ms = int((time.perf_counter() - started) * 1000)

    success = len(tasks) > 0
    message = "Initial schedule generated." if success else "No schedulable tasks generated."
    return InitialScheduleResponse(
        requestId=request.requestId,
        success=success,
        message=message,
        algorithmName=ALGORITHM_NAME,
        taskSchedules=tasks,
        kpi=kpi,
        warnings=warnings,
        runtimeMs=runtime_ms,
    )
