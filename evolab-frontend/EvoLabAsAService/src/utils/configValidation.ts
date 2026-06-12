export type ConfigValidationErrors = Record<string, string>;

type NumericRule = {
    label: string;
    min?: number;
    minExclusive?: boolean;
    max?: number;
};

const numericRules: Record<string, NumericRule> = {
    "llm.temperature": { label: "Temperature", min: 0 },
    "llm.top_p": { label: "Top P", min: 0, max: 1 },
    "llm.max_tokens": { label: "Max tokens", min: 0, minExclusive: true },
    "llm.timeout": { label: "LLM timeout", min: 0 },
    "llm.retries": { label: "LLM retries", min: 0 },
    "prompt.num_top_programs": { label: "Top program count", min: 0 },
    "prompt.num_diverse_programs": { label: "Diverse program count", min: 0 },
    "database.population_size": { label: "Population size", min: 0, minExclusive: true },
    "database.archive_size": { label: "Archive size", min: 0, minExclusive: true },
    "database.num_islands": { label: "Number of islands", min: 0, minExclusive: true },
    "database.migration_interval": { label: "Migration interval", min: 0, minExclusive: true },
    "database.migration_rate": { label: "Migration rate", min: 0, max: 1 },
    "database.elite_selection_ratio": { label: "Elite selection ratio", min: 0, max: 1 },
    "database.exploration_ratio": { label: "Exploration ratio", min: 0, max: 1 },
    "database.exploitation_ratio": { label: "Exploitation ratio", min: 0, max: 1 },
    "database.feature_bins": { label: "Feature bins", min: 0, minExclusive: true },
    "evaluator.timeout": { label: "Evaluator timeout", min: 0, minExclusive: true },
    "evaluator.max_retries": { label: "Evaluator retries", min: 0 },
    "evaluator.parallel_evaluations": { label: "Parallel evaluations", min: 0 },
    "evaluator.cascade_threshold_1": { label: "Cascade threshold 1", min: 0, max: 1 },
    "evaluator.cascade_threshold_2": { label: "Cascade threshold 2", min: 0, max: 1 },
    "evaluator.cascade_threshold_3": { label: "Cascade threshold 3", min: 0, max: 1 },
};

function validateNumericValue(value: string, rule: NumericRule): string | null {
    if (!value.trim()) return null;

    const parsed = Number(value);
    if (!Number.isFinite(parsed)) return `${rule.label} must be a valid number.`;
    if (rule.minExclusive && parsed <= (rule.min ?? 0)) {
        return `${rule.label} must be greater than ${rule.min ?? 0}.`;
    }
    if (rule.min != null && parsed < rule.min) {
        return `${rule.label} must be at least ${rule.min}.`;
    }
    if (rule.max != null && parsed > rule.max) {
        return `${rule.label} must be between ${rule.min ?? 0} and ${rule.max}.`;
    }

    return null;
}

export function validateConfigValues(
    maxIter: number,
    checkPointInterval: number,
    advancedParams: Record<string, string>,
): ConfigValidationErrors {
    const errors: ConfigValidationErrors = {};

    if (!Number.isFinite(maxIter) || maxIter < 1) {
        errors.maxIter = "Max iterations must be greater than 0.";
    }
    if (!Number.isFinite(checkPointInterval) || checkPointInterval < 1) {
        errors.checkPointInterval = "Checkpoint interval must be greater than 0.";
    }

    for (const [key, rule] of Object.entries(numericRules)) {
        const message = validateNumericValue(advancedParams[key] ?? "", rule);
        if (message) errors[key] = message;
    }

    if (advancedParams["evaluator.cascade_evaluation"] === "true") {
        const thresholdKeys = [
            "evaluator.cascade_threshold_1",
            "evaluator.cascade_threshold_2",
            "evaluator.cascade_threshold_3",
        ];
        const thresholds = thresholdKeys.map(key => Number(advancedParams[key]));
        const allPresent = thresholdKeys.every(key => advancedParams[key]?.trim());

        if (!allPresent) {
            for (const key of thresholdKeys) {
                if (!advancedParams[key]?.trim()) errors[key] = "All cascade thresholds are required.";
            }
        } else if (thresholds[0] >= thresholds[1] || thresholds[1] >= thresholds[2]) {
            errors["evaluator.cascade_threshold_2"] = "Cascade thresholds must be strictly increasing.";
        }
    }

    return errors;
}
