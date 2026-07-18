from app.algorithms.ga_scheduler import local_reschedule_by_ga


def _task_signature(tasks):
    return [
        (
            task.orderId,
            task.processSeq,
            task.equipmentId,
            task.plannedStartTime,
            task.plannedEndTime,
        )
        for task in tasks
    ]


def _key_kpi(kpi):
    return {
        "makespan": kpi["makespan"],
        "insertOrderFinishTime": kpi["insertOrderFinishTime"],
        "trueDelay": kpi["trueDelay"],
        "stabilityDelay": kpi["stabilityDelay"],
        "changedTaskCount": kpi["changedTaskCount"],
    }


def test_same_ga_seed_produces_same_schedule_and_kpi(local_request_factory):
    first_tasks, first_kpi, _ = local_reschedule_by_ga(local_request_factory(42))
    second_tasks, second_kpi, _ = local_reschedule_by_ga(local_request_factory(42))

    assert _task_signature(first_tasks) == _task_signature(second_tasks)
    assert _key_kpi(first_kpi) == _key_kpi(second_kpi)
    assert first_kpi["randomSeed"] == second_kpi["randomSeed"] == 42


def test_different_ga_seed_completes_without_error(local_request_factory):
    tasks, kpi, _ = local_reschedule_by_ga(local_request_factory(43))

    assert tasks
    assert kpi["scheduledTaskCount"] == len(tasks)
    assert kpi["randomSeed"] == 43
