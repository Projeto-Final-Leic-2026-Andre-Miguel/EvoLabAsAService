// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Projects from './Projects';

const mocks = vi.hoisted(() => ({
  start: vi.fn(),
  update: vi.fn(),
  delete: vi.fn(),
}));

vi.mock('../../hooks/usePolling', () => ({
  usePolling: () => ({
    data: {
      projects: [{
        id: 4,
        userId: 1,
        configId: 3,
        name: 'Running project',
        description: 'Active execution',
        initialProgram: 'def solve(): pass',
        evaluatorCode: 'def evaluate(): pass',
        status: 'RUNNING',
        createdAt: '2026-01-01T00:00:00Z',
      }],
      configs: [{ configId: 3, modelName: 'gpt-4o-mini', maxIter: 10 }],
    },
    isLoading: false,
    error: null,
    refresh: vi.fn(),
  }),
}));

vi.mock('./apiProjects', () => ({
  apiProjects: {
    getAll: vi.fn(),
    create: vi.fn(),
    start: mocks.start,
    restart: vi.fn(),
    update: mocks.update,
    delete: mocks.delete,
  },
}));

vi.mock('../configs/apiConfigs', () => ({ apiConfigs: { getAllMyConfigs: vi.fn() } }));
vi.mock('../../hooks/useToast', () => ({ useToast: () => ({ showSuccess: vi.fn() }) }));
vi.mock('../../hooks/usePageTitle', () => ({ usePageTitle: vi.fn() }));

afterEach(() => {
  cleanup();
  vi.clearAllMocks();
});

describe('Projects active execution actions', () => {
  it('keeps Details active and disables all mutating actions', () => {
    render(<MemoryRouter><Projects /></MemoryRouter>);

    expect((screen.getByRole('button', { name: 'Details' }) as HTMLButtonElement).disabled).toBe(false);
    expect((screen.getByRole('button', { name: 'Start' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'Update' }) as HTMLButtonElement).disabled).toBe(true);
    expect((screen.getByRole('button', { name: 'Delete' }) as HTMLButtonElement).disabled).toBe(true);
    expect(screen.queryByRole('button', { name: 'Restart' })).toBeNull();
  });

  it('does not open mutation dialogs or execute handlers from disabled buttons', () => {
    render(<MemoryRouter><Projects /></MemoryRouter>);

    fireEvent.click(screen.getByRole('button', { name: 'Start' }));
    fireEvent.click(screen.getByRole('button', { name: 'Update' }));
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }));

    expect(screen.queryAllByRole('dialog')).toHaveLength(0);
    expect(mocks.start).not.toHaveBeenCalled();
    expect(mocks.update).not.toHaveBeenCalled();
    expect(mocks.delete).not.toHaveBeenCalled();
  });
});
