from experiments.run_benchmark import run_benchmark


def test_benchmark_returns_one_feasible_row_per_algorithm():
    rows = run_benchmark(
        order_counts=[5],
        replications=1,
        base_seed=7,
        algorithms=["FIFO", "RULE", "GA"],
        population_size=6,
        generations=3,
    )

    assert [row["algorithm"] for row in rows] == ["FIFO", "RULE", "GA"]
    assert all(row["feasible"] for row in rows)
    assert all(row["scheduledTaskCount"] > 0 for row in rows)
