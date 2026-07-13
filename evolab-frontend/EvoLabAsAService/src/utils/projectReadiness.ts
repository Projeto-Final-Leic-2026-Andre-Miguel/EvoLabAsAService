type ProjectStartRequirements = {
    configId: number | null;
    initialProgram: string | null;
    evaluatorCode: string | null;
};

type ProjectStatus = 'CREATED' | 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export type ProjectActionPermissions = {
    details: boolean;
    start: boolean;
    restart: boolean;
    update: boolean;
    delete: boolean;
};

export function getMissingProjectRequirements(project: ProjectStartRequirements): string[] {
    const missing: string[] = [];

    if (project.configId == null) missing.push("configuration");
    if (!project.initialProgram?.trim()) missing.push("initial program");
    if (!project.evaluatorCode?.trim()) missing.push("evaluator code");

    return missing;
}

export function getProjectActionPermissions(status: ProjectStatus): ProjectActionPermissions {
    const isActive = status === 'QUEUED' || status === 'RUNNING';
    const isTerminal = status === 'COMPLETED' || status === 'FAILED';

    return {
        details: true,
        start: status === 'CREATED',
        restart: isTerminal,
        update: !isActive,
        delete: !isActive,
    };
}
