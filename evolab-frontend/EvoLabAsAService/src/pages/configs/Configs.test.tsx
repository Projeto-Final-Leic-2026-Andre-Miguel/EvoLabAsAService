// @vitest-environment jsdom
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import Configs from './Configs';

const mocks = vi.hoisted(() => ({
  update: vi.fn(),
  validateCredential: vi.fn().mockResolvedValue(true),
}));

vi.mock('./apiConfigs', () => ({
  apiConfigs: {
    getAllMyConfigs: vi.fn().mockResolvedValue({
      type: 'Success',
      data: [{
        configId: 7,
        projectId: null,
        userId: 1,
        llmCredentialsId: 11,
        modelName: 'gpt-4o-mini',
        maxIter: 10,
        checkPointInterval: 5,
        additionalParams: { 'llm.api_base': 'https://api.openai.com/v1' },
        createdAt: '2026-01-01T00:00:00Z',
      }],
    }),
    create: vi.fn(),
    update: mocks.update,
    delete: vi.fn(),
  },
}));

vi.mock('../credentials/apiCredentials', () => ({
  apiCredentials: {
    getAll: vi.fn().mockResolvedValue({
      type: 'Success',
      data: [{ id: 11, userId: 1, llm: 'OPENAI', createdAt: '2026-01-01T00:00:00Z' }],
    }),
    getLocalModel: vi.fn(),
  },
}));

vi.mock('../projects/apiProjects', () => ({
  apiProjects: {
    getAll: vi.fn().mockResolvedValue({ type: 'Success', data: [] }),
  },
}));

vi.mock('../../contexts/ValidCredentialsContext', () => ({
  useValidCredentials: () => ({
    validCredentialsMap: { 11: true },
    validateCredential: mocks.validateCredential,
  }),
}));

vi.mock('../../hooks/useToast', () => ({ useToast: () => ({ showSuccess: vi.fn() }) }));
vi.mock('../../hooks/usePageTitle', () => ({ usePageTitle: vi.fn() }));

afterEach(() => {
  cleanup();
  mocks.update.mockReset();
});

describe('Configs', () => {
  it('uses a stable title and descriptive metadata without exposing the config id', async () => {
    render(<MemoryRouter><Configs /></MemoryRouter>);

    expect(await screen.findByRole('heading', { name: 'Config' })).toBeTruthy();
    expect(screen.getByText('OpenAI · gpt-4o-mini')).toBeTruthy();
    expect(screen.getByText('10 iterations · checkpoint every 5 iterations')).toBeTruthy();
    expect(screen.queryByText(/Config #7/)).toBeNull();
  });

  it('does not send an update when checkpoint interval exceeds max iterations', async () => {
    render(<MemoryRouter><Configs /></MemoryRouter>);
    fireEvent.click(await screen.findByRole('button', { name: 'Update' }));

    const numericInputs = screen.getAllByRole('spinbutton');
    fireEvent.change(numericInputs[0], { target: { value: '4' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Configuration' }));

    expect(await screen.findByText(
      'Checkpoint interval cannot be greater than the maximum number of iterations.',
    )).toBeTruthy();
    await waitFor(() => expect(mocks.update).not.toHaveBeenCalled());
  });
});
