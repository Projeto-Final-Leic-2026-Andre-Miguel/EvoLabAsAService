import math
import os
import random
import statistics
import time
import traceback


# ============================================================
# Helpers
# ============================================================

def random_point_in_bounds(bounds):
    return [random.uniform(low, high) for low, high in bounds]


def clean_candidate_code(candidate_code):
    code = str(candidate_code)

    code = code.replace("\r\n", "\n").replace("\r", "\n").strip()

    # Remove markdown fences
    if code.startswith("```"):
        lines = code.split("\n")

        if lines and lines[0].startswith("```"):
            lines = lines[1:]

        if lines and lines[-1].startswith("```"):
            lines = lines[:-1]

        code = "\n".join(lines)

    return code.strip()


# ============================================================
# Benchmark functions
# ============================================================

def sphere(x):
    return sum(v ** 2 for v in x)


def rastrigin(x):
    return sum(
        v ** 2 - 10 * math.cos(2 * math.pi * v) + 10
        for v in x
    )


def rosenbrock(x):
    total = 0.0

    for i in range(len(x) - 1):
        total += (
            100 * (x[i + 1] - x[i] ** 2) ** 2
            + (1 - x[i]) ** 2
        )

    return total


# ============================================================
# Main evaluator
# ============================================================

def evaluate(candidate_code):
    try:

        # ====================================================
        # Read file if OpenEvolve passed a filepath
        # ====================================================

        if (
            isinstance(candidate_code, str)
            and os.path.isfile(candidate_code)
        ):
            with open(candidate_code, "r", encoding="utf-8") as f:
                candidate_code = f.read()

        # ====================================================
        # Clean code
        # ====================================================

        code = clean_candidate_code(candidate_code)

        # ====================================================
        # Execution namespace
        # ====================================================

        ns = {
            "random": random,
            "math": math,
            "time": time,
            "statistics": statistics,
            "random_point_in_bounds": random_point_in_bounds,
        }

        # ====================================================
        # Execute generated code
        # ====================================================

        exec(code, ns)

        minimize_function = ns.get("minimize_function")

        if not callable(minimize_function):
            return {
                "combined_score": 0.0,
                "error": "minimize_function not found"
            }

        # ====================================================
        # Optimization tasks
        # ====================================================

        tasks = [
            {
                "name": "sphere",
                "func": sphere,
                "bounds": [(-100, 100)] * 10,
            },
            {
                "name": "rastrigin",
                "func": rastrigin,
                "bounds": [(-5.12, 5.12)] * 10,
            },
            {
                "name": "rosenbrock",
                "func": rosenbrock,
                "bounds": [(-5, 5)] * 10,
            },
        ]

        all_scores = []
        all_times = []
        all_stds = []

        # ====================================================
        # Evaluate all benchmark tasks
        # ====================================================

        for task in tasks:

            results = []
            times = []

            for seed in range(5):

                random.seed(seed)

                start = time.perf_counter()

                best_x, val = minimize_function(
                    task["func"],
                    task["bounds"],
                    max_evals=80
                )

                duration = time.perf_counter() - start

                # =============================================
                # Validation
                # =============================================

                if best_x is None:
                    return {
                        "combined_score": 0.0,
                        "error": f"{task['name']} returned best_x=None"
                    }

                if not isinstance(val, (int, float)):
                    return {
                        "combined_score": 0.0,
                        "error": f"{task['name']} returned non numeric value"
                    }

                if math.isnan(val) or math.isinf(val):
                    return {
                        "combined_score": 0.0,
                        "error": f"{task['name']} produced invalid value"
                    }

                results.append(float(val))
                times.append(duration)

            avg_val = statistics.mean(results)

            std_val = (
                statistics.stdev(results)
                if len(results) > 1
                else 0.0
            )

            avg_time = statistics.mean(times)

            # =================================================
            # Scoring
            # =================================================

            quality_score = 1.0 / (1.0 + avg_val)

            stability_score = 1.0 / (1.0 + std_val)

            time_score = 1.0 / (1.0 + avg_time * 5.0)

            task_score = (
                0.70 * quality_score +
                0.20 * stability_score +
                0.10 * time_score
            )

            all_scores.append(task_score)
            all_times.append(avg_time)
            all_stds.append(std_val)

        # ====================================================
        # Final score
        # ====================================================

        combined_score = statistics.mean(all_scores)

        return {
            "combined_score": float(combined_score * 100.0),
            "task_scores": [float(s * 100.0) for s in all_scores],
            "avg_time": float(statistics.mean(all_times)),
            "avg_std": float(statistics.mean(all_stds)),
        }

    except Exception as e:
        return {
            "combined_score": 0.0,
            "error": str(e),
            "traceback": traceback.format_exc()
        }