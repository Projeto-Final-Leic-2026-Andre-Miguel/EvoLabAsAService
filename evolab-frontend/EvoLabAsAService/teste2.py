import random
import time
import statistics


def _baseline(arr):
    a = arr[:]
    n = len(a)
    for i in range(n):
        swapped = False
        for j in range(0, n - i - 1):
            if a[j] > a[j + 1]:
                a[j], a[j + 1] = a[j + 1], a[j]
                swapped = True
        if not swapped:
            break
    return a


def evaluate(candidate_code):
    try:
        ns = {}
        exec(candidate_code, ns)
        solve = ns.get("solve")
        if not callable(solve):
            return {"combined_score": 0.0, "correctness": 0.0, "speed": 0.0}

        rng = random.Random(42)
        tests = [
            [],
            [1],
            [2, 1],
            [3, 1, 2],
            [5, 5, 1, 3],
            list(range(20, 0, -1)),
            [0, -1, 5, -3, 2, -1, 0],
        ]
        for _ in range(10):
            n = rng.randint(8, 70)
            tests.append([rng.randint(-500, 500) for _ in range(n)])

        passed = 0
        for t in tests:
            out = solve(t[:])
            if out == sorted(t):
                passed += 1
        correctness = passed / len(tests)

        if correctness < 0.85:
            return {
                "combined_score": float(0.95 * correctness),
                "correctness": float(correctness),
                "speed": 0.0,
            }

        bench_input = [rng.randint(-2000, 2000) for _ in range(220)]

        def bench(fn, rounds=3):
            samples = []
            for _ in range(rounds):
                t0 = time.perf_counter()
                fn(bench_input[:])
                samples.append(time.perf_counter() - t0)
            return statistics.median(samples)

        t_candidate = bench(solve)
        t_baseline = bench(_baseline)
        speed = max(0.0, min(1.0, t_baseline / max(t_candidate * 2.0, 1e-9)))

        combined = 0.90 * correctness + 0.10 * speed
        return {
            "combined_score": float(combined),
            "correctness": float(correctness),
            "speed": float(speed),
        }
    except Exception:
        return {"combined_score": 0.0, "correctness": 0.0, "speed": 0.0}