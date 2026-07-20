import argparse
import json
from pathlib import Path

import matplotlib
import pandas as pd


matplotlib.use("Agg")
import matplotlib.pyplot as plt


METRICS = (
    "makespan",
    "totalDelayMinutes",
    "insertOrderDelayMinutes",
    "stabilityTotalDelayMinutes",
    "changedTaskRatio",
    "runtimeMs",
)
ALGORITHM_ORDER = ["FIFO", "EDD", "SPT", "RULE", "GA"]
METRIC_LABELS = {
    "makespan": "Makespan (min)",
    "totalDelayMinutes": "Total tardiness (min)",
    "insertOrderDelayMinutes": "Hot-lot tardiness (min)",
    "stabilityTotalDelayMinutes": "Stability delay (min)",
    "changedTaskRatio": "Changed-task ratio",
    "runtimeMs": "Runtime (ms)",
}


def analyze_results(input_file: Path, output_dir: Path) -> None:
    frame = pd.read_csv(input_file)
    if frame.empty:
        raise ValueError("Benchmark result file is empty")
    feasible = frame["feasible"].astype(str).str.lower().eq("true")
    if not feasible.all():
        invalid = frame.loc[~feasible, ["instanceId", "algorithm", "validationErrors"]]
        raise ValueError(f"Infeasible benchmark rows found:\n{invalid.to_string(index=False)}")

    output_dir.mkdir(parents=True, exist_ok=True)
    summary = _build_summary(frame)
    summary.to_csv(output_dir / "benchmark_summary.csv", index=False, encoding="utf-8-sig")
    improvement = _build_improvement_table(summary, "RULE")
    improvement.to_csv(output_dir / "improvement_vs_rule.csv", index=False, encoding="utf-8-sig")
    fifo_improvement = _build_improvement_table(summary, "FIFO")
    fifo_improvement.to_csv(
        output_dir / "improvement_vs_fifo.csv", index=False, encoding="utf-8-sig"
    )

    for metric in METRICS:
        _plot_metric_by_scale(summary, metric, output_dir / f"{metric}_by_scale.png")
    _plot_total_delay_boxplot(frame, output_dir / "total_delay_boxplot.png")
    convergence = _build_convergence_table(frame)
    if not convergence.empty:
        convergence.to_csv(output_dir / "ga_convergence.csv", index=False, encoding="utf-8-sig")
        _plot_ga_convergence(convergence, output_dir / "ga_convergence.png")


def _build_summary(frame: pd.DataFrame) -> pd.DataFrame:
    grouped = frame.groupby(["orderCount", "algorithm"], as_index=False)
    aggregations = {}
    for metric in METRICS:
        aggregations[f"{metric}Mean"] = (metric, "mean")
        aggregations[f"{metric}Std"] = (metric, "std")
        aggregations[f"{metric}Median"] = (metric, "median")
        aggregations[f"{metric}Min"] = (metric, "min")
        aggregations[f"{metric}Max"] = (metric, "max")
    return grouped.agg(**aggregations).fillna(0)


def _build_improvement_table(summary: pd.DataFrame, baseline_algorithm: str) -> pd.DataFrame:
    rows = []
    for order_count, scale_frame in summary.groupby("orderCount"):
        baseline_rows = scale_frame[scale_frame["algorithm"] == baseline_algorithm]
        if baseline_rows.empty:
            continue
        baseline_row = baseline_rows.iloc[0]
        for _, row in scale_frame.iterrows():
            result = {
                "orderCount": order_count,
                "baselineAlgorithm": baseline_algorithm,
                "algorithm": row["algorithm"],
            }
            for metric in METRICS:
                baseline = float(baseline_row[f"{metric}Mean"])
                value = float(row[f"{metric}Mean"])
                result[f"{metric}ImprovementPct"] = (
                    round((baseline - value) / baseline * 100, 4) if baseline != 0 else 0
                )
            rows.append(result)
    return pd.DataFrame(rows)


