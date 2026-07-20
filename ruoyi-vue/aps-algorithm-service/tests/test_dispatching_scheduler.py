import pytest

from app.algorithms.dispatching_scheduler import (
    build_order_sequence,
    local_reschedule_by_dispatching_rule,
)
from experiments.instance_generator import generate_local_reschedule_instance
from experiments.validators import validate_local_schedule


@pytest.mark.parametrize("rule", ("FIFO", "EDD", "SPT"))
def test_dispatching_rule_produces_feasible_schedule(rule, local_request_factory):
    request = local_request_factory(42)
    tasks, kpi, warnings = local_reschedule_by_dispatching_rule(request, rule)

    assert tasks
    assert not warnings
    assert kpi["dispatchingRule"] == rule
    assert not validate_local_schedule(request, tasks)


def test_spt_orders_shorter_remaining_work_first(local_request_factory):
    request = local_request_factory(42)

    assert build_order_sequence(request, "SPT") == [99, 30, 20]


def test_unknown_dispatching_rule_is_rejected(local_request_factory):
    with pytest.raises(ValueError, match="Unsupported dispatching rule"):
        build_order_sequence(local_request_factory(42), "UNKNOWN")


def test_generated_instance_is_reproducible():
    first = generate_local_reschedule_instance(10, 42, 8, 5)
    second = generate_local_reschedule_instance(10, 42, 8, 5)

    assert first.model_dump() == second.model_dump()
