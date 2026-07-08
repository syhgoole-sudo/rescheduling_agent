import random
from typing import Any, Dict, List, Tuple

from app.algorithms.schedule_decoder import build_order_sequence_rule, decode_local_reschedule
from app.schemas.schedule_schema import LocalRescheduleRequest, TaskScheduleDTO


DEFAULT_POPULATION_SIZE = 30
DEFAULT_GENERATIONS = 50
DEFAULT_CROSSOVER_RATE = 0.8
DEFAULT_MUTATION_RATE = 0.15
TOURNAMENT_SIZE = 3


def local_reschedule_by_ga(request: LocalRescheduleRequest) -> Tuple[List[TaskScheduleDTO], Dict[str, Any], List[str]]:
    chromosome_seed = build_order_sequence_rule(request)
    if len(chromosome_seed) <= 1:
        return decode_local_reschedule(request, chromosome_seed)

    config = request.strategyConfig or {}
    population_size = int(config.get("populationSize", DEFAULT_POPULATION_SIZE))
    generations = int(config.get("generations", DEFAULT_GENERATIONS))
    crossover_rate = float(config.get("crossoverRate", DEFAULT_CROSSOVER_RATE))
    mutation_rate = float(config.get("mutationRate", DEFAULT_MUTATION_RATE))

    population = _initial_population(chromosome_seed, population_size)
    best_chromosome = population[0][:]
    best_score = _fitness(request, best_chromosome)

    for _ in range(generations):
        scored = sorted(((_fitness(request, item), item) for item in population), key=lambda item: item[0])
        if scored[0][0] < best_score:
            best_score = scored[0][0]
            best_chromosome = scored[0][1][:]

        next_population = [best_chromosome[:]]
        while len(next_population) < population_size:
            parent_a = _tournament_select(scored)
            parent_b = _tournament_select(scored)
            if random.random() < crossover_rate:
                child_a, child_b = _order_crossover(parent_a, parent_b)
            else:
                child_a, child_b = parent_a[:], parent_b[:]
            if random.random() < mutation_rate:
                _swap_mutation(child_a)
            if random.random() < mutation_rate:
                _swap_mutation(child_b)
            next_population.append(child_a)
            if len(next_population) < population_size:
                next_population.append(child_b)
        population = next_population

    tasks, kpi, warnings = decode_local_reschedule(request, best_chromosome)
    warnings.append(f"GA best fitness: {round(best_score, 4)}.")
    return tasks, kpi, warnings


def _initial_population(seed: List[int], population_size: int) -> List[List[int]]:
    population = [seed[:]]
    while len(population) < population_size:
        chromosome = seed[:]
        random.shuffle(chromosome)
        population.append(chromosome)
    return population


def _fitness(request: LocalRescheduleRequest, chromosome: List[int]) -> float:
    _, kpi, _ = decode_local_reschedule(request, chromosome)
    true_delay = kpi.get("trueDelay", {})
    stability_delay = kpi.get("stabilityDelay", {})
    return (
        10 * float(true_delay.get("insertOrderTrueDelayMinutes", kpi.get("insertOrderTrueDelayMinutes", 0)))
        + 5 * float(true_delay.get("trueTotalDelayMinutes", kpi.get("totalDelayMinutes", 0)))
        + 3 * float(true_delay.get("trueMaxDelayMinutes", kpi.get("maxDelayMinutes", 0)))
        + 2 * float(stability_delay.get("stabilityTotalDelayMinutes", 0))
        + 1 * float(kpi.get("makespan", 0))
        + 2 * float(kpi.get("changedTaskCount", 0))
    )


def _tournament_select(scored_population: List[Tuple[float, List[int]]]) -> List[int]:
    contenders = random.sample(scored_population, min(TOURNAMENT_SIZE, len(scored_population)))
    contenders.sort(key=lambda item: item[0])
    return contenders[0][1][:]


def _order_crossover(parent_a: List[int], parent_b: List[int]) -> Tuple[List[int], List[int]]:
    size = len(parent_a)
    start, end = sorted(random.sample(range(size), 2))
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


def _swap_mutation(chromosome: List[int]) -> None:
    if len(chromosome) < 2:
        return
    a, b = random.sample(range(len(chromosome)), 2)
    chromosome[a], chromosome[b] = chromosome[b], chromosome[a]