def _plot_metric_by_scale(summary: pd.DataFrame, metric: str, output_file: Path) -> None:
    fig, axis = plt.subplots(figsize=(8, 5))
    algorithms = [item for item in ALGORITHM_ORDER if item in set(summary["algorithm"])]
    for algorithm in algorithms:
        rows = summary[summary["algorithm"] == algorithm].sort_values("orderCount")
        x = rows["orderCount"].to_numpy(dtype=float)
        mean = rows[f"{metric}Mean"].to_numpy(dtype=float)
        std = rows[f"{metric}Std"].to_numpy(dtype=float)
        axis.plot(x, mean, marker="o", linewidth=1.8, label=algorithm)
        axis.fill_between(x, mean - std, mean + std, alpha=0.12)
    axis.set_xlabel("Number of normal orders")
    axis.set_ylabel(METRIC_LABELS[metric])
    axis.grid(alpha=0.25)
    axis.legend()
    fig.tight_layout()
    fig.savefig(output_file, dpi=300, bbox_inches="tight")
    plt.close(fig)


def _plot_total_delay_boxplot(frame: pd.DataFrame, output_file: Path) -> None:
    scales = sorted(frame["orderCount"].unique())
    algorithms = [item for item in ALGORITHM_ORDER if item in set(frame["algorithm"])]
    fig, axes = plt.subplots(1, len(scales), figsize=(5 * len(scales), 5), squeeze=False)
    for axis, order_count in zip(axes[0], scales):
        scale_frame = frame[frame["orderCount"] == order_count]
        values = [
            scale_frame.loc[scale_frame["algorithm"] == algorithm, "totalDelayMinutes"].to_numpy()
            for algorithm in algorithms
        ]
        axis.boxplot(values, tick_labels=algorithms, showmeans=True)
        axis.set_title(f"{order_count} orders")
        axis.set_xlabel("Algorithm")
        axis.set_ylabel("Total tardiness (min)")
        axis.grid(axis="y", alpha=0.25)
    fig.tight_layout()
    fig.savefig(output_file, dpi=300, bbox_inches="tight")
    plt.close(fig)


def _build_convergence_table(frame: pd.DataFrame) -> pd.DataFrame:
    records = []
    ga_rows = frame[frame["algorithm"] == "GA"]
    for _, row in ga_rows.iterrows():
        history_text = row.get("convergenceHistory", "")
        if pd.isna(history_text) or not history_text:
            continue
        for point in json.loads(history_text):
            records.append({
                "orderCount": row["orderCount"],
                "seed": row["seed"],
                "generation": point["generation"],
                "hotLotTardiness": point["hotLotTardiness"],
                "secondaryObjective": point["secondaryObjective"],
            })
    if not records:
        return pd.DataFrame()
    detail = pd.DataFrame(records)
    return detail.groupby(["orderCount", "generation"], as_index=False).agg(
        hotLotTardinessMean=("hotLotTardiness", "mean"),
        hotLotTardinessStd=("hotLotTardiness", "std"),
        secondaryObjectiveMean=("secondaryObjective", "mean"),
        secondaryObjectiveStd=("secondaryObjective", "std"),
    ).fillna(0)


def _plot_ga_convergence(convergence: pd.DataFrame, output_file: Path) -> None:
    scales = sorted(convergence["orderCount"].unique())
    fig, axes = plt.subplots(1, len(scales), figsize=(5 * len(scales), 5), squeeze=False)
    for axis, order_count in zip(axes[0], scales):
        rows = convergence[convergence["orderCount"] == order_count].sort_values("generation")
        axis.plot(
            rows["generation"],
            rows["secondaryObjectiveMean"],
            linewidth=1.8,
            color="#2f6bff",
        )
        axis.set_title(f"{order_count} orders")
        axis.set_xlabel("Generation")
        axis.set_ylabel("Mean secondary objective")
        axis.grid(alpha=0.25)
    fig.tight_layout()
    fig.savefig(output_file, dpi=300, bbox_inches="tight")
    plt.close(fig)


def _parse_args():
    project_root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description="Summarize and plot APS benchmark results.")
    parser.add_argument(
        "--input",
        type=Path,
        default=project_root / "experiments" / "results" / "benchmark_results.csv",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=project_root / "experiments" / "results",
    )
    return parser.parse_args()


def main():
    args = _parse_args()
    analyze_results(args.input, args.output_dir)
    print(f"Wrote summaries and figures to {args.output_dir}")


if __name__ == "__main__":
    main()
