import argparse
import csv
import json
import sys
import time
from pathlib import Path
from typing import Dict, Iterable, List


PROJECT_ROOT = Path(__file__).resolve().parents[1]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from app.algorithms.dispatching_scheduler import local_reschedule_by_dispatching_rule
from app.algorithms.ga_scheduler import local_reschedule_by_ga
from app.algorithms.rule_scheduler import local_reschedule_by_rule
from experiments.instance_generator import generate_local_reschedule_instance
from experiments.validators import validate_local_schedule


SUPPORTED_ALGORITHMS = ("FIFO", "EDD", "SPT", "RULE", "GA")
RESULT_FIELDS = (
    "instanceId",
    "orderCount",
    "seed",
    "algorithm",
    "makespan",
    "delayOrderCount",
    "totalDelayMinutes",
    "maxDelayMinutes",
    "insertOrderDelayMinutes",
    "changedTaskCount",
    "changedTaskRatio",
    "stabilityTotalDelayMinutes",
    "stabilityMaxDelayMinutes",
    "scheduledTaskCount",
    "runtimeMs",
    "evaluatedChromosomeCount",
    "convergenceHistory",
    "feasible",
    "validationErrors",
    "warnings",
)


def run_benchmark(
    order_counts: Iterable[int],
    replications: int,
    base_seed: int,
    algorithms: Iterable[str],
    population_size: int,
    generations: int,
) -> List[Dict[str, object]]:
    rows = []
    normalized_algorithms = [item.upper() for item in algorithms]
    unsupported = sorted(set(normalized_algorithms) - set(SUPPORTED_ALGORITHMS))
    if unsupported:
        raise ValueError(f"Unsupported algorithms: {', '.join(unsupported)}")

    for order_count in order_counts:
        for replication in range(replications):
            seed = base_seed + order_count * 1_000 + replication
            request = generate_local_reschedule_instance(
                order_count=order_count,
                seed=seed,
                ga_population_size=population_size,
                ga_generations=generations,
            )
            for algorithm in normalized_algorithms:
                started_at = time.perf_counter()
                tasks, kpi, warnings = _run_algorithm(request, algorithm)
                runtime_ms = round((time.perf_counter() - started_at) * 1_000, 3)
                validation_errors = validate_local_schedule(request, tasks)
                true_delay = kpi.get("trueDelay") or {}
                stability_delay = kpi.get("stabilityDelay") or {}
                rows.append({
                    "instanceId": request.requestId,
                    "orderCount": order_count,
                    "seed": seed,
                    "algorithm": algorithm,
                    "makespan": kpi.get("makespan", 0),
                    "delayOrderCount": true_delay.get(
                        "trueDelayOrderCount", kpi.get("delayOrderCount", 0)
                    ),
                    "totalDelayMinutes": true_delay.get(
                        "trueTotalDelayMinutes", kpi.get("totalDelayMinutes", 0)
                    ),
                    "maxDelayMinutes": true_delay.get(
                        "trueMaxDelayMinutes", kpi.get("maxDelayMinutes", 0)
                    ),
                    "insertOrderDelayMinutes": true_delay.get(
                        "insertOrderTrueDelayMinutes", kpi.get("insertOrderDelayMinutes", 0)
                    ),
                    "changedTaskCount": kpi.get("changedTaskCount", 0),
                    "changedTaskRatio": kpi.get("changedTaskRatio", 0),
                    "stabilityTotalDelayMinutes": stability_delay.get(
                        "stabilityTotalDelayMinutes", 0
                    ),
                    "stabilityMaxDelayMinutes": stability_delay.get(
                        "stabilityMaxDelayMinutes", 0
                    ),
                    "scheduledTaskCount": kpi.get("scheduledTaskCount", len(tasks)),
                    "runtimeMs": runtime_ms,
                    "evaluatedChromosomeCount": kpi.get("evaluatedChromosomeCount", ""),
                    "convergenceHistory": json.dumps(
                        kpi.get("convergenceHistory", []), separators=(",", ":")
                    ),
                    "feasible": not validation_errors,
                    "validationErrors": " | ".join(validation_errors),
                    "warnings": " | ".join(warnings),
                })
    return rows


def write_results(rows: List[Dict[str, object]], output_file: Path) -> None:
    output_file.parent.mkdir(parents=True, exist_ok=True)
    with output_file.open("w", newline="", encoding="utf-8-sig") as handle:
        writer = csv.DictWriter(handle, fieldnames=RESULT_FIELDS)
        writer.writeheader()
        writer.writerows(rows)


def _run_algorithm(request, algorithm):
    algorithm_request = request.model_copy(deep=True)
    if algorithm in ("FIFO", "EDD", "SPT"):
        return local_reschedule_by_dispatching_rule(algorithm_request, algorithm)
    if algorithm == "RULE":
        return local_reschedule_by_rule(algorithm_request)
    return local_reschedule_by_ga(algorithm_request)


def _parse_args():
    parser = argparse.ArgumentParser(description="Run reproducible APS scheduling benchmarks.")
    parser.add_argument("--order-counts", nargs="+", type=int, default=[20, 50, 100])
    parser.add_argument("--replications", type=int, default=10)
    parser.add_argument("--base-seed", type=int, default=42)
    parser.add_argument("--algorithms", nargs="+", default=list(SUPPORTED_ALGORITHMS))
    parser.add_argument("--population-size", type=int, default=30)
    parser.add_argument("--generations", type=int, default=50)
    parser.add_argument(
        "--output",
        type=Path,
        default=PROJECT_ROOT / "experiments" / "results" / "benchmark_results.csv",
    )
    return parser.parse_args()


def main():
    args = _parse_args()
    rows = run_benchmark(
        order_counts=args.order_counts,
        replications=args.replications,
        base_seed=args.base_seed,
        algorithms=args.algorithms,
        population_size=args.population_size,
        generations=args.generations,
    )
    write_results(rows, args.output)
    infeasible_count = sum(not row["feasible"] for row in rows)
    print(f"Wrote {len(rows)} rows to {args.output}")
    print(f"Infeasible results: {infeasible_count}")


if __name__ == "__main__":
    main()
