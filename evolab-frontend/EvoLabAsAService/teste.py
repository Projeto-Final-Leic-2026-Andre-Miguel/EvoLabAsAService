import random

def minimize_function(func, bounds, max_evals=1000):
    best_x, best_val = None, float('inf')

    for _ in range(max_evals):
        x = [random.uniform(low, high) for (low, high) in bounds]
        val = func(x)

        if val < best_val:
            best_x, best_val = x, val

    return best_x, best_val