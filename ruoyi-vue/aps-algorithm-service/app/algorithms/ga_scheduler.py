import random
from typing import Any, Dict, List, Tuple

from app.algorithms.dispatching_scheduler import build_order_sequence
from app.algorithms.schedule_decoder import build_order_sequence_rule, decode_local_reschedule
from app.schemas.schedule_schema import LocalRescheduleRequest, TaskScheduleDTO


DEFAULT_POPULATION_SIZE = 30
DEFAULT_GENERATIONS = 50
DEFAULT_CROSSOVER_RATE = 0.8
DEFAULT_MUTATION_RATE = 0.15
TOURNAMENT_SIZE = 3


def local_reschedule_by_ga(request: LocalRescheduleRequest) -> Tuple[List[TaskScheduleDTO], Dict[str, Any], List[str]]:
    config = request.strategyConfig or {}
    random_seed = _resolve_random_seed(config)
    rng = random.Random(random_seed)
    chromosome_seed = build_order_sequence_rule(request)
    if len(chromosome_seed) <= 1:
        tasks, kpi, warnings = decode_local_reschedule(request, chromosome_seed)
        kpi["randomSeed"] = random_seed
        return tasks, kpi, warnings

    population_size = int(config.get("populationSize", DEFAULT_POPULATION_SIZE))
    generations = int(config.get("generations", DEFAULT_GENERATIONS))
    crossover_rate = float(config.get("crossoverRate", DEFAULT_CROSSOVER_RATE))
    mutation_rate = float(config.get("mutationRate", DEFAULT_MUTATION_RATE))

    heuristic_seeds = [
        build_order_sequence(request, rule)
        for rule in ("FIFO", "EDD", "SPT")
    ]
    population = _initial_population(chromosome_seed, heuristic_seeds, population_size, rng)
    fitness_cache: Dict[Tuple[int, ...], Tuple[float, float]] = {}

    def evaluate(chromosome: List[int]) -> Tuple[float, float]:
        key = tuple(chromosome)
        if key not in fitness_cache:
            fitness_cache[key] = _fitness(request, chromosome)
        return fitness_cache[key]

    best_chromosome = population[0][:]
    best_score = evaluate(best_chromosome)
    convergence_history = []

    for generation in range(generations):
        scored = sorted(((evaluate(item), item) for item in population), key=lambda item: item[0])
        if scored[0][0] < best_score:
            best_score = scored[0][0]
            best_chromosome = scored[0][1][:]
        convergence_history.append(_convergence_point(generation, best_score))

        next_population = [best_chromosome[:]]
        while len(next_population) < population_size:
            parent_a = _tournament_select(scored, rng)
            parent_b = _tournament_select(scored, rng)
            if rng.random() < crossover_rate:
                child_a, child_b = _order_crossover(parent_a, parent_b, rng)
            else:
                child_a, child_b = parent_a[:], parent_b[:]
            if rng.random() < mutation_rate:
                _swap_mutation(child_a, rng)
            if rng.random() < mutation_rate:
                _swap_mutation(child_b, rng)
            next_population.append(child_a)
            if len(next_population) < population_size:
                next_population.append(child_b)
        population = next_population

    final_scored = sorted(((evaluate(item), item) for item in population), key=lambda item: item[0])
    if final_scored[0][0] < best_score:
        best_score = final_scored[0][0]
        best_chromosome = final_scored[0][1][:]
    convergence_history.append(_convergence_point(generations, best_score))

    tasks, kpi, warnings = decode_local_reschedule(request, best_chromosome)
    kpi["randomSeed"] = random_seed
    kpi["evaluatedChromosomeCount"] = len(fitness_cache)
    kpi["convergenceHistory"] = convergence_history
    warnings.append(
        "GA best fitness: "
        f"hot-lot tardiness={round(best_score[0], 4)}, "
        f"secondary objective={round(best_score[1], 4)}."
    )
    return tasks, kpi, warnings


def _convergence_point(generation: int, score: Tuple[float, float]) -> Dict[str, Any]:
    return {
        "generation": generation,
        "hotLotTardiness": score[0],
        "secondaryObjective": score[1],
    }


def _resolve_random_seed(config: Dict[str, Any]) -> int:
    value = config.get("randomSeed", 42)
    return 42 if value is None or value == "" else int(value)


def _initial_population(
    seed: List[int],
    heuristic_seeds: List[List[int]],
    population_size: int,
    rng: random.Random,
) -> List[List[int]]:
    population = []
    expected_genes = set(seed)
    for chromosome in [seed, *heuristic_seeds]:
        if set(chromosome) != expected_genes or len(chromosome) != len(seed):
            continue
        if chromosome not in population:
            population.append(chromosome[:])
        if len(population) >= population_size:
            return population
    while len(population) < population_size:
        chromosome = seed[:]
        rng.shuffle(chromosome)
        population.append(chromosome)
    return population


def _fitness(request: LocalRescheduleRequest, chromosome: List[int]) -> Tuple[float, float]:
    _, kpi, _ = decode_local_reschedule(request, chromosome)
    true_delay = kpi.get("trueDelay", {})
    stability_delay = kpi.get("stabilityDelay", {})
    insert_delay = float(
        true_delay.get("insertOrderTrueDelayMinutes", kpi.get("insertOrderTrueDelayMinutes", 0))
    )
    secondary_objective = (
        5 * float(true_delay.get("trueTotalDelayMinutes", kpi.get("totalDelayMinutes", 0)))
        + 3 * float(true_delay.get("trueMaxDelayMinutes", kpi.get("maxDelayMinutes", 0)))
        + 2 * float(stability_delay.get("stabilityTotalDelayMinutes", 0))
        + 1 * float(kpi.get("makespan", 0))
        + 2 * float(kpi.get("changedTaskCount", 0))
    )
    return insert_delay, secondary_objective


def _tournament_select(
    scored_population: List[Tuple[Tuple[float, float], List[int]]],
    rng: random.Random,
) -> List[int]:
    contenders = rng.sample(scored_population, min(TOURNAMENT_SIZE, len(scored_population)))
    contenders.sort(key=lambda item: item[0])
    return contenders[0][1][:]


def _order_crossover(parent_a: List[int], parent_b: List[int], rng: random.Random) -> Tuple[List[int], List[int]]:
    size = len(parent_a)
    start, end = sorted(rng.sample(range(size), 2))
    return (
        _make_ox_child(parent_a, parent_b, start, end),
        _make_ox_child(parent_b, parent_a, start, end),
    )


def _make_ox_child(primary: List[int], secondary: List[int], start: int, end: int) -> List[int]:
    child = [None] * len(primary)
    child[start:end + 1] = primary[start:end + 1]
    fill_values = [gene for gene in secondary if gene not in child]
    fill_index = 0
    for index in range(len(child)):
        if child[index] is None:
            child[index] = fill_values[fill_index]
            fill_index += 1
    return child


def _swap_mutation(chromosome: List[int], rng: random.Random) -> None:
    if len(chromosome) < 2:
        return
    a, b = rng.sample(range(len(chromosome)), 2)
    chromosome[a], chromosome[b] = chromosome[b], chromosome[a]
