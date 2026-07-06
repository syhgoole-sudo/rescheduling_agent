import time

from app.algorithms.ga_scheduler import local_reschedule_by_ga
from app.algorithms.rule_scheduler import local_reschedule_by_rule
from app.schemas.schedule_schema import LocalRescheduleRequest, LocalRescheduleResponse


def generate_local_reschedule(request: LocalRescheduleRequest) -> LocalRescheduleResponse:
    start = time.perf_counter()
    algorithm_type = (request.strategyConfig or {}).get("algorithmType", "RULE").upper()
    algorithm_name = "GA_LOCAL_RESCHEDULE" if algorithm_type == "GA" else "RULE_LOCAL_RESCHEDULE"
    try:
        if algorithm_type == "GA":
            tasks, kpi, warnings = local_reschedule_by_ga(request)
        else:
            tasks, kpi, warnings = local_reschedule_by_rule(request)
        return LocalRescheduleResponse(
            requestId=request.requestId,
            success=True,
            message="OK",
            algorithmName=algorithm_name,
            strategyType=request.strategyConfig.get("strategyType", "LOCAL_RESCHEDULE_WITH_INSERT_PRIORITY"),
            taskSchedules=tasks,
            kpi=kpi,
            warnings=warnings,
            runtimeMs=int((time.perf_counter() - start) * 1000),
        )
    except Exception as exc:
        return LocalRescheduleResponse(
            requestId=request.requestId,
            success=False,
            message=str(exc),
            algorithmName=algorithm_name,
            strategyType=request.strategyConfig.get("strategyType", "LOCAL_RESCHEDULE_WITH_INSERT_PRIORITY"),
            taskSchedules=[],
            kpi={},
            warnings=[],
            runtimeMs=int((time.perf_counter() - start) * 1000),
        )
