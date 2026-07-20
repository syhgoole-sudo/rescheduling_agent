from app.algorithms.ga_scheduler import local_reschedule_by_ga
from app.algorithms.dispatching_scheduler import local_reschedule_by_dispatching_rule


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
    assert len(kpi["convergenceHistory"]) == 11
    assert kpi["evaluatedChromosomeCount"] > 0


def test_ga_seeded_population_is_not_worse_than_fifo_for_hierarchical_objective(local_request_factory):
    request = local_request_factory(42)
    _, ga_kpi, _ = local_reschedule_by_ga(request)
    _, fifo_kpi, _ = local_reschedule_by_dispatching_rule(request, "FIFO")

    assert _hierarchical_objective(ga_kpi) <= _hierarchical_objective(fifo_kpi)


def _hierarchical_objective(kpi):
    true_delay = kpi["trueDelay"]
    stability_delay = kpi["stabilityDelay"]
    return (
        true_delay["insertOrderTrueDelayMinutes"],
        5 * true_delay["trueTotalDelayMinutes"]
        + 3 * true_delay["trueMaxDelayMinutes"]
        + 2 * stability_delay["stabilityTotalDelayMinutes"]
        + kpi["makespan"]
        + 2 * kpi["changedTaskCount"],
    )
