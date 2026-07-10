type ProjectStartRequirements = {
    configId: number | null;
    initialProgram: string | null;
    evaluatorCode: string | null;
};

export function getMissingProjectRequirements(project: ProjectStartRequirements): string[] {
    const missing: string[] = [];

    if (project.configId == null) missing.push("configuration");
    if (!project.initialProgram?.trim()) missing.push("initial program");
    if (!project.evaluatorCode?.trim()) missing.push("evaluator code");

    return missing;
}
